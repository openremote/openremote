import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {css, CSSResult, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property} from "lit/decorators.js";
import {ifDefined} from "lit/directives/if-defined.js";
import {DefaultColor5} from "@openremote/core";
import {style} from "../style";

export class ThresholdChangeEvent extends CustomEvent<[number, string][]> {

    public static readonly NAME = "threshold-change";

    constructor(thresholds: [number, string][]) {
        super(ThresholdChangeEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: thresholds
        });
    }
}

export class TextColorsChangeEvent extends CustomEvent<[string, string][]> {

    public static readonly NAME = "text-colors-change";

    constructor(textColors: [string, string][]) {
        super(TextColorsChangeEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: textColors
        });
    }
}

export class BoolColorsChangeEvent extends CustomEvent<{ type: string, false: string, true: string }> {

    public static readonly NAME = "bool-colors-change";

    constructor(boolColors: { type: string, false: string, true: string }) {
        super(BoolColorsChangeEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: boolColors
        });
    }
}

const styling = css`

  #thresholds-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .threshold-list-item {
    display: flex;
    flex-direction: row;
    align-items: center;
  }

  .threshold-list-item-colour {
    height: 100%;
    padding: 0 4px 0 0;
  }

  .threshold-list-item:hover .button-clear {
    visibility: visible;
  }

  .button-clear {
    background: none;
    visibility: hidden;
    color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
    --or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
    display: inline-block;
    border: none;
    padding: 0;
    cursor: pointer;
  }

  .button-clear:hover {
    --or-icon-fill: var(--or-app-color4);
  }

`;

@customElement("thresholds-panel")
export class ThresholdsPanel extends LitElement {

    @property()
    protected thresholds: [number, string][] = [];

    @property()
    protected textColors: [string, string][] = [];

    @property()
    protected boolColors!: { type: string, false: string, true: string };

    @property()
    protected readonly min?: number;

    @property()
    protected readonly max?: number;

    @property()
    protected readonly valueType!: string;

    static get styles(): CSSResult[] {
        return [styling, style];
    }

    protected updated(changedProps: PropertyValues) {
        if (changedProps.has('thresholds') && this.thresholds) {
            this.dispatchEvent(new ThresholdChangeEvent(this.thresholds));
        }
        if (changedProps.has('textColors') && this.textColors) {
            this.dispatchEvent(new TextColorsChangeEvent(this.textColors));
        }
        if (changedProps.has('boolColors') && this.boolColors) {
            this.dispatchEvent(new BoolColorsChangeEvent(this.boolColors));
        }
    }

    protected render(): TemplateResult {
        return html`
            <div id="thresholds-list" class="expanded-panel">

                <!-- Thresholds by number -->
                ${(this.valueType === 'number' || this.valueType === 'positiveInteger'
                        || this.valueType === 'positiveNumber' || this.valueType === 'negativeInteger'
                        || this.valueType === 'negativeNumber') ? html`
                    ${(this.thresholds as [number, string][]).sort((x, y) => (x[0] < y[0]) ? -1 : 1).map((threshold, index) => {
                        return html`
                            <div class="threshold-list-item">
                                <div class="threshold-list-item-colour">
                                    <or-mwc-input type="${InputType.COLOUR}" value="${threshold[1]}"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                      this.thresholds[index][1] = event.detail.value;
                                                      this.requestUpdate('thresholds');
                                                  }}"
                                    ></or-mwc-input>
                                </div>
                                <or-mwc-input type="${InputType.NUMBER}" comfortable .value="${threshold[0]}"
                                              ?disabled="${index === 0 && this.max}"
                                              .min="${ifDefined(this.min)}" .max="${ifDefined(this.max)}"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  if ((!this.min || event.detail.value >= this.min) && (!this.max || event.detail.value <= this.max)) {
                                                      this.thresholds[index][0] = event.detail.value;
                                                      this.requestUpdate('thresholds');
                                                  }
                                              }}"
                                ></or-mwc-input>
                                ${index === 0 ? html`
                                    <button class="button-clear"
                                            style="margin-left: 8px;">
                                        <or-icon icon="lock" style="--or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});"></or-icon>
                                    </button>
                                ` : html`
                                    <button class="button-clear"
                                            style="margin-left: 8px;"
                                            @click="${() => {
                                                this.removeThreshold(threshold);
                                            }}">
                                        <or-icon icon="close-circle"></or-icon>
                                    </button>
                                `}
                            </div>
                        `
                    })}
                    <or-mwc-input .type="${InputType.BUTTON}" label="threshold" icon="plus"
                                  @or-mwc-input-changed="${() => this.addNewThreshold()}">
                    </or-mwc-input>
                ` : null}

                <!-- Thresholds by boolean -->
                ${(this.valueType === 'boolean') ? html`
                    <div class="threshold-list-item">
                        <div class="threshold-list-item-colour">
                            <or-mwc-input type="${InputType.COLOUR}" value="${this.boolColors.true}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              this.boolColors.true = event.detail.value;
                                              this.requestUpdate('boolColors');
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <or-mwc-input type="${InputType.TEXT}" comfortable .value="${'True'}" .readonly="${true}"
                        ></or-mwc-input>
                    </div>
                    <div class="threshold-list-item">
                        <div class="threshold-list-item-colour">
                            <or-mwc-input type="${InputType.COLOUR}" value="${this.boolColors.false}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              this.boolColors.false = event.detail.value;
                                              this.requestUpdate('boolColors');
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <or-mwc-input type="${InputType.TEXT}" comfortable .value="${'False'}" .readonly="${true}"
                        ></or-mwc-input>
                    </div>
                ` : null}

                <!-- Thresholds by string -->
                ${(this.valueType === 'text' && this.textColors) ? html`
                    ${(this.textColors as [string, string][]).map((threshold, index) => {
                        return html`
                            <div class="threshold-list-item">
                                <div class="threshold-list-item-colour">
                                    <or-mwc-input type="${InputType.COLOUR}" value="${threshold[1]}"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                      this.textColors[index][1] = event.detail.value;
                                                      this.requestUpdate('textColors');
                                                  }}"
                                    ></or-mwc-input>
                                </div>
                                <or-mwc-input type="${InputType.TEXT}" comfortable .value="${threshold[0]}"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  this.textColors[index][0] = event.detail.value;
                                                  this.requestUpdate('textColors');
                                              }}"

                                ></or-mwc-input>
                                ${index === 0 ? html`
                                    <button class="button-clear"
                                            style="margin-left: 8px;">
                                        <or-icon icon="lock" style="--or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});"></or-icon>
                                    </button>
                                ` : html`
                                    <button class="button-clear"
                                            style="margin-left: 8px;"
                                            @click="${() => {
                                                this.removeThreshold(threshold);
                                            }}">
                                        <or-icon icon="close-circle"></or-icon>
                                    </button>
                                `}
                            </div>
                        `
                    })}
                    <or-mwc-input .type="${InputType.BUTTON}" label="threshold" icon="plus"
                                  @or-mwc-input-changed="${() => this.addNewThreshold()}">
                    </or-mwc-input>
                ` : null}
            </div>
        `
    }

    protected removeThreshold(threshold: [any, string]) {
        switch (typeof threshold[0]) {
            case "number":
                this.thresholds = (this.thresholds as [number, string][]).filter((x) => x !== threshold);
                break;
            default:
                this.textColors = (this.textColors as [string, string][]).filter((x) => x !== threshold);
                break;
        }
    }

    protected addThreshold(threshold: [any, string]) {
        switch (typeof threshold[0]) {
            case "number":
                (this.thresholds as [number, string][]).push(threshold as [number, string]);
                this.requestUpdate('thresholds');
                break;
            default:
                (this.textColors as [string, string][]).push(threshold as [string, string]);
                this.requestUpdate('textColors');
                break;
        }
    }

    protected addNewThreshold() {
        if (this.valueType === 'text') {
            this.addThreshold(["new", "#000000"]);
        } else {
            const suggestedValue = (this.thresholds[this.thresholds.length - 1][0] + 10);
            this.addThreshold([(!this.max || suggestedValue <= this.max ? suggestedValue : this.max), "#000000"]);
        }
        this.updateComplete.then(() => {
            const elem = this.shadowRoot?.getElementById('thresholds-list') as HTMLElement;
            const inputField = Array.from(elem.children)[elem.children.length - 2] as HTMLElement;
            (inputField.children[1] as HTMLElement).setAttribute('focused', 'true');
        })
    }
}
