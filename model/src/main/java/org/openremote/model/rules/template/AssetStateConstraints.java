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
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssetStateConstraints extends TemplateFilter {

    public static final String TEMPLATE_PARAM_NAME = "assetStateConstraints";

    final protected Constraint[] constraints;

    public AssetStateConstraints(String filterId, Constraint... constraints) {
        super(filterId);
        this.constraints = constraints;
    }

    public String getAssetStateConstraints() {
        StringBuilder sb = new StringBuilder();
        for (Constraint constraint : constraints) {
            sb.append("AssetState(id == $assetState.id, ");
            sb.append(constraint.render());
            sb.append(")").append("\n");
        }
        return sb.toString();
    }

    public ObjectValue toObjectValue() {
        ObjectValue objectValue = Values.createObject();
        ArrayValue constraintsArray = Values.createArray();
        objectValue.put("assetStateConstraints", constraintsArray);
        for (Constraint constraint : constraints) {
            constraintsArray.add(constraint.toObjectValue());
        }
        return objectValue;
    }

    public static boolean isAssetStateConstraints(Value value) {
        return Values.getObject(value)
            .flatMap(objectValue -> objectValue.getArray("assetStateConstraints"))
            .isPresent();
    }

    public static Optional<AssetStateConstraints> fromValue(String filterId, Value value) {
        return Values.getObject(value)
            .filter(AssetStateConstraints::isAssetStateConstraints)
            .flatMap(objectValue -> objectValue.getArray("assetStateConstraints"))
            .map(arrayValue -> {
                List<Constraint> constraints = new ArrayList<>();
                for (int i = 0; i < arrayValue.length(); i++) {

                    arrayValue.getObject(i)
                        .flatMap(AttributeValueConstraint::fromValue)
                        .ifPresent(constraints::add);

                }
                return new AssetStateConstraints(filterId, constraints.toArray(new Constraint[constraints.size()]));
            });
    }
/*

    public static void main(String[] args) throws Exception {
        AssetStateConstraints constraints = new AssetStateConstraints(
            new AttributeValueConstraint("foo", ValueComparator.EQUALS, Values.create("foo123"), true)
        );

        System.out.println(constraints);

        String json = constraints.toObjectValue().toJson();

        System.err.println(json);

        constraints = Values.parse(json).flatMap(AssetStateConstraints::fromValue).get();

        System.out.println(constraints);


    }
*/
}
