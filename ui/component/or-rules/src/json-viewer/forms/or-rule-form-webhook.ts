import {RuleActionWebhook, RuleActionWebhookAuth, RuleActionWebhookHeader} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {when} from 'lit/directives/when.js';

interface RuleActionWebhookOptions extends RuleActionWebhook {
    authDetails?: {
        username?: string;
        password?: string;
        apiKey?: string;
        method?: 'GET' | 'POST' | 'PUT';
        url?: string;
        clientId?: string;
        clientSecret?: string;
    }
}

//language=css
const styling = css`
    .divider {
        border-bottom: 1px solid rgba(0, 0, 0, 12%);
    }
`

@customElement("or-rule-form-webhook")
export class OrRuleFormWebhook extends LitElement {

    @property({type: Object})
    protected action!: RuleActionWebhookOptions;

    private methodOptions: string[] = ["None", RuleActionWebhookAuth.HTTP_BASIC, RuleActionWebhookAuth.API_KEY, RuleActionWebhookAuth.OAUTH2]

    static get styles() {
        return [styling];
    }

    /* --------------------------- */

    shouldUpdate(changedProperties: Map<string, any>) {
        console.warn(changedProperties)
        if (changedProperties.has('action')) {
            if (this.action.headers == undefined) {
                this.action.headers = [];
            }
            if (this.action.method == undefined) {
                this.action.method = this.methodOptions[0];
            }
            if (this.action.method != this.methodOptions[0]) {
                if (this.action.authDetails == undefined) {
                    this.action.authDetails = {
                        method: "POST"
                    }
                }
            }
        }
        return super.shouldUpdate(changedProperties);
    }

    setActionUrl(value: any) {
        this.action.url = value;
    }

    render() {
        if (!this.action) {
            return html`${i18next.t('errorOccurred')}`
        }
        console.warn("Rendering or-rule-form-webhook!");
        return html`
            <form style="display: flex; flex-direction: column; gap: 20px; min-width: 420px;">
                <div style="display: flex; flex-direction: row; align-items: center; gap: 5px;">
                    <or-mwc-input style="flex: 0;" type="${InputType.SELECT}" .value="${this.action.method}"
                                  .options="${['GET', 'POST', 'PUT', 'DELETE']}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.action.method = ev.detail.value;
                                      this.requestUpdate("action");
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input style="flex: 1;" type="${InputType.URL}" required label="Http URL"
                                  .value="${this.action.url}" helperPersistent
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionUrl(e.detail.value)}"></or-mwc-input>
                </div>
                <div style="display: flex; flex-direction: column; gap: 5px;">
                    <span>Headers</span>
                    ${this.action.headers?.map((header: RuleActionWebhookHeader, index) => html`
                        <div style="display: flex; gap: 5px;">
                            <or-mwc-input type="${InputType.TEXT}" required label="Header"
                                          value="${header.header}" style="flex: 1;"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => header.header = ev.detail.value}"></or-mwc-input>
                            <or-mwc-input type="${InputType.TEXT}" required label="Value"
                                          value="${header.value}" style="flex: 1;"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => header.value = ev.detail.value}"></or-mwc-input>
                            <or-mwc-input type="${InputType.BUTTON}" icon="delete"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                              this.action.headers?.splice(index, 1);
                                              this.requestUpdate('action');
                                          }}"></or-mwc-input>
                        </div>
                    `)}
                    <or-mwc-input type="${InputType.BUTTON}" icon="plus" label="Add Request Header"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.action.headers?.push({header: '', value: ''});
                                      this.requestUpdate('action');
                                  }}"></or-mwc-input>
                </div>
                <div class="divider" style="margin-top: 24px;"></div>
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <or-mwc-input type="${InputType.SWITCH}" fullwidth label="Requires Authorization"
                                  .value="${this.action.authMethod != undefined}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.action.authMethod = (ev.detail.value as boolean) ? RuleActionWebhookAuth.HTTP_BASIC : undefined;
                                      this.requestUpdate('action');
                                  }}"></or-mwc-input>
                    ${when(this.action.authMethod != undefined, () => {
                        return html`
                            <or-mwc-input type="${InputType.SELECT}" label="Method"
                                          .value="${this.action.authMethod}"
                                          .options="${this.methodOptions.slice(1)}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                              this.action.authMethod = ev.detail.value;
                                              this.requestUpdate('action')
                                          }}"></or-mwc-input>
                            ${this.getAuthSettingsTemplate(this.action.authMethod)}
                            <div style="display: flex; align-items: center; justify-content: space-between;">
                                <span>Authorize using</span>
                                <or-mwc-input type="${InputType.SELECT}" .value="${'Header'}" style="width: 200px;"
                                              .options="${['Header', 'Query Parameter']}"
                            </div>
                        `
                    })}
                </div>
                ${when(this.action.authMethod != undefined, () => html`
                    <div class="divider" style="margin-top: 24px;"></div>
                `)}
                ${when((this.action.method != 'GET' && this.action.method != 'DELETE'), () => html`
                    <div style="display: flex; flex-direction: column; gap: 5px;">
                        <span>JSON Body</span>
                        <or-mwc-input type="${InputType.JSON}" label="JSON Body"></or-mwc-input>
                    </div>
                `)}
            </form>
        `
    }

    getAuthSettingsTemplate(authMethod: RuleActionWebhookAuth | undefined): TemplateResult {
        switch (authMethod) {
            case RuleActionWebhookAuth.HTTP_BASIC:
                return html`
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <or-mwc-input type="${InputType.TEXT}" label="Username"
                                      .value="${this.action.authDetails?.username}"></or-mwc-input>
                        <or-mwc-input type="${InputType.TEXT}" label="Password"
                                      .value="${this.action.authDetails?.password}"></or-mwc-input>
                    </div>
                `
            case RuleActionWebhookAuth.API_KEY:
                return html`
                    <or-mwc-input type="${InputType.TEXT}" label="API Key"
                                  .value="${this.action.authDetails?.apiKey}"></or-mwc-input>
                `;
            case RuleActionWebhookAuth.OAUTH2:
                return html`
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <div style="display: flex; flex-direction: row; align-items: center; gap: 5px;">
                            <or-mwc-input style="flex: 0;" type="${InputType.SELECT}" required
                                          .value="${this.action.authDetails?.method}"
                                          .options="${['GET', 'POST', 'PUT']}"></or-mwc-input>
                            <or-mwc-input style="flex: 1;" type="${InputType.URL}" required label="Http URL"
                                          .value="${this.action.authDetails?.url}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionUrl(e.detail.value)}"></or-mwc-input>
                        </div>
                        <or-mwc-input type="${InputType.TEXT}" label="Client ID"
                                      .value="${this.action.authDetails?.clientId}"></or-mwc-input>
                        <or-mwc-input type="${InputType.TEXT}" label="Client Secret"
                                      .value="${this.action.authDetails?.clientSecret}"></or-mwc-input>
                    </div>
                `
            default:
                return html`${i18next.t('errorOccurred')}`
        }
    }
}
