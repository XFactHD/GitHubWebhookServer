package xfacthd.ghwebhookserver.handler;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.data.Issue;

import java.util.function.Consumer;

public class IssueOpenedWebhookListener extends IssueWebhookListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IssueOpenedWebhookListener.class);

    public IssueOpenedWebhookListener(Consumer<Issue> issueConsumer) { super(issueConsumer); }

    @Override
    protected void handlePostData(JsonObject object)
    {
        if (!object.has("issue"))
        {
            LOGGER.info("Received unknown webhook message, dropping");
            return;
        }

        String repository = object.getAsJsonObject("repository").get("name").getAsString();
        JsonObject issue = object.getAsJsonObject("issue");
        int number = issue.get("number").getAsInt();

        String action = object.get("action").getAsString();
        if (action.equals("opened"))
        {
            String title = issue.get("title").getAsString();

            issueConsumer.accept(new Issue(
                    repository,
                    number,
                    title
            ));

            LOGGER.info("Received new issue in repo '{}' with number {} and title '{}'", repository, number, title);
        }
        else
        {
            LOGGER.info("Received ignored action '{}' for issue number {} in repo '{}', ignoring", action, number, repository);
        }
    }
}
