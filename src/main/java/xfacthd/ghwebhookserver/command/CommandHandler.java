package xfacthd.ghwebhookserver.command;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class CommandHandler
{
    private final List<Command> commands;
    private final Scanner scanner;

    private CommandHandler(List<Command> commands, Scanner scanner)
    {
        this.commands = commands;
        this.scanner = scanner;
    }

    public void run(CommandContext ctx)
    {
        String input = scanner.nextLine();
        for (Command cmd : commands)
        {
            if (cmd.matches(input))
            {
                cmd.execute(input, ctx);
                return;
            }
        }
    }

    public List<Command> getCommands()
    {
        return commands;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private final List<Command> commands = new ArrayList<>();
        private final Set<String> prefixes = new HashSet<>();
        @Nullable
        private Scanner scanner;

        private Builder() {}

        public Builder source(InputStream inputStream)
        {
            scanner = new Scanner(inputStream);
            return this;
        }

        public Builder register(Command command)
        {
            if (!prefixes.add(command.getPrefix()))
            {
                throw new IllegalStateException("Duplicate registration of prefix: " + command.getPrefix());
            }
            commands.add(command);
            return this;
        }

        public CommandHandler build()
        {
            if (scanner == null)
            {
                throw new IllegalStateException("No source specified");
            }
            return new CommandHandler(List.copyOf(commands), scanner);
        }
    }
}
