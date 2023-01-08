package xfacthd.ghwebhookserver.gpio;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.PullResistance;
import xfacthd.ghwebhookserver.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class GPIOListener extends Thread
{
    private final List<Listener> listeners = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void run()
    {
        while (running.get())
        {
            listeners.forEach(Listener::check);
            Util.sleep(10);
        }
    }

    public void register(DigitalInput input, Consumer<Boolean> changeHandler)
    {
        listeners.add(new Listener(input, changeHandler));
    }

    public void shutdown()
    {
        running.set(false);

        try { join(); }
        catch (InterruptedException ignore) { /* NOOP */ }
    }



    private static class Listener
    {
        private final DigitalInput input;
        private final boolean invert;
        private final Consumer<Boolean> changeHandler;
        private boolean lastState = false;

        public Listener(DigitalInput input, Consumer<Boolean> changeHandler)
        {
            this.input = input;
            this.invert = input.pull() == PullResistance.PULL_UP;
            this.changeHandler = changeHandler;
        }

        public void check()
        {
            boolean state = input.state().isHigh();
            if (lastState != state)
            {
                lastState = state;
                changeHandler.accept(state ^ invert);
            }
        }
    }
}
