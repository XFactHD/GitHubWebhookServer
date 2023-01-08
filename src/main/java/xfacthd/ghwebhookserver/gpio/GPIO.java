package xfacthd.ghwebhookserver.gpio;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

public class GPIO
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GPIO.class);
    private static final String GPIO_PROVIDER_INPUT = "pigpio-digital-input";
    private static final String GPIO_PROVIDER_OUTPUT = "pigpio-digital-output";
    private static final long DEBOUNCE_DELAY = 3000;

    private static final String NAME_SWITCH = "switch";
    private static final String NAME_PAUSE_SWITCH = "pause_switch";
    public static final String NAME_SKIP_BTN = "skip_btn";
    private static final String NAME_MOSFET = "mosfet";
    private static final int PIN_SWITCH = 21; //Header pin 40
    private static final int PIN_PAUSE_SWITCH = 20; //Header pin 38
    private static final int PIN_SKIP = 16; //Header pin 36
    private static final int PIN_MOSFET = 26; //Header pin 37

    private static GPIOListener listener;
    private static boolean enableGpio = false;
    private static boolean inputOverride = false;
    private static boolean gpioInit = false;
    private static Context ctx = null;
    private static DigitalInput inputSwitch;
    private static DigitalInput inputPauseSwitch;
    private static DigitalOutput outputMosfet;

    public static void init()
    {
        enableGpio = checkEnvironment();
        if (!enableGpio)
        {
            gpioInit = true;
            return;
        }

        if (gpioInit) { throw new IllegalStateException("GPIO already initialized"); }

        LOGGER.info("Initializing IO");
        ctx = Pi4J.newAutoContext();

        LOGGER.info("|- Building switch input");
        DigitalInputConfig cfgSwitch = DigitalInput.newConfigBuilder(ctx)
                .id(NAME_SWITCH)
                .address(PIN_SWITCH)
                .pull(PullResistance.PULL_UP)
                .debounce(DEBOUNCE_DELAY)
                .provider(GPIO_PROVIDER_INPUT)
                .build();
        inputSwitch = ctx.create(cfgSwitch);

        LOGGER.info("|- Building pause switch input");
        DigitalInputConfig cfgPauseSwitch = DigitalInput.newConfigBuilder(ctx)
                .id(NAME_PAUSE_SWITCH)
                .address(PIN_PAUSE_SWITCH)
                .pull(PullResistance.PULL_UP)
                .debounce(DEBOUNCE_DELAY)
                .provider(GPIO_PROVIDER_INPUT)
                .build();
        inputPauseSwitch = ctx.create(cfgPauseSwitch);

        LOGGER.info("|- Building skip input");
        DigitalInputConfig cfgSkip = DigitalInput.newConfigBuilder(ctx)
                .id(NAME_SKIP_BTN)
                .address(PIN_SKIP)
                .pull(PullResistance.PULL_UP)
                .debounce(DEBOUNCE_DELAY)
                .provider(GPIO_PROVIDER_INPUT)
                .build();
        ctx.create(cfgSkip);

        LOGGER.info("|- Building display output");
        DigitalOutputConfig cfgMosfet = DigitalOutput.newConfigBuilder(ctx)
                .id(NAME_MOSFET)
                .address(PIN_MOSFET)
                .provider(GPIO_PROVIDER_OUTPUT)
                .build();
        outputMosfet = ctx.create(cfgMosfet);

        LOGGER.info("Initialising IO listener");
        listener = new GPIOListener();
        listener.start();

        gpioInit = true;
        LOGGER.info("IO ready");
    }

    public static void shutdown()
    {
        checkInit();
        gpioInit = false;
        if (!enableGpio) { return; }

        LOGGER.info("Stopping IO listener");
        listener.shutdown();
        LOGGER.info("Shutting down IO");
        ctx.shutdown();
        LOGGER.info("IO shut down");
    }

    public static void ignoreInput() { inputOverride = true; }

    public static boolean readSwitchPin()
    {
        checkInit();
        return !enableGpio || inputOverride || inputSwitch.isLow();
    }

    public static boolean readPauseSwitchPin()
    {
        checkInit();
        return !enableGpio || inputOverride || inputPauseSwitch.isLow();
    }

    public static void writeDrivePin(boolean state)
    {
        checkInit();
        if (enableGpio)
        {
            outputMosfet.setState(state);
        }
    }

    public static void registerInputCallback(String inputName, Consumer<Boolean> changeHandler)
    {
        if (!enableGpio) { return; }
        if (!gpioInit)
        {
            throw new IllegalStateException("GPIO not initialized!");
        }

        if (!ctx.hasIO(inputName))
        {
            throw new IllegalArgumentException("Unknown input '" + inputName + "'");
        }

        DigitalInput input = ctx.getIO(inputName, DigitalInput.class);
        listener.register(input, changeHandler);
    }



    private static boolean checkEnvironment()
    {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux"))
        {
            LOGGER.info("Not a Linux system, disabling GPIO");
            return false;
        }

        try
        {
            Process process = Runtime.getRuntime().exec("uname -m");
            String arch = new String(process.getInputStream().readAllBytes());
            if (arch.startsWith("arm"))
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

    private static void checkInit()
    {
        if (!gpioInit)
        {
            throw new IllegalStateException("GPIO not initialized");
        }
    }
}
