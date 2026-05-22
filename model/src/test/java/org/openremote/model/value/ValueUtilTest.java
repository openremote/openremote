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
package org.openremote.model.value;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.util.ValueUtil;

import jakarta.validation.ConstraintValidatorContext;

public class ValueUtilTest {

  ConstraintValidatorContext context;
  ConstraintValidatorContext.ConstraintViolationBuilder builder;
  ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext node1;
  ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext node2;
  ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext
      containerBuilder;
  ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder containerBuilder2;
  ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext containerBuilder3;

  @BeforeEach
  void setup() {
    context = mock(ConstraintValidatorContext.class);
    builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    node1 =
        mock(
            ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext
                .class);
    node2 =
        mock(
            ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext
                .class);
    containerBuilder =
        mock(
            ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext
                .class);
    containerBuilder2 =
        mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder.class);
    containerBuilder3 =
        mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext.class);

    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);

    when(builder.addPropertyNode("attributes")).thenReturn(node1);

    when(node1.addPropertyNode("value")).thenReturn(node2);

    when(node2.inContainer(Map.class, 1)).thenReturn(containerBuilder);

    when(containerBuilder.inIterable()).thenReturn(containerBuilder2);

    when(containerBuilder2.atKey(any())).thenReturn(containerBuilder3);
  }

  @Test
  public void validatePositiveInteger() {
    AttributeDescriptor<Integer> attributeDescriptor =
        new AttributeDescriptor<>("positiveNumber", ValueType.POSITIVE_INTEGER);

    Attribute<Integer> attribute = new Attribute<>(attributeDescriptor);
    attribute.setValue(1);

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertTrue(valid);
  }

  @Test
  public void validatePositiveIntegerEmptyValue() {
    AttributeDescriptor<Integer> attributeDescriptor =
        new AttributeDescriptor<>("positiveNumber", ValueType.POSITIVE_INTEGER);

    Attribute<Integer> attribute = new Attribute<>(attributeDescriptor);

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertTrue(valid);
  }

  @Test
  public void validatePositiveIntegerFalse() {
    AttributeDescriptor<Integer> attributeDescriptor =
        new AttributeDescriptor<>("positiveNumber", ValueType.POSITIVE_INTEGER);

    Attribute<Integer> attribute = new Attribute<>(attributeDescriptor);
    attribute.setValue(-1);

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertFalse(valid);
  }

  @Test
  public void validateArrayOfPositiveIntegers() {
    AttributeDescriptor<Integer[]> attributeDescriptor =
        new AttributeDescriptor<>("arrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray());

    Attribute<Integer[]> attribute = new Attribute<>(attributeDescriptor);
    attribute.setValue(new Integer[] {1, 2});

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertTrue(valid);
  }

  @Test
  public void validateArrayOfPositiveIntegersEmptyValue() {
    AttributeDescriptor<Integer[]> attributeDescriptor =
        new AttributeDescriptor<>("arrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray());

    Attribute<Integer[]> attribute = new Attribute<>(attributeDescriptor);

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertTrue(valid);
  }

  @Test
  public void validateArrayOfPositiveIntegersFalse() {
    AttributeDescriptor<Integer[]> attributeDescriptor =
        new AttributeDescriptor<>("arrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray());

    Attribute<Integer[]> attribute = new Attribute<>(attributeDescriptor);
    attribute.setValue(new Integer[] {1, -2});

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertFalse(valid);
  }

  @Test
  public void validateArrayOfArrayOfPositiveIntegers() {
    AttributeDescriptor<Integer[][]> attributeDescriptor =
        new AttributeDescriptor<>(
            "arrayOfArrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray().asArray());

    Attribute<Integer[][]> attribute = new Attribute<>(attributeDescriptor);
    attribute.setValue(new Integer[][] {{1, 2}, {5, 2}});

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertTrue(valid);
  }

  @Test
  public void validateArrayOfArrayOfPositiveIntegersFalse() {
    AttributeDescriptor<Integer[][]> attributeDescriptor =
        new AttributeDescriptor<>(
            "arrayOfArrayOfPositiveNumbers", ValueType.POSITIVE_INTEGER.asArray().asArray());

    Attribute<Integer[][]> attribute = new Attribute<>(attributeDescriptor);
    attribute.setValue(new Integer[][] {{1, 2}, {-5, 2}});

    ValueUtil.ConstraintViolationPathProvider pathProvider =
        (constraintViolationBuilder) ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map.class, 1)
                .inIterable()
                .atKey(attribute.getName());

    Boolean valid =
        ValueUtil.validateValue(
            attributeDescriptor,
            attributeDescriptor.getType(),
            attribute,
            Instant.now(),
            context,
            pathProvider,
            attribute.getValue().orElse(null));

    assertFalse(valid);
  }
}
