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
package org.openremote.manager.asset.console;

import org.openremote.container.security.IdentityService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.UserAsset;
import org.openremote.model.console.ConsoleConfiguration;
import org.openremote.model.console.ConsoleRegistration;
import org.openremote.model.console.ConsoleResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.Tenant;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openremote.container.concurrent.GlobalLock.withLockReturning;

public class ConsoleResourceImpl extends ManagerWebResource implements ConsoleResource {

    public static final String CONSOLE_PARENT_ASSET_NAME = "Consoles";
    protected Map<String, String> realmConsoleParentMap = new HashMap<>();
    protected AssetStorageService assetStorageService;

    public ConsoleResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public ConsoleRegistration register(RequestParams requestParams, ConsoleRegistration consoleRegistration) {
        // Validate the console registration
        List<ValidationFailure> failures = new ArrayList<>();
        if (!ConsoleConfiguration.validateConsoleRegistration(consoleRegistration, failures)) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(failures).build());
        }

        Asset consoleAsset = null;

        // If console registration has an id and asset exists then ensure asset type is console
        if (!TextUtil.isNullOrEmpty(consoleRegistration.getId())) {
            consoleAsset = assetStorageService.find(consoleRegistration.getId(), true);
            // If asset doesn't exist then no harm in registering console using the supplied ID
            if (consoleAsset != null) {
                if (consoleAsset.getWellKnownType() != AssetType.CONSOLE) {
                    throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(new ValidationFailure[] {new ValidationFailure(Asset.AssetTypeFailureReason.ASSET_TYPE_MISMATCH)}).build());
                }
            }
        }

        if (consoleAsset == null) {
            consoleAsset = ConsoleConfiguration.initConsoleConfiguration(
                new Asset(consoleRegistration.getName(), AssetType.CONSOLE),
                consoleRegistration.getName(),
                consoleRegistration.getVersion(),
                consoleRegistration.getPlatform(),
                consoleRegistration.getProviders());
            if (!isAuthenticated()) {
                consoleAsset.setAccessPublicRead(true);
            }
            consoleAsset.setRealmId(getRequestTenant().getId());
            consoleAsset.setParentId(getConsoleParentAssetId(getRequestRealm()));
            consoleAsset.setId(consoleRegistration.getId());
        } else {
            ConsoleConfiguration.setConsoleName(consoleAsset, consoleRegistration.getName());
            ConsoleConfiguration.setConsolePlatform(consoleAsset, consoleRegistration.getPlatform());
            ConsoleConfiguration.setConsoleVersion(consoleAsset, consoleRegistration.getVersion());
            ConsoleConfiguration.setConsolProviders(consoleAsset, consoleRegistration.getProviders());
        }

        consoleAsset = assetStorageService.merge(consoleAsset);
        consoleRegistration.setId(consoleAsset.getId());

        // If authenticated link the console to this user
        if (isAuthenticated()) {
            assetStorageService.storeUserAsset(new UserAsset(getAuthenticatedTenant().getId(), getUserId(), consoleAsset.getId()));
        }

        return consoleRegistration;
    }

    public String getConsoleParentAssetId(String realm) {
        return withLockReturning(getClass().getSimpleName() + "::getConsoleParentAssetId", () -> {
            String id = realmConsoleParentMap.get(realm);

            if (TextUtil.isNullOrEmpty(id)) {
                Asset consoleParent = getConsoleParentAsset(assetStorageService, getRequestTenant());
                id = consoleParent.getId();
                realmConsoleParentMap.put(realm, id);
            }

            return id;
        });
    }

    public static Asset getConsoleParentAsset(AssetStorageService assetStorageService, Tenant tenant) {
        String id = consoleParentAssetIdGenerator(tenant.getRealm());
        Asset consoleParent = assetStorageService.find(id, false);
        if (consoleParent == null) {
            consoleParent = new Asset(CONSOLE_PARENT_ASSET_NAME, AssetType.THING);
            consoleParent.setId(id);
            consoleParent.setRealmId(tenant.getId());
            assetStorageService.merge(consoleParent);
        }
        return consoleParent;
    }

    protected static String consoleParentAssetIdGenerator(String realm) {
        return UniqueIdentifierGenerator.generateId(realm + "Consoles");
    }
}
