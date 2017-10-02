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
package org.openremote.manager.client.assets.asset;

import com.google.inject.Provider;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.JsonEditor;
import org.openremote.manager.client.assets.attributes.AbstractAttributeViewExtension;
import org.openremote.manager.client.assets.attributes.AttributeViewImpl;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.interop.value.ObjectValueMapper;
import org.openremote.manager.client.widget.FormButton;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.asset.AssetAttribute;

import javax.inject.Inject;
import java.util.List;

public class AssetLinkUsersActivity
    extends AbstractAssetActivity<AssetLinkUsers.Presenter, AssetLinkUsers, AssetLinkUsersPlace>
    implements AssetLinkUsers.Presenter {

    @Inject
    public AssetLinkUsersActivity(Environment environment,
                                  Tenant currentTenant,
                                  AssetBrowser.Presenter assetBrowserPresenter,
                                  Provider<JsonEditor> jsonEditorProvider,
                                  AssetLinkUsers view,
                                  MapResource mapResource,
                                  ObjectValueMapper objectValueMapper) {
        super(environment, currentTenant, assetBrowserPresenter, jsonEditorProvider, objectValueMapper, mapResource, true);
        this.presenter = this;
        this.view = view;
    }

    @Override
    public void start() {
        writeAssetToView();
    }

    @Override
    public void centerMap() {

    }

    @Override
    protected List<AbstractAttributeViewExtension> createAttributeExtensions(AssetAttribute attribute, AttributeViewImpl view) {
        return null;
    }

    @Override
    protected List<FormButton> createAttributeActions(AssetAttribute attribute, AttributeViewImpl view) {
        return null;
    }

    @Override
    protected void onAttributeModified(AssetAttribute attribute) {

    }
}
