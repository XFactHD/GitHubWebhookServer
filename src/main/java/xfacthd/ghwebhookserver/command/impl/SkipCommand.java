package xfacthd.ghwebhookserver.command.impl;

import xfacthd.ghwebhookserver.command.Command;
import xfacthd.ghwebhookserver.command.CommandContext;

public class SkipCommand extends Command
{
    public SkipCommand() { super("skip"); }

    @Override
    public void execute(String input, CommandContext ctx)
    {
        if (input.contains(" "))
        {
            LOGGER.error("Invalid argument count, expected 0!");
            return;
        }

        ctx.display().notifySkipIssueButton(true);
    }
}
