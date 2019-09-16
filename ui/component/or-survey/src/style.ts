import { html, unsafeCSS } from "lit-element";
import ORCore from "@openremote/core";

export const surveySectionStyle = html`
<style>
    :host {
        display: flex;
        flex-direction: column;
        
        --internal-or-survey-color-primary: var(--or-survey-color, var(--or-app-color1, ${unsafeCSS(ORCore.DefaultColor1)}));   
        --internal-or-survey-color-lightgrey: var(--or-survey-color, var(--or-app-color1, ${unsafeCSS(ORCore.DefaultColor1)}));   
        --internal-or-survey-color-grey: var(--or-survey-color, var(--or-app-color1, ${unsafeCSS(ORCore.DefaultColor1)}));   
        
        --internal-or-survey-color: var(--or-survey-color, var(--or-app-color1, ${unsafeCSS(ORCore.DefaultColor1)}));   
        --internal-or-survey-color2: var(--or-survey-color2, var(--or-app-color2, ${unsafeCSS(ORCore.DefaultColor2)}));     
        --internal-or-survey-color3: var(--or-survey-color3, var(--or-app-color3, ${unsafeCSS(ORCore.DefaultColor3)}));     
        --internal-or-survey-color4: var(--or-survey-color4, var(--or-app-color4, ${unsafeCSS(ORCore.DefaultColor4)}));     
        --internal-or-survey-color5: var(--or-survey-color5, var(--or-app-color5, ${unsafeCSS(ORCore.DefaultColor5)}));   
        --internal-or-survey-color6: var(--or-survey-color6, var(--or-app-color6, ${unsafeCSS(ORCore.DefaultColor6)}));     
        --internal-or-survey-color7: var(--or-survey-color7, var(--or-app-color7, ${unsafeCSS(ORCore.DefaultColor7)}));      
        
            
    }
    
    .shadow {
        -webkit-box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
        -moz-box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
        box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
    }
            
    .layout.horizontal {
        display: flex;
        flex-direction: row;
    }

    .layout.vertical {
        display: flex;
        flex-direction: column;
    }
    
    .center {
        justify-content: center; 
        align-items: center;
    }
    
    .t-center {
        text-align: center;
    }   
     
    .flex-grow {
        flex: 1 1 0;
    }
    
    :host  input {
        margin-bottom: 20px;
    }
    
    label {
        font-size: 14px;
        background-color: var(--internal-or-survey-color-grey);
    }
    
   
    button {
        font-size: 14px;
        height: 40px;
        background-color: var(--internal-or-survey-color-primary);
        margin-left: auto;
        color: #FFF;
        font-weight: bold;
        cursor: pointer;
        padding: 0px 20px;
        border-width: initial;
        border-style: none;
        border-color: initial;
        border-image: initial;
        border-radius: 5px;
    }
    
   .square-button {
        padding: 10px;
        background-color: var(--internal-or-survey-color-lightgrey);
    }
    
   .square-button or-icon {
       --or-icon-height: 16px;
       --or-icon-width: 16px;
        --or-icon-fill: var(--internal-or-survey-color-grey);
   }
   
   .square-button:hover {
        background-color: var(--internal-or-survey-color2);
   }
   
   .square-button:hover or-icon {
       --or-icon-fill: #FFFFFF;
   }
   
    .button-text {
        font-weight: bold;
        color: var(--internal-or-survey-color2);
        margin-bottom: 20px;
    }

    .button-text or-icon {
       --or-icon-height: 20px;
       --or-icon-width: 20px;
    }
    
    a {
        cursor: pointer;
    }
    
    or-icon {
        --or-icon-width: 24px;
        --or-icon-height: 24px;
        --or-icon-fill: var(--internal-or-survey-color-grey);
    }
    
    button or-icon {
        --or-icon-fill: #FFF;
    }
    
     a:hover > or-icon {
        --or-icon-fill: var(--internal-or-survey-color2);
    }
    
    .option-item .draggable,
    .option-item .delete-button {
        opacity: 0;
    }
    
    .option-item {
        margin: 0 -30px;
        padding: 0 30px;
    }
    
    .indented {
        margin-left: 40px;
    }
    
    .add-answer-option {
        margin-left: 70px;
    }
    
    .option-item:hover {
        background-color: #f2f2f2;
    }
    
    .option-item:hover .draggable,
    .option-item:hover .delete-button {
        opacity: 1;
    }
    
        .draggable-container {
            position: relative;
        }
        
       .question-item {
          position: relative;
          width: 100%;
          display: grid;
          align-items: center;
          box-sizing: border-box;
          user-select: none;
    
          transition: none;
          z-index: 1;
    
          background: white;
          font-family: sans-serif;
        }
        
        .question-item.nudgeDown:not(.dragged) {
          transform: translate3d(0, 48px, 0);
        }
        
        .question-item.nudgeUp:not(.dragged) {
          transform: translate3d(0, -48px, 0);
        }
        
        .question-item.dragged {
          box-shadow: 0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23);
          transition: none;
        }

        .dragging > .question-item:not(.dragged) {
          transition: transform 0.2s ease-out;
        }
        
        .draggable {
            cursor: pointer;
            position: absolute;
            left: -24px;
            z-index: 1;
        }
    .draggable or-icon {
        margin-top: 10px;
        --or-icon-fill: var(--app-grey-color);
    }
    
    input {
        padding: 5px 0 8px;
        margin-bottom: 30px;
        color: #333333;
        font-size: 16px;
        border-color: #f2f2f2;
    }
    
    input[disabled] {
        background-color: var(--app-lightgrey-color);
        color: var(--app-grey-color);
    }
    label {
        color: #808080;
        font-size: 12px;
        font-weight: bold;
    }
</style>
`;

export const surveyLayoutStyle =html`
<style>
            :host {
                width: 100%;
                background-color: #FFFFFF;
            }
            
            
            
            #surveyQuestions {
                width: 100%;
                overflow: hidden;
            }

            button-default > *:focus {
                background-color: transparent
            }

            p {
                text-align: center;
            }

            input {
                display: none;
            }

            input.text-input {
                display: block;
            }

            .anwser-card {
                flex: 1;
                flex-grow: 1;
                flex-basis: 0;
            }

            .anwser-card {
                background-color: var(--internal-or-survey-color4, #F3C11F);
                margin-left: 5px;
                border: 3px solid white;

                box-sizing: border-box;
                -moz-box-sizing: border-box;
                -webkit-box-sizing: border-box;
            }

            .anwser-card:first-child {
                margin-left: 0;
            }

            .anwser-card:nth-of-type(1),
            .anwser-card:nth-of-type(5),
            .anwser-card:nth-of-type(9){
                background-color: var(--internal-or-survey-color4, #F3C11F);
            }
            
            .anwser-card:nth-of-type(2),
            .anwser-card:nth-of-type(6),
            .anwser-card:nth-of-type(10){
                background-color: var(--internal-or-survey-color5, #4A99BA);
            }

            .anwser-card:nth-of-type(3),
            .anwser-card:nth-of-type(7),
            .anwser-card:nth-of-type(11){
                background-color: var(--internal-or-survey-color6, #23B099);
            }

            .anwser-card:nth-of-type(4),
            .anwser-card:nth-of-type(8),
            .anwser-card:nth-of-type(12){
                background-color: var(--internal-or-survey-color7, #EA8D31);
            }


            .anwser-card.rating {
                border: none;
                background-color: transparent;
            }

            .anwser-card.rating label {
                border-radius: 50%;
                background-color: var(--internal-or-survey-color3, #e32527);

                line-height: 30px;
                width: 30px;
                height: 30px;
                padding: 10px;
            }

            button {
                cursor: pointer;
                visibility: hidden;
                position: relative;
                -moz-appearance: none;
                -webkit-appearance: none;
                -moz-border-radius: 2px;
                -webkit-border-radius: 2px;
                border-radius: 2px;
                font-family: Arial, "Open Sans", sans-serif;
                text-align: center;
                border: 0;
                color: #fff;
                text-decoration: none;
                padding: .625rem calc(2.5rem + 10px) .625rem 1.25rem;
                background-color: var(--internal-or-survey-color3, #e32527);
                font-size: 18px;
                font-weight: 300;
    
            }
            
            
            button or-icon {
                margin-right: 20px;
            }
             
            button[visible] {
                visibility: visible;
            }

            button.previous {
                padding: .625rem 1.25rem .625rem calc(2.5rem + 10px);
            }

            button.previous:after {
                background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iaXNvLTg4NTktMSI/Pg0KPCEtLSBHZW5lcmF0b3I6IEFkb2JlIElsbHVzdHJhdG9yIDE5LjAuMCwgU1ZHIEV4cG9ydCBQbHVnLUluIC4gU1ZHIFZlcnNpb246IDYuMDAgQnVpbGQgMCkgIC0tPg0KPHN2ZyB2ZXJzaW9uPSIxLjEiIGlkPSJDYXBhXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4Ig0KCSB2aWV3Qm94PSIwIDAgMjU2IDI1NiIgc3R5bGU9ImVuYWJsZS1iYWNrZ3JvdW5kOm5ldyAwIDAgMjU2IDI1NjsiIHhtbDpzcGFjZT0icHJlc2VydmUiPg0KPGc+DQoJPGc+DQoJCTxwb2x5Z29uIHBvaW50cz0iMjA3LjA5MywzMC4xODcgMTc2LjkwNywwIDQ4LjkwNywxMjggMTc2LjkwNywyNTYgMjA3LjA5MywyMjUuODEzIDEwOS4yOCwxMjggCQkiLz4NCgk8L2c+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8L3N2Zz4NCg==');
                background-repeat: no-repeat;
                background-color: rgba(255, 255, 255, 0.5);
                background-position: center;
                background-size: 25%;
                content: "";
                width: 2.5rem;
                position: absolute;
                left: 0;
                top: 0;
                bottom: 0;
            }

            button.next:after {
                background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iaXNvLTg4NTktMSI/Pg0KPCEtLSBHZW5lcmF0b3I6IEFkb2JlIElsbHVzdHJhdG9yIDE5LjAuMCwgU1ZHIEV4cG9ydCBQbHVnLUluIC4gU1ZHIFZlcnNpb246IDYuMDAgQnVpbGQgMCkgIC0tPg0KPHN2ZyB2ZXJzaW9uPSIxLjEiIGlkPSJDYXBhXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4Ig0KCSB2aWV3Qm94PSIwIDAgMjU2IDI1NiIgc3R5bGU9ImVuYWJsZS1iYWNrZ3JvdW5kOm5ldyAwIDAgMjU2IDI1NjsiIHhtbDpzcGFjZT0icHJlc2VydmUiPg0KPGc+DQoJPGc+DQoJCTxwb2x5Z29uIHBvaW50cz0iNzkuMDkzLDAgNDguOTA3LDMwLjE4NyAxNDYuNzIsMTI4IDQ4LjkwNywyMjUuODEzIDc5LjA5MywyNTYgMjA3LjA5MywxMjggCQkiLz4NCgk8L2c+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8Zz4NCjwvZz4NCjxnPg0KPC9nPg0KPGc+DQo8L2c+DQo8L3N2Zz4NCg==');
                background-repeat: no-repeat;
                background-color: rgba(255, 255, 255, 0.5);
                background-position: center;
                background-size: 25%;
                content: "";
                width: 2.5rem;
                position: absolute;
                right: 0;
                top: 0;
                bottom: 0;
            }


            input {
                margin: 0 0 20px;
                padding: 8px;
            }
            
            input + label {
                font-size: 18px;
                color: #FFFFFF;
                display: block;
                text-align: center;
                opacity: 0.67;
                margin: 10px;
                padding: 40px;
                
                height: calc(100% - 60px);
                cursor: pointer;
                -ms-transform: scale(0.8);
                -webkit-transform: scale(0.8);
                transform: scale(0.8);

                -webkit-transition: transform 0.15s;
                -moz-transition: transform 0.15s;
                -o-transition: transform 0.15s;
                transition: transform 0.15s;
                background-color: transparent;
            }

            input + label:hover {
                opacity: 1;
                -ms-transform: scale(1);
                -webkit-transform: scale(1);
                transform: scale(1);
            }



            input[type="checkbox"]:checked + label,
            input[type="radio"]:checked + label {
                opacity: 1;
                -ms-transform: scale(1);
                -webkit-transform: scale(1);
                transform: scale(1);
            },

            input[type="radio"]:checked + label,
            input[type="checkbox"]:checked {
                border: 3px solid white;
            }
            
            textarea {
                flex-grow: 1;
                border: 1px solid var(--app-grey-color);
                height: 4em;
                line-height: 16px;
            }
            
            .answer-icon {
                width: 20px;
                height: 20px;
                display: block;
                margin: auto;
                font-size: 16px;
                line-height: 20px;
                font-weight: bold;
                border: 3px solid var(--app-white);
                border-radius: 5px;
                margin-bottom: 5px;
            }

            #survey-container {
                -ms-flex-pack: center;
                -webkit-justify-content: center;
                justify-content: center;
            }
            .anwser-card {
                width: 25%;
                min-width: 25%;
                max-width: 25%;
                flex-grow: 1;
                flex-basis: auto;
                margin-left: 0px;
            }
            @media (max-width: 1480px) {
                .anwser-card {
                    width: 50%;
                    min-width: 50%;
                    max-width: 50%;
                    flex-grow: 1;
                    flex-basis: auto;
                    margin-left: 0px;
                }
            }
            input + label {
                padding: 20px;
            }

            .anwser-card.rating {
                width: 20%;
                min-width: 20%;
                max-width: 20%;
            }
            textarea {
                height: 4em;
                line-height: 16px;
            }

            #survey-container.moveIn {
                -webkit-animation: moveIn .6s 1 forwards;
                animation: moveIn .6s 1 forwards;
            }
            #survey-container.moveOut {
                -webkit-animation: moveOut .6s 1 forwards;
                animation: moveOut .6s 1 forwards;
            }
            @-webkit-keyframes moveIn {
                0% {
                    -webkit-transform: translate(100%);
                    -moz-transform: translate(100%);
                    -ms-transform: translate(100%);
                    -o-transform: translate(100%);
                    transform: translate(100%);
                }
                100% {
                    -webkit-transform: translate(0%);
                    -moz-transform: translate(0%);
                    -ms-transform: translate(0%);
                    -o-transform: translate(0%);
                    transform: translate(0%);
                }
            }
            @keyframes moveIn {
                0% {
                    -webkit-transform: translate(100%);
                    -moz-transform: translate(100%);
                    -ms-transform: translate(100%);
                    -o-transform: translate(100%);
                    transform: translate(100%);
                }
                100% {
                    -webkit-transform: translate(0%);
                    -moz-transform: translate(0%);
                    -ms-transform: translate(0%);
                    -o-transform: translate(0%);
                    transform: translate(0%);
                }
            }

            @-webkit-keyframes moveOut {
                0% {
                    -webkit-transform: translate(-100%);
                    -moz-transform: translate(-100%);
                    -ms-transform: translate(-100%);
                    -o-transform: translate(-100%);
                    transform: translate(-100%);
                }
                100% {
                    -webkit-transform: translate(0%);
                    -moz-transform: translate(0%);
                    -ms-transform: translate(0%);
                    -o-transform: translate(0%);
                    transform: translate(0%);
                }
            }
            @keyframes moveOut {
                0% {
                    -webkit-transform: translate(-100%);
                    -moz-transform: translate(-100%);
                    -ms-transform: translate(-100%);
                    -o-transform: translate(-100%);
                    transform: translate(-100%);
                }
                100% {
                    -webkit-transform: translate(0%);
                    -moz-transform: translate(0%);
                    -ms-transform: translate(0%);
                    -o-transform: translate(0%);
                    transform: translate(0%);
                }
            }
            
            
            @media (max-width: 769px) {
                input[type="color"],
                input[type="date"],
                input[type="datetime"],
                input[type="datetime-local"],
                input[type="email"],
                input[type="month"],
                input[type="number"],
                input[type="password"],
                input[type="search"],
                input[type="tel"],
                input[type="text"],
                input[type="time"],
                input[type="url"],
                input[type="week"],
                select:focus,
                textarea {
                  font-size: 16px;
                }
            }
</style>
`;