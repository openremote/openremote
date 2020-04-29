import {css, CSSResult} from "lit-element";

// language=CSS
export const MapStyle: CSSResult = css`
   or-map-asset-card {
        height: 166px;
        position: absolute;
        bottom: 0px;
        right: 0px;
        width: calc(100vw - 10px);
        margin: 5px;
        z-index: 99;
    }

    or-map {
        display: block;
        height: 100%;
        width: 100%;
    }

    @media only screen and (min-width: 415px){
        or-map-asset-card {
            position: absolute;
            top: 20px;
            right: 20px;
            width: 320px;
            margin: 0;
            height: 400px; /* fallback for IE */
            height: max-content;
            max-height: calc(100vh - 150px);
        }
    }
`;