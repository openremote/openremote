package org.openremote.model.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

public class LocalizedNotificationMessage extends AbstractNotificationMessage {

    public static final String TYPE = "localized";

    protected String defaultLanguage;
    protected Map<String, AbstractNotificationMessage> languages;

    @JsonCreator
    public LocalizedNotificationMessage(@JsonProperty("defaultLanguage") String defaultLanguage,
                                        @JsonProperty("languages") Map<String, AbstractNotificationMessage> languages) {
        super(TYPE);
        this.defaultLanguage = defaultLanguage;
        this.languages = languages;
    }

    public LocalizedNotificationMessage() {
        super(TYPE);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public LocalizedNotificationMessage setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        return this;
    }

    public AbstractNotificationMessage getMessage(String language) {
        return Optional.ofNullable(languages.get(language)).orElse(languages.get(defaultLanguage));
    }

    public Map<String, AbstractNotificationMessage> getMessages() {
        return languages;
    }

    public void setMessage(String language, AbstractNotificationMessage message) {
        languages.put(language, message);
    }
}
