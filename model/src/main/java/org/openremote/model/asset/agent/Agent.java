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
package org.openremote.model.asset.agent;

import org.kie.api.task.model.User;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.security.UserPasswordCredentials;
import org.openremote.model.value.Values;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openremote.model.util.TextUtil.isNullOrEmpty;

/**
 * An agent is a special type of {@link Asset} with an AssetType of {@link AssetType#AGENT}.
 * <p>
 * If the agent doesn't have an attribute with the name {@link #URL_ATTRIBUTE_NAME} then it is assumed that the agent is
 * running in the current VM (i.e. a local agent).
 * <p>
 * Currently only local agents are supported.
 */
public final class Agent {

    protected static final String URL_ATTRIBUTE_NAME = "url";
    protected static final String CREDENTIALS_ATTRIBUTE_NAME = "credentials";
    protected Asset asset;

    private Agent() {
    }

    public static void initAgent(Asset asset) {
        asset.setType(AssetType.AGENT);
    }

    public static boolean hasUrl(Asset asset) {
        return asset != null && asset
            .getAttribute(URL_ATTRIBUTE_NAME)
            .flatMap(AbstractValueHolder::getValueAsString)
            .isPresent();
    }

    public static Optional<String> getUrl(Asset asset) {
        return asset == null ? Optional.empty() : asset
            .getAttribute(URL_ATTRIBUTE_NAME)
            .flatMap(AbstractValueHolder::getValueAsString);
    }

    public static void setUrl(Asset asset, String url) {
        if (asset != null) {
            if (isNullOrEmpty(url)) {
                asset.removeAttribute(URL_ATTRIBUTE_NAME);
            } else {
                asset.replaceAttribute(new AssetAttribute(URL_ATTRIBUTE_NAME, AttributeType.STRING, Values.create(url)));
            }
        }
    }

    public static boolean hasCredentials(Asset asset) {
        return asset != null && asset
            .getAttribute(CREDENTIALS_ATTRIBUTE_NAME)
            .filter(UserPasswordCredentials::isUserPasswordCredentials)
            .isPresent();
    }

    public static Optional<UserPasswordCredentials> getCredentials(Asset asset) {
        return asset == null ? Optional.empty() : asset
            .getAttribute(CREDENTIALS_ATTRIBUTE_NAME)
            .flatMap(AbstractValueHolder::getValueAsObject)
            .flatMap(UserPasswordCredentials::fromValue);
    }

    public static void setCredentials(Asset asset, UserPasswordCredentials credentials) {
        if (asset != null) {
            if (Objects.isNull(credentials)) {
                asset.removeAttribute(CREDENTIALS_ATTRIBUTE_NAME);
            } else {
                asset.replaceAttribute(new AssetAttribute(CREDENTIALS_ATTRIBUTE_NAME, AttributeType.OBJECT, credentials.toObjectValue()));
            }
        }
    }

    public static Optional<AssetAttribute> getProtocolConfiguration(Asset asset, String protocolConfigurationName) {
        if (asset == null || isNullOrEmpty(protocolConfigurationName)) {
            return Optional.empty();
        }

        return asset.getAttribute(protocolConfigurationName)
            .filter(ProtocolConfiguration::isProtocolConfiguration);
    }

    public static List<AssetAttribute> getProtocolConfigurations(Asset asset) {
        if (asset == null) {
            return Collections.emptyList();
        }

        return asset.getAttributesStream()
            .filter(ProtocolConfiguration::isProtocolConfiguration)
            .collect(Collectors.toList());
    }
}
