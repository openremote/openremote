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
package org.openremote.manager.server.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FCMMessage extends FCMBaseMessage {

    protected FCMNotification notification;
    protected boolean contentAvailable;
    protected boolean mutableContent;
    protected String priority;

    public FCMMessage(FCMNotification notification, boolean contentAvailable, boolean mutableContent, String priority, String to) {
        super(to);
        this.notification = notification;
        this.contentAvailable = contentAvailable;
        this.mutableContent = mutableContent;
        this.priority = priority;
    }

    public FCMNotification getNotification() {
        return notification;
    }

    public void setNotification(FCMNotification notification) {
        this.notification = notification;
    }

    @JsonProperty("content_available")
    public boolean getContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    @JsonProperty("mutable_content")
    public boolean getMutableContent() {
        return mutableContent;
    }

    public void setMutableContent(boolean mutableContent) {
        this.mutableContent = mutableContent;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "notification=" + notification +
            ", contentAvailable=" + contentAvailable +
            ", mutableContent=" + mutableContent +
            ", priority='" + priority + '\'' +
            '}';
    }
}
