import {customElement} from "lit/decorators.js";
import {OrAssetAttributePicker, OrAssetAttributePickerPickedEvent} from "./asset-attribute-picker";

/**
 * {@link CustomEvent} that triggers once attributes have been selected, and the user closes the dialog.
 * @deprecated Replaced this class with an abstract {@link OrAssetAttributePickerPickedEvent}, that is inherited by other classes like {@link OrAssetAttributePicker} and {@link OrAssetTypeAttributePicker}.
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

export * from "./asset-attribute-picker";
export * from "./assettype-attribute-picker";
