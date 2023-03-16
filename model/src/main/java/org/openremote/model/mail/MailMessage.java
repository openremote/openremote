/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.model.mail;

import java.util.*;

/**
 * Represents a mail message; only represents text part with combined headers from the outer message and any mime part
 * that contained the text content
 */
public class MailMessage {

    protected String content;
    protected String contentType;
    protected Map<String, List<String>> headers;
    protected String subject;
    protected Date sentDate;
    protected String[] from;

    public MailMessage(String content, String contentType, Map<String, List<String>> headers, String subject, Date sentDate, String[] from) {
        this.content = content == null ? "" : content.endsWith("\r\n") ? content.substring(0,content.length() - 2) : content;
        this.contentType = contentType.toLowerCase(Locale.ROOT);
        this.headers = headers;
        this.subject = subject;
        this.sentDate = sentDate;
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public String[] getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getHeader(String name) {
        List<String> namedHeaders = getHeaders(name);
        if (namedHeaders == null || namedHeaders.isEmpty()) {
            return null;
        }
        return namedHeaders.get(0);
    }

    public List<String> getHeaders(String name) {
        return headers.get(name);
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "MailMessage{" +
            "contentType='" + contentType + '\'' +
            ", subject='" + subject + '\'' +
            ", sentDate=" + sentDate +
            ", from=" + Arrays.toString(from) +
            '}';
    }
}
