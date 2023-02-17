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
package org.openremote.agent.protocol.mail;

import javax.mail.*;
import java.util.ArrayList;
import java.util.Properties;

public class EmailClient {

    String protocol;
    String host;
    String port;
    String user;
    String password;

    public EmailClient(String protocol, String host, String port, String user, String password) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public List<Mail> receive() {
        Store emailStore = null;
        Folder emailFolder = null;

        Properties properties = new Properties();
        properties.put("mail.store.protocol", protocol);
        properties.put("mail." + protocol + ".host", host);
        properties.put("mail." + protocol + ".port", port);
        Session emailSession = Session.getInstance(properties);

        try {
            emailStore = emailSession.getStore();
            emailStore.connect(user, password);

            emailFolder = emailStore.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);

            return getNewMails(emailFolder);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (emailFolder != null && emailFolder.isOpen()) {
                    emailFolder.close(false);
                }
                if (emailStore != null && emailStore.isConnected()) {
                    emailStore.close();
                }
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<Mail> getNewMails(Folder emailFolder) throws MessagingException {
        List<Mail> mails = new ArrayList<>();
        for (Message message : emailFolder.getMessages()) {
            if (!message.getFlags().contains(Flags.Flag.SEEN)) {
                message.setFlags(new Flags(Flags.Flag.SEEN), true);
                Mail mail = MailMapper.map(message, user);
                mails.add(mail);
            }
        }
        return mails;
    }
}
