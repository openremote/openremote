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
package org.openremote.manager.client.rules.tenant;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.asset.RulesAssetPlace;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.model.asset.AssetInfo;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class RulesTenantActivity
    extends AssetBrowsingActivity<RulesTenantView, RulesTenantPlace>
    implements RulesTenantView.Presenter {

    private static final Logger LOG = Logger.getLogger(RulesTenantActivity.class.getName());

    String realm;

    @Inject
    public RulesTenantActivity(Environment environment,
                               RulesTenantView view,
                               AssetBrowser.Presenter assetBrowserPresenter,
                               AssetResource assetResource,
                               AssetMapper assetMapper) {
        super(environment, view, assetBrowserPresenter, assetResource, assetMapper);
    }

    @Override
    protected AppActivity<RulesTenantPlace> init(RulesTenantPlace place) {
        this.realm = place.getRealm();
        return super.init(place);
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        view.setFormBusy(true);
        view.setName(realm);
        view.setFormBusy(false);
    }

    @Override
    protected void startCreateAsset() {
    }

    @Override
    protected void onAssetLoaded() {
    }

    @Override
    protected void onAssetsDeselected() {
    }

    @Override
    protected void onAssetSelectionChange(AssetInfo selectedAssetInfo) {
        environment.getPlaceController().goTo(new RulesAssetPlace(selectedAssetInfo.getId()));
    }

    @Override
    protected void onTenantSelected(String id, String realm) {
        if (this.realm == null || !this.realm.equals(realm)) {
            environment.getPlaceController().goTo(new RulesTenantPlace(realm));
        }
    }

    @Override
    protected void onBeforeAssetLoad() {
    }

    @Override
    public void onStop() {
        super.onStop();
        clearViewFieldErrors();
        view.setPresenter(null);
        view.clearFormMessages();
    }

    @Override
    public void update() {

    }

    @Override
    public void create() {

    }

    @Override
    public void delete() {

    }

    protected void clearViewFieldErrors() {
        // TODO: Validation
    }

}
