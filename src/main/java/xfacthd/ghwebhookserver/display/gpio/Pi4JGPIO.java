package xfacthd.ghwebhookserver.display.gpio;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import xfacthd.ghwebhookserver.util.BooleanConsumer;

final class Pi4JGPIO extends GPIO
{
    private static final String GPIO_PROVIDER_INPUT = "pigpio-digital-input";
    private static final String GPIO_PROVIDER_OUTPUT = "pigpio-digital-output";
    private static final int PIN_SWITCH = 21; //Header pin 40
    private static final int PIN_PAUSE_SWITCH = 20; //Header pin 38
    private static final int PIN_SKIP = 16; //Header pin 36
    private static final int PIN_MOSFET = 12; //Header pin 32

    private final Context ctx;
    private final DigitalInput inputSwitch;
    private final DigitalInput inputPauseSwitch;
    private final DigitalOutput outputMosfet;
    private boolean shutdown = false;

    Pi4JGPIO()
    {
        LOGGER.info("Initializing IO");
        this.ctx = Pi4J.newAutoContext();

        LOGGER.info("|- Building switch input");
        DigitalInputConfig cfgSwitch = DigitalInput.newConfigBuilder(ctx)
                .id(NAME_SWITCH)
                .address(PIN_SWITCH)
                .pull(PullResistance.PULL_UP)
                .debounce(DEBOUNCE_DELAY)
                .provider(GPIO_PROVIDER_INPUT)
                .build();
        this.inputSwitch = ctx.create(cfgSwitch);

        LOGGER.info("|- Building pause switch input");
        DigitalInputConfig cfgPauseSwitch = DigitalInput.newConfigBuilder(ctx)
                .id(NAME_PAUSE_SWITCH)
                .address(PIN_PAUSE_SWITCH)
                .pull(PullResistance.PULL_UP)
                .debounce(DEBOUNCE_DELAY)
                .provider(GPIO_PROVIDER_INPUT)
                .build();
        this.inputPauseSwitch = ctx.create(cfgPauseSwitch);

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
        this.outputMosfet = ctx.create(cfgMosfet);

        LOGGER.info("IO ready");
    }

    @Override
    public void shutdown()
    {
        checkRunning();
        shutdown = true;

        LOGGER.info("Shutting down IO");
        ctx.shutdown();
        LOGGER.info("IO shut down");
    }

    @Override
    public boolean readSwitchPin()
    {
        checkRunning();
        return inputSwitch.isLow();
    }

    @Override
    public boolean readPauseSwitchPin()
    {
        checkRunning();
        return inputPauseSwitch.isLow();
    }

    @Override
    public void writeDrivePin(boolean state)
    {
        checkRunning();
        outputMosfet.setState(state);
    }

    @Override
    public void registerInputCallback(String inputName, BooleanConsumer changeHandler)
    {
        checkRunning();

        if (!ctx.hasIO(inputName))
        {
            throw new IllegalArgumentException("Unknown input '" + inputName + "'");
        }

        DigitalInput input = ctx.getIO(inputName, DigitalInput.class);
        boolean invert = input.pull() == PullResistance.PULL_UP;
        input.addListener(event ->
        {
            DigitalState state = event.state();
            changeHandler.accept(invert ? state.isLow() : state.isHigh());
        });
    }

    private void checkRunning()
    {
        if (shutdown)
        {
            throw new IllegalStateException("GPIO was already shut down");
        }
    }
}
