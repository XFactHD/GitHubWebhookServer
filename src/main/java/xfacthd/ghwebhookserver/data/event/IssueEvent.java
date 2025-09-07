package xfacthd.ghwebhookserver.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record IssueEvent(String action, Repository repository, IssueData issue) implements Event
{
    static final MapCodec<IssueEvent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("action").forGetter(IssueEvent::action),
            Repository.CODEC.fieldOf("repository").forGetter(IssueEvent::repository),
            IssueData.CODEC.fieldOf("issue").forGetter(IssueEvent::issue)
    ).apply(inst, IssueEvent::new));

    @Override
    public EventType type()
    {
        return EventType.ISSUE;
    }
}
