import { customElement } from "lit/decorators.js";
import { TemplateOptions } from "./or-map-marker.ts";
import { LitElement } from "lit";

@customElement("or-default-marker")
export class OrDefaultMarker extends LitElement {
    constructor(private readonly icon: string | undefined, private readonly options?: TemplateOptions) {
        super();
    }

    render() {
        return `
        ${
            this.options && this.options.displayValue !== undefined
                ? `<div class="label"><span>${this.options.displayValue}</span></div>`
                : ``
        }
        ${
            this.options && this.options.direction
                ? `<div class="direction-icon-wrapper" style="transform: rotate(${this.options.direction}deg);">
                <svg class="direction-circle">
                 <circle/>
                </svg>
                <or-icon class="direction-icon" icon="play"></or-icon>
               </div>`
                : ``
        }
        <or-icon icon="or:marker"></or-icon>
        <or-icon class="marker-icon" icon="${this.icon || ""}"></or-icon>
    `;
    }
}
