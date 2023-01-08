package xfacthd.ghwebhookserver.command;

import com.sun.net.httpserver.HttpServer;
import xfacthd.ghwebhookserver.display.IssueDisplay;

import java.util.List;

public record CommandContext(HttpServer server, IssueDisplay display, List<String> cmdArgs) { }
