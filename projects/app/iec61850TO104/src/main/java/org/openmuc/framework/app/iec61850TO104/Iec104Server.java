package org.openmuc.framework.app.iec61850TO104;

import org.openmuc.framework.data.*;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Iec104Server {

    private static final Logger logger = LoggerFactory.getLogger(Iec104Server.class);

    private final DataAccessService das;
    private volatile boolean running = false;

    public Iec104Server(DataAccessService das) {
        this.das = das;
    }

    public void start() {
        running = true;
        logger.info("IEC 104  started .");
    }

    public void stop() {
        running = false;
        logger.info("IEC 104  stopped.");
    }


    public void updatePoint(String sourceChannelId, Value value) {
        if (!running) {
            logger.debug("IEC 104 not running; dropped update for [{}]", sourceChannelId);
            return;
        }
        logger.info("IEC 104  running;  [{}]", sourceChannelId);
        String targetChannelId = sourceChannelId + "_iec104";
        Channel target = das.getChannel(targetChannelId);

        if (target == null) {
            logger.warn("IEC 104 target channel [{}] not found in channels.xml.", targetChannelId);
            return;
        }

        logger.info("Attempting to forward [{}] → [{}]: {}",
                sourceChannelId, targetChannelId, (value != null ? value : "null"));

        try {
            if (value != null) {
                target.write(value);
            }
            else {
                // If value is null, write a placeholder or skip
                target.write(new StringValue("null"));
            }
            logger.debug("Forward complete [{}] → [{}]: {}", sourceChannelId, targetChannelId, value);
        }
        catch (Exception e) {
            try {
                if (value != null) {
                    Value converted = convertForIec104(value);
                    target.write(converted);
                    logger.info("Converted and forwarded [{}] → [{}]: {}",
                            sourceChannelId, targetChannelId, converted);
                }
                else {
                    logger.warn("No conversion done: value is null for [{}]", sourceChannelId);
                }
            }
            catch (Exception second) {
                logger.error("Failed to forward [{}] → [{}]: {}",
                        sourceChannelId, targetChannelId, second.getMessage(), second);
            }
        }
    }


    /**
     * Converts non-IEC 104 compatible types to IEC 104 acceptable ones.
     */
    private Value convertForIec104(Value in) {
        if (in == null) {
            throw new IllegalArgumentException("Null value cannot be converted.");
        }
        switch (in.getValueType()) {
            case BOOLEAN:
                return ((BooleanValue) in).asBoolean() ? new IntValue(1) : new IntValue(0);
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return in;
            case FLOAT:
            case DOUBLE:
                float f = (in.getValueType() == ValueType.DOUBLE)
                        ? (float) ((DoubleValue) in).asDouble()
                        : ((FloatValue) in).asFloat();
                return new FloatValue(f);
            case STRING:
                String s = ((StringValue) in).asString();
                if ("true".equalsIgnoreCase(s)) return new IntValue(1);
                if ("false".equalsIgnoreCase(s)) return new IntValue(0);
                throw new IllegalArgumentException("Cannot convert STRING to IEC104 numeric: " + s);
            default:
                throw new IllegalArgumentException("Unsupported type for IEC104 conversion: " + in.getValueType());
        }
    }
}
