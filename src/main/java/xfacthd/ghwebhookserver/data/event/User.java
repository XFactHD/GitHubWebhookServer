package xfacthd.ghwebhookserver.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record User(String name)
{
    public static final Codec<User> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("login").forGetter(User::name)
    ).apply(inst, User::new));
}
