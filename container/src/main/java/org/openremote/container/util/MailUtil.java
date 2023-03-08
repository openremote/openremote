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
package org.openremote.container.util;

import org.openremote.model.mail.MailMessage;

import javax.mail.*;
import javax.mail.internet.InternetAddress;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MailUtil {

    protected static class MessageContent {

        protected List<Header> headers;
        protected String mimeType;
        protected String content;

        protected MessageContent(String mimeType, String content, List<Header> headers) {
            this.mimeType = mimeType;
            this.content = content;
            this.headers = headers;
        }
    }

    protected MailUtil() {
    }

    public static MailMessage toMailMessage(Message message) throws MessagingException, IOException {
        MessageContent messageContent = getMessageContent(message, new ArrayList<>(), true);
        if (messageContent == null) {
            return null;
        }

        List<Header> headers = messageContent.headers;
        Map<String, List<String>> headerStrings = headers.stream()
            .collect(Collectors.groupingBy(
                Header::getName,
                Collectors.mapping(Header::getValue, Collectors.toList())));

        return new MailMessage(
            messageContent.content,
            messageContent.mimeType,
            headerStrings,
            message.getSubject(),
            message.getSentDate(),
            Arrays.stream(message.getFrom()).map(a -> ((InternetAddress)a).getAddress()).toArray(String[]::new));
    }

    protected static MessageContent getMessageContent(Part p, List<Header> headers, boolean isTopLevel) throws MessagingException, IOException {

        if (isTopLevel) {
            headers.addAll(Collections.list(p.getAllHeaders()));
        }

        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            String mimeType = p.getContentType();
            return new MessageContent(mimeType, s, headers);
        }

        if (p.isMimeType("multipart/alternative")) {
            // This mimetype means user agent should pick the most favourable message part
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            Enumeration<Header> partHeaders = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null) {
                        text = (String) bp.getContent();
                        partHeaders = bp.getAllHeaders();
                    }
                } else if (bp.isMimeType("text/html")) {
                    MessageContent content = getMessageContent(bp, headers, false);
                    if (content != null) {
                        return content;
                    }
                } else {
                    return getMessageContent(bp, headers, false);
                }
            }
            if (text != null) {
                headers.addAll(Collections.list(partHeaders));
                return new MessageContent("text/plain", text, headers);
            }
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                MessageContent content = getMessageContent(mp.getBodyPart(i), headers, false);
                if (content != null)
                    return content;
            }
        }

        return null;
    }
}
