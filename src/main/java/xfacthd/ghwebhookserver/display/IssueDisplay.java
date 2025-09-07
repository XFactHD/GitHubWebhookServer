package xfacthd.ghwebhookserver.display;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.data.Issue;
import xfacthd.ghwebhookserver.display.gpio.GPIO;
import xfacthd.ghwebhookserver.util.Util;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class IssueDisplay extends Display implements Consumer<Issue>
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
    private final Thread thread;
    private boolean enabled = false;
    private long lastDisplayTime = 0;

    public IssueDisplay(String comPort, boolean ignoreIO)
    {
        super(comPort, ignoreIO);
        gpio.registerInputCallback(GPIO.NAME_SKIP_BTN, this::notifySkipIssueButton);
        this.thread = Thread.ofPlatform().start(this::run);
    }

    private void run()
    {
        setCursorPos(0, 0);
        printText("GitHubWebhookServer");
        setCursorPos(1, 0);
        printText("v1");
        Util.sleep(2000);
        clearDisplay();
        Util.sleep(100);

        while (running.get())
        {
            if (waitForDisplayActive()) break;

            boolean exit = switch (retrieveIssue())
            {
                case Result.Disabled(boolean shutdown) -> shutdown;
                case Result.Success(Issue issue) -> driveDisplay(issue);
            };
            if (exit) break;
        }

        stop();
    }

    @Override
    protected void stop()
    {
        LOGGER.info("Display shutting down");
        clearDisplay();
        LOGGER.info("Display cleared");
        disableDisplay();
        LOGGER.info("Display disabled");
        super.stop();
        LOGGER.info("Display shut down");
    }

    @Override
    public void accept(Issue issue)
    {
        LOGGER.info("Enqueued new issue");
        issues.add(issue);
    }

    public void shutdown()
    {
        running.set(false);
        thread.interrupt();

        try
        {
            thread.join();
        }
        catch (InterruptedException ignore) { /* NOOP */ }
    }

    public void notifySkipIssueButton(boolean value)
    {
        if (value && displaying.get())
        {
            skipCurrent.set(true);
        }
    }

    private boolean waitForDisplayActive()
    {
        while (!(enabled = checkSwitchAndDisable()))
        {
            Util.sleep(10);
            if (!running.get())
            {
                return true;
            }
        }
        return false;
    }

    private Result retrieveIssue()
    {
        Issue issue;
        do
        {
            Util.sleep(10);

            enabled = checkSwitchAndDisable();
            if (!enabled || !running.get())
            {
                return new Result.Disabled(!running.get());
            }

            issue = issues.poll();
            if (issue == null && isActive() && System.currentTimeMillis() - lastDisplayTime > DISPLAY_TIMEOUT)
            {
                disableDisplay();
            }
        }
        while (issue == null);

        return new Result.Success(issue);
    }

    private boolean driveDisplay(Issue issue)
    {
        LOGGER.info("Issue acquired, driving to display");
        if (enableDisplay(running::get))
        {
            LOGGER.info("Cancelled display cycle while starting display due to shutdown");
            return true;
        }

        lastDisplayTime = System.currentTimeMillis();
        displaying.set(true);

        String numText = String.format("#%d", issue.number());
        setCursorPos(0, LINE_LENGTH - numText.length());
        printText(numText);

        int maxRepoLen = LINE_LENGTH - (numText.length() + 1);

        boolean firstPrint = true;
        int repoOffset = 0;
        int titleOffset = 0;
        for (int i = 0; i < JUMP_COUNT; i++)
        {
            setCursorPos(0, 0);
            repoOffset = printScrollingText(issue.repo(), repoOffset, maxRepoLen, firstPrint);

            setCursorPos(1, 0);
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
        clearDisplay();

        LOGGER.info("Display cleared, blanking");
        return Util.sleepSliced(BLANKING_TIME, running::get);
    }

    private int printScrollingText(String text, int currOffset, int maxLen, boolean firstCall)
    {
        if (maxLen >= text.length())
        {
            if (firstCall)
            {
                printText(text);
            }
            return 0;
        }
        else
        {
            if (currOffset >= text.length())
            {
                printText(" ".repeat(maxLen));
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
            printText(dispText);

            return currOffset + 1;
        }
    }

    private boolean shouldSkipIssue()
    {
        return skipCurrent.getAndSet(false);
    }

    private sealed interface Result
    {
        record Success(xfacthd.ghwebhookserver.data.Issue issue) implements Result
        {}

        record Disabled(boolean shutdown) implements Result
        {}
    }
}
