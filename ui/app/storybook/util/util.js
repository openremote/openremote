/*
 * Copyright 2025, OpenRemote Inc.
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

/**
 * Retrieves the custom element definition for the given tag name from the customElements.json file.
 * @param tagName - The tag name of the custom element. Such as `or-asset-tree`
 * @param customElementsJson - JSON object containing the custom elements definitions.
 * @returns Object - The custom element definition.
 */
export function getCustomElements(tagName, customElementsJson) {
  if(tagName) {
    const modules = (customElementsJson.modules || []);
    const tagNameModule = modules.find(m => m.declarations?.map(d => d.tagName).includes(tagName));
    return tagNameModule.declarations?.find(d => d.tagName === tagName);
  } else {
    return [];
  }
}
