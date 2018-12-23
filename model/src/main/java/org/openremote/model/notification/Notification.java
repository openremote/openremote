/*
 * Copyright 2017, OpenRemote Inc.
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

import java.util.List;

public class Notification {

    public enum Source {
        INTERNAL,
        CLIENT,
        GLOBAL_RULESET,
        TENANT_RULESET,
        ASSET_RULESET
    }

    public enum TargetType {
        TENANT,
        USER,
        ASSET
    }

    public static class Targets {

        protected TargetType type;
        protected String[] ids;

        public Targets(TargetType type, List<String> ids) {
            this(type, ids.toArray(new String[ids.size()]));
        }

        @JsonCreator
        public Targets(@JsonProperty("type") TargetType type, @JsonProperty("ids")String...ids) {
            this.type = type;
            this.ids = ids;
        }

        public TargetType getType() {
            return type;
        }

        public void setIds(String[] ids) {
            this.ids = ids;
        }

        public String[] getIds() {
            return ids;
        }



        @Override
        public String toString() {
            return getClass().getSimpleName()+ "{" +
                "type=" + type +
                ", ids=" + (ids != null ? "[" + String.join(",", ids) + "]" : null)  +
                '}';
        }
    }

    public static final String HEADER_SOURCE = Notification.class.getName() + ".SOURCE";
    public static final String HEADER_SOURCE_ID = Notification.class.getName() + ".SOURCEID";

    protected String name;
    protected AbstractNotificationMessage message;
    protected RepeatFrequency repeatFrequency;
    protected String repeatInterval;
    protected Targets targets;

    @JsonCreator
    public Notification(@JsonProperty("name") String name, @JsonProperty("message") AbstractNotificationMessage message, @JsonProperty("targets") Targets targets, @JsonProperty("repeatFrequency") RepeatFrequency repeatFrequency, @JsonProperty("repeatInterval") String repeatInterval) {
        this.name = name;
        this.message = message;
        this.targets = targets;
        this.repeatFrequency = repeatFrequency;
        this.repeatInterval = repeatInterval;
    }

    public String getName() {
        return name;
    }

    public Notification setName(String name) {
        this.name = name;
        return this;
    }

    public AbstractNotificationMessage getMessage() {
        return message;
    }

    public Notification setMessage(AbstractNotificationMessage message) {
        this.message = message;
        return this;
    }

    public Targets getTargets() {
        return targets;
    }

    public void setTargets(Targets targets) {
        this.targets = targets;
    }

    public RepeatFrequency getRepeatFrequency() {
        return repeatFrequency;
    }

    /**
     * This applies a fixed time window to a notification i.e. {@link RepeatFrequency#HOURLY} would mean that a
     * notification sent at 10:59 can then be resent at 11:00.
     */
    public Notification setRepeatFrequency(RepeatFrequency repeatFrequency) {
        this.repeatInterval = null;
        this.repeatFrequency = repeatFrequency;
        return this;
    }

    public String getRepeatInterval() {
        return repeatInterval;
    }

    /**
     * This applies a sliding time window to a notification i.e. a value of 1h would mean has it been at least 1hr
     * since the notification was last sent.
     */
    public Notification setRepeatInterval(String repeatInterval) {
        this.repeatFrequency = null;
        this.repeatInterval = repeatInterval;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+ "{" +
            "name='" + name + '\'' +
            ", message=" + message +
            ", repeatFrequency=" + repeatFrequency +
            ", repeatInterval='" + repeatInterval + '\'' +
            ", targets=" + targets +
            '}';
    }
}
