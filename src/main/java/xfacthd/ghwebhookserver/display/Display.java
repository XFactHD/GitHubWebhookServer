package xfacthd.ghwebhookserver.display;

import xfacthd.ghwebhookserver.display.gpio.GPIO;
import xfacthd.ghwebhookserver.display.serial.Serial;
import xfacthd.ghwebhookserver.util.Util;

import java.util.function.BooleanSupplier;

abstract sealed class Display permits IssueDisplay
{
    private static final int STARTUP_DELAY = 1500;
    private static final String ESC = String.valueOf((char) 0x1B);
    private static final String CLEAR_DISPLAY = ESC + "[2J";

    protected final GPIO gpio;
    private final Serial serial;
    private boolean active = false;
    private boolean shutdown = false;

    protected Display(String comPort, boolean ignoreIO)
    {
        this.gpio = GPIO.create(ignoreIO);
        this.serial = new Serial(comPort);
    }

    protected void stop()
    {
        checkRunning();
        shutdown = true;

        serial.stop();
        gpio.shutdown();
    }

    protected void setCursorPos(int row, int col)
    {
        checkRunning();
        serial.sendData(String.format("%s[%s;%sH", ESC, row + 1, col + 1));
    }

    protected void printText(String text)
    {
        checkRunning();
        serial.sendData(text);
    }

    protected void clearDisplay()
    {
        checkRunning();
        serial.sendData(CLEAR_DISPLAY);
    }

    protected boolean checkSwitchAndDisable()
    {
        checkRunning();

        boolean enabled = gpio.readSwitchPin();
        if (active && !enabled)
        {
            disableDisplay();
        }
        return enabled && gpio.readPauseSwitchPin();
    }

    protected boolean isActive()
    {
        checkRunning();
        return active;
    }

    protected boolean enableDisplay(BooleanSupplier running)
    {
        checkRunning();

        if (active) return false;

        switchDisplay(true);
        return Util.sleepSliced(STARTUP_DELAY, running);
    }

    protected void disableDisplay()
    {
        checkRunning();
        switchDisplay(false);
    }

    private void switchDisplay(boolean enable)
    {
        checkRunning();

        if (enable != active)
        {
            active = enable;
            gpio.writeDrivePin(enable);
        }
    }

    private void checkRunning()
    {
        if (shutdown)
        {
            throw new IllegalStateException("Display was already shut down");
        }
    }
}
