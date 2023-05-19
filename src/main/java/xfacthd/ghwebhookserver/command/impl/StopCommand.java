package xfacthd.ghwebhookserver.command.impl;

import xfacthd.ghwebhookserver.Main;
import xfacthd.ghwebhookserver.command.Command;
import xfacthd.ghwebhookserver.command.CommandContext;

public final class StopCommand extends Command
{
    public StopCommand()
    {
        super("stop");
    }

    @Override
    public void execute(String input, CommandContext ctx)
    {
        if (input.contains(" "))
        {
            LOGGER.error("Invalid argument count, expected 0!");
            return;
        }

        Main.stop();
    }

    @Override
    public String getDescription()
    {
        return "Gracefully shut down the server";
    }
}
