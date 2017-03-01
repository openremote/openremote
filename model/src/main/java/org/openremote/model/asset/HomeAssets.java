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
package org.openremote.model.asset;

import java.util.*;

/**
 * A user can have links to assets, these are her "home" assets.
 * <p>
 * We currently use home asset links to implement access control: if a user has home assets,
 * she can only access those assets and their children.
 *
 * TODO Supporting multiple home assets comes with some unsolved problems.
 * Can one home asset be a child of another home asset? What are the consequences? Do we
 * actually need multiple home assets? Who guarantees that all home assets are within the
 * users realm? What happens if a home asset is moved around?
 */
public class HomeAssets {

    // Name of Keycloak user attribute where the linked asset identifiers are stored
    public static final String HOME_ASSETS_ATTRIBUTE = "urn:openremote:homeAssets";

    // Name of the claim in a user's access token where the linked asset identifiers can be found
    public static final String HOME_ASSETS_CLAIM = "openremoteHomeAssets";

    public static final Map<String, String> HOME_ASSET_MAPPER_CONFIG = new HashMap<String, String>() {
        {
            put("user.attribute", HomeAssets.HOME_ASSETS_ATTRIBUTE);
            put("access.token.claim", "true");
            put("claim.name", HomeAssets.HOME_ASSETS_CLAIM);
            put("jsonType.label", "String");
            put("multivalued", "true");
        }
    };

    public static Map<String, List<String>> setAssetId(String... assetId) {
        Map<String, List<String>> userAttributes = new HashMap<>();
        addAssetId(userAttributes, assetId);
        return userAttributes;
    }

    public static void addAssetId(Map<String, List<String>> userAttributes, String... assetId) {
        if (!userAttributes.containsKey(HOME_ASSETS_ATTRIBUTE))
            userAttributes.put(HOME_ASSETS_ATTRIBUTE, new ArrayList<>());
        List<String> userAssets = userAttributes.get(HOME_ASSETS_ATTRIBUTE);
        userAssets.addAll(Arrays.asList(assetId));
    }

    public static String[] getAssetIds(Map<String, Object> accessTokenClaims) {
        if (!accessTokenClaims.containsKey(HOME_ASSETS_CLAIM))
            return new String[0];
        @SuppressWarnings("unchecked")
        List<String> homeAssetIds = (List<String>) accessTokenClaims.get(HOME_ASSETS_CLAIM);
        return homeAssetIds.toArray(new String[homeAssetIds.size()]);
    }

}
