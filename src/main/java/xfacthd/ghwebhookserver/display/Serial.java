package xfacthd.ghwebhookserver.display;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class Serial
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Serial.class);
    private static boolean running = false;
    private static boolean failedStart = false;
    private static SerialPort port;

    public static void start(String portName)
    {
        if (running)
        {
            throw new IllegalStateException("Serial already started!");
        }

        LOGGER.info("Starting serial port");

        try
        {
            port = SerialPort.getCommPort(portName);

            port.setBaudRate(9600);
            port.setNumDataBits(8);
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
            port.setParity(SerialPort.ODD_PARITY);
            port.setFlowControl(SerialPort.FLOW_CONTROL_CTS_ENABLED | SerialPort.FLOW_CONTROL_RTS_ENABLED);

            if (!port.openPort())
            {
                LOGGER.error("An error occurred while initializing serial communication: {}", port.getLastErrorCode());
                failedStart = true;
                return;
            }
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
        if (!running)
        {
            if (!failedStart)
            {
                throw new IllegalStateException("Serial not started!");
            }
            return;
        }

        LOGGER.info("Shutting down serial");
        if (!port.closePort())
        {
            LOGGER.error("An error occured while closing the serial port: {}", port.getLastErrorCode());
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

        byte[] bytes = data.getBytes(StandardCharsets.US_ASCII);
        if (port.writeBytes(bytes, bytes.length) == -1)
        {
            LOGGER.error("An error occured while sending serial data: {}", port.getLastErrorCode());
        }
    }
}
