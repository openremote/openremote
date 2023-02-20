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

import org.openremote.model.syslog.SyslogCategory;

import javax.mail.*;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class EmailClient implements ConnectionListener {

    public static final System.Logger LOG = System.getLogger(EmailClient.class.getName() + "." + SyslogCategory.AGENT.name());
    protected EmailClientBuilder config;
    protected Session session;
    protected Date lastMessageDate;
    protected Future<?> mailChecker;
    protected boolean persistenceFileAccessible;
    protected final AtomicReference<Store> store = new AtomicReference<>();
    protected List<Consumer<ConnectionEvent>> connectionListeners = new CopyOnWriteArrayList<>();
    protected List<Consumer<Message>> messageListeners = new CopyOnWriteArrayList<>();

    EmailClient(EmailClientBuilder config) {
        this.config = config;

        if (config.getEarliestMessageDatePersistencePath() != null) {
            if (Files.exists(config.getEarliestMessageDatePersistencePath())) {
                LOG.log(System.Logger.Level.INFO, "Trying to read last message date from: " + config.getEarliestMessageDatePersistencePath());
                try {
                    String millisStr = Files.readString(config.getEarliestMessageDatePersistencePath());
                    lastMessageDate = new Date(Long.parseLong(millisStr));
                    persistenceFileAccessible = true;
                } catch (IOException e) {
                    LOG.log(System.Logger.Level.WARNING, "Failed to read last message date from: " + config.getEarliestMessageDatePersistencePath(), e);
                }
            } else if (config.getEarliestMessageDate() != null) {
                lastMessageDate = config.getEarliestMessageDate();
            }
        }

        this.session = Session.getInstance(config.getProperties());
    }

    public boolean connect() {
        synchronized (store) {
            if (store.get() != null) {
                return isConnected();
            }
            return doConnect();
        }
    }

    protected boolean doConnect() {
        try {
            LOG.log(System.Logger.Level.INFO, "Connecting to server: " + config.getHost());
            Store emailStore = session.getStore();
            store.set(emailStore);
            emailStore.connect(config.getUser(), config.getPassword());
            emailStore.addConnectionListener(this);

            try (Folder emailFolder = emailStore.getFolder(config.getFolder())) {
                if (emailFolder == null || !emailFolder.exists()) {
                    LOG.log(System.Logger.Level.WARNING, "Mailbox folder doesn't exist: " + config.getFolder());
                    throw new Exception();
                }

                emailFolder.open(config.isDeleteMessageOnceProcessed() ? Folder.READ_WRITE : Folder.READ_ONLY);
            }

            // For IMAP folders we can use listener for new messages for POP3 we need to open/close the folder
//            emailFolder.addMessageCountListener(new MessageCountAdapter() {
//                public void messagesAdded(MessageCountEvent ev) {
//                    Message[] messages = ev.getMessages();
//                    LOG.log(System.Logger.Level.TRACE, "Got " + messages.length + " messages");
//
//                    if (!initialised) {
//                        initialised = true;
//                        // Filter out earlier messages on first pass
//                        Arrays.stream(messages)
//                            .filter(message -> {
//                                try {
//                                    if (lastMessageDate != null) {
//                                        if (message.getReceivedDate().before(lastMessageDate)) {
//                                            return false;
//                                        }
//                                    }
//                                } catch (MessagingException e) {
//                                    LOG.log(System.Logger.Level.TRACE, "Failed to read message received date", e);
//                                    return false;
//                                }
//                                return true;
//                            })
//                            .forEach(message -> onMessage(message));
//                    } else {
//                        Arrays.stream(messages).forEach(message -> onMessage(message));
//                    }
//                }
//            });

            mailChecker = config.getScheduledExecutorService().scheduleWithFixedDelay(this::checkForMessages, 0, config.getCheckIntervalMillis(), TimeUnit.MILLISECONDS);
            return true;

        } catch (Exception e) {
            if (e instanceof AuthenticationFailedException) {
                LOG.log(System.Logger.Level.ERROR, "Failed to connect to mailbox due to auth issue", e);
            } else if (e instanceof MessagingException) {
                LOG.log(System.Logger.Level.ERROR, "Failed to connect to mailbox", e);
            }
            try {
                store.get().close();
            } catch (MessagingException ignored) {
            }
            store.set(null);
            return false;
        }
    }

    public void disconnect() {
        synchronized (store) {
            Store emailStore = store.get();

            if (emailStore == null) {
                return;
            }
            LOG.log(System.Logger.Level.INFO, "Disconnecting server: " + config.getHost());
            mailChecker.cancel(true);
            mailChecker = null;

            try {
                if (emailStore.isConnected()) {
                    emailStore.close();
                }
            } catch (MessagingException e) {
                LOG.log(System.Logger.Level.DEBUG, "Failed to close mail store", e);
            }

            store.set(null);
        }
    }

    public boolean isConnected() {
        Store emailStore = store.get();
        return emailStore != null && emailStore.isConnected();
    }

    public void addConnectionListener(Consumer<ConnectionEvent> listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(Consumer<ConnectionEvent> listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public void opened(ConnectionEvent e) {
        connectionListeners.forEach(listener -> listener.accept(e));
    }

    @Override
    public void disconnected(ConnectionEvent e) {
        connectionListeners.forEach(listener -> listener.accept(e));
    }

    @Override
    public void closed(ConnectionEvent e) {
        connectionListeners.forEach(listener -> listener.accept(e));
    }

    public void addMessageListener(Consumer<Message> listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(Consumer<Message> listener) {
        messageListeners.remove(listener);
    }

    public void onMessage(Message message) {
        messageListeners.forEach(listener -> listener.accept(message));
    }

    protected void checkForMessages() {
        Folder emailFolder = null;

        try {
            Store emailStore = store.get();
            if (emailStore == null) {
                return;
            }
            LOG.log(System.Logger.Level.INFO, "Checking for new mail messages from: " + config.getHost());
            emailFolder = emailStore.getFolder(config.getFolder());
            if (emailFolder == null || !emailFolder.exists()) {
                LOG.log(System.Logger.Level.WARNING, "Mailbox folder doesn't exist: " + config.getFolder());
                throw new Exception();
            }

            emailFolder.open(config.deleteMessageOnceProcessed ? Folder.READ_WRITE : Folder.READ_ONLY);

            Arrays.stream(emailFolder.getMessages())
                .filter(message -> {

                    try {
                        if (message.getFlags().contains(Flags.Flag.SEEN)) {
                            LOG.log(System.Logger.Level.TRACE, "Message is older than last message date so ignoring: num=" + message.getMessageNumber());
                            return false;
                        }
                        if (lastMessageDate != null) {
                            if (message.getReceivedDate().before(lastMessageDate)) {
                                LOG.log(System.Logger.Level.TRACE, "Message is older than last message date so ignoring");
                                return false;
                            }
                        }
                    } catch (MessagingException e) {
                        LOG.log(System.Logger.Level.TRACE, "Failed to read message details");
                        return false;
                    }

                    return true;
                })
                .forEach(message -> {
                    try {
                        onMessage(message);
                        if (config.isDeleteMessageOnceProcessed()) {
                            message.setFlag(Flags.Flag.DELETED, true);
                        }
                        lastMessageDate = message.getReceivedDate();
                        if (persistenceFileAccessible) {
                            try {
                                Files.writeString(config.getEarliestMessageDatePersistencePath(), Long.toString(lastMessageDate.getTime()), StandardOpenOption.WRITE);
                            } catch (IOException e) {
                                LOG.log(System.Logger.Level.INFO, "Failed to write to earliest message date persistence file: " + config.getEarliestMessageDatePersistencePath(), e);
                            }
                        }
                    } catch (MessagingException e) {
                        LOG.log(System.Logger.Level.INFO, "An exception occurred whilst processing a message", e);
                    }
                });
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "An error occurred whilst processing mail messages", e);
        } finally {
            try {
                if (emailFolder != null) {
                    if (emailFolder.isOpen()) {
                        if (config.isDeleteMessageOnceProcessed()) {
                            emailFolder.close(true);
                        } else {
                            emailFolder.close();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
