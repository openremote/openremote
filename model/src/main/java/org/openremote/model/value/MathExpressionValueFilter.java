package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.openremote.model.util.ValueUtil;

import java.util.Optional;

@JsonSchemaTitle("Mathematical Expression")
@JsonTypeName(MathExpressionValueFilter.NAME)
@JsonClassDescription("Performs the mathematical expression submitted using Exp4j (https://www.objecthunter.net/exp4j). " +
        "Refer to that website for more information about formatting, permitted operations, etc." +
        "\n Use \"x\" as the value coming into the filter.")

public class MathExpressionValueFilter extends ValueFilter {

    public static final String NAME = "mathExpression";

    public String expression;

    @JsonCreator
    public MathExpressionValueFilter(@JsonProperty("expression") String expression) {
        this.expression = expression;
    }

    @Override
    public Object filter(Object value) {
        Optional<Double> coercedValue = ValueUtil.getDoubleCoerced(value);
        if (coercedValue.isEmpty()) return null;

        Expression e = new ExpressionBuilder(expression)
                .variables("x")
                .build()
                .setVariable("x", coercedValue.get());
        return e.evaluate();
    }
}
