/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.ngsi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.manager.shared.ngsi.params.NotificationParams;
import org.openremote.manager.shared.ngsi.params.SubscriptionParams;

import java.util.Date;

public class SubscribeRequestV2 {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String id;
    @JsonInclude
    protected SubscriptionParams subject;
    @JsonInclude
    protected NotificationParams notification;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Date expires;

    public SubscribeRequestV2() {
    }

    public SubscribeRequestV2(@JsonProperty String id, @JsonProperty SubscriptionParams subject, @JsonProperty NotificationParams notification, @JsonProperty Date expires) {
        this.id = id;
        this.subject = subject;
        this.notification = notification;
        this.expires = expires;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubscriptionParams getSubject() {
        return subject;
    }

    public void setSubject(SubscriptionParams subject) {
        this.subject = subject;
    }

    public NotificationParams getNotification() {
        return notification;
    }

    public void setNotification(NotificationParams notification) {
        this.notification = notification;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }
}
