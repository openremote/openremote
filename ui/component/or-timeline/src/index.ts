import {html, LitElement, property} from 'lit-element';

import moment from 'moment';

class OrTimeline extends LitElement {
    protected render() {
        if(this.shadowRoot){
            const range:any = this.shadowRoot.getElementById('or-timeline-slider');
            if(range) {
                range.value = Math.round(this.current);
                this.moveBubble(range);
            }
        }

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
                        box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                        
                        padding-top: 20px;
                    }
                    .slidecontainer {
                        position: relative;
                        margin: -5px 50px 0 30px;
                    }
                    
                    /* The slider itself */
                    .slider {
                      box-sizing: border-box;
                      -webkit-appearance: none;  /* Override default CSS styles */
                      appearance: none;
                      width: calc(100% + 20px);
                      margin-left: -10px;
                      height: 12px;
                      background: #bdbdbd; /* Grey background */
                      outline: none; /* Remove outline */
                      opacity: 1; /* Set transparency (for mouse-over effects on hover) */
                      -webkit-transition: .2s; /* 0.2 seconds transition on hover */
                      transition: opacity .2s;
                      
                      border-radius: 6px;
                      cursor: pointer;
                    }
                    
                    /* The slider handle (use -webkit- (Chrome, Opera, Safari, Edge) and -moz- (Firefox) to override default look) */ 
                    .slider::-webkit-slider-thumb {
                        -webkit-appearance: none;
                        appearance: none;
                           width: 18px;
                          height: 18px;
                          margin: 0;
                          border-radius: 50%;
                          cursor: pointer;
                          border: 0 !important;
                          background-color: var(--timeline-accent,  blue); /* Green background */
                    }
                    
                    .slider::-ms-thumb,
                    .slider::-moz-range-thumb,
                    .slider::-webkit-slider-thumb {
                      width: 18px;
                      height: 18px;
                      margin: 0;
                      border-radius: 50%;
                      cursor: pointer;
                      border: 0 !important;
                      background-color: var(--timeline-accent,  blue); /* Green background */
                    }
                    
                      .slider::-ms-track {
                      -webkit-appearance: none;
                      background: transparent;
                      color: transparent;
                    }
                    
                    .slider::-moz-range-track {
                      -moz-appearance: none;
                      background: transparent;
                      color: transparent;
                    }
                    
                    .slider:focus::-webkit-slider-runnable-track {
                      background: transparent;
                      border: transparent;
                    }
                    
                    .slider:focus {
                      outline: none;
                    }
                    
                    .slider::-ms-thumb {
                      pointer-events: all;
                      width: 18px;
                      height: 18px;
                      border-radius: 14px;
                      border: 0 none;
                      background: var(--timeline-accent,  blue); ;
                    }
                    
                    .slider::-moz-range-thumb {
                      pointer-events: all;
                      width: 18px;
                      height: 18px;
                      border-radius: 14px;
                      border: 0 none;
                      background: var(--timeline-accent,  blue); ;
                    }
                    
                    .slider::-webkit-slider-thumb {
                      pointer-events: all;
                      width: 18px;
                      height: 18px;
                      border-radius: 14px;
                      border: 0 none;
                      background: var(--timeline-accent,  blue); ;
                      -webkit-appearance: none;
                    }
                    
                    .slider::-ms-fill-lower {
                      background: transparent;
                      border: 0 none;
                    }
                    
                    .slider::-ms-fill-upper {
                      background: transparent;
                      border: 0 none;
                    }
                    
                    .slider::-ms-tooltip {
                      display: none;
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
                        background-color:  var(--timeline-accent,  blue);
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
                <div id="range-value">${this.current === 0 ? 'LIVE' : moment().add(this.current, 'minutes').format('HH:mm')}</div>
            </div>
            <div class="timeline-container">
                 <div id="timelineHourMarkers" class="layout horizontal justified style-scope controller-timeline">
                    ${new Array((this.maxRange/60)+1).fill(0).map((_, idx) => {
                        return html`
                        ${idx === 0 ? html`
                            <div class="timelineHourMark style-scope controller-timeline">Nu</div>
                        ` : html`
                            <div class="timelineHourMark style-scope controller-timeline">+${idx}u</div>
                        `}
                    `})}
                </div>
                
                <div class="slidecontainer">
                  <input id="or-timeline-slider" class="slider" type="range"  @input="${this.moveBubble}" @change="${this.valueChange}" value="${this.current}"  min="${this.minRange}" max="${this.maxRange}" step="${this.step}">
                </div>
            </div>
               
    `;
    }
    @property({type: Function})
    private onChange: any;

    // default value in minutes
    @property({type: Number})
    private value: number = 0;

    // default value in minutes
    @property({type: Number})
    public current: number = 0;

    // maxRange in minutes
    @property({type: Number})
    private maxRange: number = 360;

    // minRange in minutes
    @property({type: Number})
    private minRange: number = 0;

    // Steps in minutes
    @property({type: Number})
    private step: number = 5;

    moveBubble(e:any=null, value:string|null=null) {
        let el;
        if(e){
            el = e.target;
        } else {
            if(this.shadowRoot){
                el = this.shadowRoot.getElementById('or-timeline-slider');
            }
        }

        if(el) {
            if(value){
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
            let offset = 15;
            let newPlace;

            // Prevent bubble from going beyond left or right (unsupported browsers)
            if (newPoint < 0) {
                newPlace = 0;
            } else if (newPoint > 1) {
                newPlace = width - offset;
            } else {
                newPlace = (width - offset) * newPoint;

                if(newPlace < 0 ) {
                    newPlace = 0;
                }

            }

            // Move bubble
            if(this.shadowRoot) {
                const range:HTMLElement | null = this.shadowRoot.getElementById('range-value');
                if(range){
                    range.style.left = newPlace + "px";
                }
            }
        }

    }

    valueChange (e:any) {
        this.moveBubble(e);
        this.onChange();
    }

    constructor() {
        super();
    }


}

window.customElements.define('or-timeline', OrTimeline);
