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
package org.openremote.manager.client.assets.attributes;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.model.Attribute;
import org.openremote.model.Attributes;
import org.openremote.model.Runnable;

public abstract class AttributesBrowser extends AttributesView<AttributesBrowser.Container, AttributesBrowser.Style> {

    public interface Container extends AttributesView.Container<AttributesBrowser.Style> {

    }

    public interface Style extends AttributesView.Style {

    }

    protected FormGroup liveUpdatesGroup;

    public AttributesBrowser(Environment environment, Container container, Attributes attributes) {
        super(environment, container, attributes);
    }

    @Override
    protected void createAttributeGroups() {
        liveUpdatesGroup = createLiveUpdatesGroup();
        container.getPanel().add(liveUpdatesGroup);
        super.createAttributeGroups();
    }

    @Override
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        liveUpdatesGroup.setOpaque(opaque);
    }

    @Override
    protected void addAttributeActions(Attribute attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions,
                                       IsWidget editor) {

        // Allow direct read/write of attribute if we understand attribute type (have an editor)
        if (editor != null) {
            if (!attribute.isReadOnly()) {
                FormButton writeValueButton = new FormButton();
                writeValueButton.setText(container.getMessages().write());
                writeValueButton.setPrimary(true);
                writeValueButton.setIcon("cloud-upload");
                writeValueButton.addClickHandler(clickEvent -> writeAttributeValue(attribute));
                formGroupActions.add(writeValueButton);
            }

            FormButton readValueButton = new FormButton();
            readValueButton.setText(container.getMessages().read());
            readValueButton.setIcon("cloud-download");
            readValueButton.addClickHandler(clickEvent -> readAttributeValue(attribute, () -> {
                if (formField.getWidgetCount() > 0) {
                    formField.getWidget(0).removeFromParent();
                }
                IsWidget refreshedEditor = createEditor(attribute, formGroup);
                if (refreshedEditor == null)
                    refreshedEditor = createUnsupportedEditor(attribute);
                formField.add(refreshedEditor);
            }));
            formGroupActions.add(readValueButton);
        }
    }

    @Override
    protected void addAttributeExtensions(Attribute attribute,
                                          FormGroup formGroup) {
        formGroup.addExtension(createDatapointBrowser(attribute));
    }

    /* ####################################################################### */

    protected FormGroup createLiveUpdatesGroup() {
        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = new FormLabel("Show live updates");
        formGroup.addFormLabel(formLabel);

        FormField formField = new FormField();
        formGroup.addFormField(formField);

        FormCheckBox liveUpdatesCheckBox = new FormCheckBox();
        liveUpdatesCheckBox.addValueChangeHandler(event -> showInfo("TODO: Not implemented"));
        formField.add(liveUpdatesCheckBox);

        if (attributes.get().length > 0) {
            FormGroupActions formGroupActions = new FormGroupActions();

            FormButton readAllButton = new FormButton();
            readAllButton.setText("Read and refresh all attributes");
            readAllButton.addClickHandler(event -> showInfo("TODO: Not implemented"));
            formGroupActions.add(readAllButton);

            formGroup.addFormGroupActions(formGroupActions);
        }

        return formGroup;
    }

    protected Widget createDatapointBrowser(Attribute attribute) {
        // TODO
        return new Image("/static/img/Example%20Chart%20Big.png");
    }

    /* ####################################################################### */

    abstract protected void readAttributeValue(Attribute attribute, Runnable onSuccess);

    abstract protected void writeAttributeValue(Attribute attribute);

}
