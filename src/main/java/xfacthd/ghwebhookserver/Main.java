package xfacthd.ghwebhookserver;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.command.*;
import xfacthd.ghwebhookserver.command.impl.*;
import xfacthd.ghwebhookserver.gpio.GPIO;
import xfacthd.ghwebhookserver.display.IssueDisplay;
import xfacthd.ghwebhookserver.handler.IssueCommentWebhookListener;
import xfacthd.ghwebhookserver.handler.IssueOpenedWebhookListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class Main
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final List<Command> COMMANDS = new ArrayList<>();
    private static final int SHUTDOWN_DELAY = 5;
    private static boolean running = true;

    public static void main(String[] args)
    {
        if (args.length < 3 || args.length > 4)
        {
            LOGGER.error("Usage: ghwhserver <port> <com port> <author filter> [ignore i/o]");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String comPort = args[1];

        String filter = args[2];
        List<String> authorFilter = filter.equals("null") ? List.of() : Arrays.asList(filter.split(","));

        if (args.length == 4 && args[3].equals("true"))
        {
            GPIO.ignoreInput();
        }

        LOGGER.info("Preparing display");
        IssueDisplay display = new IssueDisplay(comPort);
        display.start();
        LOGGER.info("Display ready");

        LOGGER.info("Registering commands");
        registerCommands();
        LOGGER.info("Registered {} commands", COMMANDS.size());

        LOGGER.info("Starting webserver on port {}", port);
        HttpServer server = createServer(port);
        server.createContext("/issue", new IssueOpenedWebhookListener(display::enqueueIssue));
        server.createContext("/comment", new IssueCommentWebhookListener(display::enqueueIssue, authorFilter));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        LOGGER.info("Server started, accepting connections");

        Scanner scanner = new Scanner(System.in);
        CommandContext ctx = new CommandContext(server, display, List.of(args));
        while (running)
        {
            String input = scanner.nextLine();
            for (Command cmd : COMMANDS)
            {
                if (cmd.matches(input))
                {
                    cmd.execute(input, ctx);
                    break;
                }
            }
        }

        LOGGER.info("Stopping server");
        server.stop(SHUTDOWN_DELAY);
        LOGGER.info("Server stopped");

        LOGGER.info("Shutting down display");
        display.shutdown();
        LOGGER.info("Terminated");
    }

    public static void stop() { running = false; }

    private static void registerCommands()
    {
        COMMANDS.add(new StopCommand());
        COMMANDS.add(new TestCommand());
        COMMANDS.add(new SkipCommand());
    }

    private static HttpServer createServer(int port)
    {
        try
        {
            return HttpServer.create(new InetSocketAddress("localhost", port), 0);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Encountered an exception while starting HttpServer", e);
        }
    }
}
