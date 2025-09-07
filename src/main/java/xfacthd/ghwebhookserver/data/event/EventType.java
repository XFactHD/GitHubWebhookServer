package xfacthd.ghwebhookserver.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EventType
{
    ISSUE("issues", IssueEvent.CODEC),
    ISSUE_COMMENT("issue_comment", IssueCommentEvent.CODEC);

    private static final Map<String, EventType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(EventType::getName, Function.identity()));
    static final Codec<EventType> CODEC = Codec.stringResolver(EventType::getName, BY_NAME::get);

    private final String name;
    private final MapCodec<? extends Event> codec;

    EventType(String name, MapCodec<? extends Event> codec)
    {
        this.name = name;
        this.codec = codec;
    }

    private String getName()
    {
        return name;
    }

    MapCodec<? extends Event> getCodec()
    {
        return codec;
    }

    @Nullable
    public static EventType of(String name)
    {
        return BY_NAME.get(name);
    }
}
