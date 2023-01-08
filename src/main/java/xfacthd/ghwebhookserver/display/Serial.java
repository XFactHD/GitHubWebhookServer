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
    private static Serial instance;

    private final SerialComManager scm;
    private final long portHandle;

    private Serial(String port)
    {
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
        catch (IOException e)
        {
            throw new RuntimeException("An error occurred while initializing serial communication", e);
        }
    }

    private void send(String data)
    {
        try
        {
            scm.writeString(portHandle, data, StandardCharsets.US_ASCII, 0);
        }
        catch (IOException e)
        {
            LOGGER.error("An error occured while sending serial data");
            e.printStackTrace();
        }
    }

    private void destroy()
    {
        try
        {
            scm.closeComPort(portHandle);
        }
        catch (SerialComException e)
        {
            throw new RuntimeException("An error occured while closing the serial port", e);
        }
    }



    public static void start(String port)
    {
        if (instance != null)
        {
            throw new IllegalStateException("Serial already started!");
        }

        LOGGER.info("Starting serial port");
        instance = new Serial(port);
        LOGGER.info("Serial port started");
    }

    public static void stop()
    {
        if (instance == null)
        {
            throw new IllegalStateException("Serial not started!");
        }

        LOGGER.info("Shutting down serial");
        instance.destroy();
        instance = null;
        LOGGER.info("Serial shut down");
    }

    public static void sendData(String data) { instance.send(data); }
}
