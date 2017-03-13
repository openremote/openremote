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

import static org.openremote.model.Constants.NAMESPACE;

/**
 * An asset can be linked to many users, and a user can have links to many assets.
 * <p>
 * If a user has linked assets, it's a <em>restricted</em> user. Such a user can only
 * access its assigned assets and their protected data, and it has a limited
 * set of client operations available, see {@link AssetMeta.Access}:
 * <ul>
 * <li>
 * When a restricted user client loads asset data, only protected asset details are included.
 * </li>
 * <li>
 * When a restricted user client updates asset data, only a subset of protected data can be changed.
 * </li>
 * </ul>
 * This mechanism is implemented with Keycloak user attributes:
 * <p>
 * The flag {@link #RESTRICTED_ATTRIBUTE} is set to <code>true</code> if the user
 * has linked assets. This flag is also mapped into the user's access token as the claim
 * {@link #RESTRICTED_CLAIM}, so access token receivers can check if the user is supposed
 * to have a restriction to linked assets.
 * <p>
 * The actual linked assets identifiers are stored in {@link #ASSETS_ATTRIBUTE} and therefore
 * in the Keycloak table <code>USER_ATTRIBUTES</code>. We query that table directly to retrieve
 * the assets linked to a user.
 *
 * TODO This doesn't work, users can change their own attributes!
 */
public class ProtectedUserAssets {

    // Keycloak user attribute where the boolean flag "this user has linked assets" is stored
    public static final String RESTRICTED_ATTRIBUTE = "restrictedUser";

    // Keycloak user attribute where the actual linked asset identifiers are stored
    public static final String ASSETS_ATTRIBUTE = NAMESPACE + ":userAssets";

    // Name of the claim in a user's access token where the "this user has linked assets" flag can be found
    public static final String RESTRICTED_CLAIM = "openremoteRestrictedUser";

    // Map the user attribute to the claim
    public static final Map<String, String> RESTRICTED_MAPPER = new HashMap<String, String>() {
        {
            put("user.attribute", ProtectedUserAssets.RESTRICTED_ATTRIBUTE);
            put("claim.name", ProtectedUserAssets.RESTRICTED_CLAIM);
            put("access.token.claim", "true"); // Put it in the access token
            put("jsonType.label", "boolean"); // As a boolean JSON value
        }
    };

    /**
     * @return The user attributes containing the linked assets and the <em>restricted</em> flag to be stored in Keycloak.
     */
    public static Map<String, List<String>> createUserAttributes(String... assetId) {
        Map<String, List<String>> userAttributes = new HashMap<>();
        setAssetsOnUserAttributes(userAttributes, assetId);
        return userAttributes;
    }

    public static void setAssetsOnUserAttributes(Map<String, List<String>> userAttributes, String... assetId) {
        if (assetId != null && assetId.length > 0) {
            userAttributes.put(ASSETS_ATTRIBUTE, new ArrayList<>(Arrays.asList(assetId)));
            userAttributes.put(RESTRICTED_ATTRIBUTE, new ArrayList<>(Collections.singletonList("true")));
        } else {
            userAttributes.remove(ASSETS_ATTRIBUTE);
            userAttributes.remove(RESTRICTED_ATTRIBUTE);
        }
    }

    public static boolean isRestrictedUser(Map<String, Object> accessTokenClaims) {
        if (!accessTokenClaims.containsKey(RESTRICTED_CLAIM))
            return false;
        return (Boolean) accessTokenClaims.get(RESTRICTED_CLAIM);
    }

    public static String[] getAssetIdsFromUserAttributes(List<Object[]> userAttributes) {
        List<String> assetIds = new ArrayList<>();
        for (Object[] userAttribute : userAttributes) {
            String name = (String) userAttribute[0];
            String value = (String) userAttribute[1];
            if (name.equals(ASSETS_ATTRIBUTE))
                assetIds.add(value);
        }
        return assetIds.toArray(new String[assetIds.size()]);
    }

}
