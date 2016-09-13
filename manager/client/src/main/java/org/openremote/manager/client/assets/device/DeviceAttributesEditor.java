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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.device.DeviceAttributes;
import org.openremote.manager.shared.device.DeviceResource;

import static com.google.gwt.dom.client.Style.Unit.EM;

public class DeviceAttributesEditor extends AttributesEditor {

    final protected DeviceAttributes deviceAttributes;

    public DeviceAttributesEditor(Environment environment, Container container, Attributes attributes) {
        super(environment, container, attributes);
        this.deviceAttributes = new DeviceAttributes(attributes.getJsonObject());
    }

    @Override
    protected FormLabel buildFormLabel(Attribute attribute) {
        FormLabel formLabel = super.buildFormLabel(attribute);

        if (DeviceResource.isDeviceResource(attribute)) {
            DeviceResource deviceResource = new DeviceResource(attribute);

            switch (deviceResource.getAccess()) {
                case R:
                    formLabel.setText(formLabel.getText() + " (R)");
                    break;
                case W:
                    formLabel.setText(formLabel.getText() + " (W)");
                    break;
                default:
                    formLabel.setText(formLabel.getText() + " (RW)");
            }

        } else if (attribute.getName().equals("key")) {
            formLabel.setText(environment.getMessages().deviceKey());
        }
        return formLabel;
    }

    @Override
    protected FormGroup createFormGroup(Attribute attribute) {
        FormGroup formGroup = super.createFormGroup(attribute);
        if (DeviceResource.isDeviceResource(attribute)) {
            formGroup.addInfolabel(
                new Label(environment.getMessages().resourceKey())
            );

            DeviceResource deviceResource = new DeviceResource(attribute);

            // TODO These actions don't do anything
            FlowPanel actionPanel = new FlowPanel();
            actionPanel.setStyleName("flex layout horizontal center");

            FormButton readButton = new FormButton();
            FormInputText readOutput = new FormInputText();
            readOutput.getElement().getStyle().setWidth(5, EM);
            readOutput.setReadOnly(true);
            readButton.setText(environment.getMessages().readValue());

            FormButton writeButton = new FormButton();
            FormInputText writeInput = new FormInputText();
            writeInput.getElement().getStyle().setWidth(5, EM);
            writeButton.setText(environment.getMessages().writeValue());

            switch (deviceResource.getAccess()) {
                case R:
                    actionPanel.add(readButton);
                    actionPanel.add(readOutput);
                    break;
                case W:
                    actionPanel.add(writeInput);
                    actionPanel.add(writeButton);
                    break;
                default:
                    actionPanel.add(writeInput);
                    actionPanel.add(writeButton);
                    actionPanel.add(readButton);
                    actionPanel.add(readOutput);
            }
            formGroup.getFormField().add(actionPanel);
        }
        return formGroup;
    }

    @Override
    protected boolean isDefaultReadOnly(Attribute attribute) {
        return attribute.getName().equals("key") || super.isDefaultReadOnly(attribute);
    }

    @Override
    protected FormInputText createStringEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createStringEditor(style, attribute, readOnly || DeviceResource.isDeviceResource(attribute));
    }

    @Override
    protected FormInputNumber createIntegerEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createIntegerEditor(style, attribute, readOnly || DeviceResource.isDeviceResource(attribute));
    }

    @Override
    protected FormInputText createFloatEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createFloatEditor(style, attribute, readOnly || DeviceResource.isDeviceResource(attribute));
    }

    @Override
    protected FormCheckBox createBooleanEditor(Style style, Attribute attribute, boolean readOnly) {
        return super.createBooleanEditor(style, attribute, readOnly || DeviceResource.isDeviceResource(attribute));
    }
}
