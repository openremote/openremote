import {html, LitElement, property, customElement, PropertyValues, query} from "lit-element";
import {selectStyle} from "./style";

export class OrSelectChangedEvent extends CustomEvent<OrSelectChangedEventDetail> {

    public static readonly NAME = "or-select-changed";

    constructor(value?: string, previousValue?: string) {
        super(OrSelectChangedEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue
            },
            bubbles: true,
            composed: true
        });
    }
}

export interface OrSelectChangedEventDetail {
    value?: string;
    previousValue?: string;
}

declare global {
    export interface HTMLElementEventMap {
        [OrSelectChangedEvent.NAME]: OrSelectChangedEvent;
    }
}

@customElement("or-select")
export class OrSelect extends LitElement {

    static get styles() {
        return [
            selectStyle
        ];
    }

    @property({type: String})
    public value?: string;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public required: boolean = true;

    @property({type: Boolean})
    public addEmpty: boolean = true;

    @property({type: Array})
    public options: string[] | [string, string][] = [];

    @query("#or-select")
    protected _select!: HTMLSelectElement;

    protected onChange() {
        const previousValue = this.value;
        this.value = this._select.value && this._select.value.length > 0 ? this._select.value : undefined;
        this.dispatchEvent(new OrSelectChangedEvent(this.value, previousValue));
    }

    protected firstUpdated(_changedProperties: PropertyValues) {
        super.firstUpdated(_changedProperties);
        if (this.options.length === 1) {
            const opt = this.options[0];
            const val = Array.isArray(opt) ? opt[0] : opt;
            if (this.value !== val) {
                window.setTimeout(() => {
                    if (this._select) {
                        this._select.value = val;
                    }
                });
            }
        }
    }

    protected render() {
        if (this._select) {
            // If we don't do this then selected attribute on option is not respected when a select is updated
            if (!this.value) {
                this._select.selectedIndex = this.addEmpty ? 0 : -1;
            }
        }
        const isSingular = this.options.length === 1;

        return html`
               <div class="mdc-select">
                      <select id="or-select" ?required="${this.required}" @change="${this.onChange}" ?disabled="${this.readonly || isSingular}">
                        ${this.addEmpty && !isSingular ? html`<option value="" ?selected="${!this.value}"></option>` : ``};
                        ${this.options.length > 0 && Array.isArray(this.options[0]) ?
                            (this.options as [string, string][]).map((option: [string, string]) => {
                                return html`<option ?selected="${this.value === option[0]}" value="${option[0]}">${option[1]}</option>`;                            
                            }) :
                            (this.options as string[]).map((option: string) => {
                                return html`<option ?selected="${this.value === option}" value="${option}">${option}</option>`;
                            })
                        }
                      </select>
                </div>
        `;
    }
}
