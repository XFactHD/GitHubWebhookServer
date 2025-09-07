package xfacthd.ghwebhookserver.command;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Command
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(Command.class);

    private final String prefix;

    protected Command(String prefix)
    {
        this.prefix = "/" + prefix;
    }

    public final boolean matches(String input)
    {
        String prefix = input.contains(" ") ? input.substring(0, input.indexOf(' ')) : input;
        return this.prefix.equals(prefix);
    }

    public abstract void execute(String input, CommandContext ctx);

    public String getPrefix()
    {
        return prefix;
    }

    @Nullable
    public String getArgumentList()
    {
        return null;
    }

    public abstract String getDescription();
}
