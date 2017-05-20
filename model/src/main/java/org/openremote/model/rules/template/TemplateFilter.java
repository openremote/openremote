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
package org.openremote.model.rules.template;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class TemplateFilter {

    public static final String TEMPLATE_PARAM_FILTER_NAME = "filterName";
    public static final String TEMPLATE_PARAM_ASSET_STATE = "assetStatePatterns";
    public static final String TEMPLATE_PARAM_ASSET_EVENT = "assetEventPatterns";

    final protected String filterName;
    final protected TemplatePattern[] patterns;

    public TemplateFilter(String filterName, TemplatePattern... patterns) {
        this.filterName = filterName;
        this.patterns = patterns;
    }

    /**
     * Renders {@link #TEMPLATE_PARAM_FILTER_NAME}
     */
    public String getFilterName() {
        return filterName;
    }

    /**
     * Renders {@link #TEMPLATE_PARAM_ASSET_STATE}, uses LHS rule binding <code>$assetState</code>.
     */
    public String getAssetStatePatterns() {
        StringBuilder sb = new StringBuilder();
        for (TemplatePattern templatePattern : patterns) {
            sb.append("AssetState(id == $assetState.id, ");
            sb.append(templatePattern.render());
            sb.append(")").append("\n");
        }
        return sb.toString();
    }

    /**
     * Renders {@link #TEMPLATE_PARAM_ASSET_EVENT}, uses LHS rule binding <code>$assetEvent</code>.
     */
    public String getAssetEventPatterns() {
        StringBuilder sb = new StringBuilder();
        for (TemplatePattern templatePattern : patterns) {
            sb.append("AssetEvent(id == $assetEvent.id, ");
            sb.append(templatePattern.render());
            sb.append(")").append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "filterName='" + filterName + '\'' +
            ", patterns=" + Arrays.toString(patterns) +
            '}';
    }

    public Value toModelValue() {
        return Values.createArray().addAll(
            Arrays.stream(patterns)
                .map(TemplatePattern::toModelValue)
                .toArray(Value[]::new)
        );
    }

    public static Optional<TemplateFilter> fromModelValue(String filterId, Value value) {
        TemplatePattern[] templatePatterns =
            Values.getArray(value)
                .map(ArrayValue::stream)
                .orElse(Stream.empty())
                .map(AttributeValueConstraintPattern::fromModelValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(TemplatePattern[]::new);
        return templatePatterns.length > 0
            ? Optional.of(new TemplateFilter(filterId, templatePatterns))
            : Optional.empty();
    }
}
