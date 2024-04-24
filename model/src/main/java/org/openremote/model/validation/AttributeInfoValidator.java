package org.openremote.model.validation;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidator;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorInitializationContext;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;

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
public class AttributeInfoValidator implements HibernateConstraintValidator<AttributeInfoValid, AttributeInfo> {

    public static final System.Logger LOG = System.getLogger(AttributeInfoValidator.class.getName() + "." + SyslogCategory.MODEL_AND_VALUES.name());
    protected ClockProvider clockProvider;
    @Override
    public void initialize(ConstraintDescriptor<AttributeInfoValid> constraintDescriptor, HibernateConstraintValidatorInitializationContext initializationContext) {
        clockProvider = initializationContext.getClockProvider();
        HibernateConstraintValidator.super.initialize(constraintDescriptor, initializationContext);
    }

    @Override
    public boolean isValid(AttributeInfo attributeInfo, ConstraintValidatorContext context) {

        String type = attributeInfo.getAssetType();
        AssetTypeInfo assetModelInfo = ValueUtil.getAssetInfo(type).orElse(null);

        return AssetValidator.validateNameValueMetaHolder(assetModelInfo, attributeInfo, context, clockProvider.getClock().instant());
    }
}
