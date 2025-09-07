package xfacthd.ghwebhookserver.handler;

import xfacthd.ghwebhookserver.data.Issue;
import xfacthd.ghwebhookserver.data.event.Event;
import xfacthd.ghwebhookserver.data.event.EventType;

import java.util.function.Consumer;

public abstract class EventListener<E extends Event>
{
    private final EventType eventType;
    protected final Consumer<Issue> issueConsumer;

    protected EventListener(EventType eventType, Consumer<Issue> issueConsumer)
    {
        this.eventType = eventType;
        this.issueConsumer = issueConsumer;
    }

    public abstract void handle(E event);

    public final EventType getEventType()
    {
        return eventType;
    }
}
