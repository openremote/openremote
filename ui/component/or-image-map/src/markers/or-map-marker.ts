import {css, CSSResultGroup, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {markerActiveColorVar, markerColorVar} from "../style";
import {DefaultBoxShadow} from "@openremote/core";

export class OrMapMarkerChangedEvent extends CustomEvent<OrMapMarkerChangedEventDetail> {

    public static readonly NAME = "or-map-marker-changed";

    constructor(marker: OrMapMarker, prop: string) {
        super(OrMapMarkerChangedEvent.NAME, {
            detail: {
                marker: marker,
                property: prop
            },
            bubbles: true,
            composed: true
        });
    }
}

export class OrMapMarkerClickedEvent extends CustomEvent<OrMapMarkerEventDetail> {

    public static readonly NAME = "or-map-marker-clicked";

    constructor(marker: OrMapMarker) {
        super(OrMapMarkerClickedEvent.NAME, {
            detail: {
                marker: marker
            },
            bubbles: true,
            composed: true
        });
    }
}

export interface OrMapMarkerEventDetail {
    marker: OrMapMarker;
}

export interface OrMapMarkerChangedEventDetail extends OrMapMarkerEventDetail {
    property: string;
}

export interface TemplateOptions {
    displayValue?: string;
    direction?: string;
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapMarkerChangedEvent.NAME]: OrMapMarkerChangedEvent;
        [OrMapMarkerClickedEvent.NAME]: OrMapMarkerClickedEvent;
    }
}

/**
 * Base class for all map markers.
 *
 * This component doesn't directly render anything instead it generates DOM that can be added to the map component
 */
@customElement("or-map-marker")
export class OrMapMarker extends LitElement {

    static get styles(): CSSResultGroup {
        return css`
          .label {
            background-color: white;
            width: auto;
            height: 20px;
            position: absolute;
            top: -14px;
            left: 50%;
            padding: 0 3px;
            transform: translate(-50%, -50%);
            text-align: center;
            border-radius: 3px;
            -webkit-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
            -moz-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
            box-shadow: ${unsafeCSS(DefaultBoxShadow)};
          }
          .label > span {
            white-space: nowrap;
          }
          
          .direction-icon-wrapper {
            position: absolute;
            top: 11px;
            left: 16px;
          }
          .direction-circle {
            position: absolute;
            margin-top: -15px;
            margin-left: -15px;
            width: 30px;
            height: 30px;
          }
          .direction-circle circle {
            cx: 15px;
            cy: 15px;
            r: 12px;
            stroke: white;
            stroke-width: 3px;
            fill: transparent;
          }
          .direction-icon {
            position: absolute;
            top: -25px;
            left: -16px;
            transform: scale(0.75) rotate(-90deg);
          }
          
          .active .direction-icon-wrapper {
            top: 17px;
            left: 24px;
          }
          .active .direction-circle {
            margin-top: -20px;
            margin-left: -20px;
            width: 40px;
            height: 40px;
          }
          .active .direction-icon {
            top: -36px;
            left: -23px;
          }
          .active .direction-circle circle {
            cx: 20px;
            cy: 20px;
            r: 18px;
            stroke-width: 4px;
          }
        `;
    }

    protected static _defaultTemplate = (icon: string | undefined, options?: TemplateOptions) => `
        ${options && options.displayValue !== undefined 
            ? `<div class="label"><span>${options.displayValue}</span></div>` 
            : ``
        }
        ${options && options.direction
            ? `<div class="direction-icon-wrapper" style="transform: rotate(${options.direction}deg);">
                <svg class="direction-circle">
                 <circle/> 
                </svg>
                <or-icon class="direction-icon" icon="play"></or-icon>
               </div>`
            : ``
        }
        <or-icon icon="or:marker"></or-icon>
        <or-icon class="marker-icon" icon="${icon || ""}"></or-icon>
    `

    @property({type: Number, reflect: true, attribute: true})
    public lat?: number;

    @property({type: Number, reflect: true})
    public lng?: number;

    @property({type: Number, reflect: true})
    public radius?: number;

    @property({reflect: true})
    public displayValue?: string;

    @property({reflect: true})
    public direction?: string;

    @property({type: Boolean})
    public visible: boolean = true;

    @property({type: String})
    public icon?: string;

    @property({type: String})
    public color?: string;

    @property({type: String})
    public activeColor?: string;

    @property({type: Boolean})
    public interactive: boolean = true;

    @property({type: Boolean})
    active: boolean = false;

    // This is the actual map marker element not the same element as returned from createMarkerElement when using raster map
    public _actualMarkerElement?: HTMLDivElement;

    @query("slot")
    protected _slot?: HTMLSlotElement;

    public get markerContainer(): HTMLDivElement | undefined {
        if (this._actualMarkerElement) {
            return this._actualMarkerElement.firstElementChild as HTMLDivElement;
        }
    }

    public _onClick(e: MouseEvent) {
        this.dispatchEvent(new OrMapMarkerClickedEvent(this));
    }

    public _createMarkerElement(): HTMLDivElement {
        const markerElem = document.createElement("div");
        const markerContainerElem = document.createElement("div");
        markerElem.appendChild(markerContainerElem);
        this.addMarkerClassNames(markerElem);
        this.addMarkerContainerClassNames(markerContainerElem);
        let content = this.createMarkerContent();
        if (!content) {
            // Append default marker
            markerElem.classList.add("or-map-marker-default");
            content = this.createDefaultMarkerContent();
        }
        markerContainerElem.appendChild(content);
        this.updateInteractive(markerElem);
        this.updateVisibility(markerElem);
        this.updateColor(markerContainerElem);
        this.updateActive(markerElem);
        return markerElem;
    }

    /**
     * Override in sub types to customise the look of the marker. If undefined returned then a default marker will
     * be used instead.
     */
    public createMarkerContent(): HTMLElement | undefined {

        // Append child elements
        let hasChildren = false;
        let container = document.createElement("div");

        if (this._slot) {
            this._slot.assignedNodes({flatten: true}).forEach((child) => {
                if (child instanceof HTMLElement) {
                    container.appendChild(child.cloneNode(true));
                    hasChildren = true;
                }
            });
        }
        
        if (!hasChildren) {
            return;
        }

        return container;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("icon") || _changedProperties.has("displayValue") || _changedProperties.has("direction")) {
            this.refreshMarkerContent();
        }

        if (_changedProperties.has("color") || _changedProperties.has("activeColor")) {
            this.updateColor(this.markerContainer);
        }

        if (_changedProperties.has("visible")) {
            this.updateVisibility(this._actualMarkerElement);
        }

        if (_changedProperties.has("interactive")) {
            this.updateInteractive(this._actualMarkerElement);
        }

        if (_changedProperties.has("active")) {
            this.updateActive(this._actualMarkerElement);
            this.updateColor(this.markerContainer);
        }

        _changedProperties.forEach((oldValue, prop) => this._raisePropertyChange(prop as string));
        return false;
    }

    protected updateVisibility(container?: HTMLDivElement) {
        if (container) {
            if (this.visible) {
                container.removeAttribute("hidden");
            } else {
                container.setAttribute("hidden", "true");
            }
        }
    }

    protected getColor() {
        return this.color && this.color !== "unset" ? this.color : undefined;
    }

    protected getActiveColor() {
        return this.activeColor && this.activeColor !== "unset" ? this.activeColor : undefined;
    }

    protected updateColor(container?: HTMLDivElement) {
        if (container) {
            container.style.removeProperty(markerColorVar);
            container.style.removeProperty(markerActiveColorVar);

            const color = this.getColor();
            const activeColor = this.getActiveColor();

            if (color) {
                container.style.setProperty(markerColorVar, color);
            }
            if (activeColor) {
                container.style.setProperty(markerActiveColorVar, activeColor);
            }
        }
    }

    protected updateActive(container?: HTMLDivElement) {
        if (container) {
            if (this.active) {
                container.classList.add("active");
            } else {
                container.classList.remove("active");
            }
        }
    }

    protected updateInteractive(container?: HTMLDivElement) {
        if (container) {
            if (this.interactive) {
                container.classList.add("interactive");
            } else {
                container.classList.remove("interactive");
            }
        }
    }

    protected refreshMarkerContent() {
        if (this.markerContainer) {
            let content = this.createMarkerContent();
            if (!content) {
                // Append default marker
                this._actualMarkerElement!.classList.add("or-map-marker-default");
                content = this.createDefaultMarkerContent();
            } else {
                this._actualMarkerElement!.classList.remove("or-map-marker-default");
            }
            if (this.markerContainer.children.length > 0) {
                this.markerContainer.removeChild(this.markerContainer.children[0]);
            }
            this.markerContainer.appendChild(content);
        }
    }

    protected render() {
        return html`
          <slot></slot>
        `;
    }

    protected _raisePropertyChange(prop: string) {
        this.dispatchEvent(new OrMapMarkerChangedEvent(this, prop));
    }

    protected addMarkerClassNames(markerElement: HTMLElement) {
        markerElement.classList.add("or-map-marker");
    }

    protected addMarkerContainerClassNames(markerContainer: HTMLElement) {
        markerContainer.classList.add("marker-container");
    }

    protected createDefaultMarkerContent(): HTMLElement {
        const div = document.createElement("div");
        div.innerHTML = OrMapMarker._defaultTemplate(this.icon, {displayValue: this.displayValue, direction: this.direction});
        return div;
    }

    public hasPosition(): boolean {
        return typeof this.lat === "number"
            && typeof this.lng === "number"
            && this.lat >= -90 && this.lat < 90
            && this.lng >= -180 && this.lng < 180;
    }
}
