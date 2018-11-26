/*
 * Copyright 2018, OpenRemote Inc.
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

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.util.Utils;

import java.lang.reflect.Type;

/**
 * Hacky way of getting the typescript generator to only output the client which assumes the model has already been output
 * and will be imported when required; suggested way by repo owner is to use:
 * <p>
 * <a href="https://github.com/vojtechhabarta/typescript-generator/pull/276">Dependent modules</a>
 */
public class JaxRsClientOnlyProcessor implements cz.habarta.typescript.generator.TypeProcessor {

    @Override
    public Result processType(Type javaType, Context context) {
        final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
        if (rawClass != null) {
            if (!rawClass.getName().startsWith("org.openremote.model") || rawClass.getName().startsWith("org.openremote.model.value")) {
                return null;
            }
            if (!rawClass.getName().endsWith("Resource")) {
                String simpleName = rawClass.getSimpleName();
                if ("RequestParams".equals(simpleName)) {
                    simpleName = simpleName + "<any,any>";
                }
                simpleName = "Model." + simpleName;
                return new Result(new TsType.VerbatimType(simpleName));
            }
        }
        return null;
    }
}
