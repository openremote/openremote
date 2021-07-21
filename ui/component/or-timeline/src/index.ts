import {html, LitElement} from "lit";
import {customElement, property} from "lit/decorators.js";
import moment from "moment";

@customElement("or-timeline")
class OrTimeline extends LitElement {

    // default value in minutes
    @property({type: Number})
    public current: number = 0;

    @property({type: Function})
    public onChange: any;

    // default value in minutes
    @property({type: Number})
    public value: number = 0;

    // maxRange in minutes
    @property({type: Number})
    public maxRange: number = 360;

    // minRange in minutes
    @property({type: Number})
    public minRange: number = 0;

    // Steps in minutes
    @property({type: Number})
    public step: number = 5;

    constructor() {
        super();
    }

    public moveBubble(e: any= null, value: string|null= null) {
        let el;
        if (e) {
            el = e.target;
        } else {
            if (this.shadowRoot) {
                el = this.shadowRoot.getElementById("or-timeline-slider");
            }
        }

        if (el) {
            if (value) {
                this.current = parseInt(value);
            } else {
                this.current = parseInt(el.value);
            }
            // Measure width of range input
            const width = el.offsetWidth;
            const v = this.current;

            // Figure out placement percentage between left and right of input
            const newPoint = v / this.maxRange;
            // Janky value to get pointer to line up better
            const offset = 15;
            let newPlace;

            // Prevent bubble from going beyond left or right (unsupported browsers)
            if (newPoint < 0) {
                newPlace = 0;
            } else if (newPoint > 1) {
                newPlace = width - offset;
            } else {
                newPlace = (width - offset) * newPoint;

                if (newPlace < 0 ) {
                    newPlace = 0;
                }

            }

            // Move bubble
            if (this.shadowRoot) {
                const range: HTMLElement | null = this.shadowRoot.getElementById("range-value");
                if (range) {
                    range.style.left = newPlace + "px";
                }
            }
        }
    }

    public valueChange(e: any) {
        this.moveBubble(e);
        this.onChange();
    }

    protected render() {

        return html`
              <style>
                  :host {
                    position: relative;
                  }
                    .timeline-container {
                        display: block;
                        height: 52px;
                        background-color: var(--app-white, #FFFFFF);
                        
                        -webkit-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                        -moz-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                        box-shadow: 1px 1px 2px 1px rgba(0,0,0,0.28);
                        
                        padding-top: 20px;
                    }
                    .slidecontainer {
                        position: relative;
                        margin: -5px 44px 0 19px;
                    }
                    
                    /* The slider itself */
                    .slider {
                      color: var(--timeline-accent,  #1D5632);
                      box-sizing: border-box;
                      -webkit-appearance: none;
                      outline: none;
                      width: 100%;
                    }
                    .slider::-webkit-slider-runnable-track {                      
                      cursor: pointer;
                      border: transparent;
                      background: #bdbdbd;
                      border-radius: 6px;
                      height: 12px;
                    }
                    .slider::-webkit-slider-thumb {
                      height: 18px;
                      width: 18px;
                      border-radius: 50%;
                      cursor: pointer;
                      -webkit-appearance: none;
                      background-color: var(--timeline-accent,  #1D5632); /* Green background */
                      pointer-events: all;
                      margin-top: -2px;
                    }

                    .slider::-moz-range-track {
                      cursor: pointer;
                      border: transparent;
                      background: #bdbdbd;
                      border-radius: 6px;
                      height: 12px;
                    }
                    .slider::-moz-range-thumb {
                      border: 0 none;
                      height: 18px;
                      width: 18px;
                      border-radius: 50%;
                      cursor: pointer;
                      -webkit-appearance: none;
                      background-color: var(--timeline-accent,  #1D5632); /* Green background */
                      pointer-events: all;
                      margin-top: -2px;
                    }
                    .slider::-moz-range-progress {
                        background-color: var(--timeline-accent,  #1D5632);
                        height: 12px;
                        border-radius: 6px;
                    }
                    
                    .slider::-ms-track {
                      cursor: pointer;
                      border: transparent;
                      background: #bdbdbd;
                      border-radius: 6px;
                      height: 12px;
                    }
                    .slider::-ms-thumb {
                      border: 0 none;
                      height: 18px;
                      width: 18px;
                      border-radius: 50%;
                      cursor: pointer;
                      -webkit-appearance: none;
                      background-color: var(--timeline-accent,  currentColor); /* Bug in Edge set colour on slider itself */
                      pointer-events: all;
                      margin-top: -2px;
                    }
                    
                    .slider::-ms-fill-lower {
                      background-color:  var(--timeline-accent,  currentColor);
                      border: 0 none;
                      border-radius: 6px;
                    }
                    
                    .slider::-ms-fill-upper {
                      background: transparent;
                      border: 0 none;
                    }
                    
                    #timelineHourMarkers {
                      position: relative;
                      display: -webkit-box;
                      display: -moz-box;
                      display: -ms-flexbox;
                      display: -webkit-flex;
                      display: flex;
                      -webkit-box-orient: horizontal;
                      -moz-box-orient: horizontal;
                      -webkit-box-direction: normal;
                      -moz-box-direction: normal;
                      -webkit-flex-direction: row;
                      -ms-flex-direction: row;
                      flex-direction: row;
                      
                      margin: 0 50px 0 30px;
                    }
                    
                    #timelineHourMarkers > .timelineHourMark{
                        display: flex;
                        flex: 1 100%;
                        flex-grow: 1;
                        flex-shrink: 1;
                        flex-basis: 100%;
                        
                        overflow: visible;
                        color: var(--timeline-grey,  darkgrey);
                        border-left: 1px solid var(--timeline-grey,  darkgrey);
                        font-size: 12px;
                        -webkit-user-select: none;
                        -moz-user-select: none;
                        -ms-user-select: none;
                        user-select: none;
                        padding-left: 4px;
                        padding-bottom: 4px;
                    }
                    #timelineHourMarkers  .timelineHourMark:last-child {
                        position: absolute;
                        right: -25px;
                    }
                  
                    #range-value {
                        position: absolute;
                        background-color:  var(--timeline-accent,  #1D5632);
                        height: 30px; 
                        width: 60px;
                        margin-left: -30px;
                        text-align: center; 
                        color: white; 
                        display: inline-block; 
                        left: 0;
                        font-size: 12px;
                        font-weight: bold;
                        line-height: 30px;
                    }
                    
                    .time-value-container {
                        width: calc(100% - 80px);
                        display: block;
                        position: relative;
                        height: 30px;
                        margin: 0 50px 0 30px;
                    }
              </style>
            
            
            <div class="time-value-container">
                <div id="range-value">${this.current === 0 ? "LIVE" : moment().add(this.current, "minutes").format("HH:mm")}</div>
            </div>
            <div class="timeline-container">
                 <div id="timelineHourMarkers" class="layout horizontal justified style-scope controller-timeline">
                    ${new Array((this.maxRange / 60) + 1).fill(0).map((_, idx) => {
                        return html`
                        ${idx === 0 ? html`
                            <div class="timelineHourMark style-scope controller-timeline">Nu</div>
                        ` : html`
                            <div class="timelineHourMark style-scope controller-timeline">+${idx}u</div>
                        `}
                    `; })}
                </div>
                
                <div class="slidecontainer">
                  <input id="or-timeline-slider" class="slider" type="range" @input="${this.moveBubble}" @change="${this.valueChange}" value="${this.current}"  min="${this.minRange}" max="${this.maxRange}" step="${this.step}">
                </div>
            </div>
               
    `;
    }

}
