/*
 * Copyright 2018, OpenRemote Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EmailNotificationMessage extends AbstractNotificationMessage {

    public static class Recipient {

        protected final String name;
        protected final String address;

        public Recipient(String address) {
            this(null, address);
        }

        @JsonCreator
        public Recipient(@JsonProperty("name") String name, @JsonProperty("address") String address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
        }
    }

    public static final String TYPE = "email";
    protected Recipient from;
    protected Recipient replyTo;
    protected String subject;
    protected String text;
    protected String html;
    protected List<Recipient> to;
    protected List<Recipient> cc;
    protected List<Recipient> bcc;

    public EmailNotificationMessage() {
        super(TYPE);
    }

    public Recipient getFrom() {
        return from;
    }

    public EmailNotificationMessage setFrom(String address) {
        return setFrom(null, address);
    }

    public EmailNotificationMessage setFrom(String name, String address) {
        return setFrom(new Recipient(name, address));
    }

    public EmailNotificationMessage setFrom(Recipient sender) {
        this.from = sender;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public EmailNotificationMessage setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getText() {
        return text;
    }

    public EmailNotificationMessage setText(String text) {
        this.text = text;
        return this;
    }

    public String getHtml() {
        return html;
    }

    public EmailNotificationMessage setHtml(String html) {
        this.html = html;
        return this;
    }

    public Recipient getReplyTo() {
        return replyTo;
    }

    public EmailNotificationMessage setReplyTo(Recipient replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    public List<Recipient> getTo() {
        return to;
    }

    public EmailNotificationMessage setTo(String...addresses) {
        return setTo(addresses != null ? Arrays.stream(addresses).map(Recipient::new).collect(Collectors.toList()) : null);
    }

    public EmailNotificationMessage setTo(Recipient...recipients) {
        return setTo(recipients != null ? Arrays.asList(recipients) : null);
    }

    public EmailNotificationMessage setTo(List<Recipient> recipients) {
        to = null;
        if (recipients == null) {
            return this;
        }

        return addTo(recipients);
    }

    public EmailNotificationMessage addTo(String...addresses) {
        return addTo(Arrays.stream(addresses).map(Recipient::new).collect(Collectors.toList()));
    }

    public EmailNotificationMessage addTo(Recipient...recipients) {
        return addTo(Arrays.asList(recipients));
    }

    public EmailNotificationMessage addTo(List<Recipient> recipients) {
        if (to == null) {
            to = new ArrayList<>();
        }

        to.addAll(recipients);
        return this;
    }

    public List<Recipient> getCc() {
        return cc;
    }

    public EmailNotificationMessage setCc(String...addresses) {
        return setCc(addresses != null ? Arrays.stream(addresses).map(Recipient::new).collect(Collectors.toList()) : null);
    }

    public EmailNotificationMessage setCc(Recipient...recipients) {
        return setCc(recipients != null ? Arrays.asList(recipients) : null);
    }

    public EmailNotificationMessage setCc(List<Recipient> recipients) {
        cc = null;
        if (recipients == null) {
            return this;
        }

        return addCc(recipients);
    }

    public EmailNotificationMessage addCc(String...addresses) {
        return addCc(Arrays.stream(addresses).map(Recipient::new).collect(Collectors.toList()));
    }

    public EmailNotificationMessage addCc(Recipient...recipients) {
        return addCc(Arrays.asList(recipients));
    }

    public EmailNotificationMessage addCc(List<Recipient> recipients) {
        if (cc == null) {
            cc = new ArrayList<>();
        }

        cc.addAll(recipients);
        return this;
    }

    public List<Recipient> getBcc() {
        return bcc;
    }

    public EmailNotificationMessage setBcc(String...addresses) {
        return setBcc(addresses != null ? Arrays.stream(addresses).map(Recipient::new).collect(Collectors.toList()) : null);
    }

    public EmailNotificationMessage setBcc(Recipient...recipients) {
        return setBcc(recipients != null ? Arrays.asList(recipients) : null);
    }

    public EmailNotificationMessage setBcc(List<Recipient> recipients) {
        bcc = null;
        if (recipients == null) {
            return this;
        }

        return addBcc(recipients);
    }

    public EmailNotificationMessage addBcc(String...address) {
        return addBcc(Arrays.stream(address).map(Recipient::new).collect(Collectors.toList()));
    }

    public EmailNotificationMessage addBcc(Recipient...recipients) {
        return addBcc(Arrays.asList(recipients));
    }

    public EmailNotificationMessage addBcc(List<Recipient> recipients) {
        if (bcc == null) {
            bcc = new ArrayList<>();
        }

        to.addAll(recipients);
        return this;
    }
}
