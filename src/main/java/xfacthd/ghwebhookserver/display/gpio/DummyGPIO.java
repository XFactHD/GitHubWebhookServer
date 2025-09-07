package xfacthd.ghwebhookserver.display.gpio;

import xfacthd.ghwebhookserver.util.BooleanConsumer;

final class DummyGPIO extends GPIO
{
    DummyGPIO() { }

    @Override
    public void shutdown() { }

    @Override
    public boolean readSwitchPin()
    {
        return true;
    }

    @Override
    public boolean readPauseSwitchPin()
    {
        return true;
    }

    @Override
    public void writeDrivePin(boolean state) { }

    @Override
    public void registerInputCallback(String inputName, BooleanConsumer changeHandler) { }
}
