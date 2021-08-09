import { html } from "lit";

export const surveyResultStyle = html`
<style>
           :host {
                width: 100%;
                background-color: #FFFFFF;
                --internal-or-survey-results-max-width: var(--or-survey-results-max-width, none);
           }
           
           .flex {
                -ms-flex: 1 1 0.000000001px;
                -webkit-flex: 1;
                flex: 1;
                -webkit-flex-basis: 0.000000001px;
                flex-basis: 0.000000001px;
              }
                .layout.horizontal,
                  .layout.vertical {
                    display: -ms-flexbox;
                    display: -webkit-flex;
                    display: flex;
                  }
                  .layout.horizontal {
                    -ms-flex-direction: row;
                    -webkit-flex-direction: row;
                    flex-direction: row;
                  }
                  .layout.vertical {
                    -ms-flex-direction: column;
                    -webkit-flex-direction: column;
                    flex-direction: column;
                  }
       
            .button-default {
                cursor: pointer;
                margin: auto 1px;
                background-color: #EF9091;
                padding: 10px 20px;
                border-radius: 0;
                color: #FFFFFF;
            }

            .button-default.active {
                background-color: var(--or-survey-color-button, var(--app-primary-color))
            }

            .bar span {
                background-color: #F3C11F;
            }

            .bar:nth-child(2n+2) span {
                background-color: #4A99BA;
            }

            .bar:nth-child(3n+3) span {
                background-color: #23B099;
            }

            .bar:nth-child(4n+4) span {
                background-color: #EA8D31;
            }

            .bar:nth-child(5n+5) span {
                background-color: #e32527;
            }

            .label {
            }

            .label-x {
                display: block;
                margin: 0 10px;
            }

            #chart-container {
                padding-top: 20px;
                margin: auto;
                width: 100%;
                max-width: var(--internal-or-survey-results-max-width);
                overflow-x: scroll;
            }

            .chart {
                display: table;
                table-layout: fixed;

                width: 200px;
                height: 300px;
                margin: 0 auto;
                border-right: 1px solid grey;
            }

            .bar {
                position: relative;
                vertical-align: bottom;
                height: 300px;
                width: 25px;
                float: left;
            }

            .bar span {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                -webkit-box-sizing: border-box;

                position: absolute;
                bottom: 0;
                display: block;
                min-height: 5px;
                height: 50%;
                width: 90%;
                animation: draw 1s ease-in-out;

                -moz-transition: height 1s ease-in-out;
                -webkit-transition: height 1s ease-in-out;
                -o-transition: height 1s ease-in-out;
                transition: height 1s ease-in-out;
            }

            .bar span label {
                z-index: 1;
                position: absolute;
                left: 0;
                right: 0;
                top: 50%;
                width: 150px;
                padding: 5px 1em 0;
                display: none;
            }

            .bar:hover span label {
                display: block;
            }

            .labels .label {
                padding-top: 20px;
                border-top: 1px solid var(--app-darkgrey);
                min-width: 200px;
                margin-bottom: 20px;
            }

            .labels .label:first-child {
                margin-left: 46px;
            }

            .label-y {
                position: relative;
                margin-right: 10px;
                font-size: 14px;
                border-top: 1px solid grey;
            }

            .label-y:after {
                display: block;
                content: '';
                position: absolute;
            }

            .border-right {
                border-right: 1px solid var(--app-darkgrey);
                margin-right: 10px;
            }

            .bar-top-label {
                text-align: center;
                position: relative;
                margin-top: -16px;
                line-height: 12px;
                font-size: 11px;
            }

            @keyframes draw {
                0% {
                    height: 0;
                }
            }


</style>
`;