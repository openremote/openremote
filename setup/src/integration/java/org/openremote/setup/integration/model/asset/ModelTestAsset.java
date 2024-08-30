package org.openremote.setup.integration.model.asset;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import java.io.Serializable;
import java.util.Date;

@Entity
public class ModelTestAsset extends Asset<ModelTestAsset> {

    public enum TestValue {
        ENUM_1,
        ENUM_2,
        ENUM_3,
        ENUM_4
    }

    public static class TestObject implements Serializable {
        @Min(100)
        @Max(200)
        protected int range;
        @NotNull
        protected Boolean active;

        public TestObject(int range, Boolean active) {
            this.range = range;
            this.active = active;
        }

        public int getRange() {
            return range;
        }

        public Boolean getActive() {
            return active;
        }
    }

    public static final ValueDescriptor<TestValue> TEST_VALUE_DESCRIPTOR = new ValueDescriptor<>("testValue", TestValue.class);
    public static final ValueDescriptor<TestObject> TEST_OBJECT_VALUE_DESCRIPTOR = new ValueDescriptor<>("testObjectValue", TestObject.class);

    public static final AttributeDescriptor<Integer> REQUIRED_POSITIVE_INT_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("positiveInt", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.NotNull()))
    );

    public static final AttributeDescriptor<Integer> NEGATIVE_INT_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("negativeInt", ValueType.NEGATIVE_INTEGER).withOptional(true);

    public static final AttributeDescriptor<String> SIZE_STRING_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("sizeString", ValueType.TEXT,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Size(5,10)))
    ).withOptional(true);
    public static final AttributeDescriptor<ValueType.DoubleMap> SIZE_MAP_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("sizeMap", ValueType.NUMBER_MAP,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Size(2,3)))
    ).withOptional(true);
    public static final AttributeDescriptor<String[]> SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("sizeArray", ValueType.TEXT.asArray(),
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Size(2,3)))
    ).withOptional(true);

    public static final AttributeDescriptor<TestValue> ALLOWED_VALUES_ENUM_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("allowedValuesEnum", TEST_VALUE_DESCRIPTOR).withOptional(true);
    public static final AttributeDescriptor<TestObject> OBJECT_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("object", TEST_OBJECT_VALUE_DESCRIPTOR).withOptional(true);
    public static final AttributeDescriptor<String> ALLOWED_VALUES_STRING_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("allowedValuesString", ValueType.TEXT,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.AllowedValues("Allowed1", "Allowed2")))
    ).withOptional(true);
    public static final AttributeDescriptor<Double> ALLOWED_VALUES_NUMBER_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("allowedValuesNumber", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.AllowedValues(1.5d, 2.5d)))
    ).withOptional(true);

    public static final AttributeDescriptor<Long> PAST_TIMESTAMP_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("pastTimestamp", ValueType.TIMESTAMP,
        new MetaItem<>(MetaItemType.CONSTRAINTS)
    ).withConstraints(ValueConstraint.constraints(new ValueConstraint.Past())).withOptional(true);

    public static final AttributeDescriptor<Date> PAST_OR_PRESENT_DATE_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("pastOrPresentDate", ValueType.DATE_AND_TIME,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.PastOrPresent()))
    ).withOptional(true);
    public static final AttributeDescriptor<String> FUTURE_ISO8601_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("futureISO8601", ValueType.TIMESTAMP_ISO8601,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Future()))
    ).withOptional(true);
    public static final AttributeDescriptor<Long> FUTURE_OR_PRESENT_TIMESTAMP_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("futureOrPresentTimestamp", ValueType.TIMESTAMP,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.FutureOrPresent()))
    ).withOptional(true);
    public static final AttributeDescriptor<String> NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("notEmptyString", ValueType.TEXT,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.NotEmpty()))
    ).withOptional(true);
    public static final AttributeDescriptor<Integer[]> NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("notEmptyArray", ValueType.INTEGER.asArray(),
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.NotEmpty()))
    ).withOptional(true);
    public static final AttributeDescriptor<ValueType.BooleanMap> NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("notEmptyMap", ValueType.BOOLEAN_MAP,
        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.NotEmpty()))
    ).withOptional(true);

    public static final AttributeDescriptor<String> NOT_BLANK_STRING_ATTRIBUTE_DESCRIPTOR = new AttributeDescriptor<>("notBlankString", ValueType.TEXT,
        new MetaItem<>(MetaItemType.CONSTRAINTS)
    ).withConstraints(ValueConstraint.constraints(new ValueConstraint.NotBlank().setMessage("Not blank custom message")))
        .withOptional(true);

    public static final AssetDescriptor<ModelTestAsset> MODEL_TEST_ASSET_DESCRIPTOR = new AssetDescriptor<>("", "", ModelTestAsset.class);

    protected ModelTestAsset() {
    }

    public ModelTestAsset(String name) {
        super(name);
    }
}
