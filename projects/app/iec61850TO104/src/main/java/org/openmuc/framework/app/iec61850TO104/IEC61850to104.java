package org.openmuc.framework.app.iec61850TO104;

import org.openmuc.framework.data.*;
import org.openmuc.framework.data.Record;

import org.openmuc.framework.dataaccess.*;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
public class IEC61850to104 {

    private static final Logger logger = LoggerFactory.getLogger(IEC61850to104.class);
    private static final String APP_NAME = "OpenMuc - IEC-61850 APP";

    private DataAccessService dataAccessService;
    private final Map<Channel, RecordListener> channelListenerMap = new HashMap<>();

    // IEC104 forwarding facade
    private Iec104Server iec104Server;

    @Reference
    public void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    @Activate
    public void activate() {
        logger.info("{} Activated", APP_NAME);

        // Start IEC104 forwarding
        iec104Server = new Iec104Server(dataAccessService);
        iec104Server.start();

        String[] channelIds = {"healthStatus", "breakerPosition", "frequency"};

        for (String channelId : channelIds) {
            Channel channel = dataAccessService.getChannel(channelId);
            if (channel != null) {
                logger.info("Channel [{}] initialized successfully", channelId);
                RecordListener listener = new IecListener(channelId, iec104Server);
                channel.addListener(listener);
                channelListenerMap.put(channel, listener);
            } else {
                logger.error("Failed to initialize channel [{}]", channelId);
            }
        }
    }

    private static class IecListener implements RecordListener {
        private final String channelId;
        private final Iec104Server iec104Server;

        public IecListener(String channelId, Iec104Server iec104Server) {
            this.channelId = channelId;
            this.iec104Server = iec104Server;
        }

        @Override
        public void newRecord(Record record) {
            Value value = record.getValue();
            if (record.getValue() != null) {

                logger.info("New value from [{}]: {}, Timestamp: {}, Type: {}",
                        channelId, value, record.getTimestamp(), value.getValueType());


            } else {
                logger.warn("Received null record or value from [{}]", channelId);
            }
            iec104Server.updatePoint(channelId, value);
        }
    }

    @Deactivate
    public void deactivate() {
        logger.info("Deactivating {}", APP_NAME);
        for (Map.Entry<Channel, RecordListener> entry : channelListenerMap.entrySet()) {
            entry.getKey().removeListener(entry.getValue());
            logger.info("Removed listener from channel [{}]", entry.getKey().getId());
        }
        channelListenerMap.clear();

        if (iec104Server != null) {
            iec104Server.stop();
        }
    }
}


