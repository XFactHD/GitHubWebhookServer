package xfacthd.ghwebhookserver.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record IssueData(int number, String title)
{
    static final Codec<IssueData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("number").forGetter(IssueData::number),
            Codec.STRING.fieldOf("title").forGetter(IssueData::title)
    ).apply(inst, IssueData::new));
}
