/*
 * Copyright 2024, OpenRemote Inc.
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
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { customElement } from "lit/decorators.js";
import { OrAssetAttributePicker, OrAssetAttributePickerPickedEvent } from "./asset-attribute-picker";
import { AssetTypeAttributePickerPickedEvent, AssetTypeAttributePicker } from "./assettype-attribute-picker";

/**
 * {@link CustomEvent} that triggers once attributes have been selected, and the user closes the dialog.
 * @deprecated Replaced this class with an abstract {@link OrAssetAttributePickerPickedEvent}, that is inherited by other classes like {@link OrAssetAttributePicker} and {@link AssetTypeAttributePicker}.
 */
export class OrAttributePickerPickedEvent extends OrAssetAttributePickerPickedEvent {

}

/**
 * Dialog to pick attributes of supplied asset(s).
 * @deprecated Replaced this class with {@link OrAssetAttributePicker}.
 */
@customElement("or-attribute-picker")
export class OrAttributePicker extends OrAssetAttributePicker {
}

/**
 * TODO: Remove this, and export the ./assettype-attribute-picker file
 */
export class OrAssetTypeAttributePickerPickedEvent extends AssetTypeAttributePickerPickedEvent {

}

/**
 * TODO: Remove this, and export the ./assettype-attribute-picker file
 */
@customElement("or-assettype-attribute-picker")
export class OrAssetTypeAttributePicker extends AssetTypeAttributePicker {

}
