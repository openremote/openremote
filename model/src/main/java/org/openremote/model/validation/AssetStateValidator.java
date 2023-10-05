package org.openremote.model.validation;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidator;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorInitializationContext;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * A JSR-380 validator that uses {@link ValueUtil} to ensure that the
 * {@link Asset#getAttributes} conforms to the {@link AttributeDescriptor}s for the
 * given {@link Asset} type. Checks the following:
 * <ul>
 * <li>Required attributes are present and the type matches the {@link AttributeDescriptor};
 * if no {@link AssetTypeInfo} can be found for the given {@link Asset} then this step is skipped</li>
 * <li></li>
 * </ul>
 */
@TsIgnore
public class AssetStateValidator implements HibernateConstraintValidator<AssetStateValid, AssetStateStore> {

    public static final String ASSET_TYPE_INVALID = "{Asset.type.Invalid}";
    public static final String ASSET_ATTRIBUTE_MISSING = "{Asset.attribute.Missing}";
    public static final String ASSET_ATTRIBUTE_TYPE_MISMATCH = "{Asset.attribute.type.Mismatch}";
    public static final System.Logger LOG = System.getLogger(AssetStateValidator.class.getName() + "." + SyslogCategory.MODEL_AND_VALUES.name());
    protected ClockProvider clockProvider;
    @Override
    public void initialize(ConstraintDescriptor<AssetStateValid> constraintDescriptor, HibernateConstraintValidatorInitializationContext initializationContext) {
        clockProvider = initializationContext.getClockProvider();
        HibernateConstraintValidator.super.initialize(constraintDescriptor, initializationContext);
    }

    @Override
    public boolean isValid(AssetStateStore assetState, ConstraintValidatorContext context) {

        String type = assetState.getAssetType();
        AssetTypeInfo assetModelInfo = ValueUtil.getAssetInfo(type).orElse(null);

        return validateAttribute(assetModelInfo, assetState.getAttribute(), context, clockProvider.getClock().instant());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static boolean validateAttribute(AssetTypeInfo assetModelInfo, Attribute<?> attribute, ConstraintValidatorContext context, Instant now) {
        boolean valid = true;

        AttributeDescriptor<?> descriptor = assetModelInfo == null ? null : assetModelInfo.getAttributeDescriptors().get(attribute.getName());

        // Compare descriptor to the attribute value
        if (descriptor != null) {
            if (!Objects.equals(attribute.getType(), descriptor.getType())) {
                context.buildConstraintViolationWithTemplate(ASSET_ATTRIBUTE_TYPE_MISMATCH).addPropertyNode("attributes").addPropertyNode(attribute.getName()).addConstraintViolation();
                valid = false;
            }

            // Attribute value type must match the attributes value descriptor
            if (attribute.getValue().isPresent()) {
                if (!descriptor.getType().getType().isAssignableFrom(attribute.getValue().map(Object::getClass).get())) {
                    context.buildConstraintViolationWithTemplate(ASSET_ATTRIBUTE_TYPE_MISMATCH).addPropertyNode("attributes").addPropertyNode(attribute.getName()).addConstraintViolation();
                    valid = false;
                }
            }
        }

        // Validate the value against the various constraints
        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable().atKey(attribute.getName());

        if (!ValueUtil.validateValue(descriptor, attribute.getType(), attribute, now, context, pathProvider, attribute.getValue().orElse(null))) {
            valid = false;
        }

        return valid;
    }
}
