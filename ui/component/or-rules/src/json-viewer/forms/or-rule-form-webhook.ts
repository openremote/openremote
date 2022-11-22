import {RuleActionWebhook, Webhook, WebhookAuthMethod, WebhookHeader, WebhookMethod} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {when} from 'lit/directives/when.js';


//language=css
const styling = css`
    .divider {
        border-bottom: 1px solid rgba(0, 0, 0, 12%);
    }
`

@customElement("or-rule-form-webhook")
export class OrRuleFormWebhook extends LitElement {

    @property({type: Object})
    protected webhook!: Webhook;

    private methodOptions: WebhookAuthMethod[] = [WebhookAuthMethod.NONE, WebhookAuthMethod.HTTP_BASIC, WebhookAuthMethod.API_KEY, WebhookAuthMethod.OAUTH2]

    static get styles() {
        return [styling];
    }

    /* --------------------------- */

    shouldUpdate(changedProperties: Map<string, any>) {
        console.warn(changedProperties)
        if (changedProperties.has('webhook')) {
            if (this.webhook.headers == undefined) {
                this.webhook.headers = [];
            }
            if (this.webhook.method == undefined) {
                this.webhook.authMethod = this.methodOptions[0];
            }
            if (this.webhook.authMethod != this.methodOptions[0]) {
                if (this.webhook.authDetails == undefined) {
                    this.webhook.authDetails = {}
                }
            }
        }
        return super.shouldUpdate(changedProperties);
    }

    setWebhookUrl(value: any) {
        this.webhook.url = value;
    }

    render() {
        if (!this.webhook) {
            return html`${i18next.t('errorOccurred')}`
        }
        console.warn("Rendering or-rule-form-webhook!");
        return html`
            <form style="display: flex; flex-direction: column; gap: 20px; min-width: 420px;">
                <div style="display: flex; flex-direction: row; align-items: center; gap: 5px;">
                    <or-mwc-input style="flex: 0;" type="${InputType.SELECT}" .value="${this.webhook.method}"
                                  .options="${['GET', 'POST', 'PUT', 'DELETE']}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.webhook.method = ev.detail.value;
                                      this.requestUpdate("webhook");
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input style="flex: 1;" type="${InputType.URL}" required label="Http URL"
                                  .value="${this.webhook.url}" helperPersistent
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setWebhookUrl(e.detail.value)}"></or-mwc-input>
                </div>
                <div style="display: flex; flex-direction: column; gap: 5px;">
                    <span>Headers</span>
                    ${this.webhook.headers?.map((header: WebhookHeader, index) => html`
                        <div style="display: flex; gap: 5px;">
                            <or-mwc-input type="${InputType.TEXT}" required label="Header"
                                          value="${header.header}" style="flex: 1;"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => header.header = ev.detail.value}"></or-mwc-input>
                            <or-mwc-input type="${InputType.TEXT}" required label="Value"
                                          value="${header.value}" style="flex: 1;"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => header.value = ev.detail.value}"></or-mwc-input>
                            <or-mwc-input type="${InputType.BUTTON}" icon="delete"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                              this.webhook.headers?.splice(index, 1);
                                              this.requestUpdate('webhook');
                                          }}"></or-mwc-input>
                        </div>
                    `)}
                    <or-mwc-input type="${InputType.BUTTON}" icon="plus" label="Add Request Header"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.webhook.headers?.push({header: '', value: ''});
                                      this.requestUpdate('webhook');
                                  }}"></or-mwc-input>
                </div>
                <div class="divider" style="margin-top: 24px;"></div>
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <or-mwc-input type="${InputType.SWITCH}" fullwidth label="Requires Authorization"
                                  .value="${this.webhook.authMethod != WebhookAuthMethod.NONE}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.webhook.authMethod = (ev.detail.value as boolean) ? WebhookAuthMethod.HTTP_BASIC : WebhookAuthMethod.NONE;
                                      this.requestUpdate('webhook');
                                  }}"></or-mwc-input>
                    ${when(this.webhook.authMethod != WebhookAuthMethod.NONE, () => {
                        return html`
                            <or-mwc-input type="${InputType.SELECT}" label="Method"
                                          .value="${this.webhook.authMethod}"
                                          .options="${this.methodOptions.slice(1)}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                              this.webhook.authMethod = ev.detail.value;
                                              this.requestUpdate('webhook')
                                          }}"></or-mwc-input>
                            ${this.getAuthSettingsTemplate(this.webhook.authMethod)}
                            <div style="display: flex; align-items: center; justify-content: space-between;">
                                <span>Authorize using</span>
                                <or-mwc-input type="${InputType.SELECT}" .value="${'Header'}" style="width: 200px;"
                                              .options="${['Header', 'Query Parameter']}"
                            </div>
                        `
                    })}
                </div>
                ${when(this.webhook.authMethod != undefined, () => html`
                    <div class="divider" style="margin-top: 24px;"></div>
                `)}
            </form>
        `
    }

    getAuthSettingsTemplate(authMethod: WebhookAuthMethod | undefined): TemplateResult | undefined {
        switch (authMethod) {
            case WebhookAuthMethod.HTTP_BASIC:
                return html`
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <or-mwc-input type="${InputType.TEXT}" label="Username"
                                      .value="${this.webhook.authDetails?.username}"></or-mwc-input>
                        <or-mwc-input type="${InputType.TEXT}" label="Password"
                                      .value="${this.webhook.authDetails?.password}"></or-mwc-input>
                    </div>
                `
            case WebhookAuthMethod.API_KEY:
                return html`
                    <or-mwc-input type="${InputType.TEXT}" label="API Key"
                                  .value="${this.webhook.authDetails?.apiKey}"></or-mwc-input>
                `;
            case WebhookAuthMethod.OAUTH2:
                return html`
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <div style="display: flex; flex-direction: row; align-items: center; gap: 5px;">
                            <or-mwc-input style="flex: 0;" type="${InputType.SELECT}" required
                                          .value="${this.webhook.authDetails?.method}"
                                          .options="${['GET', 'POST', 'PUT']}"></or-mwc-input>
                            <or-mwc-input style="flex: 1;" type="${InputType.URL}" required label="Http URL"
                                          .value="${this.webhook.authDetails?.url}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setWebhookUrl(e.detail.value)}"></or-mwc-input>
                        </div>
                        <or-mwc-input type="${InputType.TEXT}" label="Client ID"
                                      .value="${this.webhook.authDetails?.clientId}"></or-mwc-input>
                        <or-mwc-input type="${InputType.TEXT}" label="Client Secret"
                                      .value="${this.webhook.authDetails?.clientSecret}"></or-mwc-input>
                    </div>
                `
            case WebhookAuthMethod.NONE:
                return undefined;
            default:
                return html`${i18next.t('errorOccurred')}`
        }
    }
}
