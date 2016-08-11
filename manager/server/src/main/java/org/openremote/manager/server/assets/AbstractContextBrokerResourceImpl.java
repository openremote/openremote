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
package org.openremote.manager.server.assets;

import org.jgroups.util.Tuple;
import org.openremote.manager.shared.ngsi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractContextBrokerResourceImpl implements RegistrationProvider {
    protected static final Logger LOG = Logger.getLogger(AbstractContextBrokerResourceImpl.class.getName());
    public static final int REFRESH_INTERVAL = 180;
    // Key = ENTITY_ID
    // Value = MAP<ATTRIBUTE, PROVIDER>
    protected Map<Tuple<String,String>, Map<Attribute, AssetProvider>> providers = new HashMap<>();
    protected int refreshInterval = REFRESH_INTERVAL;

    @Override
    public synchronized boolean registerAssetProvider(String assetType, String assetId, List<Attribute> attributes, AssetProvider provider) {
        LOG.fine("Registering Asset Provider for '" + assetId + "'");
        List<Attribute> regAttributes = new ArrayList<>(attributes);
        Map.Entry<Tuple<String, String>, Map<Attribute, AssetProvider>> providerEntry = providers
                .entrySet()
                .stream()
                .filter(es -> es.getKey().getVal1().equalsIgnoreCase(assetId))
                .findFirst()
                .orElse(null);
        Map<Attribute, AssetProvider> providerInfo = providerEntry != null ? providerEntry.getValue() : null;

        if (providerInfo != null) {
            // If any attributes are already registered with another provider then don't proceed
            Stream<AssetProvider> providers = attributes.stream().filter(providerInfo::containsKey).map(providerInfo::get).distinct();

            if (providers.count() > 0 && providers.anyMatch(p -> p != provider)) {
                LOG.info("Request failed because one or more attributes already registered with another provider");
                return false;
            }

            regAttributes = Stream.concat(regAttributes.stream(), providerInfo.keySet().stream()).collect(Collectors.toList());
        } else {
            providerInfo = new HashMap<>();
        }

        // Register all asset attributes with the Context Broker (updates or creates registration)
        updateRegistration(assetType, assetId, regAttributes);

        for (Attribute attr : attributes) {
            providerInfo.put(attr, provider);
        }

        providers.putIfAbsent(new Tuple<>(assetId, assetType), providerInfo);
        return true;
    }

    @Override
    public synchronized void unregisterAssetProvider(AssetProvider provider) {
        providers
                .entrySet()
                .stream()
                .filter(es -> es.getValue().containsValue(provider))
                .forEach(es -> unregisterAssetProvider(es.getKey().getVal1(), es.getKey().getVal2(), provider));
    }

    @Override
    public synchronized void unregisterAssetProvider(String assetType, String assetId, AssetProvider provider) {
        Map.Entry<Tuple<String, String>, Map<Attribute, AssetProvider>> providerEntry = providers
                .entrySet()
                .stream()
                .filter(es -> es.getKey().getVal1().equalsIgnoreCase(assetId))
                .findFirst()
                .orElse(null);
        Map<Attribute, AssetProvider> providerInfo = providerEntry != null ? providerEntry.getValue() : null;

        if (providerInfo == null) {
            return;
        }

        // Split attributes into those to be removed and those to remain
        Map<Boolean, List<Map.Entry<Attribute, AssetProvider>>> splitAttributes = providerInfo.entrySet().stream().collect(Collectors.partitioningBy(es -> es.getValue() == provider));

        // Remove obsolete attributes
        splitAttributes.get(true).forEach(es -> providerInfo.remove(es.getKey()));

        // Update context provider registration
        List<Attribute> remainingAttributes = splitAttributes.get(false).stream().map(Map.Entry::getKey).collect(Collectors.toList());
        updateRegistration(assetType, assetId, remainingAttributes);

        if (remainingAttributes.size() == 0) {
            providers.remove(providerEntry.getKey());
        }
    }

    @Override
    public int getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public void setRefreshInterval(int seconds) {
        seconds = Math.max(30, seconds);
        this.refreshInterval = seconds;
    }

    protected abstract void updateRegistration(String assetType, String assetId, List<Attribute> attributes);
}
