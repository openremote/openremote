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
package org.openremote.model.value

import jakarta.validation.ConstraintValidatorContext
import org.openremote.model.attribute.Attribute
import org.openremote.model.util.ValueUtil
import spock.lang.Specification

import java.time.Instant

class ValueUtilTest extends Specification {

    ConstraintValidatorContext context

    def setup() {
        def builder = Stub(ConstraintValidatorContext.ConstraintViolationBuilder)
        def attributesNode = Stub(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext)
        def valueNode = Stub(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext)
        def containerNode = Stub(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext)
        def iterableNode = Stub(ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder)
        def keyNode = Stub(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext)

        context = Stub(ConstraintValidatorContext) {
            buildConstraintViolationWithTemplate(_) >> builder
        }
        builder.addPropertyNode("attributes") >> attributesNode
        attributesNode.addPropertyNode("value") >> valueNode
        valueNode.inContainer(Map, 1) >> containerNode
        containerNode.inIterable() >> iterableNode
        iterableNode.atKey(_) >> keyNode
    }

    def "validating #description returns #expected"() {
        given:
        def descriptor = new AttributeDescriptor(attributeName, valueType)
        def attribute = new Attribute(descriptor)
        if (value != null) {
            attribute.value = value
        }
        ValueUtil.ConstraintViolationPathProvider pathProvider = { constraintViolationBuilder ->
            constraintViolationBuilder
                .addPropertyNode("attributes")
                .addPropertyNode("value")
                .inContainer(Map, 1)
                .inIterable()
                .atKey(attribute.name)
        }

        expect:
        ValueUtil.validateValue(
            descriptor,
            descriptor.type,
            attribute,
            Instant.EPOCH,
            context,
            pathProvider,
            attribute.value.orElse(null)
        ) == expected

        where:
        description                                  | attributeName                   | valueType                                              | value                                                   || expected
        "a positive integer"                         | "positiveNumber"                | ValueType.POSITIVE_INTEGER                             | 1                                                       || true
        "an empty positive integer"                  | "positiveNumber"                | ValueType.POSITIVE_INTEGER                             | null                                                    || true
        "a negative integer as a positive integer"   | "positiveNumber"                | ValueType.POSITIVE_INTEGER                             | -1                                                      || false
        "an array of positive integers"              | "arrayOfPositiveNumbers"         | ValueType.POSITIVE_INTEGER.asArray()                   | [1, 2] as Integer[]                                     || true
        "an empty array of positive integers"        | "arrayOfPositiveNumbers"         | ValueType.POSITIVE_INTEGER.asArray()                   | null                                                    || true
        "an array containing a negative integer"     | "arrayOfPositiveNumbers"         | ValueType.POSITIVE_INTEGER.asArray()                   | [1, -2] as Integer[]                                    || false
        "nested arrays of positive integers"         | "arrayOfArrayOfPositiveNumbers"  | ValueType.POSITIVE_INTEGER.asArray().asArray()         | [[1, 2] as Integer[], [5, 2] as Integer[]] as Integer[][] || true
        "nested arrays containing a negative integer"| "arrayOfArrayOfPositiveNumbers"  | ValueType.POSITIVE_INTEGER.asArray().asArray()         | [[1, 2] as Integer[], [-5, 2] as Integer[]] as Integer[][]|| false
    }
}
