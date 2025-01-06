/*
 * Copyright 2024, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
