package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

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

        if (value == null) return null;

        if (value instanceof String){
            try{
                value = Double.valueOf((String) value);
            } catch (Exception ignored) {}
        }

        if (!(value instanceof Number)) {
            return null;
        }

        Expression e = new ExpressionBuilder(expression)
                .variables("x")
                .build()
                .setVariable("x", ((Number) value).doubleValue());
        return e.evaluate();
    }
}
