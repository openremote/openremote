/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.client.assets.device;

import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.shared.asset.AssetAttributeType;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.Attributes;

public class DeviceAttributesEditor extends AttributesEditor {

    public DeviceAttributesEditor(Environment environment, Container container, Attributes attributes) {
        super(environment, container, attributes);
    }

    @Override
    protected FormLabel buildFormLabel(Attribute attribute) {
        FormLabel formLabel = super.buildFormLabel(attribute);

        if (AssetAttributeType.isSensor(attribute)) {
            formLabel.setIcon("sign-out");
        } else if (AssetAttributeType.isActuator(attribute)) {
            formLabel.setIcon("sign-in");
        }

        return formLabel;
    }

    @Override
    protected FormInputText createStringEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createStringEditor(style, attribute, AssetAttributeType.isSensor(attribute));
    }

    @Override
    protected FormInputNumber createIntegerEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createIntegerEditor(style, attribute, AssetAttributeType.isSensor(attribute));
    }

    @Override
    protected FormInputText createFloatEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createFloatEditor(style, attribute, AssetAttributeType.isSensor(attribute));
    }

    @Override
    protected FormCheckBox createBooleanEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createBooleanEditor(style, attribute, AssetAttributeType.isSensor(attribute));
    }
}
