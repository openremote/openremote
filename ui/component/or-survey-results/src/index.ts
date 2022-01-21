import {html, LitElement, PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";
import {Asset, AssetQuery} from "@openremote/model";
import {surveyResultStyle} from "./style";
import manager from "@openremote/core";

export interface ProcessedResultAnswer {
    name: string,
    count: number
}

export interface ProcessedResult {
    name: string,
    value: ProcessedResultAnswer[]
}

export interface ProcessedResults {
    name: string;
    result: ProcessedResult[]
}

@customElement("or-survey-results")
class OrSurveyResults extends LitElement {

    @property({type: Object})
    public survey?: Asset;

    @property({type: Array})
    public questions?: Asset[];

    @property({type: String})
    public surveyId?: string;

    @property({type: Number})
    public activeResult?: number;

    @property({type: Number})
    public maxAmount?: number;

    updated(_changedProperties: PropertyValues) {
        if(_changedProperties.has('surveyId')) {
            this.getSurvey();
        }
    }

    protected render() {
        if (!this.survey) {
            return html``;
        }


        let activeResult: ProcessedResults;
        let results: any = [];
        let maxAmount = 0;

        if (this.survey.attributes && this.survey.attributes.processedResults) {
            results = this.computeResults(this.survey.attributes.processedResults.value);
        }

        if (!this.activeResult) {
            activeResult = {...results[0]};
        } else {
            activeResult = {...results[this.activeResult]};
        }

        if (this.survey.attributes) {
            maxAmount = this.survey.attributes.responseAmount.value;
        }

        return html`
            ${surveyResultStyle}
             <div id="surveyResults" class="layout vertical">
             
                        <div class="layout horizontal">
                            ${results ? results.map((item: ProcessedResult, index: number) => {
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
                                        
                                        ${activeResult.result.map((item: ProcessedResult) => {
            return html`
                                                <div class="flex">
                                                    <div class="chart">
                                                         ${item.value.map((bar: ProcessedResultAnswer) => {
                return html`
                                                                <div class="bar">
                                                                <span style="height:${this.computeHeight(bar.count)};">
                                                                    <div class="bar-top-label">${this.computeHeight(bar.count)}</div>
                                                                    <label>${bar.name} (${bar.count})</label>
                                                                </span>
                                                                </div>
                                                        `
            })}
                                                    </div>
                                                </div>
                                        `
        })}
                                        
                                    </div>
                                
                                    <div class="chart-child labels layout horizontal">
                                        ${activeResult.result.map((item: ProcessedResult) => {
            return html`
                                                <div class="flex label">
                                                    <span class="label-x">${item.name}</span>
                                                </div> 
                                        `
        })}
                                    </div>
                                </div>
                                
                     ` : ``}
                        
            </div>
            
                  
        `;
    }
    getSurvey() {
        let surveyId: string;
        if(this.surveyId){
            surveyId = this.surveyId;
        } else {
            return;
        }

        const surveyQuery: AssetQuery = {
            ids: [surveyId]
        };

        manager.rest.api.AssetResource.queryAssets(surveyQuery).then((response) => {
            if (response && response.data) {
                this.survey = response.data[0];
                this.requestUpdate();
            }
        }).catch((reason) => {
            console.error("Error: " + reason);
        });

        const surveyQuestQuery: AssetQuery = {
            parents: [{id:surveyId}]
        };

        manager.rest.api.AssetResource.queryAssets(surveyQuestQuery).then((response) => {
            if (response && response.data) {
                this.questions = response.data;
                this.requestUpdate();
            }
        }).catch((reason) => {
            console.error("Error: " + reason);
        });

    }
    
    computeHeight(barAmount: number) {
        if (barAmount && this.survey && this.survey.attributes) {
            const maxAmount = this.survey.attributes.responseAmount.value;
            return Math.round((barAmount / maxAmount) * 100)+"%";
        } else {
            return "0%";
        }
    }

    computeResults(results: ProcessedResult[]) {
        if (!results) {
            return;
        }
        const computedResults = results.map((category: any) => {
            const categoryName = Object.keys(category)[0];
            return {
                name: categoryName,
                result: category[categoryName].map((question: any) => {
                    const questionId = Object.keys(question)[0];
                    const assetQuestion = this.questions ? this.questions.find((question)=> question.id === questionId) : null
                    return {
                        name: assetQuestion ? assetQuestion.name : "",
                        value: question[questionId].map((answer: any) => {
                            const answerName = Object.keys(answer)[0];
                            const amount = answer[answerName];
                            if (typeof amount === "number" &&
                                isFinite(amount) &&
                                Math.floor(amount) === amount) {
                                return {name: answerName, count: amount}
                            }
                        })
                    }
                })
            }
        });

        return computedResults;
    }

    changeActiveResult(index: number) {
        this.activeResult = index;
    }

}
