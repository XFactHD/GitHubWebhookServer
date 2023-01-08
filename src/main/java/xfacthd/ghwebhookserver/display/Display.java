package xfacthd.ghwebhookserver.display;

import xfacthd.ghwebhookserver.gpio.GPIO;
import xfacthd.ghwebhookserver.util.Util;

import java.util.function.BooleanSupplier;

public class Display
{
    private static final int STARTUP_DELAY = 1500;
    private static final String ESC = String.valueOf((char) 0x1B);
    private static final String CLEAR_DISPLAY = ESC + "[2J";
    private static boolean active = false;

    public static void start(String port)
    {
        GPIO.init();
        Serial.start(port);
    }

    public static void stop()
    {
        Serial.stop();
        GPIO.shutdown();
    }

    public static void setCursorPos(int row, int col)
    {
        Serial.sendData(String.format("%s[%s;%sH", ESC, row + 1, col + 1));
    }

    public static void printText(String text) { Serial.sendData(text); }

    public static void clearDisplay() { Serial.sendData(CLEAR_DISPLAY); }

    public static boolean checkSwitchAndDisable()
    {
        boolean enabled = GPIO.readSwitchPin();
        if (active && !enabled)
        {
            disableDisplay();
        }
        return enabled && GPIO.readPauseSwitchPin();
    }

    public static boolean isActive() { return active; }

    public static boolean enableDisplay(BooleanSupplier running)
    {
        if (active) { return false; }

        switchDisplay(true);
        return Util.sleepSliced(STARTUP_DELAY, running);
    }

    public static void disableDisplay() { switchDisplay(false); }

    private static void switchDisplay(boolean enable)
    {
        if (enable != active)
        {
            active = enable;
            GPIO.writeDrivePin(enable);
        }
    }
}
