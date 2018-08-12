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

import org.openremote.model.notification.Notification;
import org.openremote.model.notification.PushNotificationMessage;
import org.openremote.model.util.TextUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FilterOptions {

    protected final static String[] NOTIFICATION_TYPES = new String[] {
        PushNotificationMessage.TYPE
    };

    protected final static String[] TARGET_TYPES = new String[] {
        Notification.TargetType.USER.name(),
        Notification.TargetType.ASSET.name()
    };

    public enum SentInLast {
        DAY,
        WEEK,
        MONTH
    }

    final protected Map<String, String> realms;
    final protected BiConsumer<FilterOptions, Consumer<Map<String, String>>> targetsSupplier;
    protected Map<String, String> targets = new HashMap<>();
    protected String selectedNotificationType = NOTIFICATION_TYPES[0]; // Only one type at the moment so select it
    protected String selectedRealm;
    protected Notification.TargetType selectedTargetType;
    protected String selectedTarget;
    protected SentInLast selectedSentIn = SentInLast.DAY;
    protected Runnable targetsChangedCallback;
    protected Runnable changedCallback;

    public FilterOptions(Map<String, String> realms, BiConsumer<FilterOptions, Consumer<Map<String, String>>> targetsSupplier) {
        this.realms = realms;
        this.targetsSupplier = targetsSupplier;
    }

    public String[] getNotificationTypes() {
        return NOTIFICATION_TYPES;
    }

    public String[] getTargetTypes() {
        return TARGET_TYPES;
    }

    public Map<String, String> getRealms() {
        return realms;
    }

    public Map<String, String> getTargets() {
        return targets;
    }

    protected void setTargets(Map<String, String> targets) {
        this.targets = targets;

        if (targets == null || !targets.containsKey(selectedTarget)) {
            selectedTarget = null;
        }

        if (targetsChangedCallback != null) {
            targetsChangedCallback.run();
        }
    }

    public String getSelectedNotificationType() {
        return selectedNotificationType;
    }

    public void setSelectedNotificationType(String selectedNotificationType) {
        if (Objects.equals(selectedNotificationType, this.selectedNotificationType)) {
            return;
        }
        this.selectedNotificationType = selectedNotificationType;
        notifyChangedCallback();
    }

    public String getSelectedRealm() {
        return selectedRealm;
    }

    public void setSelectedRealm(String selectedRealm) {
        this.selectedRealm = selectedRealm;
        this.selectedTarget = null;
        notifyChangedCallback();
        refreshTargets();
    }

    public Notification.TargetType getSelectedTargetType() {
        return selectedTargetType;
    }

    public void setSelectedTargetType(String selectedTargetType) {
        Notification.TargetType targetType = null;
        if (!TextUtil.isNullOrEmpty(selectedTargetType)) {
            try {
                targetType = Notification.TargetType.valueOf(selectedTargetType);
            } catch (IllegalArgumentException ignored) {
            }
        }
        setSelectedTargetType(targetType);
    }

    public void setSelectedTargetType(Notification.TargetType selectedTargetType) {
        this.selectedTargetType = selectedTargetType;
        this.selectedTarget = null;
        notifyChangedCallback();
        refreshTargets();
    }

    public String getSelectedTarget() {
        return selectedTarget;
    }

    public void setSelectedTarget(String selectedTarget) {
        this.selectedTarget = selectedTarget;
        notifyChangedCallback();
    }

    public SentInLast getSelectedSentIn() {
        return selectedSentIn;
    }

    public void setSelectedSentIn(SentInLast selectedSentIn) {
        this.selectedSentIn = selectedSentIn;
        notifyChangedCallback();
    }

    public void setTargetsChangedCallback(Runnable targetsChangedCallback) {
        this.targetsChangedCallback = targetsChangedCallback;
    }

    public void setChangedCallback(Runnable filterOptionsChangedCallback) {
        this.changedCallback = filterOptionsChangedCallback;
    }

    protected void refreshTargets() {
        setTargets(null);
        targetsSupplier.accept(this, this::setTargets);
    }

    protected void notifyChangedCallback() {
        if (changedCallback != null) {
            changedCallback.run();
        }
    }
}
