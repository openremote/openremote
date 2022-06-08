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

import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.EmailNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.NotificationSendResult;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.UserAssetPredicate;
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
import static org.openremote.manager.security.ManagerKeycloakIdentityProvider.KEYCLOAK_USER_ATTRIBUTE_EMAIL_NOTIFICATIONS_DISABLED;
import static org.openremote.model.Constants.*;

public class EmailNotificationHandler implements NotificationHandler {

    private static final Logger LOG = Logger.getLogger(EmailNotificationHandler.class.getName());
    protected String defaultFrom;
    protected Mailer mailer;
    protected Map<String, String> headers;
    protected ManagerIdentityService managerIdentityService;
    protected AssetStorageService assetStorageService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {

        managerIdentityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);

        // Configure SMTP
        String host = container.getConfig().getOrDefault(OR_EMAIL_HOST, null);
        int port = getInteger(container.getConfig(), OR_EMAIL_PORT, OR_EMAIL_PORT_DEFAULT);
        String user = container.getConfig().getOrDefault(OR_EMAIL_USER, null);
        String password = container.getConfig().getOrDefault(OR_EMAIL_PASSWORD, null);

        String headersStr = container.getConfig().getOrDefault(OR_EMAIL_X_HEADERS, null);
        if (!TextUtil.isNullOrEmpty(headersStr)) {
            headers = Arrays.stream(headersStr.split("\\R"))
                .map(s -> s.split(":", 2))
                .collect(Collectors.toMap(
                    arr -> arr[0].trim(),
                    arr -> arr.length == 2 ? arr[1].trim() : ""
                ));
        }

        defaultFrom = container.getConfig().getOrDefault(OR_EMAIL_FROM, OR_EMAIL_FROM_DEFAULT);

        if (!TextUtil.isNullOrEmpty(host) && !TextUtil.isNullOrEmpty(user) && !TextUtil.isNullOrEmpty(password)) {
            MailerBuilder.MailerRegularBuilder mailerBuilder = MailerBuilder.withSMTPServer(host, port, user, password);
            boolean startTls = getBoolean(container.getConfig(), OR_EMAIL_TLS, OR_EMAIL_TLS_DEFAULT);

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
                UserQuery userQuery = null;

                switch (targetType) {

                    case REALM:
                        userQuery = new UserQuery().realm(new RealmPredicate(targetId));
                        break;
                    case USER:
                        userQuery = new UserQuery().ids(targetId);
                        break;
                    case CUSTOM:
                        // Nothing to do here
                        mappedTargets.add(new Notification.Target(targetType, targetId));
                        break;
                    case ASSET:
                        // If asset has an email attribute include that in the targets
                        Asset<?> asset = assetStorageService.find(targetId);
                        if (asset != null) {
                            asset.getEmail().map(email -> {
                                    Notification.Target assetTarget = new Notification.Target(Notification.TargetType.ASSET, asset.getId());
                                    assetTarget.setData(new EmailNotificationMessage.Recipient(asset.getName(), email));
                                    return assetTarget;
                                }
                            ).ifPresent(mappedTargets::add);
                        }

                        userQuery = new UserQuery().asset(new UserAssetPredicate(targetId));
                        break;
                }

                // Filter users that don't have disabled email notifications attribute
                if (userQuery != null) {
                    List<Notification.Target> userTargets = Arrays.stream(managerIdentityService
                            .getIdentityProvider()
                            .queryUsers(userQuery))
                        .filter(user -> !Boolean.parseBoolean(user.getAttributes().getOrDefault(KEYCLOAK_USER_ATTRIBUTE_EMAIL_NOTIFICATIONS_DISABLED, Collections.singletonList("false")).get(0)))
                        .filter(user -> !TextUtil.isNullOrEmpty(user.getEmail()))
                        .map(user -> {
                            Notification.Target emailTarget = new Notification.Target(Notification.TargetType.USER, user.getId());
                            emailTarget.setData(new EmailNotificationMessage.Recipient(user.getFullName(), user.getEmail()));
                            return emailTarget;
                        }).toList();

                    if (userTargets.isEmpty()) {
                        LOG.info("No email targets have been mapped");
                    } else {
                        mappedTargets.addAll(
                            userTargets
                                .stream()
                                .filter(userTarget -> mappedTargets.stream().noneMatch(t -> t.getId().equals(userTarget.getId())))
                                .toList());
                    }
                }
            });
        }

        EmailNotificationMessage email = (EmailNotificationMessage)message;

        // Map to/cc/bcc into a custom target for traceability in sent notifications
        List<String> addresses = new ArrayList<>();

        if (email.getTo() != null) {
            addresses.addAll(
                email.getTo().stream()
                    .map(EmailNotificationMessage.Recipient::getAddress)
                    .map(address -> "to:" + address).toList());

            email.setTo((List<EmailNotificationMessage.Recipient>) null);
        }
        if (email.getCc() != null) {
            addresses.addAll(
                email.getCc().stream()
                    .map(EmailNotificationMessage.Recipient::getAddress)
                    .map(address -> "cc:" + address).toList());

            email.setCc((List<EmailNotificationMessage.Recipient>) null);
        }
        if (email.getBcc() != null) {
            addresses.addAll(
                email.getBcc().stream()
                    .map(EmailNotificationMessage.Recipient::getAddress)
                    .map(address -> "bcc:" + address).toList());

            email.setBcc((List<EmailNotificationMessage.Recipient>) null);
        }

        if (!addresses.isEmpty()) {
            mappedTargets.add(new Notification.Target(Notification.TargetType.CUSTOM, String.join(";", addresses)));
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

        List<EmailNotificationMessage.Recipient> toRecipients = new ArrayList<>();
        List<EmailNotificationMessage.Recipient> ccRecipients = new ArrayList<>();
        List<EmailNotificationMessage.Recipient> bccRecipients = new ArrayList<>();
        Notification.TargetType targetType = target.getType();
        String targetId = target.getId();

        switch (targetType) {

            case USER:
            case ASSET:
                // Recipient should be stored from earlier mapping call
                EmailNotificationMessage.Recipient recipient = (EmailNotificationMessage.Recipient)target.getData();

                if (recipient == null) {
                    LOG.warning("User or asset recipient missing: id=" + targetId);
                } else {
                    LOG.finest("Adding to recipient: " + recipient);
                    toRecipients.add(recipient);
                }
                break;
            case CUSTOM:
                // This recipient list is the target ID
                Arrays.stream(targetId.split(";")).forEach(customRecipient -> {
                    if (customRecipient.startsWith("to:")) {
                        String email = customRecipient.substring(3);
                        LOG.finest("Adding to recipient: " + email);
                        toRecipients.add(new EmailNotificationMessage.Recipient(email));
                    } else if (customRecipient.startsWith("cc:")) {
                        String email = customRecipient.substring(3);
                        LOG.finest("Adding cc recipient: " + email);
                        ccRecipients.add(new EmailNotificationMessage.Recipient(email));
                    } else if (customRecipient.startsWith("bcc:")) {
                        String email = customRecipient.substring(4);
                        LOG.finest("Adding bcc recipient: " + email);
                        bccRecipients.add(new EmailNotificationMessage.Recipient(email));
                    } else {
                        LOG.finest("Adding to recipient: " + customRecipient);
                        toRecipients.add(new EmailNotificationMessage.Recipient(customRecipient));
                    }
                });

                break;
            default:
                LOG.warning("Target type not supported: " + targetType);
        }

        EmailNotificationMessage emailNotificationMessage = (EmailNotificationMessage) message;
        EmailPopulatingBuilder emailBuilder = buildEmailBuilder(id, emailNotificationMessage);

        if (!toRecipients.isEmpty()) {
            toRecipients.forEach(recipient -> {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    emailBuilder.to(convertRecipient(recipient));
                }
            });
        }
        if (!ccRecipients.isEmpty()) {
            ccRecipients.forEach(recipient -> {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    emailBuilder.cc(convertRecipient(recipient));
                }
            });
        }
        if (!bccRecipients.isEmpty()) {
            bccRecipients.forEach(recipient -> {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    emailBuilder.bcc(convertRecipient(recipient));
                }
            });
        }

        if (emailBuilder.getRecipients().isEmpty()) {
            return NotificationSendResult.failure("No recipients set for " + targetType.name().toLowerCase() + ": " + targetId);
        }

        // Set from based on source if not already set
        if (emailBuilder.getFromRecipient() == null) {
            emailBuilder.from(defaultFrom);
        }

        return sendMessage(emailBuilder.buildEmail());
    }

    protected NotificationSendResult sendMessage(Email email) {
        try {
            mailer.sendMail(email);
            return NotificationSendResult.success();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Email send failed: " + e.getMessage(), e);
            return NotificationSendResult.failure("Email send failed: " + e.getMessage());
        }
    }

    protected EmailPopulatingBuilder buildEmailBuilder(long id, EmailNotificationMessage emailNotificationMessage) {
        EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
            .withReplyTo(convertRecipient(emailNotificationMessage.getReplyTo()))
            .withSubject(emailNotificationMessage.getSubject())
            .withPlainText(emailNotificationMessage.getText())
            .withHTMLText(emailNotificationMessage.getHtml());

        if (headers != null) {
            emailBuilder.withHeaders(headers);
        }

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
