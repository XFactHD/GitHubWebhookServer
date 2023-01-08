package xfacthd.ghwebhookserver.handler;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.ghwebhookserver.data.Issue;

import java.util.List;
import java.util.function.Consumer;

public class IssueCommentWebhookListener extends IssueWebhookListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IssueCommentWebhookListener.class);
    private final List<String> authorFilter;

    public IssueCommentWebhookListener(Consumer<Issue> issueConsumer, List<String> authorFilter)
    {
        super(issueConsumer);
        this.authorFilter = authorFilter;
    }

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
        if (object.get("action").getAsString().equals("created"))
        {
            String commenter = object.getAsJsonObject("comment").getAsJsonObject("user").get("login").getAsString();
            if (authorFilter.contains(commenter))
            {
                LOGGER.info(String.format("Received issue comment on %s#%d from filtered user %s, ignoring", repository, number, commenter));
                return;
            }

            issueConsumer.accept(new Issue(
                    repository,
                    number,
                    "Re: " + issue.get("title").getAsString()
            ));

            LOGGER.info("Received new comment in repo '{}' on issue number {}", repository, number);
        }
        else
        {
            LOGGER.info("Received ignored action '{}' for a comment in issue number {} in repo '{}', ignoring", action, number, repository);
        }
    }
}
