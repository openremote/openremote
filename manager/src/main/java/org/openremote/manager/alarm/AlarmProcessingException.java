package org.openremote.manager.alarm;

import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.alarm.Alarm.Source;

public class AlarmProcessingException extends RuntimeException {
    public enum Reason {

        /**
         * Missing {@link SentAlarm}.
         */
        MISSING_ALARM,

        /**
         * Missing {@link SentAlarm}.
         */
        MISSING_CONTENT,

        /**
         * Missing {@link Source}.
         */
        MISSING_SOURCE
    }

    final protected Reason reason;
    final protected String message;

    public AlarmProcessingException(Reason reason) {
        this(reason, null);
    }

    public AlarmProcessingException(Reason reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    public Reason getReason() {
        return reason;
    }

    public String getReasonPhrase() {
        return getReason() + (message != null ? " (" + message + ")": "");
    }
}
