import {LitElement, property, query, customElement, PropertyValues, html} from "lit-element";
import {MapWidget} from "../mapwidget";

/**
 * Base class for all map markers
 */
@customElement("or-map-marker")
export class OrMapMarker extends LitElement {
    protected _added: boolean = false;
    protected _attached: boolean = false;

    @property({type: Number})
    lat: number = 0;

    @property({type: Number})
    lng: number = 0;

    @property({type: Boolean})
    visible: boolean = true;

    @property({type: Object})
    _ele!: HTMLElement;

    @property({type: Object})
    map?: MapWidget;

    @query("slot")
    _slot!: HTMLSlotElement;

    protected render() {
        return html`
          <style>
            :host {
                display: block;
                overflow: hidden;
            }
            #map {            
                position: relative;
                width: 100%;
                height: 100%;
            }
            slot {
                display: none;
            }
          </style>
          <slot></slot>
        `;
    }

    connectedCallback() {
        super.connectedCallback();
        this._attached = true;
        this._ele = this._createMarkerElement();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._removeMarker();
        this._attached = false;
    }

    
    protected updated(changedProps: PropertyValues){
        //@observe("visible", "lat", "lng", "map")
        if(changedProps.has('visible')){
            this._updateMarker();
        }
        if(changedProps.has('lat')){
            this._updateMarker();
        }
        if(changedProps.has('lng')){
            this._updateMarker();
        }
        if(changedProps.has('map')){
            this._updateMarker();
        }
    }
    _updateMarker() {
        if (!this._attached || !this.map) return;

        if (!this._ele) {
            this._ele = this._createMarkerElement();
        }

        if (!this._ele) {
            return;
        }

        if (!this.visible) {
            this._removeMarker();
        } else {
            this._addMarker();
            this.map!.updateMarkerPosition(this);
        }
    }

    _addMarker() {
        if (!this._added) {
            this.map!.addMarker(this);
            this._added = true;
        }
    }

    _removeMarker() {
        if (this._added) {
            this.map!.removeMarker(this);
            this._added = false;
        }
    }

    //TODO make this optional?
    _onClick(e:any) {
    }

    _createMarkerElement(): HTMLElement {
        let children = this._slot.assignedNodes({flatten:true});
        let len = children.length;
        let className = ("or-map-marker " + this.className).trim();
        let ele = document.createElement("div");
        ele.className = className;

        if (len > 0) {
            // if more than 1 ele put inside wrapper
            for (var i=0; i<len; ++i) {
                ele.appendChild(children[i]);
            }
        }
        return ele;
    }
}