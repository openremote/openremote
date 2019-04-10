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

import com.google.gwt.core.client.JsArray;
import elemental.client.Browser;
import elemental.html.Blob;
import elemental.html.FileReader;
import org.openremote.app.client.widget.FileUploadLabelled;
import org.openremote.app.client.widget.FormButton;
import org.openremote.app.client.widget.FormGroupActions;
import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetSelector;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.file.FileInfo;

import java.util.function.BiConsumer;

public class ProtocolDiscoveryView extends AbstractAttributeViewExtension {

    public static class DiscoveryRequest {
        protected final String protocolConfigurationName;
        protected final String parentId;
        protected final String realm;
        protected final FileInfo fileInfo;

        public DiscoveryRequest(String protocolConfigurationName, String parentId, String realm, FileInfo fileInfo) {
            this.protocolConfigurationName = protocolConfigurationName;
            this.parentId = parentId;
            this.realm = realm;
            this.fileInfo = fileInfo;
        }

        public String getProtocolConfigurationName() {
            return protocolConfigurationName;
        }

        public String getParentId() {
            return parentId;
        }

        public String getRealm() {
            return realm;
        }

        public FileInfo getFileInfo() {
            return fileInfo;
        }
    }

    protected final ProtocolDescriptor protocolDescriptor;
    protected final AssetSelector assetSelector;
    protected final BiConsumer<DiscoveryRequest, Runnable> discoveryRequestConsumer;
    protected String importRealm;
    protected String importParentId;
    protected FileUploadLabelled fileUpload;

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
            environment.getMessages().protocolLinkDiscoveryParent(),
            environment.getMessages().selectAssetDescription(),
            true,
            treeNode -> {
                importParentId = null;
                importRealm = null;

                if (treeNode instanceof TenantTreeNode) {
                    TenantTreeNode tenantTreeNode = (TenantTreeNode)treeNode;
                    importRealm = tenantTreeNode.getId();
                } else if (treeNode instanceof AssetTreeNode) {
                    AssetTreeNode assetTreeNode = (AssetTreeNode)treeNode;
                    importParentId = assetTreeNode.getId();
                }
            }
        ) {
            @Override
            public void beginSelection() {
                fileUpload.setDisabled(true);
                super.beginSelection();
            }

            @Override
            public void endSelection() {
                super.endSelection();
                fileUpload.setDisabled(false);
            }
        };

        assetSelector.setAlignStart(false);
        assetSelector.getFormLabel().addStyleName("larger");

        add(assetSelector);

        if (protocolDescriptor.isDeviceImport()) {
            fileUpload = new FileUploadLabelled();
            fileUpload.setIcon("upload");
            fileUpload.getElement().addClassName(environment.getWidgetStyle().FormControl());
            fileUpload.getElement().addClassName(environment.getWidgetStyle().FormFileUploadLabel());
            fileUpload.setText(environment.getMessages().uploadProtocolFile());

            fileUpload.getFileUpload().addChangeHandler(event -> {
                JsArray files = (JsArray) fileUpload.getFileUpload().getElement().getPropertyJSO("files");
                if (files.length() != 1) {
                    return;
                }
                Blob file = (Blob) files.get(0);
                final FileReader reader = Browser.getWindow().newFileReader();
                onDeviceDiscoveryImportStart();
                if (file.getType().matches("text.*")) {
                    reader.setOnloadend(evt -> doDeviceImport(fileUpload.getFileUpload().getFilename(), reader.getResult().toString(), false, this::onDeviceDiscoveryImportEnd));
                    reader.readAsText(file, "UTF-8");
                } else {
                    reader.setOnloadend(evt -> doDeviceImport(fileUpload.getFileUpload().getFilename(), reader.getResult().toString(), true, this::onDeviceDiscoveryImportEnd));
                    reader.readAsDataURL(file);
                }
            });
            assetSelector.getFormGroupActions().add(fileUpload);
        }

        if (protocolDescriptor.isDeviceDiscovery()) {
            FormGroupActions actions = new FormGroupActions();
            FormButton btn = new FormButton(environment.getMessages().discoverDevices());
            btn.setIcon("search");
            btn.addClickHandler(event -> {
                onDeviceDiscoveryImportStart();
                doDeviceDiscovery(this::onDeviceDiscoveryImportEnd);
            });
            actions.add(btn);
            assetSelector.getFormGroupActions().add(btn);
        }
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

    protected void doDeviceImport(String name, String data, boolean binary, Runnable callback) {
        FileInfo fileInfo = new FileInfo(name, data, binary);
        DiscoveryRequest discoveryRequest = new DiscoveryRequest(attribute.getName().orElse(""), importParentId, importRealm, fileInfo);
        if (discoveryRequestConsumer != null) {
            discoveryRequestConsumer.accept(discoveryRequest, callback);
        }
    }

    protected void doDeviceDiscovery(Runnable callback) {
        DiscoveryRequest discoveryRequest = new DiscoveryRequest(attribute.getName().orElse(""), importParentId, importRealm, null);
        if (discoveryRequestConsumer != null) {
            discoveryRequestConsumer.accept(discoveryRequest, callback);
        }
    }


    protected void onDeviceDiscoveryImportStart() {
        fileUpload.clearInput();
        this.setDisabled(true);
    }

    protected void onDeviceDiscoveryImportEnd() {
        this.setDisabled(false);
    }
}
