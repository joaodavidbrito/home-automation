package ha.joaobrito.pt.ha.domain;

import java.io.Serializable;

/**
 * Define all types of events and it's timestamps
 */
public class LogEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType implements Serializable {
        ANDROID_START,
        ANDROID_SEND,
        ANDROID_RESEND,
        ANDROID_END,
        ANDROID_MQTT_PUBLISH,
        ANDROID_FALSE_DETECTION,
        ANDROID_POSITIVE_DETECTION,
        SERVER_START_MATCHING,
        SERVER_NOT_FOUND,
        SERVER_FOUND,
        SERVER_RESTART_MATCHING,
        SERVER_END,
        SERVER_END_MATCHING,
        SERVER_REEND_MATCHING,
        SERVER_ERROR,
        ANDROID_LIMIT_REACHED,
        ANDROID_USER_ABORTED,
        ANDROID_USER_NOT_ABORTED;
    }

    public LogEvent(long timestamp, EventType event) {
        this.timestamp = timestamp;
        this.event = event;
    }

    private long timestamp;
    private EventType event;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public EventType getEvent() {
        return event;
    }

    public void setEvent(EventType event) {
        this.event = event;
    }
}
