package xfacthd.ghwebhookserver.handler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.data.Issue;
import xfacthd.ghwebhookserver.data.event.EventType;
import xfacthd.ghwebhookserver.data.event.IssueCommentEvent;
import xfacthd.ghwebhookserver.handler.EventListener;

import java.util.List;
import java.util.function.Consumer;

public final class IssueCommentEventListener extends EventListener<IssueCommentEvent>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IssueCommentEventListener.class);
    private final List<String> authorFilter;

    public IssueCommentEventListener(Consumer<Issue> issueConsumer, List<String> authorFilter)
    {
        super(EventType.ISSUE_COMMENT, issueConsumer);
        this.authorFilter = authorFilter;
    }

    @Override
    public void handle(IssueCommentEvent event)
    {
        if (!event.action().equals("created"))
        {
            LOGGER.info(
                    "Received ignored action '{}' for a comment in issue number {} in repo '{}', ignoring",
                    event.action(), event.issue().number(), event.repository().name()
            );
            return;
        }

        if (authorFilter.contains(event.sender().name()))
        {
            LOGGER.info(
                    "Received issue comment on {}#{} from filtered user {}, ignoring",
                    event.repository().name(), event.issue().number(), event.sender().name()
            );
            return;
        }

        issueConsumer.accept(new Issue(
                event.repository().name(),
                event.issue().number(),
                "Re: " + event.issue().title()
        ));

        LOGGER.info("Received new comment in repo '{}' on issue number {}", event.repository().name(), event.issue().number());
    }
}
