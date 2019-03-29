import {css, customElement, LitElement, property, PropertyValues, query, html} from "lit-element";
import {html as litHtml, render} from "lit-html";

export enum OrMapMarkerEvent {
    CLICKED = "or-map-marker-clicked",
    CHANGED = "or-map-marker-changed"
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapMarkerEvent.CHANGED]: OrMapMarkerChangedEvent;
        [OrMapMarkerEvent.CLICKED]: OrMapMarkerClickedEvent;
    }
}

export class OrMapMarkerChangedEvent extends CustomEvent<MarkerChangedEventDetail> {

    constructor(marker: OrMapMarker, prop: string) {
        super(OrMapMarkerEvent.CHANGED, {
            detail: {
                marker: marker,
                property: prop
            },
            bubbles: true,
            composed: true
        });
    }
}

export class OrMapMarkerClickedEvent extends CustomEvent<MarkerEventDetail> {

    constructor(marker: OrMapMarker) {
        super(OrMapMarkerEvent.CLICKED, {
            detail: {
                marker: marker
            },
            bubbles: true,
            composed: true
        });
    }
}

export interface MarkerEventDetail {
    marker: OrMapMarker;
}

export interface MarkerChangedEventDetail extends MarkerEventDetail {
    property: string;
}

/**
 * Base class for all map markers.
 *
 * This component doesn't directly render anything instead it generates DOM that can be added to the map component
 */
@customElement("or-map-marker")
export class OrMapMarker extends LitElement {

    public static styles = css`
        :host {
            display: none;
        }
        
        slot {
            display: none;
        }
    `;

    protected static _defaultTemplate = (icon: string | undefined) => litHtml`
        <div class="or-map-marker-default">
            <or-icon icon="or:marker"></or-icon>
            <or-icon class="marker-icon" icon="${icon || ""}"></or-icon>
        </div>
    `

    @property({type: Number})
    public lat: number = 0;

    @property({type: Number})
    public lng: number = 0;

    @property({type: Boolean})
    public visible: boolean = true;

    @property({type: String})
    public icon?: string;

    @property({type: Boolean})
    public interactive: boolean = true;

    @query("slot")
    protected _slot?: HTMLSlotElement;

    public _onClick(e: MouseEvent) {
        this.dispatchEvent(new OrMapMarkerClickedEvent(this));
    }

    /**
     * Override in sub types to return custom marker HTML
     */
    public createMarkerElement(): HTMLElement {

        const ele = document.createElement("div");
        this.setMarkerElementClassNames(ele);

        // Append child elements
        let hasChildren = false;
        
        if (this._slot) {
            this._slot.assignedNodes({flatten: true}).forEach((child) => {
                if (child instanceof HTMLElement) {
                    ele.appendChild(child.cloneNode(true));
                    hasChildren = true;
                }
            });
        }
        
        if (!hasChildren) {
            // Append default marker
            this.addDefaultMarkerContent(ele);
        }

        return ele;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        Object.keys(_changedProperties).forEach((prop) => this._raisePropertyChange(prop));
        return false;
    }

    protected render() {
        return html`
          <slot></slot>
        `;
    }

    protected _raisePropertyChange(prop: string) {
        this.dispatchEvent(new OrMapMarkerChangedEvent(this, prop));
    }

    protected setMarkerElementClassNames(markerElement: HTMLElement) {
        let classes = "or-map-marker ";
        if (this.interactive) {
            classes += "interactive ";
        }
        const className = (classes + this.className).trim();
        markerElement.className = className;
    }

    protected addDefaultMarkerContent(markerElement: HTMLElement) {
        render(OrMapMarker._defaultTemplate(this.icon), markerElement);
    }
}
