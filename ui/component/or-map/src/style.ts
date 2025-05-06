import {css, unsafeCSS} from "lit";
import { DefaultColor1, DefaultColor2, DefaultColor3, DefaultColor4, DefaultColor5, DefaultHeaderHeight, DefaultBoxShadow} from "@openremote/core";

export const markerColorVar = "--internal-or-map-marker-color";
export const markerActiveColorVar = "--internal-or-map-marker-active-color";

// language=CSS
export const style = css`
    :host {
        --internal-or-map-width: var(--or-map-width, 100%);
        --internal-or-map-min-height: var(--or-map-min-height, 300px);
        --internal-or-map-marker-transform: var(--or-map-marker-transform, translate(-16px, -29px));
        --internal-or-map-marker-width: var(--or-map-marker-width, 32px);
        --internal-or-map-marker-height: var(--or-map-marker-height, 32px);
        --internal-or-map-marker-color: var(--or-map-marker-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-map-marker-stroke: var(--or-map-marker-stroke, none);
        --internal-or-map-marker-icon-color: var(--or-map-marker-icon-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-map-marker-icon-stroke: var(--or-map-marker-icon-stroke, none);
        --internal-or-map-marker-icon-width: var(--or-map-marker-icon-width, 16px);
        --internal-or-map-marker-icon-height: var(--or-map-marker-icon-height, 16px);
        --internal-or-map-marker-icon-transform: var(--or-map-marker-icon-transform, translate(-50%, -14px));
        --internal-or-map-marker-active-transform: var(--or-map-marker-active-transform, translate(-24px, -44px));
        --internal-or-map-marker-active-width: var(--or-map-marker-active-width, 48px);
        --internal-or-map-marker-active-height: var(--or-map-marker-active-height, 48px);
        --internal-or-map-marker-active-color: var(--or-map-marker-active-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-map-marker-active-stroke: var(--or-map-marker-active-stroke, 2px);
        --internal-or-map-marker-icon-active-color: var(--or-map-marker-icon-active-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-map-marker-icon-active-stroke: var(--or-map-marker-icon-active-stroke, none);
        --internal-or-map-marker-icon-active-width: var(--or-map-marker-icon-active-width, 24px);
        --internal-or-map-marker-icon-active-height: var(--or-map-marker-icon-active-height, 24px);
        --internal-or-map-marker-icon-active-transform: var(--or-map-marker-icon-active-transform, translate(-50%, -20px));
        display: block;
        overflow: hidden;
        
        min-height: var(--internal-or-map-min-height);
        width: var(--internal-or-map-width);
    }
    
    canvas {
        outline: none !important;
    }

    :host([hidden]) {
        display: none;
    }

    slot {
        display: none;
    }
          
    #container {
        position: relative;
        width: 100%;
        height: 100%;    
    }
    
    #map {
        position: absolute;
        left: 0;
        right: 0;
        top: 0;
        bottom: 0;
    }
    .mapboxgl-ctrl-geocoder,
    .maplibregl-ctrl-geocoder--input {
        width: calc(100% - 20px)
    }
    
    /* Prevents overflow from elements outside the map component (like menu overlays). See #1844 */
    .maplibregl-ctrl-bottom-left,.maplibregl-ctrl-bottom-right, .maplibregl-ctrl-top-left, .maplibregl-ctrl-top-right {
        z-index: 1;
    }
    .leaflet-marker-icon, .maplibregl-marker, .mapboxgl-marker {
        pointer-events: none !important;
    }

    .or-map-marker {
        position: absolute; /* This makes mapboxJS behave like mapboxGL */
    }
    
    .or-map-marker.active {
        z-index: 1;
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
    
    @media only screen and (max-width: 640px) {
        #openremote {
            bottom: 40px;
            right: 12px;
        }
    }
`;

export const mapAssetCardStyle = css`
            :host {
                --internal-or-map-asset-card-header-color: var(--or-map-asset-card-header-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
                --internal-or-map-asset-card-header-text-color: var(--or-map-asset-card-header-text-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
                --internal-or-map-asset-card-header-height: var(--or-map-asset-card-header-height, calc(${unsafeCSS(DefaultHeaderHeight)} - 10px));
                --internal-or-map-asset-card-background-color: var(--or-map-asset-card-background-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
                --internal-or-map-asset-card-background-text-color: var(--or-map-asset-card-background-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
                --internal-or-map-asset-card-separator-color: var(--or-map-asset-card-separator-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
                
                display: block;
            }

            #card-container {
                display: flex;
                flex-direction: column;
                height: 100%;
                background-color: var(--internal-or-map-asset-card-background-color);
                -webkit-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                -moz-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);  
            }
            
            #header {
                height: var(--internal-or-map-asset-card-header-height);
                background-color: var(--internal-or-map-asset-card-header-color);
                line-height: var(--internal-or-map-asset-card-header-height);
                border-bottom: 1px solid ${unsafeCSS(DefaultColor5)};
                text-align: center;
                color: var(--internal-or-map-asset-card-header-text-color);
                --or-icon-fill: var(--internal-or-map-asset-card-header-text-color);
                --or-icon-width: 20px;
                --or-icon-height: 20px;
                z-index: 99999;
            }

            #header > or-icon {
                margin-right: 5px;
            }
            
            #title {
                font-weight: 500;
            }
            
            #attribute-list {
                flex: 1;                
                color: var(--internal-or-map-asset-card-background-text-color);
                padding: 10px 20px;
                overflow: auto;
                font-size: 14px;
            }
            
            ul {
                list-style-type: none;
                margin: 0;
                padding: 0;
            }
            
            li {
                display: flex;
                line-height: 30px;
            }
            li.highlighted {
                font-weight: bold;
            }
            
            .attribute-name {
                flex: 1;            
            }
            
            .attribute-value {
                overflow: hidden;
                padding-left: 20px;
                text-align: right;
            }
            
            #footer {
                height: var(--internal-or-map-asset-card-header-height);
                border-top: 1px solid var(--internal-or-map-asset-card-separator-color);
                text-align: right;
                padding: 5px 12px;
            }
            
            @media only screen and (min-width: 40em){
                #card-container {
                    height: 400px; /* fallback for IE */
                    height: max-content;
                    max-height: calc(100vh - 150px);
                    min-height: 134px;
                }
            }
`
