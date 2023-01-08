package xfacthd.ghwebhookserver.util;

import java.util.function.BooleanSupplier;

public class Util
{
    private static final int SLEEP_SLICE = 10;

    public static void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ignored)
        {

        }
    }

    /**
     * Sleep in small intervals and check the flag supplier on each interval
     * @return true if the flag supplier returned false
     */
    public static boolean sleepSliced(long time, BooleanSupplier running)
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < time)
        {
            if (!running.getAsBoolean())
            {
                return true;
            }
            sleep(SLEEP_SLICE);
        }
        return false;
    }
}
