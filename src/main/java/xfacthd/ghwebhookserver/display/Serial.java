package xfacthd.ghwebhookserver.display;

import com.serialpundit.core.SerialComException;
import com.serialpundit.serial.SerialComManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Serial
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Serial.class);
    private static boolean running = false;
    private static boolean failedStart = false;
    private static SerialComManager scm;
    private static long portHandle;

    public static void start(String port)
    {
        if (running)
        {
            throw new IllegalStateException("Serial already started!");
        }

        LOGGER.info("Starting serial port");

        try
        {
            scm = new SerialComManager();
            portHandle = scm.openComPort(port, true, true, true);

            scm.configureComPortData(
                    portHandle,
                    SerialComManager.DATABITS.DB_8,
                    SerialComManager.STOPBITS.SB_1,
                    SerialComManager.PARITY.P_ODD,
                    SerialComManager.BAUDRATE.B9600,
                    0
            );

            scm.configureComPortControl(
                    portHandle,
                    SerialComManager.FLOWCONTROL.RTS_CTS,
                    'x',
                    'x',
                    false,
                    false
            );
        }
        catch (Throwable t)
        {
            LOGGER.error("An error occurred while initializing serial communication", t);

            failedStart = true;
            return;
        }

        running = true;
        failedStart = false;
        LOGGER.info("Serial port started");
    }

    public static void stop()
    {
        if (!running && !failedStart)
        {
            throw new IllegalStateException("Serial not started!");
        }

        LOGGER.info("Shutting down serial");
        try
        {
            scm.closeComPort(portHandle);
        }
        catch (SerialComException e)
        {
            LOGGER.error("An error occured while closing the serial port", e);
        }
        running = false;
        LOGGER.info("Serial shut down");
    }

    public static void sendData(String data)
    {
        if (!running)
        {
            if (!failedStart)
            {
                throw new IllegalStateException("Serial not started!");
            }
            return;
        }

        try
        {
            scm.writeString(portHandle, data, StandardCharsets.US_ASCII, 0);
        }
        catch (IOException e)
        {
            LOGGER.error("An error occured while sending serial data", e);
        }
    }
}
