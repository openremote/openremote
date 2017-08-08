/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openremote.container.web.jsapi;

import java.lang.annotation.Annotation;

public class MethodParamMetaData {

    public enum MethodParamType {
        QUERY_PARAMETER, HEADER_PARAMETER, COOKIE_PARAMETER, PATH_PARAMETER, MATRIX_PARAMETER, FORM_PARAMETER, FORM, ENTITY_PARAMETER
    }

    private Class<?> type;
    private Annotation[] annotations;

    private MethodParamType paramType;
    private String paramName;

    public MethodParamMetaData(Class<?> type, Annotation[] annotations,
                               MethodParamType paramType, String paramName) {
        super();
        this.type = type;
        this.annotations = annotations;
        this.paramType = paramType;
        this.paramName = paramName;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public MethodParamType getParamType() {
        return paramType;
    }

    public void setParamType(MethodParamType paramType) {
        this.paramType = paramType;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

}
