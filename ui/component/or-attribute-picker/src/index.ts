import {customElement} from "lit/decorators.js";
import {OrAssetAttributePicker, OrAssetAttributePickerPickedEvent} from "./asset-attribute-picker";
import {AssetTypeAttributePickerPickedEvent, AssetTypeAttributePicker} from "./assettype-attribute-picker";

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
