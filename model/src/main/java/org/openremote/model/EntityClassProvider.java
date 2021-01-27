/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model;

import org.openremote.model.util.TsIgnore;

import java.util.List;

/**
 * Interface for providing a list of entity class names to be registered for JPA, this can be used for registering
 * custom {@link org.openremote.model.asset.Asset} types etc. Instances should be registered using the standard
 * {@link java.util.ServiceLoader} mechanism.
 */
@TsIgnore
public interface EntityClassProvider {

    List<Class<?>> getEntityClasses();
}
