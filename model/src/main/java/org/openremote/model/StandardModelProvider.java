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
package org.openremote.model;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.*;

import java.util.*;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.MODEL_AND_VALUES;

/**
 * Built in model provider that scans the model classes for asset classes and also includes {@link MetaItemType} and
 * {@link ValueType} classes.
 */
@TsIgnore
@ModelDescriptor(assetType = Asset.class, provider = MetaItemType.class)
@ModelDescriptor(assetType = Asset.class, provider = ValueType.class)
public class StandardModelProvider implements AssetModelProvider {

    protected static Logger LOG = SyslogCategory.getLogger(MODEL_AND_VALUES, StandardModelProvider.class);

    @Override
    public AssetDescriptor<?>[] getAssetDescriptors() {
        return null;
    }

    @Override
    public boolean useAutoScan() {
        return true;
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<AttributeDescriptor<?>>> getAttributeDescriptors() {
        return null;
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<MetaItemDescriptor<?>>> getMetaItemDescriptors() {
        return null;
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<ValueDescriptor<?>>> getValueDescriptors() {
        return null;
    }

    @Override
    public void onAssetModelFinished() {
        // Inject allowed asset types into GroupAsset
        List<ValueConstraint> constraints = ValueType.ASSET_TYPE.getConstraints() != null ? new ArrayList<>(Arrays.asList(ValueType.ASSET_TYPE.getConstraints())): new ArrayList<>();
        constraints.add(new ValueConstraint.AllowedValues(Arrays.stream(ValueUtil.getAssetClasses(null)).map(Class::getSimpleName).toArray()));
        ValueType.ASSET_TYPE.updateConstraints(constraints.toArray(new ValueConstraint[0]));
    }
}
