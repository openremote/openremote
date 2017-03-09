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
package org.openremote.manager.server.attribute;

// TODO: create more granular results and react to them accordingly
public enum AttributeStateConsumerResult {
    OK,                         // Consumer is happy for attribute state to continue through the system
    UNEXPECTED_ASSET_TYPE,      // The asset type referred to by the state change was not expected by the consumer
    INVALID_VALUE,              // The attribute value provided is not valid
    UNKOWN_ERROR,               // An unknown error has occurred
}
