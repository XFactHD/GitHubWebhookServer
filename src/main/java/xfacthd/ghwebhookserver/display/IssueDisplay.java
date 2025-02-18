package xfacthd.ghwebhookserver.display;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.gpio.GPIO;
import xfacthd.ghwebhookserver.util.Either;
import xfacthd.ghwebhookserver.util.Util;
import xfacthd.ghwebhookserver.data.Issue;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class IssueDisplay extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IssueDisplay.class);
    private static final int LINE_LENGTH = 20;
    private static final long DISPLAY_TIMEOUT = Duration.ofHours(1).toMillis();
    private static final long BLANKING_TIME = 2000;
    private static final long DISPLAY_TIME = 60000;
    private static final long JUMP_INTERVAL = 500;
    private static final int JUMP_COUNT = (int) (DISPLAY_TIME / JUMP_INTERVAL);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Queue<Issue> issues = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean displaying = new AtomicBoolean(false);
    private final AtomicBoolean skipCurrent = new AtomicBoolean(false);
    private final String comPort;
    private boolean enabled = false;
    private long lastDisplayTime = 0;

    public IssueDisplay(String comPort) { this.comPort = comPort; }

    @Override
    public void run()
    {
        LOGGER.info("Starting display");
        Display.start(comPort);
        GPIO.registerInputCallback(GPIO.NAME_SKIP_BTN, this::notifySkipIssueButton);
        LOGGER.info("Display ready");

        while (running.get())
        {
            if (waitForDisplayActive()) { break; }

            Either<Issue, Boolean> issue = retrieveIssue();
            if (issue.hasRight() && issue.getRight())
            {
                break;
            }

            if (issue.hasLeft() && driveDisplay(issue.getLeft()))
            {
                break;
            }
        }

        LOGGER.info("Display shutting down");
        Display.clearDisplay();
        LOGGER.info("Display cleared");
        Display.disableDisplay();
        LOGGER.info("Display disabled");
        Display.stop();
        LOGGER.info("Display shut down");
    }

    public void enqueueIssue(Issue issue)
    {
        LOGGER.info("Enqueued new issue");
        issues.add(issue);
    }

    public void shutdown()
    {
        running.set(false);
        interrupt();

        try { join(); }
        catch (InterruptedException ignore) { /* NOOP */ }
    }

    public void notifySkipIssueButton(boolean value)
    {
        if (!value || !displaying.get()) { return; }

        skipCurrent.set(true);
    }



    private boolean waitForDisplayActive()
    {
        while (!(enabled = Display.checkSwitchAndDisable()))
        {
            Util.sleep(10);

            if (!running.get()) { return true; }
        }
        return false;
    }

    private Either<Issue, Boolean> retrieveIssue()
    {
        Issue issue;
        do
        {
            Util.sleep(10);

            enabled = Display.checkSwitchAndDisable();
            if (!enabled || !running.get()) { return Either.right(!running.get()); }

            issue = issues.poll();

            if (issue == null && Display.isActive() && System.currentTimeMillis() - lastDisplayTime > DISPLAY_TIMEOUT)
            {
                Display.disableDisplay();
            }
        }
        while (issue == null);

        return Either.left(issue);
    }

    private boolean driveDisplay(Issue issue)
    {
        LOGGER.info("Issue acquired, driving to display");
        if (Display.enableDisplay(running::get))
        {
            LOGGER.info("Cancelled display cycle while starting display due to shutdown");
            return true;
        }

        lastDisplayTime = System.currentTimeMillis();
        displaying.set(true);

        String numText = String.format("#%d", issue.number());
        Display.setCursorPos(0, LINE_LENGTH - numText.length());
        Display.printText(numText);

        int maxRepoLen = LINE_LENGTH - (numText.length() + 1);

        boolean firstPrint = true;
        int repoOffset = 0;
        int titleOffset = 0;
        for (int i = 0; i < JUMP_COUNT; i++)
        {
            Display.setCursorPos(0, 0);
            repoOffset = printScrollingText(issue.repo(), repoOffset, maxRepoLen, firstPrint);

            Display.setCursorPos(1, 0);
            titleOffset = printScrollingText(issue.title(), titleOffset, LINE_LENGTH, firstPrint);

            firstPrint = false;

            if (shouldSkipIssue())
            {
                LOGGER.info("Skipping issue due to user input");
                displaying.set(false);
                break;
            }
            if (Util.sleepSliced(JUMP_INTERVAL, running::get))
            {
                LOGGER.info("Interrupting running display cycle");
                displaying.set(false);
                return true;
            }
        }

        LOGGER.info("Display cycle completed, clearing display");
        Display.clearDisplay();

        LOGGER.info("Display cleared, blanking");
        return Util.sleepSliced(BLANKING_TIME, running::get);
    }

    private int printScrollingText(String text, int currOffset, int maxLen, boolean firstCall)
    {
        if (maxLen >= text.length())
        {
            if (firstCall)
            {
                Display.printText(text);
            }
            return 0;
        }
        else
        {
            if (currOffset >= text.length())
            {
                Display.printText(" ".repeat(maxLen));
                return 0;
            }

            int end = currOffset + maxLen;
            String dispText;
            if (end > text.length())
            {
                dispText = text.substring(currOffset) + " ".repeat(end - text.length());
            }
            else
            {
                dispText = text.substring(currOffset, currOffset + maxLen);
            }
            Display.printText(dispText);

            return currOffset + 1;
        }
    }

    private boolean shouldSkipIssue() { return skipCurrent.getAndSet(false); }
}
