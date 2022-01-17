import {html, LitElement, PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";
import {Asset, AssetQuery} from "@openremote/model";
import {surveyLayoutStyle, surveySectionStyle} from "./style";
import set from "lodash-es/set";
import get from "lodash-es/get";
import orderBy from "lodash-es/orderBy";

import * as momentImported from 'moment';
import manager, {EventCallback} from "@openremote/core";
import filter from "lodash-es/filter";
import "@openremote/or-translate";

const moment = momentImported;
declare var MANAGER_URL: string;

export interface OrComputeGridEventDetail {
}

export class OrComputeGridEvent extends CustomEvent<OrComputeGridEventDetail> {

    public static readonly NAME = "or-asset-viewer-compute-grid-event";

    constructor() {
        super(OrComputeGridEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
export interface AnswerOption {
    value: string
}

export interface SurveyAnswers {
    [key: string]: string | string[]
}

@customElement("or-survey")
class OrSurvey extends LitElement {

    @property({type: Object})
    public survey?: Asset;

    @property({type: String})
    public surveyId?: string;

    @property({type: Boolean})
    public isAddingQuestion?: boolean;

    @property({type: Boolean})
    public hasIntroQuestion?: boolean;

    @property({type: Object})
    public question?: Asset;

    @property({type: Object})
    public surveyAnswers?: SurveyAnswers;

    @property({type: Array})
    public questions?: Asset[];

    public readonly?: boolean;

    public questionIndex?: number;
    public questionIndexLabel?: number;

    public nextButtonLabel?: string;

    @property({type: Boolean})
    public saveanswers?: boolean;

    @property({type: Boolean})
    public completed?: boolean;

    @property({type: Boolean})
    public hasSubmission?: boolean;

    @property({type: Boolean})
    public previousButton?: boolean;

    @property({type: Boolean})
    public nextButton?: boolean;

    public questionAnimation?: string;

    protected _initCallback?: EventCallback;

    constructor() {
        super();

        this.resetSurvey();
        this.questions = [];
        this.completed = false;
    }

    protected render() {
        if (!this.survey) {
            return html``;
        }

        const orderedQuestions = orderBy(this.questions, ['attributes.order.value'],['asc']);
        const status = this.checkAssetPeriode(this.survey);
        let currentAnswer:string | string[];

        if(this.questions && this.questionIndex && this.questions[this.questionIndex]) {
            const currentQuestion = this.questions[this.questionIndex];
            if(currentQuestion && currentQuestion.id && this.surveyAnswers) {
                currentAnswer = this.surveyAnswers[currentQuestion.id];
            }
        }
        
        return html`
                ${surveySectionStyle}
                ${surveyLayoutStyle}
                 <div id="surveyQuestions"> 
                        ${this.completed || this.hasSubmission ? html`
                                <p>${this.survey.attributes ? this.survey.attributes.thankYouMessage.value : ''}</p>
                                ${!this.saveanswers ? html`
                                    <button visible="${this.previousButton}" class="previous" @click="${this.resetSurvey}"
                                            aria-label="To survey"><or-translate value="To survey"></or-translate>
                                            <svg viewBox="0 0 32 32" class="icon icon-chevron-left" viewBox="0 0 32 32" aria-hidden="true"><path d="M14.19 16.005l7.869 7.868-2.129 2.129-9.996-9.997L19.937 6.002l2.127 2.129z"/></svg>
                                    </button>   
                                ` : ``}
                        ` : html`
                            ${status === 'before' ? html`
                                <p>${this.survey.attributes ? this.survey.attributes.beforeValidMessage.value : ''}</p>
                            ` : ``}
                            
                            ${status === 'after' ? html`
                                <p>${this.survey.attributes ? this.survey.attributes.afterValidMessage.value : ''}</p>
                            ` : ``}
                            
                            ${status === 'live' || (status === 'not_published' && !this.saveanswers) ? html`
                                         ${orderedQuestions.map((question: Asset, index:number) => {

            return html`
                                                    ${get(question, 'attributes.active.value') && index === this.questionIndex ? html`
                                                        <div class="${this.questionAnimation}">
                                                            <p>${question.name}</p>
                                                            
                                                            <div id="survey-container" style="flex-wrap: wrap;" class="layout horizontal ${this.questionAnimation}">
                                
                                                                   ${this.getType(question.type) === 'text' ? html`
                                                                        <textarea rows="4" class="text-input" type="${this.getInputType(question.type)}" id="${question.id}" name="${question.id}_${index}">${currentAnswer}</textarea>
                                                                   ` : html`
                                                                        ${question.attributes && question.attributes.answerOptions.value.map((answer:AnswerOption, index:number) => {
                return html`
                                                                                <div class="anwser-card ${this.getType(question.type)}">
                                                                                    
                                                                                    ${!currentAnswer ? html`
                                                                                        <input type="${this.getInputType(question.type)}" id="${question.id}_${index}"
                                                                                               name="${question.id}" value="${answer.value}"
                                                                                               autofocus />
                                                                                   `: html`
                                                                                        ${this.getType(question.type) === 'multiSelect' ? html`
                                                                                           <input type="${this.getInputType(question.type)}" id="${question.id}_${index}"
                                                                                                       name="${question.id}" value="${answer.value}"
                                                                                                       ?checked="${currentAnswer.includes(answer.value)}"
                                                                                                       autofocus />
                                                                                       ` : html`
                                                                                            <input type="${this.getInputType(question.type)}" id="${question.id}_${index}"
                                                                                                           name="${question.id}" value="${answer.value}"
                                                                                                           ?checked="${currentAnswer === answer.value}"
                                                                                                           autofocus />
                                                                                       `}
                                                                                   `}
                                                                                    <label class="flex-grow" @click="${(e:Event) => this.onAnswer(e, answer)}" for="${question.id}_${index}">
                                                                                        ${answer.value}
                                                                                    </label>
                                                                                </div>
                                                                        `})}
                                                                   `}
                                                               
                                                            </div>
                                                        </div>
                                                        
                                                    ` : ``}
                                            `})}
                                            
                                            <div style="margin-top:20px;" class="layout horizontal justified center-center">
                                                <button ?visible="${this.previousButton}" class="previous" @click="${this.previousQuestion}"
                                                        aria-label="Previous">
                                                        <or-translate value="Previous"></or-translate>
                                                        <svg viewBox="0 0 32 32" class="icon icon-chevron-left" viewBox="0 0 32 32" aria-hidden="true"><path d="M14.19 16.005l7.869 7.868-2.129 2.129-9.996-9.997L19.937 6.002l2.127 2.129z"/></svg>
                                                </button>
                                                <div style="line-height: 40px;" class="flex-grow t-center">${this.questionIndexLabel} / ${this.questions ? this.questions.length : 0}</div>
                                                <button ?visible="${this.nextButton}" class="next" @click="${this.onAnswer}" data-autoforward="true"
                                                        aria-label="${this.nextButtonLabel}">
                                                        <or-translate value="${this.nextButtonLabel}"></or-translate>
                                                        <svg viewBox="0 0 32 32" class="icon icon-chevron-right" viewBox="0 0 32 32" aria-hidden="true"><path d="M18.629 15.997l-7.083-7.081L13.462 7l8.997 8.997L13.457 25l-1.916-1.916z"/></svg>
                                                </button>
                                            </div>
                                           
                                ` : ``}
                            `}
            </div>

                  
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        this.getSurvey();
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if(_changedProperties.has('survey') || _changedProperties.has('surveyId')) {
            this.resetSurvey();
        }

        this.dispatchEvent(new OrComputeGridEvent())
    }

    checkAssetPeriode(asset:Asset) {
        const today = moment();
        const startDate = get(asset, 'attributes.validity.value.start');
        const endDate = get(asset, 'attributes.validity.value.end');
        // not published
        if(get(asset, 'attributes.published.value') === false) {
            return 'not_published';
        } else if(today.diff(startDate) > 0 && today.diff(endDate) < 0) {
            // between dates
            return 'live';
        } else if (today.diff(startDate) < 0) {
            // before start date
            return 'before';
        } else if (today.diff(endDate) > 0) {
            // after end date
            return 'after';
        } else {
            return 'not_published'
        }
    }

    getInputType(type:string|undefined) {
        if (type) {
            if (type.includes('singleSelect') || type.includes('rating')) {
                return "radio";
            }
            else if (type.includes('multiSelect')) {
                return "checkbox";
            }
            else if (type.includes('text')) {
                return "text";
            }
        }
    }

    getType(type:string|undefined) {
        if (type) {
            const parts = type.split(':');
            const label = parts[parts.length - 1];
            return label;
        }
    }

    checkButtons() {
        if(!this.survey || !this.questions || typeof this.questionIndex === "undefined") {
            return;
        }

        this.questions.sort((a, b) => a.attributes && b.attributes ? a.attributes.order.value - b.attributes.order.value : 0);
        this.questions = [...this.questions];
        
        if (this.questionIndex > 0) {
            this.previousButton = true;
            this.nextButton = true;
        } else {
            if(this.questions[0]) {
                if(this.questions[0].attributes && !this.questions[0].attributes.categorizeInResult.value) {
                    this.nextButton = true;
                }else{
                    this.nextButton = false;
                }
            }
            this.previousButton = false;
        }

        if (this.questionIndex + 1 === this.questions.length) {
            this.nextButtonLabel= 'Send';
        } else {
            this.nextButtonLabel= 'Next';
        }

        if(this.questionIndex === this.questions.length && this.questions.length !== 0) {
            this.completed = true;
            if (this.survey.attributes && this.survey.attributes.published.value && this.saveanswers) {
                localStorage.setItem('survey'+this.survey.id, "set");

                const xhttp = new XMLHttpRequest();
                const url = manager.config.managerUrl ? manager.config.managerUrl+"/rest/survey/" : window.location.origin+"/rest/survey/";
                xhttp.open("POST", url + this.survey.id, true);
                xhttp.setRequestHeader("Content-type", "application/json");
                xhttp.send(JSON.stringify(this.surveyAnswers));
            }
        }

        this.requestUpdate();
    }

    nextQuestion() {
        if(typeof this.questionIndex === 'undefined' || typeof this.questionIndexLabel === 'undefined' ) {
            return;
        }

        this.questionAnimation = 'moveIn';
        this.questionIndex = this.questionIndex + 1;
        this.questionIndexLabel = this.questionIndexLabel + 1;
        this.checkButtons();
    }

    resetSurvey() {
        this.questionIndex = 0;
        this.questionIndexLabel = 1;
        this.nextButtonLabel = "Next";
        this.completed = false;
        this.surveyAnswers = {};
        this.checkButtons();
    }

    previousQuestion() {
        if(typeof this.questionIndex === 'undefined' || typeof this.questionIndexLabel === 'undefined' ) {
            return;
        }

        this.questionAnimation = 'moveOut';
        this.questionIndex = this.questionIndex - 1;
        this.questionIndexLabel = this.questionIndexLabel - 1;
        this.checkButtons();
    }

    onAnswer(e: Event, answer:AnswerOption) {
        if(!this.questions || typeof this.questionIndex === 'undefined' || typeof this.questionIndexLabel === 'undefined' ) {
            return;
        }
        const target = e.currentTarget;
        const surveyAnswers = this.surveyAnswers;
        const currQuestion = this.questions[this.questionIndex];
        if (this.getType(currQuestion.type) === "singleSelect" || this.getType(currQuestion.type) === "rating") {
            if (!(target instanceof HTMLButtonElement) && !(target instanceof HTMLLabelElement)) {
                return;
            }
            if (target && target.dataset.autoforward === "true") {
                this.nextQuestion();
            } else {
                if(currQuestion && currQuestion.id && surveyAnswers){
                    surveyAnswers[currQuestion.id] = answer.value;
                    this.surveyAnswers = surveyAnswers;
                    this.nextQuestion();
                }
            }

        }
        else if (this.getType(currQuestion.type) === "text") {
            const id = this.questions[this.questionIndex].id;
            if(this.shadowRoot && id) {
                const element = this.shadowRoot.getElementById(id) as HTMLInputElement;
                if (element && currQuestion && currQuestion.id && surveyAnswers) {
                    const value = element.value;
                    surveyAnswers[currQuestion.id] = value;
                }
            }
            this.nextQuestion();
        }
        else if (this.getType(currQuestion.type) === "multiSelect") {
            if (!(target instanceof HTMLButtonElement) && !(target instanceof HTMLLabelElement)) {
                return;
            }

            if (target.dataset.autoforward === "true") {
                this.nextQuestion();
            } else {
                if(currQuestion && currQuestion.id && surveyAnswers && target instanceof HTMLLabelElement) {
                    if (!surveyAnswers[currQuestion.id]) {
                        surveyAnswers[currQuestion.id] = [];
                    }
                    const array = surveyAnswers[currQuestion.id];
                    if(this.shadowRoot && Array.isArray(array)) {
                        const input = this.shadowRoot.getElementById(target.htmlFor) as HTMLInputElement;
                        if (!input.checked) {
                            array.push(answer.value);
                        } else {
                            // Delete answer from array
                            const index = surveyAnswers[currQuestion.id].indexOf(answer.value);
                            array.splice(index, 1);
                        }
                        this.surveyAnswers = surveyAnswers;
                    }
                }
            }
        }
    }

    getSurvey() {
        let surveyId: string;
        if (location.hash.indexOf('survey') !== -1) {
            surveyId = location.hash.split('/')[1];
            if(!surveyId) {
                this.checkButtons();
                return;
            }
        } else if(this.surveyId){
            surveyId = this.surveyId;
        } else {
            this.checkButtons();
            return;
        }

        const surveyQuery: AssetQuery = {
            ids: [surveyId],
            types: ["SurveyAsset"]
        };
        manager.rest.api.AssetResource.queryAssets(surveyQuery).then((response) => {
            if (response && response.data) {
                this.survey = response.data[0];
                this.requestUpdate();
            }
        }).catch((reason) => {
            console.error("Error: " + reason);
        });

        if(localStorage.getItem('survey'+surveyId) && this.saveanswers){
            this.hasSubmission = true;
            return;
        }

        const questionQuery: AssetQuery = {
            types: ["SurveyQuestionAsset"]
        };

        manager.rest.api.AssetResource.queryAssets(questionQuery).then((response) => {
            if (response && response.data) {
                const questions = response.data;
                questions.sort((a, b) => a.attributes && b.attributes ? a.attributes.order.value - b.attributes.order.value : 0);
                this.questions = [...questions];

                if(this.survey){
                    this.questions = filter(this.questions, ['parentId', this.survey.id]);
                }
                    
                this.checkButtons();
                this.requestUpdate();

            }
        }).catch((reason) => {
            console.error("Error: " + reason);
        });
    }

}
