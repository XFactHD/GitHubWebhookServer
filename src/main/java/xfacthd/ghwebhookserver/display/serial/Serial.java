package xfacthd.ghwebhookserver.display.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class Serial
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Serial.class);

    private final boolean failedStart;
    @Nullable
    private final SerialPort port;
    private boolean shutdown = false;

    public Serial(String portName)
    {
        LOGGER.info("Starting serial port");

        SerialPort port = null;
        boolean failed = false;
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
                failed = true;
                return;
            }
        }
        catch (Throwable t)
        {
            LOGGER.error("An error occurred while initializing serial communication", t);

            failed = true;
            return;
        }
        finally
        {
            this.port = failed ? null : port;
            this.failedStart = failed;
        }

        LOGGER.info("Serial port started");
    }

    public void stop()
    {
        checkRunning();
        shutdown = true;

        if (failedStart) return;

        LOGGER.info("Shutting down serial");
        if (!Objects.requireNonNull(port).closePort())
        {
            LOGGER.error("An error occured while closing the serial port: {}", port.getLastErrorCode());
        }
        LOGGER.info("Serial shut down");
    }

    public void sendData(String data)
    {
        checkRunning();

        if (failedStart) return;

        byte[] bytes = data.getBytes(StandardCharsets.US_ASCII);
        if (Objects.requireNonNull(port).writeBytes(bytes, bytes.length) == -1)
        {
            LOGGER.error("An error occured while sending serial data: {}", port.getLastErrorCode());
        }
    }

    private void checkRunning()
    {
        if (shutdown)
        {
            throw new IllegalStateException("Serial was already shut down");
        }
    }
}
