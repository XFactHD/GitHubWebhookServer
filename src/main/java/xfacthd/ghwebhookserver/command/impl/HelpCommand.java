package xfacthd.ghwebhookserver.command.impl;

import xfacthd.ghwebhookserver.Main;
import xfacthd.ghwebhookserver.command.Command;
import xfacthd.ghwebhookserver.command.CommandContext;

public final class HelpCommand extends Command
{
    public HelpCommand()
    {
        super("help");
    }

    @Override
    public void execute(String input, CommandContext ctx)
    {
        if (input.contains(" "))
        {
            LOGGER.error("Invalid argument count, expected 0!");
            return;
        }

        StringBuilder builder = new StringBuilder("List of available commands:");
        for (Command cmd : Main.getCommands())
        {
            builder.append("\n - ").append(cmd.getPrefix());
            String args = cmd.getArgumentList();
            if (args != null)
            {
                builder.append(" ").append(args);
            }
            builder.append("\t- ").append(cmd.getDescription());
        }
        LOGGER.info(builder.toString());
    }

    @Override
    public String getDescription()
    {
        return "List all available commands";
    }
}
