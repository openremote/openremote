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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.openremote.model.util.JSONSchemaUtil.JsonSchemaTitle;
import org.openremote.model.util.ValueUtil;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

@JsonSchemaTitle("Mathematical Expression")
@JsonTypeName(MathExpressionValueFilter.NAME)
@JsonClassDescription(
    "Performs the mathematical expression submitted using Exp4j (https://www.objecthunter.net/exp4j). "
        + "Refer to that website for more information about formatting, permitted operations, etc."
        + "\n Use \"x\" as the value coming into the filter.")
public class MathExpressionValueFilter extends ValueFilter {

  public static final String NAME = "mathExpression";

  public String expression;

  @JsonCreator
  public MathExpressionValueFilter(@JsonProperty("expression") String expression) {
    this.expression = expression;
  }

  @Override
  public Double filter(Object value) {
    return ValueUtil.getDoubleCoerced(value)
        .map(
            v -> {
              Expression e =
                  new ExpressionBuilder(expression).variables("x").build().setVariable("x", v);
              return e.evaluate();
            })
        .orElse(null);
  }
}
