import {
    HTTPMethod,
    OAuthGrant,
    OAuthClientCredentialsGrant,
    Webhook,
    OAuthPasswordGrant
} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from 'lit/directives/when.js';
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";


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

    @state()
    protected loading: boolean = false;

    private httpMethodOptions: HTTPMethod[] = [HTTPMethod.GET, HTTPMethod.POST, HTTPMethod.PUT, HTTPMethod.DELETE];
    private authMethodOptions: Map<string, string> = new Map<string, string>([
        ["username_password", (i18next.t('username') + " & " + i18next.t('password'))],
        ["client_credentials", "oauth Client Credentials Grant"],
        ["password", "oauth Password Grant"]
    ]);

    static get styles() {
        return [styling];
    }

    /* --------------------------- */

    // Lifecycle methods

    shouldUpdate(changedProperties: Map<string, any>) {
        if (changedProperties.has('webhook')) {
            if (this.webhook.headers == undefined) {
                this.webhook.headers = {};
            }
        }
        return super.shouldUpdate(changedProperties);
    }

    /* --------------- */

    // Util

    getAuthMethod(webhook: Webhook): string | undefined {
        if (webhook.oAuthGrant != undefined) {
            return this.authMethodOptions.get(webhook.oAuthGrant.grant_type);
        } else {
            return this.authMethodOptions.get("username_password");
        }
    }

    getOAuthGrant(authMethodKey: string): OAuthGrant | undefined {
        if (authMethodKey == 'client_credentials' || authMethodKey == 'password') {
            return {grant_type: authMethodKey} as OAuthGrant;
        } else {
            return undefined;
        }
    }

    reloadHeaders() {
        this.loading = true;
        this.updateComplete.then(() => this.loading = false);
        this.notifyWebhookUpdate(false)
    }

    notifyWebhookUpdate(requestUpdate: boolean = true) {
        if (requestUpdate) {
            this.requestUpdate("webhook");
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }

    /* ---------------- */

    // Template rendering
    render() {
        return when(!this.webhook, () => html`
            ${i18next.t('errorOccurred')}
        `, () => html`
            <form style="display: flex; flex-direction: column; min-width: 520px;">
                <!-- HTTP Method & URL -->
                <div style="display: flex; flex-direction: row; align-items: center; gap: 5px; margin-bottom: 28px;">
                    <or-mwc-input style="flex: 0;" type="${InputType.SELECT}" .value="${this.webhook.httpMethod}"
                                  .options="${this.httpMethodOptions}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.webhook.httpMethod = ev.detail.value;
                                      this.notifyWebhookUpdate();
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input style="flex: 1;" type="${InputType.URL}" required label="${i18next.t('webUrl')}"
                                  .value="${this.webhook.url}" helperPersistent
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      this.webhook.url = e.detail.value;
                                      this.notifyWebhookUpdate();
                                  }}"></or-mwc-input>
                </div>
                <!-- Headers -->
                <div style="display: flex; flex-direction: column; gap: 5px; margin-bottom: 28px;">
                    <span>Headers</span>
                    ${when(this.loading, () => html`
                        ${this.getHeadersTemplate(this.webhook.headers!, true)}
                    `, () => html`
                        ${this.getHeadersTemplate(this.webhook.headers!, false)}
                    `)}
                    <or-mwc-input type="${InputType.BUTTON}" icon="plus" label="addRequestHeader"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      if ((this.webhook.headers ? this.webhook.headers[''] : undefined) != undefined) {
                                          this.webhook.headers![''].push('');
                                      } else {
                                          this.webhook.headers![''] = [''];
                                      }
                                      this.reloadHeaders();
                                  }}"></or-mwc-input>
                </div>
                <!-- Authorization -->
                <div style="display: flex; flex-direction: column; gap: 10px; margin-bottom: ${this.webhook.oAuthGrant || this.webhook.usernamePassword ? '28px' : '0'};">
                    <or-mwc-input type="${InputType.SWITCH}" fullwidth label="${i18next.t('requiresAuthorization')}"
                                  .value="${this.webhook.oAuthGrant || this.webhook.usernamePassword}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.webhook.usernamePassword = ev.detail.value ? {
                                          username: 'admin',
                                          password: 'secret'
                                      } : undefined;
                                      this.notifyWebhookUpdate();
                                  }}"></or-mwc-input>
                    ${when(this.webhook.oAuthGrant || this.webhook.usernamePassword, () => {
                        const values: string[] = Array.from(this.authMethodOptions.values());
                        return html`
                            <or-mwc-input type="${InputType.SELECT}" label="${i18next.t('method')}"
                                          .value="${this.getAuthMethod(this.webhook)}"
                                          .options="${values}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                              const entry = [...this.authMethodOptions.entries()].find((entry) => entry[1] == ev.detail.value);
                                              this.webhook.oAuthGrant = this.getOAuthGrant(entry![0]);
                                              this.notifyWebhookUpdate();
                                          }}"></or-mwc-input>
                            ${this.getAuthSettingsTemplate(this.webhook)}
                        `
                    })}
                </div>
                <!-- Payload -->
                <div style="display: flex; flex-direction: column; gap: 5px;">
                    ${when(this.webhook.httpMethod != HTTPMethod.GET && this.webhook.httpMethod != HTTPMethod.DELETE, () => html`
                        <or-mwc-input type="${InputType.SWITCH}" fullwidth label="${i18next.t('includeBodyInRequest')}"
                                      .value="${this.webhook.payload != undefined}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                          this.webhook.payload = ev.detail.value ? JSON.stringify({
                                              rule: "%RULESET_NAME%",
                                              assets: "%TRIGGER_ASSETS%"
                                          }, null, 4) : undefined;
                                          this.notifyWebhookUpdate();
                                      }}"
                        ></or-mwc-input>
                        ${when(this.webhook.payload != undefined, () => {
                            return html`
                                <or-mwc-input type="${InputType.TEXTAREA}" .value="${this.webhook.payload}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                                  this.webhook.payload = ev.detail.value;
                                                  this.notifyWebhookUpdate();
                                              }}"></or-mwc-input>
                            `
                        })}
                    `)}
                </div>
            </form>
        `)
    }

    getHeadersTemplate(headers: { [p: string]: string[] }, loading: boolean) {
        return Object.keys(this.webhook.headers!)
            .sort((a, b) => -(a.localeCompare(b)))
            .map((key, keyIndex, keys) => {
                const values: string[] = this.webhook.headers![key];
                return values.map((value, valueIndex) => html`
                    <div style="display: flex; gap: 5px;">
                        <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('header')}" value="${key}"
                                      style="flex: 1;" .disabled="${loading}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                          values.length > 0 ? values.splice(valueIndex, 1) : delete this.webhook.headers![key];
                                          const newValues = this.webhook.headers![ev.detail.value];
                                          if (newValues && newValues.length > 0) {
                                              newValues.push(value);
                                          } else {
                                              this.webhook.headers![ev.detail.value] = [value]
                                          }
                                          this.reloadHeaders();
                                      }}"
                        ></or-mwc-input>
                        <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('value')}" value="${value}"
                                      style="flex: 1;" .disabled="${loading}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                          this.webhook.headers![key][valueIndex] = ev.detail.value;
                                          this.notifyWebhookUpdate();
                                      }}"
                        ></or-mwc-input>
                        <or-mwc-input type="${InputType.BUTTON}" icon="delete" .disabled="${loading}"
                                      @or-mwc-input-changed="${() => {
                                          values.splice(valueIndex, 1);
                                          this.reloadHeaders();
                                      }}"></or-mwc-input>
                    </div>
                `)
            })
    }

    getAuthSettingsTemplate(webhook: Webhook): TemplateResult | undefined {
        const authGrant = webhook.oAuthGrant;
        if (authGrant == undefined) {
            return html`
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('username')}"
                                  .value="${webhook.usernamePassword?.username}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      if (!this.webhook.usernamePassword) {
                                          this.webhook.usernamePassword = {};
                                      }
                                      this.webhook.usernamePassword.username = ev.detail.value;
                                      this.notifyWebhookUpdate();
                                  }}"></or-mwc-input>
                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('password')}"
                                  .value="${this.webhook.usernamePassword?.password}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      if (!this.webhook.usernamePassword) {
                                          this.webhook.usernamePassword = {};
                                      }
                                      this.webhook.usernamePassword.password = ev.detail.value;
                                      this.notifyWebhookUpdate();
                                  }}"></or-mwc-input>
                </div>
            `
        } else {
            return html`
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <div style="display: flex; flex-direction: row; align-items: center; gap: 5px;">
                        <or-mwc-input style="flex: 1;" type="${InputType.URL}" required
                                      label="${i18next.t('tokenUrl')}" .value="${authGrant.tokenEndpointUri}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                          authGrant.tokenEndpointUri = ev.detail.value;
                                          this.notifyWebhookUpdate();
                                      }}"></or-mwc-input>
                    </div>
                    ${when(authGrant.grant_type != undefined, () => {
                        switch (authGrant.grant_type) {
                            case "client_credentials": {
                                const grant = authGrant as OAuthClientCredentialsGrant;
                                return html`
                                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('clientId')}"
                                                  .value="${grant.client_id}"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                                      grant.client_id = ev.detail.value;
                                                      this.notifyWebhookUpdate();
                                                  }}"></or-mwc-input>
                                    <or-mwc-input type="${InputType.PASSWORD}" label="${i18next.t('clientSecret')}"
                                                  .value="${grant.client_secret}"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                                      grant.client_secret = ev.detail.value;
                                                      this.notifyWebhookUpdate();
                                                  }}"></or-mwc-input>
                                `;
                            }
                            case "password": {
                                const grant = authGrant as OAuthPasswordGrant;
                                return html`
                                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('username')}"
                                                  .value="${grant.username}"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                                      grant.username = ev.detail.value;
                                                      this.notifyWebhookUpdate();
                                                  }}"></or-mwc-input>
                                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('password')}"
                                                  .value="${grant.password}"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                                      grant.password = ev.detail.value;
                                                      this.notifyWebhookUpdate();
                                                  }}"></or-mwc-input>
                                `
                            }
                            default:
                                return html`${i18next.t('errorOccurred')}`
                        }
                    })}
                </div>
            `
        }
    }
}
