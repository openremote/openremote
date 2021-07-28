import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {AssetDescriptor, WellknownAssets} from "@openremote/model";
import {IconSetAddedEvent, ORIconSets, AssetModelUtil} from "@openremote/core";

export function getAssetDescriptorIconTemplate(descriptor: AssetDescriptor | undefined, fallbackColor?: string | undefined, fallbackIcon?: string | undefined, overrideColor?: string | undefined, overrideIcon?: string | undefined): TemplateResult {
    const color = overrideColor ? overrideColor : AssetModelUtil.getAssetDescriptorColour(descriptor, fallbackColor);
    let icon = overrideIcon ? overrideIcon : AssetModelUtil.getAssetDescriptorIcon(descriptor, fallbackIcon);
    if (!icon) {
        icon = AssetModelUtil.getAssetDescriptorIcon(WellknownAssets.UNKNOWNASSET);
    }
    return html`<or-icon style="--or-icon-fill: ${color ? "#" + color : "unset"}" icon="${icon}"></or-icon>`;
}

const _iconSets = new ORIconSets();

export const IconSets = _iconSets;

// language=CSS
const style = css`
    
    :host {    
        --internal-or-icon-width: var(--or-icon-width, 24px);
        --internal-or-icon-height: var(--or-icon-height, 24px);
        --internal-or-icon-fill: var(--or-icon-fill, currentColor);
        --internal-or-icon-stroke: var(--or-icon-fill, none);
        --internal-or-icon-stroke-width: var(--or-icon-stroke-width, 0);
    
        display: inline-block;
        position: relative;
        vertical-align: text-bottom;
        fill: var(--internal-or-icon-fill);
        stroke: var(--internal-or-icon-stroke);
        stroke-width: var(--internal-or-icon-stroke-width);
        width: var(--internal-or-icon-width);
        height: var(--internal-or-icon-height);
    }
    
    :host([hidden]) {
        display: none;
    }
`;

@customElement("or-icon")
export class OrIcon extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: String, reflect: true})
    icon?: string;

    protected _iconElement?: Element;
    protected _handler = (evt: Event) => {
        this._onIconsetAdded(evt);
    };

    protected render() {
        return html`${this._iconElement}`;
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        window.removeEventListener(IconSetAddedEvent.NAME, this._handler);
    }

    protected shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("icon")) {
            this._updateIcon(false);
        }

        return true;
    }

    protected _updateIcon(requestUpdate: boolean) {
        this._iconElement = undefined;
        window.removeEventListener(IconSetAddedEvent.NAME, this._handler);
        this._iconElement = IconSets.getIcon(this.icon);

        if (this.icon && !this._iconElement) {
            window.addEventListener(IconSetAddedEvent.NAME, this._handler);
        }

        if (requestUpdate) {
            this.requestUpdate();
        }
    }

    protected _onIconsetAdded(evt: Event) {
        this._updateIcon(true);
    }
}
