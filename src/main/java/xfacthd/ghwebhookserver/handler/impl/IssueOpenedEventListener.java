package xfacthd.ghwebhookserver.handler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.data.Issue;
import xfacthd.ghwebhookserver.data.event.EventType;
import xfacthd.ghwebhookserver.data.event.IssueEvent;
import xfacthd.ghwebhookserver.handler.EventListener;

import java.util.function.Consumer;

public final class IssueOpenedEventListener extends EventListener<IssueEvent>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IssueOpenedEventListener.class);

    public IssueOpenedEventListener(Consumer<Issue> issueConsumer)
    {
        super(EventType.ISSUE, issueConsumer);
    }

    @Override
    public void handle(IssueEvent event)
    {
        if (!event.action().equals("opened"))
        {
            LOGGER.info(
                    "Received ignored action '{}' for issue number {} in repo '{}', ignoring",
                    event.action(), event.issue().number(), event.repository().name()
            );
            return;
        }

        issueConsumer.accept(new Issue(
                event.repository().name(),
                event.issue().number(),
                event.issue().title()
        ));

        LOGGER.info(
                "Received new issue in repo '{}' with number {} and title '{}'",
                event.repository().name(), event.issue().number(), event.issue().title()
        );
    }
}
