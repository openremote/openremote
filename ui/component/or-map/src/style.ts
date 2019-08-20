import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor4} from "@openremote/core";

export const markerColorVar = "--internal-or-map-marker-color";
export const markerActiveColorVar = "--internal-or-map-marker-active-color";

// language=CSS
export const style = css`
    :host {
        --internal-or-map-marker-transform: var(--or-map-marker-transform, translate(-24px, -45px));
        --internal-or-map-marker-width: var(--or-map-marker-width, 48px);
        --internal-or-map-marker-height: var(--or-map-marker-height, 48px);
        --internal-or-map-marker-color: var(--or-map-marker-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-map-marker-stroke: var(--or-map-marker-stroke, none);
        --internal-or-map-marker-icon-color: var(--or-map-marker-icon-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-map-marker-icon-stroke: var(--or-map-marker-icon-stroke, none);
        --internal-or-map-marker-icon-width: var(--or-map-marker-icon-width, 24px);
        --internal-or-map-marker-icon-height: var(--or-map-marker-icon-height, 24px);
        --internal-or-map-marker-icon-transform: var(--or-map-marker-icon-transform, translate(-50%, -19px));
        --internal-or-map-marker-active-transform: var(--or-map-marker-active-transform, translate(-30px, -57px));
        --internal-or-map-marker-active-width: var(--or-map-marker-active-width, 60px);
        --internal-or-map-marker-active-height: var(--or-map-marker-active-height, 60px);
        --internal-or-map-marker-active-color: var(--or-map-marker-active-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-map-marker-active-stroke: var(--or-map-marker-active-stroke, none);
        --internal-or-map-marker-icon-active-color: var(--or-map-marker-icon-active-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-map-marker-icon-active-stroke: var(--or-map-marker-icon-active-stroke, none);
        --internal-or-map-marker-icon-active-width: var(--or-map-marker-icon-active-width, 32px);
        --internal-or-map-marker-icon-active-height: var(--or-map-marker-icon-active-height, 32px);
        --internal-or-map-marker-icon-active-transform: var(--or-map-marker-icon-active-transform, translate(-50%, -24px));
        display: block;
        overflow: hidden;
    }

    :host([hidden]) {
        display: none;
    }

    slot {
        display: none;
    }
    
    #map {
        position: relative;
        width: 100%;
        height: 100%;
    }

    .leaflet-marker-icon, .mapboxgl-marker {
        pointer-events: none !important;
    }

    .or-map-marker {
        position: absolute; /* This makes mapboxJS behave like mapboxGL */
    }

    .marker-container {
        position: relative;
        cursor: pointer;
        transform: var(--internal-or-map-marker-transform);
        --or-icon-fill: var(--internal-or-map-marker-color);
        --or-icon-width: var(--internal-or-map-marker-width);
        --or-icon-height: var(--internal-or-map-marker-height);
        --or-icon-stroke: var(--internal-or-map-marker-stroke);
    }

    .or-map-marker.active .marker-container {
        transform: var(--internal-or-map-marker-active-transform);
        --or-icon-fill: var(--internal-or-map-marker-active-color);
        --or-icon-width: var(--internal-or-map-marker-active-width);
        --or-icon-height: var(--internal-or-map-marker-active-height);
        --or-icon-stroke: var(--internal-or-map-marker-active-stroke);
    }

    .or-map-marker.interactive .marker-container {
        pointer-events: all;
    }

    .or-map-marker-default.interactive .marker-container {
        pointer-events: none;
        --or-icon-pointer-events: visible;
    }

    .or-map-marker .marker-icon {
        position: absolute;
        left: 50%;
        top: 50%;
        z-index: 1000;
        --or-icon-fill: var(--internal-or-map-marker-icon-color);
        --or-icon-stroke: var(--internal-or-map-marker-icon-stroke);
        --or-icon-width: var(--internal-or-map-marker-icon-width);
        --or-icon-height: var(--internal-or-map-marker-icon-height);
        transform: var(--internal-or-map-marker-icon-transform);
    }
    
    .or-map-marker.active .marker-icon {
        transform: var(--internal-or-map-marker-icon-active-transform);
        --or-icon-fill: var(--internal-or-map-marker-icon-active-color);
        --or-icon-stroke: var(--internal-or-map-marker-icon-active-stroke);
        --or-icon-width: var(--internal-or-map-marker-icon-active-width);
        --or-icon-height: var(--internal-or-map-marker-icon-active-height);
    }
    
    #openremote {
        position: absolute;
        bottom: 25px;
        right: 5px;
        height: 20px;
        width: 20px;
        cursor: pointer;
    }
    
    #openremote img {
        height: 20px;
        width: 20px;
    }
    
    @media only screen and (max-width: 768px) {
        #openremote {
            bottom: 40px;
            right: 12px;
        }
    }
    }
`;