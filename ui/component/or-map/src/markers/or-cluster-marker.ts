// import { css, CSSResultGroup, html, LitElement, PropertyValues, unsafeCSS } from "lit";
// import { customElement, property, query } from "lit/decorators.js";
// import { markerActiveColorVar, markerColorVar } from "../style";
// import { DefaultBoxShadow } from "@openremote/core";

// export class OrMapMarkerChangedEvent extends CustomEvent<OrMapMarkerChangedEventDetail> {
//     public static readonly NAME = "or-map-marker-changed";

//     constructor(marker: OrMapMarker, prop: string) {
//         super(OrMapMarkerChangedEvent.NAME, {
//             detail: {
//                 marker: marker,
//                 property: prop,
//             },
//             bubbles: true,
//             composed: true,
//         });
//     }
// }

// export class OrMapMarkerClickedEvent extends CustomEvent<OrMapMarkerEventDetail> {
//     public static readonly NAME = "or-map-marker-clicked";

//     constructor(marker: OrMapMarker) {
//         super(OrMapMarkerClickedEvent.NAME, {
//             detail: {
//                 marker: marker,
//             },
//             bubbles: true,
//             composed: true,
//         });
//     }
// }

// export interface OrMapMarkerEventDetail {
//     marker: OrMapMarker;
// }

// export interface OrMapMarkerChangedEventDetail extends OrMapMarkerEventDetail {
//     property: string;
// }

// export interface TemplateOptions {
//     displayValue?: string;
//     direction?: string;
// }

// declare global {
//     export interface HTMLElementEventMap {
//         [OrMapMarkerChangedEvent.NAME]: OrMapMarkerChangedEvent;
//     }
// }

// /**
//  * Base class for all map markers.
//  *
//  * This component doesn't directly render anything instead it generates DOM that can be added to the map component
//  */
// @customElement("or-map-marker")
// export class OrMapMarker extends LitElement {

//     // This is the actual map marker element not the same element as returned from createMarkerElement when using raster map
//     public _actualMarkerElement?: HTMLDivElement;

//     @query("slot")
//     protected _slot?: HTMLSlotElement;

//     public get markerContainer(): HTMLDivElement | undefined {
//         if (this._actualMarkerElement) {
//             return this._actualMarkerElement.firstElementChild as HTMLDivElement;
//         }
//     }

//     public _onClick(e: MouseEvent) {
//         this.dispatchEvent(new OrMapMarkerClickedEvent(this));
//     }

//     protected render() {
//         return html` <slot></slot> `;
//     }
// }
