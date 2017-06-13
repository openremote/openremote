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
package org.openremote.model.notification;

import org.openremote.model.AbstractTypeHolder;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;


/**
 * Wraps {@link ObjectValue}.
 */
public class AlertAction extends AbstractTypeHolder {


    public AlertAction() {
        super(Values.createObject());
    }

    public AlertAction(String title, ActionType type, String assetId, String attributeName, String rawJson) {
        this();
        setTitle(title);
        setActionType(type);
        setAssetId(assetId);
        setAttributeName(attributeName);
        setRawJson(rawJson);
    }

    private void setRawJson(String rawJson) {
        if (rawJson != null) {
            objectValue.put("rawJson", Values.create(rawJson));
        } else if (objectValue.hasKey("rawJson")) {
            objectValue.remove("rawJson");
        }
    }

    private void setAttributeName(String attributeName) {
        if (attributeName != null) {
            objectValue.put("attributeName", Values.create(attributeName));
        } else if (objectValue.hasKey("attributeName")) {
            objectValue.remove("attributeName");
        }
    }

    public String getTitle() {
        return objectValue.getString("title").orElse(null);
    }

    public void setTitle(String title) {
        if (title != null) {
            objectValue.put("title", Values.create(title));
        } else if (objectValue.hasKey("title")) {
            objectValue.remove("title");
        }
    }

    public ActionType getActionType() {
        return getType().map(ActionType::valueOf).orElse(null);
    }

    public void setActionType(ActionType type) {
        setType(type != null ? type.name() : null);
    }

    public void setAssetId(String assetId) {
        if (assetId != null) {
            objectValue.put("assetId", Values.create(assetId));
        } else if (objectValue.hasKey("assetId")) {
            objectValue.remove("assetId");
        }
    }
}
