import {html, LitElement, property, customElement, PropertyValues, query} from "lit-element";
import {optionColorVar, selectStyle} from "./style";

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
    public readonly?: boolean;

    @property({type: Boolean})
    public required?: boolean;

    @property({type: Boolean})
    public autoSize?: boolean = true;

    @property({type: Boolean, attribute: true})
    public showEmptyOption?: boolean = true;

    @property({type: Array})
    public options!: string[] | [string, string][];

    @query("#or-select")
    protected _select!: HTMLSelectElement;

    @query("#width_tmp_select")
    protected _tmpSelect!: HTMLSelectElement;

    protected _optColor?: string;

    connectedCallback(): void {
        super.connectedCallback();

        // Take color from host as select will override color inheritance otherwise
        const style = window.getComputedStyle(this);

        if(style.getPropertyValue("--internal-or-select-option-text-color").indexOf("inherit") >= 0) {
            this._optColor = style.getPropertyValue("color");
        }
    }

    protected onChange() {
        if (this._select.value == this.value) {
            return;
        }

        if (this.autoSize) {
            this._resize();
        }
        const previousValue = this.value;
        this.value = this._select.value && this._select.value.length > 0 ? this._select.value : undefined;
        this.dispatchEvent(new OrSelectChangedEvent(this.value, previousValue));
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);

        if (_changedProperties.has("options") || _changedProperties.has("value")) {
            let val = this.value;

            if (this.options.length === 1 || (!this.showEmptyOption && !val)) {
                const opt = this.options[0];
                const firstValue = Array.isArray(opt) ? opt[0] : opt;
                if (this.value !== firstValue) {
                    val = firstValue;
                }
            }

            let index: number;
            if (this.options.length === 1 || (!this.showEmptyOption && !val)) {
                index = 0;
            } else {
                index = this.options.findIndex((opt: string | [string, string]) => {
                    const value = Array.isArray(opt) ? opt[0] : opt;
                    return value === val;
                });

                if(this.showEmptyOption) {
                    index = index + 1;
                }
            }

            const indexChanged = this._select.selectedIndex != index;
            this._select.selectedIndex = index;

            if (val !== this.value) {
                this.onChange();
            } else {
                if (this.autoSize) {
                    this._resize();
                }
            }
        }
    }

    protected render() {
        const isSingular = this.options.length === 1;

        return html`
               <div class="mdc-select">
                      <select id="or-select" ?required="${this.required}" @change="${this.onChange}" ?disabled="${this.readonly || isSingular}">
                        ${this.showEmptyOption && !isSingular ? html`<option value=""></option>` : ``}
                        ${this.options.length > 0 && Array.isArray(this.options[0]) ?
                            (this.options as [string, string][]).map((option: [string, string]) => {
                                return html`<option style="color: ${this._optColor || optionColorVar};" value="${option[0]}">${option[1]}</option>`;                            
                            }) :
                            (this.options as string[]).map((option: string) => {
                                return html`<option style="color: ${this._optColor || optionColorVar};" value="${option}">${option}</option>`;
                            })
                        }
                      </select>
                      <svg viewBox="0 0 24 24">
                        <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z" />
                      </svg>
                      <select id="width_tmp_select">
                        <option id="width_tmp_option"></option>
                      </select>
                </div>
        `;
    }

    protected _resize() {
        let width = 20;
        if (this._select.selectedOptions.length > 0) {
            (this._tmpSelect.options.item(0)! as HTMLOptionElement).innerHTML = this._select.selectedOptions.item(0)!.text;
            width = this._tmpSelect.offsetWidth;
        }
        this._select.style.setProperty("width",  width + "px");
    }
}
