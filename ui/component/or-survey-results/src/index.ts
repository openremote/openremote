import {customElement, html, LitElement, TemplateResult, property} from "lit-element";
import {Asset} from "@openremote/model";
import {surveyResultStyle} from "./style";

@customElement("or-survey-results")
class OrSurveyResults extends LitElement {

    @property({type: Object})
    public survey?: Asset;

    @property({type: Object})
    public results?: Asset;

    @property({type: Number})
    public activeResult?: number;

    @property({type: Number})
    public maxAmount?: number;

    protected render() {
        if (!this.survey) {
            return html``;
        }


        let activeResult:any;
        let results = [];
        let maxAmount = 0;

        if(this.survey.attributes && this.survey.attributes.processedResults) {
            results = this.computeResults(this.survey.attributes.processedResults.value);
        }

        if(!this.activeResult) {
            activeResult =  {...results[0]};
        } else {
            activeResult =  {...results[this.activeResult]};
        }

        if(this.survey.attributes){
            maxAmount = this.survey.attributes.responseAmount.value;
        }

        return html`
            ${surveyResultStyle}
             <div id="surveyResults" class="layout vertical">
             
                        <div class="layout horizontal">
                            ${results ? results.map((item, index) => {
            return html`
                                     <div class="button-default flex ${item.name === activeResult.name ? 'active' : ''}" @click="${() => this.changeActiveResult(index)}"> ${item.name}</div>
                                    `
        }) : ``}
                        </div>
                        
                        <h4>Aantal deelnemers: ${maxAmount}</h4>
                        
                        
                        ${activeResult && activeResult.result ? html`
                                <div id="chart-container" class="layout vertical">
                                    <div class="chart-child layout horizontal">
                                        <div class="layout vertical border-right">
                                            <span class="flex label-y">100%</span>
                                            <span class="flex label-y">75%</span>
                                            <span class="flex label-y">50%</span>
                                            <span class="flex label-y">25%</span>
                                        </div>
                                        
                                        ${activeResult.result.map(item => {
            return html`
                                                <div class="flex">
                                                    <div class="chart">
                                                         ${item.value.map(bar => {
                return html`
                                                                <div class="bar">
                                                                <span style="height:${this.computeHeight(bar.count)}%;">
                                                                    <div class="bar-top-label">${this.computeHeight(bar.count)}%</div>
                                                                    <label>${bar.name} (${bar.count})</label>
                                                                </span>
                                                                </div>
                                                        `})}
                                                    </div>
                                                </div>
                                        `})}
                                        
                                    </div>
                                
                                    <div class="chart-child labels layout horizontal">
                                        ${activeResult.result.map(item => {
            return html`
                                                <div class="flex label">
                                                    <span class="label-x">${item.name}</span>
                                                </div> 
                                        `})}
                                    </div>
                                </div>
                                
                     ` : ``}
                        
            </div>
            
                  
        `;
    }

    computeHeight(barAmount:number) {
        if (barAmount && this.survey && this.survey.attributes) {
            const maxAmount = this.survey.attributes.responseAmount.value;
            return Math.round((barAmount / maxAmount) * 100);
        } else {
            return 0;
        }
    }

    computeResults(results:any) {
        if (!results) {
            return;
        }
        const computedResults = results.map(category => {
            const categoryName = Object.keys(category)[0];
            return {
                name : categoryName,
                result : category[categoryName].map(question => {
                    const questionName = Object.keys(question)[0];
                    return {
                        name : questionName,
                        value : question[questionName].map(answer => {
                            const answerName = Object.keys(answer)[0];
                            const amount = answer[answerName];
                            if (typeof amount === "number" &&
                                isFinite(amount) &&
                                Math.floor(amount) === amount) {
                                return {name : answerName, count : amount}
                            }
                        })
                    }
                })
            }
        });


        return computedResults;
    }

    changeActiveResult(index:number) {
        this.activeResult = index;
    }

}
