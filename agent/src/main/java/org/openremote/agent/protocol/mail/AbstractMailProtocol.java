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

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.mail.MailMessage;
import org.openremote.model.syslog.SyslogCategory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractMailProtocol<T extends AbstractMailAgent<T, U, V>, U extends AbstractMailProtocol<T, U, V>, V extends AgentLink<V>> extends AbstractProtocol<T, V> {
    protected MailClient mailClient;
    protected ConcurrentMap<AttributeRef, Function<MailMessage, String>> attributeMessageProcessorMap = new ConcurrentHashMap<>();
    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractMailProtocol.class);
    protected static int INITIAL_CHECK_DELAY_SECONDS = 10;

    public AbstractMailProtocol(T agent) {
        super(agent);
    }

    @Override
    protected void doStart(Container container) throws Exception {

        Path storageDir = container.getService(PersistenceService.class).getStorageDir();

        Path persistenceDir = storageDir.resolve("protocol").resolve("mail");
        Optional<OAuthGrant> oAuthGrant = getAgent().getOAuthGrant();
        UsernamePassword userPassword = getAgent().getUsernamePassword().orElseThrow();

        MailClientBuilder clientBuilder = new MailClientBuilder(
            container.getExecutorService(),
            getAgent().getProtocol().orElseThrow(),
            getAgent().getHost().orElseThrow(),
            getAgent().getPort().orElseThrow()
        )
            .setCheckIntervalSeconds(
                getAgent().getCheckIntervalSeconds().orElse(MailClientBuilder.DEFAULT_CHECK_INTERVAL_SECONDS)
            )
            .setDeleteMessageOnceProcessed(
                getAgent().getDeleteProcessedMail().orElse(false)
            )
            .setFolder(getAgent().getMailFolderName().orElse(null))
            .setPersistenceDir(persistenceDir)
            // Set an initial delay to allow attributes to be linked before we read messages - not perfect but it should do
            .setCheckInitialDelaySeconds(INITIAL_CHECK_DELAY_SECONDS)
            .setPreferHTML(getAgent().getPreferHTML().orElse(false))
            .setEarliestMessageDate(agent.getCreatedOn());

        oAuthGrant.map(oAuth -> clientBuilder.setOAuth(userPassword.getUsername(), oAuth)).orElseGet(() ->
            clientBuilder.setBasicAuth(userPassword.getUsername(), userPassword.getPassword()));

        mailClient = clientBuilder.build();
        mailClient.addConnectionListener(this::onConnectionEvent);
        mailClient.addMessageListener(this::onMailMessage);
        mailClient.connect();
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (mailClient != null) {
            mailClient.disconnect();
        }
        setConnectionStatus(ConnectionStatus.STOPPED);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, V agentLink) throws RuntimeException {

        Predicate<MailMessage> messageFilter = getAttributeMailMessageFilter(assetId, attribute, agentLink);

        Function<MailMessage, String> mailMessageProcessor = mailMessage -> {
            if (messageFilter == null || messageFilter.test(mailMessage)) {
                return getMailMessageAttributeValue(assetId, attribute, agentLink, mailMessage);
            }

            return null;
        };

        attributeMessageProcessorMap.put(new AttributeRef(assetId, attribute.getName()), mailMessageProcessor);
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, V agentLink) {
        attributeMessageProcessorMap.remove(new AttributeRef(assetId, attribute.getName()));
    }

    @Override
    protected void doLinkedAttributeWrite(V agentLink, AttributeEvent event, Object processedValue) {
        // Not supported
    }

    protected void onConnectionEvent(ConnectionStatus status) {
        setConnectionStatus(status);
    }

    protected void onMailMessage(MailMessage mailMessage) {
        attributeMessageProcessorMap.forEach(((attributeRef, mailMessageStringFunction) -> {
            String value = mailMessageStringFunction.apply(mailMessage);
            if (value != null) {
                updateLinkedAttribute(new AttributeState(attributeRef, value));
            } else if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "MailMessage failed to match linked attribute:" + attributeRef + ", " + mailMessage);
            }
        }));
    }

    protected abstract Predicate<MailMessage> getAttributeMailMessageFilter(String assetId, Attribute<?> attribute, V agentLink);

    protected abstract String getMailMessageAttributeValue(String assetId, Attribute<?> attribute, V agentLink, MailMessage mailMessage);
}
