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
package org.openremote.model.console;

import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.Meta;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openremote.model.attribute.MetaItemType.ACCESS_PUBLIC_WRITE;
import static org.openremote.model.attribute.MetaItemType.ACCESS_RESTRICTED_WRITE;
import static org.openremote.model.attribute.MetaItemType.RULE_STATE;

public final class ConsoleConfiguration {

    public enum ValidationFailureReason implements ValidationFailure.Reason {
        NAME_MISSING_OR_INVALID,
        VERSION_MISSING_OR_INVALID,
        PLATFORM_MISSING_OR_INVALID,
        PROVIDERS_INVALID
    }

    private ConsoleConfiguration() {}

    public static Asset initConsoleConfiguration(Asset asset, String name, String version, String platform, Map<String, ConsoleProvider> providerMap, boolean allowPublicLocationWrite, boolean allowRestrictedLocationWrite) {
        if (!isConsole(asset)) {
            throw new IllegalArgumentException("Asset must be of type console");
        }
        TextUtil.requireNonNullAndNonEmpty(name);
        TextUtil.requireNonNullAndNonEmpty(version);
        TextUtil.requireNonNullAndNonEmpty(platform);

        setConsoleName(asset, name);
        setConsoleVersion(asset, version);
        setConsolePlatform(asset, platform);
        setConsolProviders(asset, providerMap);

        asset.setAccessPublicRead(true);

        Meta locationMeta = new Meta(new MetaItem(RULE_STATE));
        if (allowPublicLocationWrite) {
            locationMeta.add(new MetaItem(ACCESS_PUBLIC_WRITE));
        }
        if (allowRestrictedLocationWrite) {
            locationMeta.add(new MetaItem(ACCESS_RESTRICTED_WRITE));
        }
        asset.replaceAttribute(
            new AssetAttribute(AttributeType.LOCATION)
                .setMeta(locationMeta)
        );

        return asset;
    }

    public static boolean isConsole(Asset asset) {
        return asset != null && asset.getWellKnownType() == AssetType.CONSOLE;
    }

    public static boolean validateConsoleConfiguration(Asset asset, List<ValidationFailure> validationFailures) {
        boolean valid = isConsole(asset);

        if (!valid) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(Asset.AssetTypeFailureReason.ASSET_TYPE_MISMATCH));
            }
            return false;
        }

        if (!getConsoleName(asset).isPresent()) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValidationFailureReason.NAME_MISSING_OR_INVALID));
            }
            valid = false;
        }

        if (!getConsoleVersion(asset).isPresent()) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValidationFailureReason.VERSION_MISSING_OR_INVALID));
            }
            valid = false;
        }

        if (!getConsolePlatform(asset).isPresent()) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValidationFailureReason.PLATFORM_MISSING_OR_INVALID));
            }
            valid = false;
        }

        Value providerValue = asset.getAttribute(AttributeType.CONSOLE_PROVIDERS.getAttributeName()).flatMap(AbstractValueHolder::getValue).orElse(null);

        if (providerValue != null && !getConsoleProviders(asset).isPresent()) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValidationFailureReason.PROVIDERS_INVALID));
            }
            valid = false;
        }

        return valid;
    }

    public static boolean validateConsoleRegistration(ConsoleRegistration consoleRegistration, List<ValidationFailure> validationFailures) {
        boolean valid = true;

        if (consoleRegistration == null) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID));
            }
            return false;
        }

        if (TextUtil.isNullOrEmpty(consoleRegistration.getName())) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValidationFailureReason.NAME_MISSING_OR_INVALID));
            }
            valid = false;
        }

        if (TextUtil.isNullOrEmpty(consoleRegistration.getVersion())) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValidationFailureReason.VERSION_MISSING_OR_INVALID));
            }
            valid = false;
        }

        if (TextUtil.isNullOrEmpty(consoleRegistration.getPlatform())) {
            if (validationFailures != null) {
                validationFailures.add(new ValidationFailure(ValidationFailureReason.PLATFORM_MISSING_OR_INVALID));
            }
            valid = false;
        }

        return valid;
    }

    public static Optional<String> getConsoleName(Asset asset) {
        return asset == null ? Optional.empty() : asset.getAttribute(AttributeType.CONSOLE_NAME).flatMap(AbstractValueHolder::getValueAsString);
    }

    public static Asset setConsoleName(Asset asset, String name) {
        Objects.requireNonNull(asset);
        TextUtil.requireNonNullAndNonEmpty(name);
        asset.replaceAttribute(new AssetAttribute(AttributeType.CONSOLE_NAME,
                               Values.create(name)));
        return asset;
    }

    public static Optional<String> getConsoleVersion(Asset asset) {
        return asset == null ? Optional.empty() : asset.getAttribute(AttributeType.CONSOLE_VERSION).flatMap(AbstractValueHolder::getValueAsString);
    }

    public static Asset setConsoleVersion(Asset asset, String version) {
        Objects.requireNonNull(asset);
        TextUtil.requireNonNullAndNonEmpty(version);
        asset.replaceAttribute(new AssetAttribute(AttributeType.CONSOLE_VERSION,
                                                                   Values.create(version)));
        return asset;
    }

    public static Optional<String> getConsolePlatform(Asset asset) {
        return asset == null ? Optional.empty() : asset.getAttribute(AttributeType.CONSOLE_PLATFORM).flatMap(AbstractValueHolder::getValueAsString);
    }

    public static Asset setConsolePlatform(Asset asset, String platform) {
        Objects.requireNonNull(asset);
        TextUtil.requireNonNullAndNonEmpty(platform);
        asset.replaceAttribute(new AssetAttribute(AttributeType.CONSOLE_PLATFORM,
                                                                   Values.create(platform)));
        return asset;
    }

    public static Optional<Map<String, ConsoleProvider>> getConsoleProviders(Asset asset) {
        return asset == null ? Optional.empty() : asset.getAttribute(AttributeType.CONSOLE_PROVIDERS).flatMap(ConsoleConfiguration::getConsoleProviders);
    }

    public static Optional<ConsoleProvider> getConsoleProvider(Asset asset, String providerName) {
        return asset == null || TextUtil.isNullOrEmpty(providerName) ? Optional.empty() :
            asset.getAttribute(AttributeType.CONSOLE_PROVIDERS)
                .flatMap(AbstractValueHolder::getValueAsObject)
                .flatMap(obj -> obj.getObject(providerName))
                .flatMap(ConsoleProvider::fromValue);
    }

    public static Asset setConsolProviders(Asset asset, Map<String, ConsoleProvider> consoleProviderMap) {
        Objects.requireNonNull(asset);
        if (consoleProviderMap == null || consoleProviderMap.isEmpty()) {
            asset.removeAttribute(AttributeType.CONSOLE_PROVIDERS.getAttributeName());
            return asset;
        }

        asset.replaceAttribute(new AssetAttribute(AttributeType.CONSOLE_PROVIDERS,
                                                                   ConsoleProvider.toValue(consoleProviderMap)));
        return asset;
    }

    public static void addOrReplaceConsoleProvider(Asset asset, String name, ConsoleProvider consoleProvider) {
        Objects.requireNonNull(asset);
        TextUtil.requireNonNullAndNonEmpty(name);
        AssetAttribute providerAttribute = asset.getAttribute(AttributeType.CONSOLE_PROVIDERS).orElseGet(() -> {
            AssetAttribute attr = new AssetAttribute(AttributeType.CONSOLE_PROVIDERS, Values.createObject());
            asset.addAttributes(attr);
            return attr;
        });
        ObjectValue providerObj = Values.getObject(providerAttribute.getValue().orElseGet(() -> {
            Value val = Values.createObject();
            providerAttribute.setValue(val);
            return val;
        })).orElseThrow(() ->
            new IllegalStateException("Console provider attribute value is not an object")
        );

        if (consoleProvider == null) {
            providerObj.remove(name);
            if (!providerObj.hasKeys()) {
                asset.removeAttribute(AttributeType.CONSOLE_PROVIDERS.getAttributeName());
            }
        } else {
            providerObj.put(name, consoleProvider.toValue());
        }
    }

    public static Optional<Map<String, ConsoleProvider>> getConsoleProviders(AssetAttribute attribute) {
        if (attribute == null || !AttributeType.CONSOLE_PROVIDERS.getAttributeName().equals(attribute.getName().orElse(null))) {
            return Optional.empty();
        }

        return attribute.getValueAsObject().map(obj ->
            obj.stream()
                .map(providerNameValuePair ->
                new Pair<>(providerNameValuePair.key, ConsoleProvider.fromValue(providerNameValuePair.value).orElse(null)))
                .filter(pair -> pair.value != null)
                .collect(Collectors.toMap(pair -> pair.key, pair -> pair.value)));
    }
}
