/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.ValidationFailure;

import java.util.*;

/**
 * Represents the validation result of an attribute.
 * <p>
 * {@link #getAttributeFailures} returns any attribute validation failures along with an optional failure message
 * parameter for each failure.
 * <p>
 * {@link #getMetaFailures()} returns a map of meta item validation failures along with optional failure message
 * parameter for each failure; the key represents the meta item index that has failed.
 */
public class AttributeValidationResult {

    @JsonProperty
    protected String attributeName;
    @JsonProperty
    protected List<ValidationFailure> attributeFailures;
    @JsonProperty
    protected Map<Integer, List<ValidationFailure>> metaFailures;

    public AttributeValidationResult(String attributeName) {
        this.attributeName = attributeName;
    }

    @JsonCreator
    public AttributeValidationResult(@JsonProperty("attributeName") String attributeName,
                                     @JsonProperty("attributeFailures") List<ValidationFailure> attributeFailures,
                                     @JsonProperty("metaFailures") Map<Integer, List<ValidationFailure>> metaFailures) {
        this.attributeName = attributeName;
        this.attributeFailures = attributeFailures;
        this.metaFailures = metaFailures;
    }

    public void addAttributeFailure(ValidationFailure attributeFailure) {
        if (attributeFailures == null) {
            attributeFailures = new ArrayList<>();
        }

        attributeFailures.add(attributeFailure);
    }

    public void addMetaFailure(ValidationFailure failure) {
        addMetaFailure(-1, failure);
    }

    public void addMetaFailure(int metaItemindex, ValidationFailure failure) {
        if (metaFailures == null) {
            metaFailures = new HashMap<>();
        }
        if (!metaFailures.containsKey(metaItemindex)) {
            metaFailures.put(metaItemindex, new ArrayList<>());
        }
        metaFailures.get(metaItemindex).add(failure);
    }

    public String getAttributeName() {
        return attributeName;
    }

    public List<ValidationFailure> getAttributeFailures() {
        return attributeFailures;
    }

    public Map<Integer, List<ValidationFailure>> getMetaFailures() {
        return metaFailures;
    }

    public boolean hasAttributeFailures() {
        return attributeFailures != null && !attributeFailures.isEmpty();
    }

    public boolean hasMetaFailures() {
        return metaFailures != null && !metaFailures.isEmpty();
    }

    public boolean isValid() {
        return !hasAttributeFailures() && !hasMetaFailures();
    }
}
