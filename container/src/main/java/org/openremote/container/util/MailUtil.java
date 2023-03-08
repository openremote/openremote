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

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import java.io.IOException;

public class MailUtil {

    public static class MessageContent {

        protected String mimeType;
        protected String content;

        protected MessageContent(String mimeType, String content) {
            this.mimeType = mimeType;
            this.content = content;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getContent() {
            return content;
        }
    }

    protected MailUtil() {
    }

    public static MessageContent getMessageContent(Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            String mimeType = p.getContentType();
            return new MessageContent(mimeType, s);
        }

        if (p.isMimeType("multipart/alternative")) {
            // This mimetype means user agent should pick the most favourable message part
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = (String)bp.getContent();
                } else if (bp.isMimeType("text/html")) {
                    MessageContent content = getMessageContent(bp);
                    if (content != null) {
                        return content;
                    }
                } else {
                    return getMessageContent(bp);
                }
            }
            if (text != null) {
                return new MessageContent("text/plain", text);
            }
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                MessageContent content = getMessageContent(mp.getBodyPart(i));
                if (content != null)
                    return content;
            }
        }

        return null;
    }
}
