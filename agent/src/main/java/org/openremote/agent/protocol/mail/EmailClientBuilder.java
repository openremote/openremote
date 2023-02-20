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

import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

public class EmailClientBuilder {
    public static final String DEFAULT_FOLDER_NAME = "INBOX";
    public static final int DEFAULT_CHECK_INTERVAL_MILLIS = 5 * 60000;
    protected ScheduledExecutorService scheduledExecutorService;
    protected int checkIntervalMillis = DEFAULT_CHECK_INTERVAL_MILLIS;
    protected String folder;
    protected Date earliestMessageDate;
    protected Path earliestMessageDatePersistencePath;
    protected String protocol;
    protected String host;
    protected int port;
    protected String user;
    protected String password;
    protected boolean deleteMessageOnceProcessed;
    protected Properties properties = new Properties();

    public EmailClientBuilder(ScheduledExecutorService scheduledExecutorService, String protocol, String host, int port, String user, String password) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;

        properties.put("mail.store.protocol", protocol);
        properties.put("mail." + protocol + ".host", host);
        properties.put("mail." + protocol + ".port", port);
    }

    public EmailClientBuilder setCheckIntervalMillis(int checkIntervalMillis) {
        this.checkIntervalMillis = Math.max(checkIntervalMillis, 10000);
        return this;
    }

    public EmailClientBuilder setFolder(String folder) {
        this.folder = folder;
        return this;
    }

    public EmailClientBuilder setEarliestMessageDate(Date earliestMessageDate) {
        this.earliestMessageDate = earliestMessageDate;
        return this;
    }

    public EmailClientBuilder setEarliestMessageDatePersistencePath(Path earliestMessageDatePersistencePath) {
        this.earliestMessageDatePersistencePath = earliestMessageDatePersistencePath;
        return this;
    }

    public EmailClientBuilder setDeleteMessageOnceProcessed(boolean deleteMessageOnceProcessed) {
        this.deleteMessageOnceProcessed = deleteMessageOnceProcessed;
        return this;
    }

    public EmailClientBuilder setProperty(String property, Object value) {
        properties.put(property, value);
        return this;
    }

    public String getFolder() {
        return folder != null ? folder : DEFAULT_FOLDER_NAME;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public int getCheckIntervalMillis() {
        return checkIntervalMillis;
    }

    public Date getEarliestMessageDate() {
        return earliestMessageDate;
    }

    public Path getEarliestMessageDatePersistencePath() {
        return earliestMessageDatePersistencePath;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean isDeleteMessageOnceProcessed() {
        return deleteMessageOnceProcessed;
    }

    public Properties getProperties() {
        return properties;
    }

    public EmailClient build() {
        return new EmailClient(this);
    }
}
