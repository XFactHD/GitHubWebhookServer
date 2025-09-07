package xfacthd.ghwebhookserver.display.gpio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.util.BooleanConsumer;

import java.io.IOException;
import java.util.Locale;

public abstract sealed class GPIO permits DummyGPIO, Pi4JGPIO
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(GPIO.class);
    protected static final long DEBOUNCE_DELAY = 3000;

    protected static final String NAME_SWITCH = "switch";
    protected static final String NAME_PAUSE_SWITCH = "pause_switch";
    public static final String NAME_SKIP_BTN = "skip_btn";
    protected static final String NAME_MOSFET = "mosfet";

    public static GPIO create(boolean ignoreIO)
    {
        boolean enableGpio = checkEnvironment();
        if (!enableGpio || ignoreIO)
        {
            return new DummyGPIO();
        }
        return new Pi4JGPIO();
    }

    protected GPIO() { }

    public abstract void shutdown();

    public abstract boolean readSwitchPin();

    public abstract boolean readPauseSwitchPin();

    public abstract void writeDrivePin(boolean state);

    public abstract void registerInputCallback(String inputName, BooleanConsumer changeHandler);

    private static boolean checkEnvironment()
    {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux"))
        {
            LOGGER.info("Not a Linux system, disabling GPIO");
            return false;
        }

        try
        {
            Process process = Runtime.getRuntime().exec(new String[] { "uname", "-m" });
            String arch = new String(process.getInputStream().readAllBytes());
            if (arch.startsWith("arm") || arch.startsWith("aarch64"))
            {
                return true;
            }
            LOGGER.info("Not an ARM processor, disabling GPIO");
            return false;
        }
        catch (IOException e)
        {
            LOGGER.error("Encountered an error while checking CPU architecture, disabling GPIO");
            return false;
        }
    }
}
