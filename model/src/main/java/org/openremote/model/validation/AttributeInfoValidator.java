/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.validation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidator;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorInitializationContext;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.metadata.ConstraintDescriptor;

/**
 * A JSR-380 validator that uses {@link ValueUtil} to ensure that the {@link Asset#getAttributes}
 * conforms to the {@link AttributeDescriptor}s for the given {@link Asset} type. Checks the
 * following:
 *
 * <ul>
 *   <li>Required attributes are present and the type matches the {@link AttributeDescriptor}; if no
 *       {@link AssetTypeInfo} can be found for the given {@link Asset} then this step is skipped
 *   <li>
 * </ul>
 */
@TsIgnore
public class AttributeInfoValidator
    implements HibernateConstraintValidator<AttributeInfoValid, AttributeInfo> {

  public static final System.Logger LOG =
      System.getLogger(
          AttributeInfoValidator.class.getName() + "." + SyslogCategory.MODEL_AND_VALUES.name());
  protected ClockProvider clockProvider;

  @Override
  public void initialize(
      ConstraintDescriptor<AttributeInfoValid> constraintDescriptor,
      HibernateConstraintValidatorInitializationContext initializationContext) {
    clockProvider = initializationContext.getClockProvider();
    HibernateConstraintValidator.super.initialize(constraintDescriptor, initializationContext);
  }

  @Override
  public boolean isValid(AttributeInfo attributeInfo, ConstraintValidatorContext context) {

    String type = attributeInfo.getAssetType();
    AssetTypeInfo assetModelInfo = ValueUtil.getAssetInfo(type).orElse(null);

    return AssetValidator.validateNameValueMetaHolder(
        assetModelInfo, attributeInfo, context, clockProvider.getClock().instant());
  }
}
