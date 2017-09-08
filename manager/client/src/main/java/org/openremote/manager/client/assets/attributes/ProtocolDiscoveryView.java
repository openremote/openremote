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
package org.openremote.manager.client.assets.attributes;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import elemental.client.Browser;
import elemental.html.Blob;
import elemental.html.FileReader;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetSelector;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.assets.browser.TenantTreeNode;
import org.openremote.manager.client.interop.BiConsumer;
import org.openremote.manager.client.widget.FormGroup;
import org.openremote.manager.client.widget.FormGroupActions;
import org.openremote.manager.client.widget.FormInlineLabel;
import org.openremote.manager.client.widget.FormLabel;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.file.FileInfo;

public class ProtocolDiscoveryView extends AbstractAttributeViewExtension {

    public static class DiscoveryRequest {
        protected final String protocolConfigurationName;
        protected final String parentId;
        protected final String realmId;
        protected final FileInfo fileInfo;

        public DiscoveryRequest(String protocolConfigurationName, String parentId, String realmId, FileInfo fileInfo) {
            this.protocolConfigurationName = protocolConfigurationName;
            this.parentId = parentId;
            this.realmId = realmId;
            this.fileInfo = fileInfo;
        }

        public String getProtocolConfigurationName() {
            return protocolConfigurationName;
        }

        public String getParentId() {
            return parentId;
        }

        public String getRealmId() {
            return realmId;
        }

        public FileInfo getFileInfo() {
            return fileInfo;
        }
    }

    protected final ProtocolDescriptor protocolDescriptor;
    protected final AssetSelector assetSelector;
    protected final BiConsumer<DiscoveryRequest, Runnable> discoveryRequestConsumer;
    protected String importRealmId;
    protected String importParentId;

    public ProtocolDiscoveryView(
        Environment environment,
        Style style,
        AttributeViewImpl parentView,
        AssetAttribute attribute,
        AssetBrowser assetBrowser,
        ProtocolDescriptor protocolDescriptor,
        BiConsumer<DiscoveryRequest, Runnable> discoveryRequestConsumer) {

        super(environment, style, parentView, attribute, environment.getMessages().protocolLinkDiscovery());
        this.protocolDescriptor = protocolDescriptor;
        this.discoveryRequestConsumer = discoveryRequestConsumer;

        this.assetSelector = new AssetSelector(
            assetBrowser.getPresenter(),
            environment.getMessages(),
            environment.getMessages().parentAsset(),
            environment.getMessages().selectAssetDescription(),
            true,
            treeNode -> {
                importParentId = null;
                importRealmId = null;

                if (treeNode instanceof TenantTreeNode) {
                    TenantTreeNode tenantTreeNode = (TenantTreeNode)treeNode;
                    importRealmId = tenantTreeNode.getId();
                } else if (treeNode instanceof AssetTreeNode) {
                    AssetTreeNode assetTreeNode = (AssetTreeNode)treeNode;
                    importParentId = assetTreeNode.getId();
                }
            }
        ) {
            @Override
            public void beginSelection() {
                ProtocolDiscoveryView.this.setDisabled(true);
                super.beginSelection();
            }

            @Override
            public void endSelection() {
                super.endSelection();
                ProtocolDiscoveryView.this.setDisabled(false);
            }
        };

        add(assetSelector);

        if (protocolDescriptor.isDeviceImport()) {
            FormGroup importGroup = new FormGroup();
            importGroup.setFormLabel(new FormLabel(environment.getMessages().importProtocolLinks()));
            FormGroupActions actions = new FormGroupActions();
            FileUpload fileUpload = new FileUpload();
            fileUpload.setVisible(false);

            fileUpload.addChangeHandler(event -> {
                JsArray files = (JsArray) fileUpload.getElement().getPropertyJSO("files");
                if (files.length() != 1) {
                    return;
                }
                // Base64 encode non-text files
                Blob file = (Blob) files.get(0);
                final FileReader reader = Browser.getWindow().newFileReader();
                if (file.getType().matches("text.*")) {
                    reader.setOnloadend(evt -> doDiscoveryImport(reader.getResult().toString(), false));
                    reader.readAsText(file, "UTF-8");
                } else {
                    reader.setOnloadend(evt -> doDiscoveryImport(reader.getResult().toString(), true));
                    reader.readAsDataURL(file);
                }
            });

            FormInlineLabel label = new FormInlineLabel(environment.getMessages().uploadProtocolFile());
            label.setIcon("upload");
            FlowPanel fileUploadWrapper = new FlowPanel();
            fileUploadWrapper.getElement().getStyle().setDisplay(com.google.gwt.dom.client.Style.Display.INLINE);
            fileUploadWrapper.addStyleName(environment.getWidgetStyle().FormControl());
            fileUploadWrapper.addStyleName(environment.getWidgetStyle().FormFileUploadLabel());
            fileUploadWrapper.add(fileUpload);
            fileUploadWrapper.add(label);

            actions.add(fileUploadWrapper);
            importGroup.setFromGroupActions(actions);
            add(importGroup);
        }
    }

    @Override
    public void onValidationStateChange(AttributeValidationResult validationResult) {

    }

    @Override
    public void onAttributeChanged() {

    }

    @Override
    public void setBusy(boolean busy) {

    }

    protected void doDiscoveryImport(String data, boolean binary) {

    }
}
