import {css, customElement, html, LitElement, property, PropertyValues} from "lit-element";
import orIconSet from "./or-icon-set";

export interface IconSetSvg {
    size: number;
    icons: {[name: string]: string};
}
export class IconSetAddedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-icon-iconset-added";

    constructor() {
        super(IconSetAddedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [IconSetAddedEvent.NAME]: IconSetAddedEvent;
    }
}

class _IconSets {
    private _icons: {[name: string]: IconSetSvg} = {
        or: orIconSet
    };

    addIconSet(name: string, iconset: IconSetSvg) {
        this._icons[name] = iconset;
        window.dispatchEvent(new IconSetAddedEvent());
    }

    getIconSet(name: string) {
        return this._icons[name];
    }

    getIcon(icon: string | undefined): Element | undefined {
        if (!icon) {
            return undefined;
        }

        let parts = (icon || "").split(":");
        let iconName = parts.pop();
        let iconSetName = parts.pop() || OrIcon.DEFAULT_ICONSET;
        if (!iconSetName || iconSetName === "" || !iconName || iconName === "") {
            return;
        }

        let iconSet = IconSets.getIconSet(iconSetName);
        iconName = iconName.replace(/-([a-z])/g, function (g) { return g[1].toUpperCase(); });

        if (!iconSet || !iconSet.icons.hasOwnProperty(iconName)) {
            return;
        }

        const iconData = iconSet.icons[iconName];
        const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("viewBox", "0 0 " + iconSet.size + " " + iconSet.size);
        svg.style.cssText = "pointer-events: none; display: block; width: 100%; height: 100%;";
        svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
        svg.setAttribute("focusable", "false");
        if (iconData.startsWith("<")) {
            svg.innerHTML = iconData;
        } else {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", iconData);
            path.style.pointerEvents = "pointer-events: var(--or-icon-pointer-events, none);";
            svg.appendChild(path);
        }
        return svg;
    }
}

export const IconSets = new _IconSets();

@customElement("or-icon")
export class OrIcon extends LitElement {

    static DEFAULT_ICONSET: string = "mdi";

    static styles = css`
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
