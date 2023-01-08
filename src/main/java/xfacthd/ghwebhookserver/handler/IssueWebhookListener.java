package xfacthd.ghwebhookserver.handler;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.data.Issue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.function.Consumer;
import java.util.stream.Stream;

/*
https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#issues
https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks
*/
public abstract class IssueWebhookListener implements HttpHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IssueWebhookListener.class);
    private static final String PARAM_SIGNATURE = "X-Hub-Signature-256";
    private static final Gson GSON = new Gson();

    protected final Consumer<Issue> issueConsumer;
    private final byte[] secretKey;

    protected IssueWebhookListener(Consumer<Issue> issueConsumer)
    {
        this.issueConsumer = issueConsumer;
        this.secretKey = readKey();
    }

    @Override
    public void handle(HttpExchange exch) throws IOException
    {
        LOGGER.info("Received incoming request from {}", exch.getRemoteAddress());

        if (!exch.getRequestMethod().equals("POST"))
        {
            LOGGER.error("Invalid method, expected POST, aborting");
            sendResponse(exch, 400);
            return;
        }

        Headers headers = exch.getRequestHeaders();
        if (!headers.containsKey(PARAM_SIGNATURE))
        {
            LOGGER.error("Signature not received, aborting");
            sendResponse(exch, 400);
            return;
        }

        String content = new String(exch.getRequestBody().readAllBytes());

        String signature = "sha256=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey).hmacHex(content);
        String remoteSignature = exch.getRequestHeaders().get(PARAM_SIGNATURE).get(0);
        if (!MessageDigest.isEqual(signature.getBytes(), remoteSignature.getBytes()))
        {
            LOGGER.error("Signature doesn't match, aborting");
            sendResponse(exch, 400);
            return;
        }

        JsonObject object = GSON.fromJson(content, JsonObject.class);
        handlePostData(object);

        sendResponse(exch, 200);

        LOGGER.info("Request handled");
    }

    protected abstract void handlePostData(JsonObject object);

    private void sendResponse(HttpExchange exchange, int returnCode) throws IOException
    {
        exchange.sendResponseHeaders(returnCode, 1);

        OutputStream out = exchange.getResponseBody();
        out.write("\n".getBytes());
        out.flush();

        exchange.close();
    }

    private static byte[] readKey()
    {
        try (Stream<String> lines = Files.lines(Path.of("./secret/key")))
        {
            return lines.findFirst().orElseThrow().getBytes();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read secret key", e);
        }
    }
}
