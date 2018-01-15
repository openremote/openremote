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
package org.openremote.model.http;

import jsinterop.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class ConstraintViolationReport {

    @JsProperty
    protected String exception;

    @JsProperty
    protected ConstraintViolation[] fieldViolations;

    @JsProperty
    protected ConstraintViolation[] propertyViolations;

    @JsProperty
    protected ConstraintViolation[] classViolations;

    @JsProperty
    protected ConstraintViolation[] parameterViolations;

    @JsProperty
    protected ConstraintViolation[] returnValueViolations;

    @JsOverlay
    final public String getException() {
        return exception;
    }

    @JsOverlay
    final public void setException(String exception) {
        this.exception = exception;
    }

    @JsOverlay
    final public ConstraintViolation[] getFieldViolations() {
        return fieldViolations != null ? fieldViolations : new ConstraintViolation[0];
    }

    @JsOverlay
    final public void setFieldViolations(ConstraintViolation[] fieldViolations) {
        this.fieldViolations = fieldViolations;
    }

    @JsOverlay
    final public ConstraintViolation[] getPropertyViolations() {
        return propertyViolations != null ?  propertyViolations : new ConstraintViolation[0];
    }

    @JsOverlay
    final public void setPropertyViolations(ConstraintViolation[] propertyViolations) {
        this.propertyViolations = propertyViolations;
    }

    @JsOverlay
    final public ConstraintViolation[] getClassViolations() {
        return classViolations != null ? classViolations : new ConstraintViolation[0];
    }

    @JsOverlay
    final public void setClassViolations(ConstraintViolation[] classViolations) {
        this.classViolations = classViolations;
    }

    @JsOverlay
    final public ConstraintViolation[] getParameterViolations() {
        return parameterViolations != null ? parameterViolations : new ConstraintViolation[0];
    }

    @JsOverlay
    final public void setParameterViolations(ConstraintViolation[] parameterViolations) {
        this.parameterViolations = parameterViolations;
    }

    @JsOverlay
    final public ConstraintViolation[] getReturnValueViolations() {
        return returnValueViolations != null ? returnValueViolations : new ConstraintViolation[0];
    }

    @JsOverlay
    final public void setReturnValueViolations(ConstraintViolation[] returnValueViolations) {
        this.returnValueViolations = returnValueViolations;
    }

    @JsOverlay
    final public boolean hasViolations() {
        return getFieldViolations().length > 0
            || getPropertyViolations().length > 0
            || getClassViolations().length > 0
            || getParameterViolations().length > 0
            || getReturnValueViolations().length > 0;
    }

    @JsOverlay
    final public ConstraintViolation[] getAllViolations() {
        List<ConstraintViolation> violations = new ArrayList<>();
        violations.addAll(Arrays.asList(getFieldViolations()));
        violations.addAll(Arrays.asList(getPropertyViolations()));
        violations.addAll(Arrays.asList(getClassViolations()));
        violations.addAll(Arrays.asList(getParameterViolations()));
        violations.addAll(Arrays.asList(getReturnValueViolations()));
        return violations.toArray(new ConstraintViolation[violations.size()]);
    }

}