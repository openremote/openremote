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
package org.openremote.model.validation;

import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
@Repeatable(AssetValid.List.class)
@Constraint(validatedBy = AssetValid.AssetValidValidator.class)
@Documented
@TsIgnore
public @interface AssetValid {

    String message() default "{org.openremote.model.validation.AssetValid.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    @Target({ METHOD, FIELD, ANNOTATION_TYPE, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    @interface List {

        AssetValid[] value();
    }

    /**
     * A JSR-380 validator that uses {@link org.openremote.model.util.ValueUtil} to ensure that the
     * {@link Asset#getAttributes} are valid based on the {@link org.openremote.model.value.AttributeDescriptor}s
     * for the given {@link Asset} type.
     */
    @TsIgnore
    class AssetValidValidator implements ConstraintValidator<AssetValid, Asset<?>> {

        public static final String ASSET_TYPE_INVALID = "{Asset.type.Invalid}";
        public static final String ASSET_ATTRIBUTE_MISSING = "{Asset.attribute.Missing}";
        public static final String ASSET_ATTRIBUTE_TYPE_MISMATCH = "{Asset.attribute.type.Mismatch}";

        @Override
        public boolean isValid(Asset<?> value, ConstraintValidatorContext context) {

            String type = value.getType();
            AssetTypeInfo assetModelInfo = ValueUtil.getAssetInfo(type).orElse(null);

            if (assetModelInfo == null || value.getClass() != assetModelInfo.getAssetDescriptor().getType()) {
                context.buildConstraintViolationWithTemplate(ASSET_TYPE_INVALID).addConstraintViolation();
                return false;
            }

            // Validate the attributes
            AtomicBoolean valid = new AtomicBoolean(true);
            Arrays.stream(assetModelInfo.getAttributeDescriptors())
            .filter(attributeDescriptor -> !attributeDescriptor.isOptional())
            .forEach(requiredAttributeDescriptor -> {
                Attribute<?> foundAttribute = value.getAttribute(requiredAttributeDescriptor).orElse(null);

                if (foundAttribute == null) {
                    context.buildConstraintViolationWithTemplate(ASSET_ATTRIBUTE_MISSING).addPropertyNode("attributes").addPropertyNode(requiredAttributeDescriptor.getName()).addConstraintViolation();
                    valid.set(false);
                }
            });

            value.getAttributes().values().forEach(attribute -> {
                    AttributeDescriptor<?> descriptor = Arrays.stream(assetModelInfo.getAttributeDescriptors())
                        .filter(attributeDescriptor -> attributeDescriptor.getName().equals(attribute.getName()))
                        .findFirst()
                        .orElse(null);

                    if (descriptor != null && !Objects.equals(attribute.getType(), descriptor.getType())) {
                        context.buildConstraintViolationWithTemplate(ASSET_ATTRIBUTE_TYPE_MISMATCH).addPropertyNode("attributes").addPropertyNode(attribute.getName()).addConstraintViolation();
                        valid.set(false);
                    }
                }
            );

            return valid.get();
        }
    }
}
