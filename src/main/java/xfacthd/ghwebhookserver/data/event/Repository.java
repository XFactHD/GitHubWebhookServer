package xfacthd.ghwebhookserver.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Repository(String name)
{
    public static final Codec<Repository> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(Repository::name)
    ).apply(inst, Repository::new));
}
