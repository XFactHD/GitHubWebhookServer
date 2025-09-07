package xfacthd.ghwebhookserver.data.event;

import com.mojang.serialization.Codec;
import xfacthd.ghwebhookserver.handler.WebhookMessageHandler;

public sealed interface Event permits IssueCommentEvent, IssueEvent
{
    Codec<Event> CODEC = EventType.CODEC.dispatch(
            WebhookMessageHandler.PARAM_EVENT_TYPE,
            Event::type,
            EventType::getCodec
    );

    EventType type();
}
