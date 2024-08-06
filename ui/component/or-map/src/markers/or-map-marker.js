var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var OrMapMarker_1;
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { markerActiveColorVar, markerColorVar } from "../style";
import { DefaultBoxShadow } from "@openremote/core";
export class OrMapMarkerChangedEvent extends CustomEvent {
    constructor(marker, prop) {
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
OrMapMarkerChangedEvent.NAME = "or-map-marker-changed";
export class OrMapMarkerClickedEvent extends CustomEvent {
    constructor(marker) {
        super(OrMapMarkerClickedEvent.NAME, {
            detail: {
                marker: marker
            },
            bubbles: true,
            composed: true
        });
    }
}
OrMapMarkerClickedEvent.NAME = "or-map-marker-clicked";
/**
 * Base class for all map markers.
 *
 * This component doesn't directly render anything instead it generates DOM that can be added to the map component
 */
let OrMapMarker = OrMapMarker_1 = class OrMapMarker extends LitElement {
    constructor() {
        super(...arguments);
        this.visible = true;
        this.interactive = true;
        this.active = false;
    }
    static get styles() {
        return css `
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
    get markerContainer() {
        if (this._actualMarkerElement) {
            return this._actualMarkerElement.firstElementChild;
        }
    }
    _onClick(e) {
        this.dispatchEvent(new OrMapMarkerClickedEvent(this));
    }
    _createMarkerElement() {
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
    createMarkerContent() {
        // Append child elements
        let hasChildren = false;
        let container = document.createElement("div");
        if (this._slot) {
            this._slot.assignedNodes({ flatten: true }).forEach((child) => {
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
    shouldUpdate(_changedProperties) {
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
        _changedProperties.forEach((oldValue, prop) => this._raisePropertyChange(prop));
        return false;
    }
    updateVisibility(container) {
        if (container) {
            if (this.visible) {
                container.removeAttribute("hidden");
            }
            else {
                container.setAttribute("hidden", "true");
            }
        }
    }
    getColor() {
        return this.color && this.color !== "unset" ? this.color : undefined;
    }
    getActiveColor() {
        return this.activeColor && this.activeColor !== "unset" ? this.activeColor : undefined;
    }
    updateColor(container) {
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
    updateActive(container) {
        if (container) {
            if (this.active) {
                container.classList.add("active");
            }
            else {
                container.classList.remove("active");
            }
        }
    }
    updateInteractive(container) {
        if (container) {
            if (this.interactive) {
                container.classList.add("interactive");
            }
            else {
                container.classList.remove("interactive");
            }
        }
    }
    refreshMarkerContent() {
        if (this.markerContainer) {
            let content = this.createMarkerContent();
            if (!content) {
                // Append default marker
                this._actualMarkerElement.classList.add("or-map-marker-default");
                content = this.createDefaultMarkerContent();
            }
            else {
                this._actualMarkerElement.classList.remove("or-map-marker-default");
            }
            if (this.markerContainer.children.length > 0) {
                this.markerContainer.removeChild(this.markerContainer.children[0]);
            }
            this.markerContainer.appendChild(content);
        }
    }
    render() {
        return html `
          <slot></slot>
        `;
    }
    _raisePropertyChange(prop) {
        this.dispatchEvent(new OrMapMarkerChangedEvent(this, prop));
    }
    addMarkerClassNames(markerElement) {
        markerElement.classList.add("or-map-marker");
    }
    addMarkerContainerClassNames(markerContainer) {
        markerContainer.classList.add("marker-container");
    }
    createDefaultMarkerContent() {
        const div = document.createElement("div");
        div.innerHTML = OrMapMarker_1._defaultTemplate(this.icon, { displayValue: this.displayValue, direction: this.direction });
        return div;
    }
    hasPosition() {
        return typeof this.lat === "number"
            && typeof this.lng === "number"
            && this.lat >= -90 && this.lat < 90
            && this.lng >= -180 && this.lng < 180;
    }
};
OrMapMarker._defaultTemplate = (icon, options) => `
        ${options && options.displayValue !== undefined
    ? `<div class="label"><span>${options.displayValue}</span></div>`
    : ``}
        ${options && options.direction
    ? `<div class="direction-icon-wrapper" style="transform: rotate(${options.direction}deg);">
                <svg class="direction-circle">
                 <circle/> 
                </svg>
                <or-icon class="direction-icon" icon="play"></or-icon>
               </div>`
    : ``}
        <or-icon icon="or:marker"></or-icon>
        <or-icon class="marker-icon" icon="${icon || ""}"></or-icon>
    `;
__decorate([
    property({ type: Number, reflect: true, attribute: true })
], OrMapMarker.prototype, "lat", void 0);
__decorate([
    property({ type: Number, reflect: true })
], OrMapMarker.prototype, "lng", void 0);
__decorate([
    property({ type: Number, reflect: true })
], OrMapMarker.prototype, "radius", void 0);
__decorate([
    property({ reflect: true })
], OrMapMarker.prototype, "displayValue", void 0);
__decorate([
    property({ reflect: true })
], OrMapMarker.prototype, "direction", void 0);
__decorate([
    property({ type: Boolean })
], OrMapMarker.prototype, "visible", void 0);
__decorate([
    property({ type: String })
], OrMapMarker.prototype, "icon", void 0);
__decorate([
    property({ type: String })
], OrMapMarker.prototype, "color", void 0);
__decorate([
    property({ type: String })
], OrMapMarker.prototype, "activeColor", void 0);
__decorate([
    property({ type: Boolean })
], OrMapMarker.prototype, "interactive", void 0);
__decorate([
    property({ type: Boolean })
], OrMapMarker.prototype, "active", void 0);
__decorate([
    query("slot")
], OrMapMarker.prototype, "_slot", void 0);
OrMapMarker = OrMapMarker_1 = __decorate([
    customElement("or-map-marker")
], OrMapMarker);
export { OrMapMarker };
//# sourceMappingURL=or-map-marker.js.map