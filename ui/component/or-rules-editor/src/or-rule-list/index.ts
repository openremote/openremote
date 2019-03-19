import {html, LitElement, property, PropertyValues} from 'lit-element';
import {Rule} from '@openremote/model';

class OrRulesList extends LitElement {
    @property({type: Array})
    rules: Rule[] = [];

    protected render() {

        return html`
            <style>
                        .d-flex {
                            display: -webkit-box;
                            display: -moz-box;
                            display: -ms-flexbox;
                            display: -webkit-flex;
                            display: flex;
                        }
                    
                        .flex {
                            -webkit-box-flex: 1;
                            -moz-box-flex: 1;
                            -webkit-flex: 1;
                            -ms-flex: 1;
                            flex: 1;
                        }
                        
                        .list-container {
                        }
                        
                        .list-item {
                            text-decoration: none;
                            height: 24px;
                            padding: 20px 0;
                            border-bottom: 1px solid var(--app-lightgrey-color);
                            border-left: 5px solid transparent;
                            color: var(--app-grey-color);
                            cursor: pointer;
                        }
                        
                        .list-item:hover {
                            border-left-color: var(--app-primary-color);
                            background-color: var(--app-lightgrey-color);
                        }
                        
                        .list-item > span {
                            font-size: 18px;
                        }
                        
                        .rule-status {
                            width: 8px;
                            height: 8px;
                            border-radius: 8px;
                            margin: 8px 10px;
                        }
                        
                        .bg-green {
                            background-color: green;
                        }
            </style>
            <div class="list-container">
                ${this.rules && this.rules.map((rule: Rule, index:number) => {
                    return html`
                        <a class="d-flex list-item" @click="${()=> this.setActiveRule(this.rules[index])}">
                            <span class="rule-status ${!rule.name ? '' : 'bg-green'}"></span>
                            <div class="flex">
                                <span>${rule.name}</span>
                            </div>
                        </a>
                    `
                })}
            </div>
        `;
    }

    setActiveRule (rule:Rule) {
        this.requestUpdate();
        let event = new CustomEvent('rules:set-active-rule', {
            detail: {rule: rule},
            bubbles: true,
            composed: true
        });
        this.dispatchEvent(event);
    }

    constructor() {
        super();
    }


}

window.customElements.define('or-rule-list', OrRulesList);
