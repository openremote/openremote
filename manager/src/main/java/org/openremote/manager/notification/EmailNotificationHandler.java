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

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.EmailNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.security.User;
import org.openremote.model.util.TextUtil;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.util.MapAccess.*;
import static org.openremote.model.Constants.*;
import static org.openremote.model.security.User.EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE;

public class EmailNotificationHandler implements NotificationHandler {

    private static final Logger LOG = Logger.getLogger(EmailNotificationHandler.class.getName());
    protected String defaultFrom;
    protected Session mailSession;
    protected Transport mailTransport;
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
            boolean startTls = getBoolean(container.getConfig(), OR_EMAIL_TLS, OR_EMAIL_TLS_DEFAULT);
            String protocol = startTls ? "smtp" : getString(container.getConfig(), OR_EMAIL_PROTOCOL, OR_EMAIL_PROTOCOL_DEFAULT);
            Properties props = new Properties();
            props.put("mail." + protocol + ".auth", true);
            props.put("mail." + protocol + ".starttls.enable", startTls);
            props.put("mail." + protocol + ".host", host);
            props.put("mail." + protocol + ".port", port);

            mailSession = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            });

            boolean valid;

            try {
                mailTransport = mailSession.getTransport(protocol);
                mailTransport.connect();
                valid = mailTransport.isConnected();
                try {
                    mailTransport.close();
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                valid = false;
                LOG.log(Level.SEVERE, "Failed to connect to SMTP server so disabling email notifications", e);
            }

            if (!valid) {
                mailTransport = null;
                mailSession = null;
                LOG.log(Level.INFO, "SMTP credentials are not valid so email notifications will not function");
            }
        }
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {
        if (mailTransport != null) {
            try {
                mailTransport.close();
            } catch (Exception ignored) {
            }
        }
        mailSession = null;
    }

    @Override
    public boolean isValid() {
        return mailTransport != null;
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

                        userQuery = new UserQuery().assets(targetId);
                        break;
                }

                // Filter users that don't have disabled email notifications attribute
                if (userQuery != null) {
                    // Exclude service accounts, system accounts and accounts with disabled email notifications
                    userQuery.serviceUsers(false).attributes(
                        new UserQuery.AttributeValuePredicate(true, new StringPredicate(User.SYSTEM_ACCOUNT_ATTRIBUTE)),
                        new UserQuery.AttributeValuePredicate(true, new StringPredicate(EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE), new StringPredicate("true"))
                    );
                    List<Notification.Target> userTargets = Arrays.stream(managerIdentityService
                            .getIdentityProvider()
                            .queryUsers(userQuery))
                        // Exclude system accounts and accounts without emails
                        .filter(user -> !user.isSystemAccount() && !TextUtil.isNullOrEmpty(user.getEmail()))
                        .map(user -> {
                            Notification.Target emailTarget = new Notification.Target(Notification.TargetType.USER, user.getId());
                            emailTarget.setData(new EmailNotificationMessage.Recipient(user.getFullName(), user.getEmail()));
                            return emailTarget;
                        }).toList();

                    if (userTargets.isEmpty()) {
                        LOG.fine("No email targets have been mapped");
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
    public void sendMessage(long id, Notification.Source source, String sourceId, Notification.Target target, AbstractNotificationMessage message) throws Exception {

        List<EmailNotificationMessage.Recipient> toRecipients = new ArrayList<>();
        List<EmailNotificationMessage.Recipient> ccRecipients = new ArrayList<>();
        List<EmailNotificationMessage.Recipient> bccRecipients = new ArrayList<>();
        Notification.TargetType targetType = target.getType();
        String targetId = target.getId();

        switch (targetType) {
            case USER, ASSET -> {
                // Recipient should be stored from earlier mapping call
                EmailNotificationMessage.Recipient recipient = (EmailNotificationMessage.Recipient) target.getData();
                if (recipient == null) {
                    LOG.warning("User or asset recipient missing: id=" + targetId);
                } else {
                    LOG.finest("Adding to recipient: " + recipient);
                    toRecipients.add(recipient);
                }
            }
            case CUSTOM ->
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
            default -> LOG.warning("Target type not supported: " + targetType);
        }

        MimeMessage email = new MimeMessage(mailSession);

        if (!toRecipients.isEmpty()) {
            for (EmailNotificationMessage.Recipient recipient : toRecipients) {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    email.addRecipient(Message.RecipientType.TO, convertRecipient(recipient));
                }
            }
        }
        if (!ccRecipients.isEmpty()) {
            for (EmailNotificationMessage.Recipient recipient : ccRecipients) {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    email.addRecipient(Message.RecipientType.CC, convertRecipient(recipient));
                }
            }
        }
        if (!bccRecipients.isEmpty()) {
            for (EmailNotificationMessage.Recipient recipient : bccRecipients) {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    email.addRecipient(Message.RecipientType.BCC, convertRecipient(recipient));
                }
            }
        }

        buildEmail(id, (EmailNotificationMessage) message, email);

        Address[] recipients = email.getAllRecipients();
        if (recipients == null || recipients.length == 0) {
            throw new NotificationProcessingException(NotificationProcessingException.Reason.INVALID_MESSAGE, "No recipients set for " + targetType.name().toLowerCase() + ": " + targetId);
        }

        // Set from based on source if not already set
        if (email.getFrom() == null || email.getFrom().length == 0) {
            email.setFrom(new InternetAddress(defaultFrom));
        }

        sendMessage(email);
    }

    protected void sendMessage(Message email) throws Exception {
        if (!mailTransport.isConnected()) {
            mailTransport.connect();
        }
        mailTransport.sendMessage(email, email.getAllRecipients());
    }

    protected void buildEmail(long id, EmailNotificationMessage emailNotificationMessage, MimeMessage email) throws Exception {
        if (emailNotificationMessage.getReplyTo() != null) {
            email.setReplyTo(new Address[]{convertRecipient(emailNotificationMessage.getReplyTo())});
        }
        if (emailNotificationMessage.getFrom() != null) {
            email.setFrom(convertRecipient(emailNotificationMessage.getFrom()));
        }
        if (emailNotificationMessage.getSubject() != null) {
            email.setSubject(emailNotificationMessage.getSubject());
        }
        if (headers != null) {
            for (Map.Entry<String, String >entry : headers.entrySet()) {
                email.addHeader(entry.getKey(), entry.getValue());
            }
        }
        if (emailNotificationMessage.getTo() != null) {
            for (EmailNotificationMessage.Recipient recipient : emailNotificationMessage.getTo()) {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    email.addRecipient(Message.RecipientType.TO, convertRecipient(recipient));
                }
            }
        }
        if (emailNotificationMessage.getCc() != null) {
            for (EmailNotificationMessage.Recipient recipient : emailNotificationMessage.getCc()) {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    email.addRecipient(Message.RecipientType.CC, convertRecipient(recipient));
                }
            }
        }
        if (emailNotificationMessage.getBcc() != null) {
            for (EmailNotificationMessage.Recipient recipient : emailNotificationMessage.getBcc()) {
                if (!TextUtil.isNullOrEmpty(recipient.getAddress())) {
                    email.addRecipient(Message.RecipientType.BCC, convertRecipient(recipient));
                }
            }
        }
        if (emailNotificationMessage.getText() != null) {
            email.setText(emailNotificationMessage.getText());
        } else if (emailNotificationMessage.getHtml() != null) {
            email.setContent(emailNotificationMessage.getHtml(), "text/html");
        }
    }

    protected InternetAddress convertRecipient(EmailNotificationMessage.Recipient recipient) {
        try {
            return recipient == null ? null : new InternetAddress(recipient.getAddress(), recipient.getName());
        } catch (UnsupportedEncodingException e) {
            LOG.log(Level.WARNING, "Failed to process email recipient: " + recipient.getAddress(), e);
        }
        return null;
    }
}
