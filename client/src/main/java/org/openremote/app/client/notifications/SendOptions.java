/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.app.client.notifications;

import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.Notification;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SendOptions extends FilterOptions {

    protected final static String[] TARGET_TYPES = Arrays.stream(Notification.TargetType.values()).map(Enum::name).toArray(String[]::new);

    protected Supplier<AbstractNotificationMessage> messageSupplier;
    protected AbstractNotificationMessage message;

    public SendOptions(Map<String, String> realms, Map<String, String> realmIds,  BiConsumer<FilterOptions, Consumer<Map<String, String>>> targetsSupplier, Supplier<AbstractNotificationMessage> messageSupplier) {
        super(realms, realmIds, targetsSupplier);
        this.messageSupplier = messageSupplier;
    }

    @Override
    public String[] getTargetTypes() {
        return TARGET_TYPES;
    }

    public AbstractNotificationMessage getMessage() {
        if (message == null) {
            message = messageSupplier.get();
        } else if (!Objects.equals(message.getType(), getSelectedNotificationType())) {
            message = null;

            if (getSelectedNotificationType() != null) {
                message = messageSupplier.get();
            }
        }
        return message;
    }
}
