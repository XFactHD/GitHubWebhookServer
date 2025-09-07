package xfacthd.ghwebhookserver.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record IssueCommentEvent(String action, Repository repository, IssueData issue, User sender) implements Event
{
    static final MapCodec<IssueCommentEvent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("action").forGetter(IssueCommentEvent::action),
            Repository.CODEC.fieldOf("repository").forGetter(IssueCommentEvent::repository),
            IssueData.CODEC.fieldOf("issue").forGetter(IssueCommentEvent::issue),
            User.CODEC.fieldOf("sender").forGetter(IssueCommentEvent::sender)
    ).apply(inst, IssueCommentEvent::new));

    @Override
    public EventType type()
    {
        return EventType.ISSUE_COMMENT;
    }
}
