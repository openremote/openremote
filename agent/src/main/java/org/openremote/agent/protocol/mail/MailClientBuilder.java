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

import org.openremote.container.web.OAuthFilter;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.UsernamePassword;

import jakarta.ws.rs.client.Client;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class MailClientBuilder {
    public static final String DEFAULT_FOLDER_NAME = "INBOX";
    public static final int DEFAULT_CHECK_INTERVAL_SECONDS = 5 * 60;
    protected static int MIN_CHECK_INTERVAL_SECONDS = 10;
    protected static final AtomicReference<Client> jaxrsClient = new AtomicReference<>();
    protected ScheduledExecutorService scheduledExecutorService;
    protected int checkInitialDelaySeconds = 0;
    protected int checkIntervalSeconds = DEFAULT_CHECK_INTERVAL_SECONDS;
    protected String folder;
    protected boolean preferHTML;
    protected Date earliestMessageDate;
    protected Path persistenceDir;
    protected String protocol;
    protected String host;
    protected OAuthGrant oAuthGrant;
    protected OAuthFilter oAuthFilter;
    protected int port;
    protected String user;
    protected String password;
    protected boolean deleteMessageOnceProcessed;
    protected Properties properties = new Properties();

    public MailClientBuilder(ScheduledExecutorService scheduledExecutorService, String protocol, String host, int port) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.protocol = protocol;
        this.host = host;
        this.port = port;

        properties.put("mail.store.protocol", protocol);
        properties.put("mail." + protocol + ".host", host);
        properties.put("mail." + protocol + ".port", port);
    }

    public MailClientBuilder setBasicAuth(String user, String password) {
        this.user = user;
        this.password = password;
        return this;
    }

    public MailClientBuilder setOAuth(String user, OAuthGrant oAuthGrant) {
        this.user = user;
        this.oAuthGrant = oAuthGrant;
        setProperty("auth.mechanisms", "XOAUTH2");
        setProperty("mail.imap.sasl.enable", "true");
        setProperty("auth.login.disable", "true");
        setProperty("auth.plain.disable", "true");
        return this;
    }

    public MailClientBuilder setCheckIntervalSeconds(int checkIntervalSeconds) {
        this.checkIntervalSeconds = Math.max(checkIntervalSeconds, MIN_CHECK_INTERVAL_SECONDS);
        return this;
    }

    public MailClientBuilder setCheckInitialDelaySeconds(int checkInitialDelaySeconds) {
        this.checkInitialDelaySeconds = Math.max(checkInitialDelaySeconds, 0);
        return this;
    }

    public MailClientBuilder setFolder(String folder) {
        this.folder = folder;
        return this;
    }

    public MailClientBuilder setEarliestMessageDate(Date earliestMessageDate) {
        this.earliestMessageDate = earliestMessageDate;
        return this;
    }

    /**
     * Directory to store information about received message dates between restarts
     */
    public MailClientBuilder setPersistenceDir(Path persistenceDir) {
        this.persistenceDir = persistenceDir;
        return this;
    }

    public MailClientBuilder setDeleteMessageOnceProcessed(boolean deleteMessageOnceProcessed) {
        this.deleteMessageOnceProcessed = deleteMessageOnceProcessed;
        return this;
    }

    public MailClientBuilder setProperty(String property, Object value) {
        if (!property.startsWith("mail." + protocol + ".")) {
            property = "mail." + protocol + "." + property;
        }
        properties.put(property, value);
        return this;
    }

    public MailClientBuilder setPreferHTML(boolean preferHTML) {
        this.preferHTML = preferHTML;
        return this;
    }

    public String getFolder() {
        return folder != null ? folder : DEFAULT_FOLDER_NAME;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public int getCheckInitialDelaySeconds() {
        return checkInitialDelaySeconds;
    }

    public Date getEarliestMessageDate() {
        return earliestMessageDate;
    }

    public Path getPersistenceDir() {
        return persistenceDir;
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

    public OAuthGrant getOAuthGrant() {
        return oAuthGrant;
    }

    public boolean isDeleteMessageOnceProcessed() {
        return deleteMessageOnceProcessed;
    }

    public boolean isPreferHTML() {
        return preferHTML;
    }

    public Properties getProperties() {
        return properties;
    }

    public MailClient build() {
        return new MailClient(this);
    }

    UsernamePassword getAuth() throws SocketException, NullPointerException {
        Objects.requireNonNull(user, "User must be supplied");

        if (oAuthGrant != null) {
            synchronized (jaxrsClient) {
                if (jaxrsClient.get() == null) {
                    jaxrsClient.set(WebTargetBuilder.createClient(scheduledExecutorService));
                }
            }
            synchronized (this) {
                if (oAuthFilter == null) {
                    oAuthFilter = new OAuthFilter(jaxrsClient.get(), oAuthGrant);
                }
            }
            return new UsernamePassword(user, oAuthFilter.getAccessToken());
        }

        Objects.requireNonNull(password, "Password must be supplied");

        return new UsernamePassword(user, password);
    }
}
