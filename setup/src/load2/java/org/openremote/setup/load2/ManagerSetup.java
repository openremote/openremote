/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.setup.load2;


import org.apache.commons.io.IOUtils;
import org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler;
import org.openremote.manager.provisioning.ProvisioningService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.asset.impl.BuildingAsset;
import org.openremote.model.asset.impl.LightAsset;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.provisioning.X509ProvisioningConfig;
import org.openremote.model.provisioning.X509ProvisioningData;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler.UNIQUE_ID_PLACEHOLDER;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.rules.Ruleset.Lang.GROOVY;
import static org.openremote.model.syslog.SyslogCategory.DATA;
import static org.openremote.model.value.ValueType.NUMBER;
import static org.openremote.setup.load2.KeycloakSetup.OR_SETUP_USERS;

public class ManagerSetup extends org.openremote.manager.setup.ManagerSetup {
    public static final String OR_SETUP_ASSETS = "OR_SETUP_ASSETS";

    protected ProvisioningService provisioningService;
    protected Container container;
    protected Executor executor;

    private static final Logger LOG = SyslogCategory.getLogger(DATA, ManagerSetup.class);

    public ManagerSetup(Container container, Executor executor) {
        super(container);
        this.container = container;
        this.executor = executor;
        provisioningService = container.getService(ProvisioningService.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onStart() throws Exception {
        int accounts = getInteger(container.getConfig(), OR_SETUP_USERS, 0);
        int assets = getInteger(container.getConfig(), OR_SETUP_ASSETS, 0);

        AtomicInteger createdAccounts = new AtomicInteger(0);
        if (accounts > 1) {
            IntStream.rangeClosed(1, accounts).forEach(i -> {
                executor.execute(() -> {
                    createAssets(i, assets);
                    createdAccounts.incrementAndGet();
                });
            });

            // Wait until all devices created
            int waitCounter = 0;
            while (createdAccounts.get() < accounts) {
                if (waitCounter > 200) {
                    throw new IllegalStateException("Failed to provision all requested devices in the specified time");
                }
                waitCounter++;
                Thread.sleep(10000);
            }
        }
    }

    private void createAssets(int account, int assets) {
        String userName = "user" + account;
        String serviceUserName = User.SERVICE_ACCOUNT_PREFIX + "serviceuser" + account;

        User user = identityService.getIdentityProvider().getUserByUsername(MASTER_REALM, userName);
        String userId = user.getId();
        User serviceUser = identityService.getIdentityProvider().getUserByUsername(MASTER_REALM, serviceUserName);
        String serviceUserId = serviceUser.getId();

        Asset building = new BuildingAsset("Building " + account);
        building.setRealm(MASTER_REALM);
        building = assetStorageService.merge(building);
        LOG.info("Created building asset: " + building.getName());
        assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(MASTER_REALM, userId, building.getId())));

        for (int i = 1; i <= assets; i++) {
            String uniqueId = "light-" + account + "-" +  i;
            String assetId = UniqueIdentifierGenerator.generateId(MASTER_REALM + uniqueId);
            Asset<?> asset = new LightAsset("Light - " + account + " - " + i);
            asset.getAttribute(LightAsset.BRIGHTNESS).ifPresent( attribute -> {
                attribute.addMeta(new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE));
            });
            asset.setId(assetId);
            asset.setRealm(MASTER_REALM);
            asset.setParent(building);
            asset = assetStorageService.merge(asset);
            LOG.info("Created light asset: " + asset.getName());

            assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(MASTER_REALM, serviceUserId, assetId)));
            assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(asset.getRealm(), userId, asset.getId())));
        }
    }
}
