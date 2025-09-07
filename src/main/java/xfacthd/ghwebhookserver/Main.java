package xfacthd.ghwebhookserver;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinGson;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.command.CommandContext;
import xfacthd.ghwebhookserver.command.CommandHandler;
import xfacthd.ghwebhookserver.command.impl.HelpCommand;
import xfacthd.ghwebhookserver.command.impl.SkipCommand;
import xfacthd.ghwebhookserver.command.impl.StopCommand;
import xfacthd.ghwebhookserver.command.impl.TestCommand;
import xfacthd.ghwebhookserver.data.event.EventType;
import xfacthd.ghwebhookserver.display.IssueDisplay;
import xfacthd.ghwebhookserver.handler.impl.IssueCommentEventListener;
import xfacthd.ghwebhookserver.handler.impl.IssueOpenedEventListener;
import xfacthd.ghwebhookserver.handler.WebhookMessageHandler;
import xfacthd.ghwebhookserver.util.KeyHolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Main
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final int SHUTDOWN_DELAY = 5000;
    private static boolean running = true;

    public static void main(String[] args)
    {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        OptionSpec<Integer> portOpt = parser.accepts("port", "Port of the webserver")
                .withRequiredArg()
                .ofType(Integer.class)
                .required();
        OptionSpec<String> comPortOpt = parser.accepts("com_port", "COM port of the display")
                .withRequiredArg()
                .ofType(String.class)
                .required();
        OptionSpec<Path> keyFileOpt = parser.accepts("key_file", "File containing key for message signature validation")
                .withRequiredArg()
                .withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING, PathProperties.READABLE));
        OptionSpec<String> authorFilterOpt = parser.accepts("author_filter", "Comma-separated list of GH users whose issue comments to ignore")
                .withRequiredArg()
                .withValuesSeparatedBy(',')
                .ofType(String.class);
        OptionSpec<Boolean> ignoreIoOpt = parser.accepts("ignore_io", "If present and true, GPIO is disabled and any inputs are ignored")
                .withRequiredArg()
                .ofType(Boolean.class)
                .defaultsTo(Boolean.FALSE);

        OptionSet options;
        try
        {
            options = parser.parse(args);
        }
        catch (OptionException e)
        {
            try
            {
                parser.printHelpOn(System.out);
            }
            catch (IOException ignored) {}
            return;
        }

        int port = options.valueOf(portOpt);
        String comPort = options.valueOf(comPortOpt);
        Path keyFile = options.has(keyFileOpt) ? options.valueOf(keyFileOpt) : null;
        List<String> authorFilter = options.has(authorFilterOpt) ? options.valuesOf(authorFilterOpt) : List.of();
        boolean ignoreIO = options.valueOf(ignoreIoOpt);

        LOGGER.info("Starting display");
        IssueDisplay display = new IssueDisplay(comPort, ignoreIO);
        LOGGER.info("Display ready");

        LOGGER.info("Registering commands");
        CommandHandler commands = CommandHandler.builder()
                .source(System.in)
                .register(new HelpCommand())
                .register(new StopCommand())
                .register(new TestCommand())
                .register(new SkipCommand())
                .build();
        LOGGER.info("Registered {} commands", commands.getCommands().size());

        LOGGER.info("Setting up event listeners");
        KeyHolder keyHolder = keyFile != null ? new KeyHolder(keyFile) : null;
        WebhookMessageHandler handler = new WebhookMessageHandler(keyHolder);
        handler.registerListener(EventType.ISSUE, new IssueOpenedEventListener(display));
        handler.registerListener(EventType.ISSUE_COMMENT, new IssueCommentEventListener(display, authorFilter));
        LOGGER.info("Starting webserver on port {}", port);
        Javalin server = Javalin.create(Main::configureServer)
                .post("/webhook", handler)
                .start("localhost", port);
        LOGGER.info("Server started, accepting connections");

        CommandContext ctx = new CommandContext(commands, server, display, List.of(args));
        while (running)
        {
            commands.run(ctx);
        }

        LOGGER.info("Stopping server");
        server.stop();
        LOGGER.info("Server stopped");

        LOGGER.info("Shutting down display");
        display.shutdown();
        LOGGER.info("Terminated");
    }

    private static void configureServer(JavalinConfig cfg)
    {
        cfg.http.strictContentTypes = true;
        cfg.jetty.modifyServer(server -> server.setStopTimeout(SHUTDOWN_DELAY));
        cfg.useVirtualThreads = true;
        cfg.showJavalinBanner = false;
        cfg.jsonMapper(new JavalinGson());
    }

    public static void stop()
    {
        running = false;
    }

    private Main() {}
}
