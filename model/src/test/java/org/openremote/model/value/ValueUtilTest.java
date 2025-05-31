package org.openremote.model.value;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.util.ValueUtil;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueUtilTest {

    ConstraintValidatorContext context;
    ConstraintValidatorContext.ConstraintViolationBuilder builder;
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext node1;
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext node2;
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext containerBuilder;
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder containerBuilder2;
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext containerBuilder3;

    @BeforeEach
    void setup() {
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        node1 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        node2 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        containerBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        containerBuilder2 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder.class);
        containerBuilder3 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(builder);

        when(builder.addPropertyNode("attributes"))
                .thenReturn(node1);

        when(node1.addPropertyNode("value"))
                .thenReturn(node2);

        when(node2.inContainer(Map.class, 1))
                .thenReturn(containerBuilder);

        when(containerBuilder.inIterable())
                .thenReturn(containerBuilder2);

        when(containerBuilder2.atKey(any()))
                .thenReturn(containerBuilder3);
    }

    @Test
    public void validatePositiveInteger() {
        AttributeDescriptor<Integer> attributeDescriptor = new AttributeDescriptor<>("positiveNumber", ValueType.POSITIVE_INTEGER);

        Attribute<Integer> attribute = new Attribute<>(attributeDescriptor);
        attribute.setValue(1);

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertTrue(valid);
    }

    @Test
    public void validatePositiveIntegerEmptyValue() {
        AttributeDescriptor<Integer> attributeDescriptor = new AttributeDescriptor<>("positiveNumber", ValueType.POSITIVE_INTEGER);

        Attribute<Integer> attribute = new Attribute<>(attributeDescriptor);

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertTrue(valid);
    }

    @Test
    public void validatePositiveIntegerFalse() {
        AttributeDescriptor<Integer> attributeDescriptor = new AttributeDescriptor<>("positiveNumber", ValueType.POSITIVE_INTEGER);

        Attribute<Integer> attribute = new Attribute<>(attributeDescriptor);
        attribute.setValue(-1);

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertFalse(valid);
    }

    @Test
    public void validateArrayOfPositiveIntegers() {
        AttributeDescriptor<Integer[]> attributeDescriptor = new AttributeDescriptor<>("arrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray());

        Attribute<Integer[]> attribute = new Attribute<>(attributeDescriptor);
        attribute.setValue(new Integer[]{1, 2});

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertTrue(valid);
    }

    @Test
    public void validateArrayOfPositiveIntegersEmptyValue() {
        AttributeDescriptor<Integer[]> attributeDescriptor = new AttributeDescriptor<>("arrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray());

        Attribute<Integer[]> attribute = new Attribute<>(attributeDescriptor);

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertTrue(valid);
    }

    @Test
    public void validateArrayOfPositiveIntegersFalse() {
        AttributeDescriptor<Integer[]> attributeDescriptor = new AttributeDescriptor<>("arrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray());

        Attribute<Integer[]> attribute = new Attribute<>(attributeDescriptor);
        attribute.setValue(new Integer[]{1, -2});

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertFalse(valid);
    }

    @Test
    public void validateArrayOfArrayOfPositiveIntegers() {
        AttributeDescriptor<Integer[][]> attributeDescriptor = new AttributeDescriptor<>("arrayOfArrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray().asArray());

        Attribute<Integer[][]> attribute = new Attribute<>(attributeDescriptor);
        attribute.setValue(new Integer[][]{{1, 2},{5, 2}});

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertTrue(valid);
    }

    @Test
    public void validateArrayOfArrayOfPositiveIntegersFalse() {
        AttributeDescriptor<Integer[][]> attributeDescriptor = new AttributeDescriptor<>("arrayOfArrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray().asArray());

        Attribute<Integer[][]> attribute = new Attribute<>(attributeDescriptor);
        attribute.setValue(new Integer[][]{{1, 2},{-5, 2}});

        ValueUtil.ConstraintViolationPathProvider pathProvider = (constraintViolationBuilder) ->
                constraintViolationBuilder
                        .addPropertyNode("attributes")
                        .addPropertyNode("value")
                        .inContainer(Map.class, 1)
                        .inIterable().atKey(attribute.getName());

        Boolean valid = ValueUtil.validateValue(attributeDescriptor, attributeDescriptor.getType(), attribute, Instant.now(), context, pathProvider, attribute.getValue().orElse(null));

        assertFalse(valid);
    }

}
