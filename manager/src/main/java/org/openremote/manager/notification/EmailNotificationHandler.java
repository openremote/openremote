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
package org.openremote.manager.notification;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.EmailNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.NotificationSendResult;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.*;
import org.openremote.model.security.User;
import org.openremote.model.util.TextUtil;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.email.EmailPopulatingBuilder;
import org.simplejavamail.email.Recipient;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.model.Constants.*;

public class EmailNotificationHandler implements NotificationHandler {

    private static final Logger LOG = Logger.getLogger(EmailNotificationHandler.class.getName());
    protected String defaultFrom;
    protected Mailer mailer;
    protected ManagerIdentityService managerIdentityService;
    protected AssetStorageService assetStorageService;
    // Keep 100 user email addresses in cache for quick lookup
    protected LinkedHashMap<String, EmailNotificationMessage.Recipient> userEmails = new LinkedHashMap<String, EmailNotificationMessage.Recipient>(100) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, EmailNotificationMessage.Recipient> eldest) {
            return size() > 100;
        }
    };
    // Keep 1000 asset email addresses in cache for quick lookup
    protected LinkedHashMap<String, EmailNotificationMessage.Recipient> assetEmails = new LinkedHashMap<String, EmailNotificationMessage.Recipient>(1000) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, EmailNotificationMessage.Recipient> eldest) {
            return size() > 1000;
        }
    };

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {

        managerIdentityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);

        // Configure SMTP
        String host = container.getConfig().getOrDefault(SETUP_EMAIL_HOST, null);
        int port = getInteger(container.getConfig(), SETUP_EMAIL_PORT, SETUP_EMAIL_PORT_DEFAULT);
        String user = container.getConfig().getOrDefault(SETUP_EMAIL_USER, null);
        String password = container.getConfig().getOrDefault(SETUP_EMAIL_PASSWORD, null);

        defaultFrom = container.getConfig().getOrDefault(SETUP_EMAIL_FROM, SETUP_EMAIL_FROM_DEFAULT);

        if (!TextUtil.isNullOrEmpty(host)) {
            MailerBuilder.MailerRegularBuilder mailerBuilder = MailerBuilder.withSMTPServer(host, port, user, password);
            boolean startTls = getBoolean(container.getConfig(), SETUP_EMAIL_TLS, SETUP_EMAIL_TLS_DEFAULT);

            mailerBuilder.withTransportStrategy(startTls ? TransportStrategy.SMTP_TLS : TransportStrategy.SMTP);
            mailer = mailerBuilder.buildMailer();
            try {
                mailer.testConnection();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to connect to SMTP server so disabling email notifications", e);
                mailer = null;
            }
        }
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public boolean isValid() {
        return mailer != null;
    }

    @Override
    public String getTypeName() {
        return EmailNotificationMessage.TYPE;
    }

    @Override
    public boolean isMessageValid(AbstractNotificationMessage message) {
        return (message instanceof EmailNotificationMessage);
//        if (!(message instanceof EmailNotificationMessage)) {
//            LOG.warning("Invalid message: '" + message.getClass().getSimpleName() + "' is not an instance of PushNotificationMessage");
//            return false;
//        }
//
//        EmailNotificationMessage emailMessage = (EmailNotificationMessage) message;
//        if (emailMessage.getFrom() == null || (
//                (emailMessage.getTo() == null || emailMessage.getTo().isEmpty())
//                        && (emailMessage.getCc() == null || emailMessage.getCc().isEmpty())
//                        && (emailMessage.getBcc() == null || emailMessage.getBcc().isEmpty()))) {
//            LOG.warning("Invalid message: must contain a from and at least one recipient");
//            return false;
//        }
//
//        return true;
    }

    @Override
    public List<Notification.Target> getTargets(Notification.Source source, String sourceId, List<Notification.Target> targets, AbstractNotificationMessage message) {

        List<Notification.Target> mappedTargets = new ArrayList<>();

        if (targets != null) {

            targets.forEach(target -> {
                Notification.TargetType targetType = target.getType();
                String targetId = target.getId();

                switch (targetType) {

                    case TENANT:
                        // Find all users in this tenant
                        User[] users = managerIdentityService
                            .getIdentityProvider()
                            .getUsers(new UserQuery().tenant(new TenantPredicate(targetId)));

                        if (users.length == 0) {
                            LOG.fine("No users found in target realm: " + targetId);
                            return;
                        }

                        Arrays.stream(users).forEach(user -> {
                            EmailNotificationMessage.Recipient recipient =
                                new EmailNotificationMessage.Recipient(user.getFullName(), user.getEmail());
                            userEmails.put(user.getId(), recipient);
                        });

                        mappedTargets.addAll(
                            Arrays.stream(users)
                                .map(user -> new Notification.Target(Notification.TargetType.USER, user.getId()))
                                .collect(Collectors.toList()));
                        break;
                    case USER:
                        // Nothing to do here
                        mappedTargets.add(new Notification.Target(targetType, targetId));
                        break;
                    case ASSET:
                        // Find descendant assets with email attribute
                        List<Asset> assets = assetStorageService.findAll(
                            new AssetQuery()
                                .select(AssetQuery.Select.selectExcludePathAndParentInfo()
                                    .attributes(AttributeType.EMAIL.getAttributeName()))
                                .paths(new PathPredicate(targetId))
                                .attributes(new AttributePredicate(
                                    new StringPredicate(AttributeType.EMAIL.getAttributeName()),
                                    new ValueNotEmptyPredicate())));

                        if (assets.isEmpty()) {
                            LOG.fine("No assets with email attribute descendants of target asset");
                            return;
                        }

                        assets.forEach(asset -> {
                            EmailNotificationMessage.Recipient recipient =
                                new EmailNotificationMessage.Recipient(
                                    asset.getName(),
                                    asset.getAttribute(AttributeType.EMAIL)
                                        .flatMap(AbstractValueHolder::getValueAsString)
                                        .orElse(null));
                            assetEmails.put(asset.getId(), recipient);
                        });

                        mappedTargets.addAll(assets.stream()
                            .map(asset -> new Notification.Target(Notification.TargetType.ASSET, asset.getId()))
                            .collect(Collectors.toList()));
                        break;
                }
            });
        }
        EmailNotificationMessage email = (EmailNotificationMessage)message;

        // Move to/cc/bcc to targets (for traceability) in sent notifications
        if (email.getTo() != null) {
            mappedTargets.addAll(
                email.getTo().stream()
                    .map(recipient ->
                        new Notification.Target(Notification.TargetType.CUSTOM, recipient.getAddress()))
                    .collect(Collectors.toList()));
            email.setTo((List<EmailNotificationMessage.Recipient>) null);
        }
        if (email.getCc() != null) {
            mappedTargets.addAll(
                email.getCc().stream()
                    .map(recipient ->
                        new Notification.Target(Notification.TargetType.CUSTOM, recipient.getAddress()))
                    .collect(Collectors.toList()));
            email.setCc((List<EmailNotificationMessage.Recipient>) null);
        }
        if (email.getBcc() != null) {
            mappedTargets.addAll(
                email.getBcc().stream()
                    .map(recipient ->
                        new Notification.Target(Notification.TargetType.CUSTOM, recipient.getAddress()))
                    .collect(Collectors.toList()));
            email.setBcc((List<EmailNotificationMessage.Recipient>) null);
        }

        return mappedTargets;
    }

    @Override
    public NotificationSendResult sendMessage(long id, Notification.Source source, String sourceId, Notification.Target target, AbstractNotificationMessage message) {

        // Check handler is valid
        if (!isValid()) {
            LOG.warning("SMTP invalid configuration so ignoring");
            return NotificationSendResult.failure("SMTP invalid configuration so ignoring");
        }

        EmailNotificationMessage.Recipient recipient = null;
        Notification.TargetType targetType = target.getType();
        String targetId = target.getId();

        switch (targetType) {

            case USER:
                recipient = getUserRecipient(targetId);
                break;
            case ASSET:
                recipient = getAssetRecipient(targetId);
                break;
            case CUSTOM:
                // This recipient is the target ID
                recipient = new EmailNotificationMessage.Recipient(targetId);
                break;
            default:
                LOG.warning("Target type not supported: " + targetType);
        }

        EmailNotificationMessage emailNotificationMessage = (EmailNotificationMessage) message;
        EmailPopulatingBuilder emailBuilder = buildEmailBuilder(id, emailNotificationMessage);

        if (recipient != null && !TextUtil.isNullOrEmpty(recipient.getAddress())) {
            emailBuilder.to(convertRecipient(recipient));
        }

        if ((emailNotificationMessage.getTo() == null || emailNotificationMessage.getTo().isEmpty())
            && (emailNotificationMessage.getCc() == null || emailNotificationMessage.getCc().isEmpty())
            && (emailNotificationMessage.getBcc() == null || emailNotificationMessage.getBcc().isEmpty())
            && recipient == null) {
            LOG.warning("No recipient found for " + targetType.name().toLowerCase() + ": " + targetId);
            return NotificationSendResult.failure("No recipients set for " + targetType.name().toLowerCase() + ": " + targetId);
        }

        // Set from based on source if not already set
        if (emailNotificationMessage.getFrom() == null) {
            emailBuilder.from(defaultFrom);
        }

        return sendMessage(emailBuilder.buildEmail());
    }

    public NotificationSendResult sendMessage(Email email) {
        try {
            mailer.sendMail(email);
            return NotificationSendResult.success();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Email send failed: " + e.getMessage(), e);
            return NotificationSendResult.failure("Email send failed: " + e.getMessage());
        }
    }

    protected EmailNotificationMessage.Recipient getUserRecipient(String userId) {
        if (userEmails.containsKey(userId)) {
            return userEmails.get(userId);
        }

        User[] users = managerIdentityService.getIdentityProvider().getUsers(Collections.singletonList(userId));
        if (users == null || users.length == 0) {
            return null;
        }
        return new EmailNotificationMessage.Recipient(users[0].getFullName(), users[0].getEmail());
    }

    protected EmailNotificationMessage.Recipient getAssetRecipient(String assetId) {
        if (assetEmails.containsKey(assetId)) {
            return assetEmails.get(assetId);
        }

        Asset asset = assetStorageService.find(assetId);
        if (asset == null) {
            return null;
        }

        return new EmailNotificationMessage.Recipient(asset.getName(), asset.getAttribute(AttributeType.EMAIL)
            .flatMap(AbstractValueHolder::getValueAsString)
            .orElse(null));
    }

    protected EmailPopulatingBuilder buildEmailBuilder(long id, EmailNotificationMessage emailNotificationMessage) {
        EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
            .withReplyTo(convertRecipient(emailNotificationMessage.getReplyTo()))
            .withSubject(emailNotificationMessage.getSubject())
            .withPlainText(emailNotificationMessage.getText())
            .withHTMLText(emailNotificationMessage.getHtml());

        if (emailNotificationMessage.getFrom() != null) {
            emailBuilder.from(convertRecipient(emailNotificationMessage.getFrom()));
        }

        if (emailNotificationMessage.getTo() != null) {
            emailBuilder.to(
                emailNotificationMessage.getTo().stream()
                    .map(this::convertRecipient).collect(Collectors.toList()));
        }

        if (emailNotificationMessage.getCc() != null) {
            emailBuilder.cc(
                emailNotificationMessage.getCc().stream()
                    .map(this::convertRecipient).collect(Collectors.toList()));
        }

        if (emailNotificationMessage.getBcc() != null) {
            emailBuilder.bcc(
                emailNotificationMessage.getBcc().stream()
                    .map(this::convertRecipient).collect(Collectors.toList()));
        }

        // Use the notification ID as the message ID
        emailBuilder.fixingMessageId("<" + id + "@openremote.io>");

        return emailBuilder;
    }

    protected Recipient convertRecipient(EmailNotificationMessage.Recipient recipient) {
        return recipient == null ? null : new Recipient(recipient.getName(), recipient.getAddress(), null);
    }
}
