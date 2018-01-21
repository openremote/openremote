/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstraintViolationReport implements java.io.Serializable {

    public static final String VIOLATION_EXCEPTION_HEADER = "validation-exception";

    protected String exception;
    protected ConstraintViolation[] fieldViolations = new ConstraintViolation[0];
    protected ConstraintViolation[] propertyViolations = new ConstraintViolation[0];
    protected ConstraintViolation[] classViolations = new ConstraintViolation[0];
    protected ConstraintViolation[] parameterViolations = new ConstraintViolation[0];
    protected ConstraintViolation[] returnValueViolations = new ConstraintViolation[0];

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public ConstraintViolation[] getFieldViolations() {
        return fieldViolations;
    }

    public void setFieldViolations(ConstraintViolation[] fieldViolations) {
        this.fieldViolations = fieldViolations;
    }

    public ConstraintViolation[] getPropertyViolations() {
        return propertyViolations;
    }

    public void setPropertyViolations(ConstraintViolation[] propertyViolations) {
        this.propertyViolations = propertyViolations;
    }

    public ConstraintViolation[] getClassViolations() {
        return classViolations;
    }

    public void setClassViolations(ConstraintViolation[] classViolations) {
        this.classViolations = classViolations;
    }

    public ConstraintViolation[] getParameterViolations() {
        return parameterViolations;
    }

    public void setParameterViolations(ConstraintViolation[] parameterViolations) {
        this.parameterViolations = parameterViolations;
    }

    public ConstraintViolation[] getReturnValueViolations() {
        return returnValueViolations;
    }

    public void setReturnValueViolations(ConstraintViolation[] returnValueViolations) {
        this.returnValueViolations = returnValueViolations;
    }

    public boolean hasViolations() {
        return getFieldViolations().length > 0
            || getPropertyViolations().length > 0
            || getClassViolations().length > 0
            || getParameterViolations().length > 0
            || getReturnValueViolations().length > 0;
    }

    public ConstraintViolation[] getAllViolations() {
        List<ConstraintViolation> violations = new ArrayList<>();
        violations.addAll(Arrays.asList(getFieldViolations()));
        violations.addAll(Arrays.asList(getPropertyViolations()));
        violations.addAll(Arrays.asList(getClassViolations()));
        violations.addAll(Arrays.asList(getParameterViolations()));
        violations.addAll(Arrays.asList(getReturnValueViolations()));
        return violations.toArray(new ConstraintViolation[violations.size()]);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "exception='" + exception + '\'' +
            ", fieldViolations=" + Arrays.toString(fieldViolations) +
            ", propertyViolations=" + Arrays.toString(propertyViolations) +
            ", classViolations=" + Arrays.toString(classViolations) +
            ", parameterViolations=" + Arrays.toString(parameterViolations) +
            ", returnValueViolations=" + Arrays.toString(returnValueViolations) +
            '}';
    }
}