/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.asset.impl;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.console.ConsoleProviders;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class ConsoleAsset extends Asset<ConsoleAsset> {

    public static final AttributeDescriptor<String> CONSOLE_NAME = new AttributeDescriptor<>("consoleName", ValueType.TEXT);
    public static final AttributeDescriptor<String> CONSOLE_VERSION = new AttributeDescriptor<>("consoleVersion", ValueType.TEXT);
    public static final AttributeDescriptor<String> CONSOLE_PLATFORM = new AttributeDescriptor<>("consolePlatform", ValueType.TEXT);
    public static final AttributeDescriptor<ConsoleProviders> CONSOLE_PROVIDERS = new AttributeDescriptor<>("consoleProviders", ValueType.CONSOLE_PROVIDERS);

    public static final AssetDescriptor<ConsoleAsset> DESCRIPTOR = new AssetDescriptor<>("monitor-cellphone", null, ConsoleAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ConsoleAsset() {
    }

    public ConsoleAsset(String name) {
        super(name);
    }

    public Optional<String> getConsoleName() {
        return getAttributes().getValue(CONSOLE_NAME);
    }

    public ConsoleAsset setConsoleName(String name) {
        TextUtil.requireNonNullAndNonEmpty(name);
        getAttributes().getOrCreate(CONSOLE_NAME).setValue(name);
        return this;
    }

    public Optional<String> getConsoleVersion() {
        return getAttributes().getValue(CONSOLE_VERSION);
    }

    public ConsoleAsset setConsoleVersion(String version) {
        TextUtil.requireNonNullAndNonEmpty(version);
        getAttributes().getOrCreate(CONSOLE_VERSION).setValue(version);
        return this;
    }

    public Optional<String> getConsolePlatform() {
        return getAttributes().getValue(CONSOLE_PLATFORM);
    }

    public ConsoleAsset setConsolePlatform(String platform) {
        TextUtil.requireNonNullAndNonEmpty(platform);
        getAttributes().getOrCreate(CONSOLE_PLATFORM).setValue(platform);
        return this;
    }

    public Optional<ConsoleProviders> getConsoleProviders() {
        return getAttributes().getValue(CONSOLE_PROVIDERS);
    }

    public ConsoleAsset setConsoleProviders(ConsoleProviders providers) {
        getAttributes().getOrCreate(CONSOLE_PROVIDERS).setValue(providers);
        return this;
    }

    public ConsoleAsset setConsoleProvider(String name, ConsoleProvider consoleProvider) {
        ConsoleProviders providers = getAttributes().getValue(CONSOLE_PROVIDERS).orElse(new ConsoleProviders());
        providers.put(name, consoleProvider);
        setConsoleProviders(providers);
        return this;
    }

    // TODO: Replace with standard validation
//    public static boolean validateConsoleConfiguration(Asset<?> asset, List<ValidationFailure> validationFailures) {
//        boolean valid = isConsole(asset);
//
//        if (!valid) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(Asset.AssetTypeFailureReason.ASSET_TYPE_MISMATCH));
//            }
//            return false;
//        }
//
//        if (!getConsoleName(asset).isPresent()) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ConsoleConfiguration.ValidationFailureReason.NAME_MISSING_OR_INVALID));
//            }
//            valid = false;
//        }
//
//        if (!getConsoleVersion(asset).isPresent()) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ConsoleConfiguration.ValidationFailureReason.VERSION_MISSING_OR_INVALID));
//            }
//            valid = false;
//        }
//
//        if (!getConsolePlatform(asset).isPresent()) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ConsoleConfiguration.ValidationFailureReason.PLATFORM_MISSING_OR_INVALID));
//            }
//            valid = false;
//        }
//
//        Value providerValue = asset.getAttribute(Asset.CONSOLE_PROVIDERS.getAttributeName()).flatMap(AbstractValueHolder::getValue).orElse(null);
//
//        if (providerValue != null && !getConsoleProviders(asset).isPresent()) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ConsoleConfiguration.ValidationFailureReason.PROVIDERS_INVALID));
//            }
//            valid = false;
//        }
//
//        return valid;
//    }

    // TODO: Replace with standard validation
//    public static boolean validateConsoleRegistration(ConsoleRegistration consoleRegistration, List<ValidationFailure> validationFailures) {
//        boolean valid = true;
//
//        if (consoleRegistration == null) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_INVALID));
//            }
//            return false;
//        }
//
//        if (TextUtil.isNullOrEmpty(consoleRegistration.getName())) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ConsoleConfiguration.ValidationFailureReason.NAME_MISSING_OR_INVALID));
//            }
//            valid = false;
//        }
//
//        if (TextUtil.isNullOrEmpty(consoleRegistration.getVersion())) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ConsoleConfiguration.ValidationFailureReason.VERSION_MISSING_OR_INVALID));
//            }
//            valid = false;
//        }
//
//        if (TextUtil.isNullOrEmpty(consoleRegistration.getPlatform())) {
//            if (validationFailures != null) {
//                validationFailures.add(new ValidationFailure(ConsoleConfiguration.ValidationFailureReason.PLATFORM_MISSING_OR_INVALID));
//            }
//            valid = false;
//        }
//
//        return valid;
//    }
}
