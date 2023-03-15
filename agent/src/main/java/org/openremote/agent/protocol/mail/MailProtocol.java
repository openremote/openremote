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

import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.mail.MailMessage;

import javax.mail.event.ConnectionEvent;
import java.nio.file.Path;
import java.util.List;

public class MailProtocol extends AbstractMailProtocol<MailAgent, MailProtocol, MailAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "Mail Client";
    protected MailClient mailClient;

    public MailProtocol(MailAgent agent) {
        super(agent);
    }

    @Override
    protected void doStart(Container container) throws Exception {

        UsernamePassword usernamePassword = getAgent().getUsernamePassword().orElseThrow();
        Path storageDir = container.getService(PersistenceService.class).getStorageDir();

        Path persistenceDir = storageDir.resolve("protocol").resolve("mail");

        mailClient = new MailClientBuilder(
            container.getExecutorService(),
            getAgent().getProtocol().orElseThrow(),
            getAgent().getHost().orElseThrow(),
            getAgent().getPort().orElseThrow(),
            usernamePassword.getUsername(),
            usernamePassword.getPassword()
        )
            .setCheckIntervalMillis(
                getAgent().getCheckIntervalSeconds().orElse(MailClientBuilder.DEFAULT_CHECK_INTERVAL_MILLIS)
            )
            .setDeleteMessageOnceProcessed(
                getAgent().getDeleteProcessedMail().orElse(false)
            )
            .setFolder(getAgent().getMailFolderName().orElse(null))
            .setPersistenceDir(persistenceDir)
            .setCheckInitialDelayMillis(10000) // Set an initial delay to allow attributes to be linked
            .setPreferHTML(getAgent().getPreferHTML().orElse(false))
            .build();

        mailClient.addConnectionListener(this::onConnectionEvent);
        mailClient.addMessageListener(this::onMailMessage);
    }

    @Override
    public void startComplete() {
        if (!mailClient.connect()) {
            setConnectionStatus(ConnectionStatus.ERROR);
        }
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (mailClient != null) {
            mailClient.disconnect();
        }
        setConnectionStatus(ConnectionStatus.STOPPED);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, MailAgentLink agentLink) throws RuntimeException {

    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, MailAgentLink agentLink) {

    }

    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, MailAgentLink agentLink, AttributeEvent event, Object processedValue) {

    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "mailClient://" + mailClient.config;
    }

    protected void onConnectionEvent(ConnectionEvent event) {
        if (event.getType() == ConnectionEvent.OPENED) {
            setConnectionStatus(ConnectionStatus.CONNECTED);
        } else if (event.getType() == ConnectionEvent.CLOSED) {
            setConnectionStatus(ConnectionStatus.WAITING);
        }
    }

    protected void onMailMessage(MailMessage mailMessage) {

    }
}
