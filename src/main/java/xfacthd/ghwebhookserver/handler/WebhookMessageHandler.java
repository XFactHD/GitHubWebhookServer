package xfacthd.ghwebhookserver.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.data.event.Event;
import xfacthd.ghwebhookserver.data.event.EventType;
import xfacthd.ghwebhookserver.util.KeyHolder;

import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.Map;

/*
https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#issues
https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks
*/
public final class WebhookMessageHandler implements Handler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookMessageHandler.class);
    private static final String PARAM_SIGNATURE = "X-Hub-Signature-256";
    public static final String PARAM_EVENT_TYPE = "X-GitHub-Event";

    @Nullable
    private final KeyHolder keyHolder;
    private final Map<EventType, EventListener<?>> listeners = new EnumMap<>(EventType.class);

    public WebhookMessageHandler(@Nullable KeyHolder keyHolder)
    {
        this.keyHolder = keyHolder;
    }

    public void registerListener(EventType type, EventListener<?> listener)
    {
        if (listener.getEventType() != type)
        {
            throw new IllegalArgumentException("Invalid listener for type: " + type);
        }
        if (listeners.putIfAbsent(type, listener) != null)
        {
            throw new IllegalStateException("Duplicate listener registration for type: " + type);
        }
    }

    @Override
    public void handle(Context ctx)
    {
        LOGGER.info("Received incoming request from {}", ctx.ip());

        if (ctx.method() != HandlerType.POST)
        {
            LOGGER.error("Invalid method, expected POST, aborting");
            sendResponse(ctx, 400);
            return;
        }

        if (keyHolder != null)
        {
            String remoteSignature = ctx.header(PARAM_SIGNATURE);
            if (remoteSignature == null)
            {
                LOGGER.error("Signature not received, aborting");
                sendResponse(ctx, 400);
                return;
            }

            String signature = "sha256=" + keyHolder.hmac(HmacAlgorithms.HMAC_SHA_256).hmacHex(ctx.body());
            if (!MessageDigest.isEqual(signature.getBytes(), remoteSignature.getBytes()))
            {
                LOGGER.error("Signature doesn't match, aborting");
                sendResponse(ctx, 400);
                return;
            }
        }

        String remoteEventType = ctx.header(PARAM_EVENT_TYPE);
        if (remoteEventType == null)
        {
            LOGGER.error("Event type not received, aborting");
            sendResponse(ctx, 400);
            return;
        }

        EventType eventType = EventType.of(remoteEventType);
        if (eventType == null)
        {
            LOGGER.info("Received unknown event type {}, ignoring", remoteEventType);
            return;
        }

        JsonObject object = ctx.bodyAsClass(JsonObject.class);
        object.addProperty(PARAM_EVENT_TYPE, remoteEventType);
        DataResult<Pair<Event, JsonElement>> result = Event.CODEC.decode(JsonOps.INSTANCE, object);
        if (result.isError() || result.result().isEmpty())
        {
            LOGGER.error("Event decode failed: {}", result.error().map(DataResult.Error::message).orElse("Unknown error"));
            sendResponse(ctx, 500);
            return;
        }

        Event event = result.getOrThrow().getFirst();
        EventListener<?> listener = listeners.get(event.type());
        if (listener == null)
        {
            LOGGER.info("No listener registered ");
            sendResponse(ctx, 200);
            return;
        }

        try
        {
            if (event.type() != eventType)
            {
                LOGGER.error("Decoded event has unexpected type: {}", event.type());
                sendResponse(ctx, 500);
                return;
            }

            handleEventUnchecked(listener, event);
        }
        catch (Throwable t)
        {
            LOGGER.error("Request handler errored", t);
            sendResponse(ctx, 500);
            return;
        }

        sendResponse(ctx, 200);

        LOGGER.info("Request handled");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleEventUnchecked(EventListener<?> listener, Event event)
    {
        ((EventListener) listener).handle(event);
    }

    private static void sendResponse(Context ctx, int returnCode)
    {
        ctx.status(returnCode);
        ctx.result("\n");
    }
}
