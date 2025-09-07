package xfacthd.ghwebhookserver.util;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class KeyHolder
{
    private final byte[] key;

    public KeyHolder(Path keyFile)
    {
        try (Stream<String> lines = Files.lines(keyFile))
        {
            this.key = lines.findFirst().orElseThrow().getBytes();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read secret key", e);
        }
    }

    public HmacUtils hmac(HmacAlgorithms algo)
    {
        return new HmacUtils(algo, key);
    }
}
