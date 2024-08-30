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
package org.openremote.setup.load1;


import org.apache.commons.io.IOUtils;
import org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler;
import org.openremote.manager.provisioning.ProvisioningService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.provisioning.X509ProvisioningConfig;
import org.openremote.model.provisioning.X509ProvisioningData;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.manager.mqtt.UserAssetProvisioningMQTTHandler.UNIQUE_ID_PLACEHOLDER;
import static org.openremote.model.rules.Ruleset.Lang.GROOVY;
import static org.openremote.model.value.ValueType.NUMBER;

public class ManagerSetup extends org.openremote.manager.setup.ManagerSetup {
    public static final String OR_SETUP_DEVICES = "OR_SETUP_DEVICES";
    public static final String OR_SETUP_DEVICES_SKIP_COUNT = "OR_SETUP_DEVICES_SKIP_COUNT";
    protected ProvisioningService provisioningService;
    protected Container container;
    protected Executor executor;

    public ManagerSetup(Container container, Executor executor) {
        super(container);
        this.container = container;
        this.executor = executor;
        provisioningService = container.getService(ProvisioningService.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onStart() throws Exception {
        int devices = getInteger(container.getConfig(), OR_SETUP_DEVICES, 0);
        int devicesSkipCount = getInteger(container.getConfig(), OR_SETUP_DEVICES_SKIP_COUNT, 100);
        String caCert;

        WeatherAsset templateAsset = new WeatherAsset(UNIQUE_ID_PLACEHOLDER).addAttributes(
            new Attribute<>("calculated", NUMBER).addMeta(
                new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
                new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE)
            )
        );

        try (InputStream inputStream = getClass().getResourceAsStream("/org/openremote/setup/load1/ca.pem")) {
            caCert = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        templateAsset.getAttribute(WeatherAsset.RAINFALL).ifPresent(attr -> attr.addMeta(
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
        ));
        templateAsset.getAttribute(WeatherAsset.TEMPERATURE).ifPresent(attr -> attr.addMeta(
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
        ));

         X509ProvisioningConfig provisioningConfig = new X509ProvisioningConfig("OEM Device",
            new X509ProvisioningData()
                .setCACertPEM(caCert)
        ).setAssetTemplate(
                ValueUtil.asJSON(templateAsset).orElse("")
            ).setRealm("master")
            .setRestrictedUser(true)
            .setUserRoles(new ClientRole[] {
                ClientRole.WRITE_ASSETS,
                ClientRole.WRITE_ATTRIBUTES,
                ClientRole.READ_ASSETS
            });

        provisioningConfig = provisioningService.merge(provisioningConfig);

        RealmRuleset ruleset = new RealmRuleset(
            Constants.MASTER_REALM,
            "Weather calculations",
            GROOVY,
            IOUtils.toString(getClass().getResource("/org/openremote/setup/load1/WeatherAssetCalculations.groovy"), StandardCharsets.UTF_8)
        );
        ruleset = rulesetStorageService.merge(ruleset);

        if (devices > 1) {
            AtomicInteger createdDevices = new AtomicInteger(0);
            int deviceSkipMod = devicesSkipCount > 0 ? devicesSkipCount : 1;
            X509ProvisioningConfig finalProvisioningConfig = provisioningConfig;
            IntStream.rangeClosed(1, devices).forEach(i -> {
                if (i % deviceSkipMod > 0) {
                    executor.execute(() -> {
                        String uniqueId = "device" + i;
                        User user = UserAssetProvisioningMQTTHandler.getCreateClientServiceUser(
                            Constants.MASTER_REALM,
                            ((ManagerKeycloakIdentityProvider)identityService.getIdentityProvider()),
                            uniqueId,
                            finalProvisioningConfig
                        );
                        UserAssetProvisioningMQTTHandler.getCreateClientAsset(
                            assetStorageService,
                            Constants.MASTER_REALM,
                            uniqueId,
                            user,
                            finalProvisioningConfig
                        );
                        createdDevices.incrementAndGet();
                    });
                } else {
                    createdDevices.incrementAndGet();
                }
            });

            // Wait until all devices created
            int waitCounter = 0;
            while (createdDevices.get() < devices) {
                if (waitCounter > 180) {
                    throw new IllegalStateException("Failed to provision all requested devices in the specified time");
                }
                waitCounter++;
                Thread.sleep(10000);
            }
        }
    }
}
