package xfacthd.ghwebhookserver.command.impl;

import xfacthd.ghwebhookserver.command.Command;
import xfacthd.ghwebhookserver.command.CommandContext;
import xfacthd.ghwebhookserver.data.Issue;

// /test FramedBlocks 88 Some Framed blocks aren't rendering due to Optifine
public class TestCommand extends Command
{
    public TestCommand() { super("test"); }

    @Override
    public void execute(String input, CommandContext ctx)
    {
        String[] parts = input.split(" ");
        if (parts.length < 4)
        {
            LOGGER.error("Invalid test issue description. Expected at least 4 parts, got " + parts.length);
        }
        else
        {
            StringBuilder title = new StringBuilder();
            for (int i = 3; i < parts.length; i++)
            {
                title.append(parts[i]).append(' ');
            }
            ctx.display().enqueueIssue(new Issue(parts[1], Integer.parseInt(parts[2]), title.toString().trim()));
        }
    }
}
