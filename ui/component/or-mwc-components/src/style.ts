import {css, unsafeCSS} from "lit";
import {DefaultColor4} from "@openremote/core";

// language=CSS
export const progressCircular = css`
           
            /*  https://codepen.io/finnhvman/pen/bmNdNr  */
            .pure-material-progress-circular {
                -webkit-appearance: none;
                -moz-appearance: none;
                appearance: none;
                box-sizing: border-box;
                border: none;
                border-radius: 50%;
                padding: 0.25em;
                width: 3em;
                height: 3em;
                color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
                background-color: transparent;
                font-size: 16px;
                overflow: hidden;
            }

            .pure-material-progress-circular::-webkit-progress-bar {
                background-color: transparent;
            }

            /* Indeterminate */
            .pure-material-progress-circular:indeterminate {
                -webkit-mask-image: linear-gradient(transparent 50%, black 50%), linear-gradient(to right, transparent 50%, black 50%);
                mask-image: linear-gradient(transparent 50%, black 50%), linear-gradient(to right, transparent 50%, black 50%);
                animation: pure-material-progress-circular 6s infinite cubic-bezier(0.3, 0.6, 1, 1);
            }

            :-ms-lang(x), .pure-material-progress-circular:indeterminate {
                animation: none;
            }

            .pure-material-progress-circular:indeterminate::before,
            .pure-material-progress-circular:indeterminate::-webkit-progress-value {
                content: "";
                display: block;
                box-sizing: border-box;
                margin-bottom: 0.25em;
                border: solid 0.25em transparent;
                border-top-color: currentColor;
                border-radius: 50%;
                width: 100% !important;
                height: 100%;
                background-color: transparent;
                animation: pure-material-progress-circular-pseudo 0.75s infinite linear alternate;
            }

            .pure-material-progress-circular:indeterminate::-moz-progress-bar {
                box-sizing: border-box;
                border: solid 0.25em transparent;
                border-top-color: currentColor;
                border-radius: 50%;
                width: 100%;
                height: 100%;
                background-color: transparent;
                animation: pure-material-progress-circular-pseudo 0.75s infinite linear alternate;
            }

            .pure-material-progress-circular:indeterminate::-ms-fill {
                animation-name: -ms-ring;
            }

            @keyframes pure-material-progress-circular {
                0% {
                    transform: rotate(0deg);
                }
                12.5% {
                    transform: rotate(180deg);
                    animation-timing-function: linear;
                }
                25% {
                    transform: rotate(630deg);
                }
                37.5% {
                    transform: rotate(810deg);
                    animation-timing-function: linear;
                }
                50% {
                    transform: rotate(1260deg);
                }
                62.5% {
                    transform: rotate(1440deg);
                    animation-timing-function: linear;
                }
                75% {
                    transform: rotate(1890deg);
                }
                87.5% {
                    transform: rotate(2070deg);
                    animation-timing-function: linear;
                }
                100% {
                    transform: rotate(2520deg);
                }
            }

            @keyframes pure-material-progress-circular-pseudo {
                0% {
                    transform: rotate(-30deg);
                }
                29.4% {
                    border-left-color: transparent;
                }
                29.41% {
                    border-left-color: currentColor;
                }
                64.7% {
                    border-bottom-color: transparent;
                }
                64.71% {
                    border-bottom-color: currentColor;
                }
                100% {
                    border-left-color: currentColor;
                    border-bottom-color: currentColor;
                    transform: rotate(225deg);
                }
            }
`;
