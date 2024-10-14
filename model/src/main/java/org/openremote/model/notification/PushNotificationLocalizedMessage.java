package org.openremote.model.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PushNotificationLocalizedMessage extends AbstractNotificationMessage implements LocalizedNotificationMessage {

    public static final String TYPE = "push_localized";

    public String defaultLanguage;
    public Map<String, PushNotificationMessage> languages;

    @JsonCreator
    public PushNotificationLocalizedMessage(@JsonProperty("defaultLanguage") String defaultLanguage,
                                            @JsonProperty("languages") Map<String, PushNotificationMessage> languages) {
        super(PushNotificationMessage.TYPE);
        this.defaultLanguage = defaultLanguage;
        this.languages = languages;
    }

    public PushNotificationLocalizedMessage() {
        super(PushNotificationMessage.TYPE);
    }

    public String getTitle(String language) {
        return getMessage(language).getTitle();
    }

    public String getBody(String language) {
        return getMessage(language).getBody();
    }

    public PushNotificationAction getAction(String language) {
        return getMessage(language).getAction();
    }

    public List<PushNotificationButton> getButtons(String language) {
        return getMessage(language).getButtons();
    }

    public Map<String, Object> getData(String language) {
        return getMessage(language).getData();
    }

    public PushNotificationMessage.MessagePriority getPriority(String language) {
        return getMessage(language).getPriority();
    }

    public PushNotificationMessage.TargetType getTargetType(String language) {
        return getMessage(language).getTargetType();
    }

    public String getTarget(String language) {
        return getMessage(language).getTarget();
    }

    public Long getTtlSeconds(String language) {
        return getMessage(language).getTtlSeconds();
    }

    @Override
    public PushNotificationMessage getMessage(String language) {
        return Optional.ofNullable(languages.get(language)).orElse(languages.get(defaultLanguage));
    }

    @Override
    public Map<String, PushNotificationMessage> getMessages() {
        return languages;
    }
}
