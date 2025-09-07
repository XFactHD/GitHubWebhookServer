package xfacthd.ghwebhookserver.command;

import io.javalin.Javalin;
import xfacthd.ghwebhookserver.display.IssueDisplay;

import java.util.List;

public record CommandContext(CommandHandler handler, Javalin server, IssueDisplay display, List<String> cmdArgs) { }
