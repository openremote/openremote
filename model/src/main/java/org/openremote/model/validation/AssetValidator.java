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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A JSR-380 validator that uses {@link org.openremote.model.util.ValueUtil} to ensure that the
 * {@link Asset#getAttributes} conforms to the {@link org.openremote.model.value.AttributeDescriptor}s for the
 * given {@link Asset} type. Checks the following:
 * <ul>
 * <li>Required attributes are present and the type matches the {@link org.openremote.model.value.AttributeDescriptor};
 * if no {@link AssetTypeInfo} can be found for the given {@link Asset} then this step is skipped</li>
 * <li></li>
 * </ul>
 */
@TsIgnore
public
class AssetValidator implements HibernateConstraintValidator<AssetValid, Asset<?>> {

    public static final String ASSET_TYPE_INVALID = "{Asset.type.Invalid}";
    public static final String ASSET_ATTRIBUTE_MISSING = "{Asset.attribute.Missing}";
    public static final String ASSET_ATTRIBUTE_TYPE_MISMATCH = "{Asset.attribute.type.Mismatch}";
    public static final System.Logger LOG = System.getLogger(AssetValidator.class.getName() + "." + SyslogCategory.MODEL_AND_VALUES.name());
    protected ClockProvider clockProvider;
    @Override
    public void initialize(ConstraintDescriptor<AssetValid> constraintDescriptor, HibernateConstraintValidatorInitializationContext initializationContext) {
        clockProvider = initializationContext.getClockProvider();
        HibernateConstraintValidator.super.initialize(constraintDescriptor, initializationContext);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public boolean isValid(Asset<?> asset, ConstraintValidatorContext context) {

        String type = asset.getType();
        AssetTypeInfo assetModelInfo = ValueUtil.getAssetInfo(type).orElse(null);

        // Check the type of the asset matches the descriptor
        if (assetModelInfo != null && asset.getClass() != assetModelInfo.getAssetDescriptor().getType()) {
            context.buildConstraintViolationWithTemplate(ASSET_TYPE_INVALID).addConstraintViolation();
            return false;
        }

        AtomicBoolean valid = new AtomicBoolean(true);

        // Check for required attributes
        if (assetModelInfo != null) {
            Arrays.stream(assetModelInfo.getAttributeDescriptors())
                .filter(attributeDescriptor -> !attributeDescriptor.isOptional())
                .forEach(requiredAttributeDescriptor -> {
                    Attribute<?> foundAttribute = asset.getAttribute(requiredAttributeDescriptor).orElse(null);

                    if (foundAttribute == null) {
                        context.buildConstraintViolationWithTemplate(ASSET_ATTRIBUTE_MISSING).addPropertyNode("attributes").addPropertyNode(requiredAttributeDescriptor.getName()).addConstraintViolation();
                        valid.set(false);
                    }
                });
        }

        // Check attribute types match descriptors and value constraints are met
        asset.getAttributes().values().forEach(attribute -> {
            AttributeDescriptor<?> descriptor = assetModelInfo == null ? null : Arrays.stream(assetModelInfo.getAttributeDescriptors())
                .filter(attributeDescriptor -> attributeDescriptor.getName().equals(attribute.getName()))
                .findFirst()
                .orElse(null);

            // Compare descriptor to the attribute value
            if (descriptor != null) {
                if (!Objects.equals(attribute.getType(), descriptor.getType())) {
                    context.buildConstraintViolationWithTemplate(ASSET_ATTRIBUTE_TYPE_MISMATCH).addPropertyNode("attributes").addPropertyNode(attribute.getName()).addConstraintViolation();
                    valid.set(false);
                }

                // Attribute value type must match the attributes value descriptor
                if (attribute.getValue().isPresent()) {
                    if (!descriptor.getType().getType().isAssignableFrom(attribute.getValue().map(Object::getClass).get())) {
                        context.buildConstraintViolationWithTemplate(ASSET_ATTRIBUTE_TYPE_MISMATCH).addPropertyNode("attributes").addPropertyNode(attribute.getName()).addConstraintViolation();
                        valid.set(false);
                    }
                }
            }

            // Validate the value against the various constraints
            if (!ValueUtil.validateValue(descriptor, attribute.getType(), attribute, clockProvider.getClock().instant(), context, attribute.getValue().orElse(null))) {
                valid.set(false);
            }
        });

        return valid.get();
    }
}
