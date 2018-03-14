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
package org.openremote.app.client.assets.attributes;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import org.openremote.app.client.Environment;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeValidationResult;

public class ProtocolLinksEditor extends AbstractAttributeViewExtension {

    protected final ProtocolDescriptor protocolDescriptor;
    protected final Grid panel;
    protected final boolean discovery;

    public ProtocolLinksEditor(
        Environment environment,
        Style style,
        AttributeViewImpl parentView,
        AssetAttribute attribute,
        ProtocolDescriptor protocolDescriptor,
        boolean discovery) {

        super(environment, style, parentView, attribute, discovery ? environment.getMessages().protocolLinkDiscovery() : environment.getMessages().protocolLinks());
        this.protocolDescriptor = protocolDescriptor;
        this.discovery = discovery;

        panel = new Grid(1, 2);
        panel.setHeight("500px");
        panel.setWidth("100%");
        panel.getCellFormatter().setWidth(0,0,"40%");
        add(panel);

        // Asset/Attribute Tree
        Label assetTree = new Label(discovery ? "Discovered Asset/Attribute tree here" : "Linked Asset/Attribute tree here");
        assetTree.setHeight("100%");
        assetTree.getElement().getStyle().setBorderColor("black");
        assetTree.getElement().getStyle().setBorderStyle(com.google.gwt.dom.client.Style.BorderStyle.SOLID);
        assetTree.getElement().getStyle().setBorderWidth(1, com.google.gwt.dom.client.Style.Unit.PX);

        // Link editor
        Label linkEditor = new Label(discovery ?
            "Parent selector here with import button - will create assets/attributes under specified parent" :
            "Link editor here - when attribute selected on the left this shows agent link related meta items in a meta editor");
        linkEditor.setHeight("100%");
        linkEditor.getElement().getStyle().setBorderColor("black");
        linkEditor.getElement().getStyle().setBorderStyle(com.google.gwt.dom.client.Style.BorderStyle.SOLID);
        linkEditor.getElement().getStyle().setBorderWidth(1, com.google.gwt.dom.client.Style.Unit.PX);

        panel.setWidget(0, 0, assetTree);
        panel.setWidget(0, 1, linkEditor);
    }

    @Override
    public void onValidationStateChange(AttributeValidationResult validationResult) {

    }

    @Override
    public void onAttributeChanged(long timestamp) {

    }

    @Override
    public void setBusy(boolean busy) {

    }
}
