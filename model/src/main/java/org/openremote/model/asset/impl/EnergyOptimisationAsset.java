/*
 * Copyright 2021, OpenRemote Inc.
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
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import javax.persistence.Entity;

import java.util.Optional;

import static org.openremote.model.Constants.*;
import static org.openremote.model.value.MetaItemType.READ_ONLY;
import static org.openremote.model.value.ValueType.*;

@Entity
public class EnergyOptimisationAsset extends Asset<EnergyOptimisationAsset> {

    public static final AttributeDescriptor<Integer> FINANCIAL_WEIGHTING = new AttributeDescriptor<>("financialWeighting", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Double> INTERVAL_SIZE = new AttributeDescriptor<>("intervalSize", POSITIVE_NUMBER);
    public static final AttributeDescriptor<Boolean> OPTIMISATION_DISABLED = new AttributeDescriptor<>("optimisationDisabled", BOOLEAN, new MetaItem<>(READ_ONLY));
    public static final AttributeDescriptor<Double> FINANCIAL_SAVING = new AttributeDescriptor<>("financialSaving", NUMBER, new MetaItem<>(READ_ONLY)).withUnits("EUR");
    public static final AttributeDescriptor<Double> CARBON_SAVING = new AttributeDescriptor<>("carbonSaving", NUMBER, new MetaItem<>(READ_ONLY)).withUnits(UNITS_KILO, UNITS_GRAM);

    public static final AssetDescriptor<EnergyOptimisationAsset> DESCRIPTOR = new AssetDescriptor<>("flash", "C4DB0D", EnergyOptimisationAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected EnergyOptimisationAsset() {
    }

    public EnergyOptimisationAsset(String name) {
        super(name);
    }

    public Optional<Integer> getFinancialWeighting() {
        return getAttribute(FINANCIAL_WEIGHTING).flatMap(AbstractNameValueHolder::getValue);
    }

    public EnergyOptimisationAsset setFinancialWeighting(Integer value) {
        getAttributes().getOrCreate(FINANCIAL_WEIGHTING).setValue(value);
        return this;
    }

    public Optional<Double> getIntervalSize() {
        return getAttribute(INTERVAL_SIZE).flatMap(AbstractNameValueHolder::getValue);
    }

    public EnergyOptimisationAsset setIntervalSize(Double value) {
        getAttributes().getOrCreate(INTERVAL_SIZE).setValue(value);
        return this;
    }

    public Optional<Boolean> isOptimisationDisabled() {
        return getAttribute(OPTIMISATION_DISABLED).flatMap(AbstractNameValueHolder::getValue);
    }

    public EnergyOptimisationAsset setOptimisationDisabled(Boolean value) {
        getAttributes().getOrCreate(OPTIMISATION_DISABLED).setValue(value);
        return this;
    }

    public Optional<Double> getFinancialSaving() {
        return getAttribute(FINANCIAL_SAVING).flatMap(AbstractNameValueHolder::getValue);
    }

    public EnergyOptimisationAsset setFinancialSaving(Double value) {
        getAttributes().getOrCreate(FINANCIAL_SAVING).setValue(value);
        return this;
    }

    public Optional<Double> getCarbonSaving() {
        return getAttribute(CARBON_SAVING).flatMap(AbstractNameValueHolder::getValue);
    }

    public EnergyOptimisationAsset setCarbonSaving(Double value) {
        getAttributes().getOrCreate(CARBON_SAVING).setValue(value);
        return this;
    }
}
