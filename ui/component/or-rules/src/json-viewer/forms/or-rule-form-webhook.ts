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
import {OrVaadinSelect, SelectItem} from "@openremote/or-vaadin-components/or-vaadin-select";
import {OrVaadinTextField} from "@openremote/or-vaadin-components/or-vaadin-text-field";


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
                <div style="display: flex; flex-direction: row; align-items: baseline; gap: 5px; margin-bottom: 28px;">
                    <or-vaadin-select value=${this.webhook.httpMethod} .items=${this.httpMethodOptions.map(o => ({value: o, label: o}))} required style="flex: 0 0 100px;"
                                      @change=${(ev: Event) => {
                                          this.webhook.httpMethod = (ev.currentTarget as OrVaadinSelect).value as HTTPMethod;
                                          this.notifyWebhookUpdate()
                                      }}>
                        <or-translate slot="label" value="method"></or-translate>
                    </or-vaadin-select>
                    <or-vaadin-text-field type="url" value=${this.webhook.url} required style="flex: 1;"
                                          @change=${(ev: Event) => {
                                              this.webhook.url = (ev.currentTarget as OrVaadinTextField).value;
                                              this.notifyWebhookUpdate();
                                          }}>
                        <or-translate slot="label" value="webUrl"></or-translate>
                    </or-vaadin-text-field>
                </div>
                <!-- Headers -->
                <div style="display: flex; flex-direction: column; gap: 5px; margin-bottom: 28px;">
                    <span>Headers</span>
                    ${when(this.loading, () => html`
                        ${this.getHeadersTemplate(this.webhook.headers!, true)}
                    `, () => html`
                        ${this.getHeadersTemplate(this.webhook.headers!, false)}
                    `)}
                    <or-vaadin-button @click=${() => {
                        if ((this.webhook.headers ? this.webhook.headers[''] : undefined) != undefined) {
                            this.webhook.headers![''].push('');
                        } else {
                            this.webhook.headers![''] = [''];
                        }
                        this.reloadHeaders();
                    }}>
                        <or-icon slot="prefix" icon="plus"></or-icon>
                        <or-translate value="addRequestHeader"></or-translate>
                    </or-vaadin-button>
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
                        const values: SelectItem[] = Array.from(this.authMethodOptions.entries()).map(([value, label]) => ({value, label}));
                        return html`
                            <or-vaadin-select value=${this.webhook.oAuthGrant?.grant_type ?? values[0].value} .items=${values}
                                              @change=${(ev: Event) => {
                                                  this.webhook.oAuthGrant = this.getOAuthGrant((ev.currentTarget as OrVaadinSelect).value);
                                                  this.notifyWebhookUpdate();
                                              }}>
                                <or-translate slot="label" value="method"></or-translate>
                            </or-vaadin-select>
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
                                <or-vaadin-text-area value=${this.webhook.payload} style="min-height: 200px;"
                                                     @change=${(ev: Event) => {
                                                         this.webhook.payload = (ev.currentTarget as HTMLInputElement).value;
                                                         this.notifyWebhookUpdate();
                                                     }}>
                                </or-vaadin-text-area>
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
                    <div style="display: flex; align-items: baseline; gap: 5px;">
                        <or-vaadin-text-field value=${key} ?disabled=${loading} style="flex: 1;"
                                              @change=${(ev: Event) => {
                                                  const inputValue = (ev.currentTarget as HTMLInputElement).value;
                                                  values.length > 0 ? values.splice(valueIndex, 1) : delete this.webhook.headers![key];
                                                  const newValues = this.webhook.headers![inputValue];
                                                  if (newValues && newValues.length > 0) {
                                                      newValues.push(value);
                                                  } else {
                                                      this.webhook.headers![inputValue] = [value]
                                                  }
                                                  this.reloadHeaders();
                                              }}>
                            <or-translate slot="label" value="header"></or-translate>
                        </or-vaadin-text-field>
                        <or-vaadin-text-field value=${value} ?disabled=${loading} style="flex: 1;"
                                              @change=${(ev: Event) => {
                                                  this.webhook.headers![key][valueIndex] = (ev.currentTarget as HTMLInputElement).value;
                                                  this.notifyWebhookUpdate();
                                              }}>
                            <or-translate slot="label" value="value"></or-translate>
                        </or-vaadin-text-field>
                        <or-vaadin-button theme="icon" ?disabled=${loading} 
                                          @click=${() => {
                                              values.splice(valueIndex, 1);
                                              this.reloadHeaders();
                                          }}>
                            <or-icon icon="delete"></or-icon>
                        </or-vaadin-button>
                    </div>
                `)
            })
    }

    getAuthSettingsTemplate(webhook: Webhook): TemplateResult | undefined {
        const authGrant = webhook.oAuthGrant;
        if (authGrant == undefined) {
            return html`
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <or-vaadin-text-field value=${webhook.usernamePassword?.username}
                                          @change=${(ev: Event) => {
                                              this.webhook.usernamePassword ??= {};
                                              this.webhook.usernamePassword.username = (ev.currentTarget as HTMLInputElement).value;
                                              this.notifyWebhookUpdate();
                                          }}>
                        <or-translate slot="label" value="username"></or-translate>
                    </or-vaadin-text-field>
                    <or-vaadin-password-field value=${webhook.usernamePassword?.password}
                                              @change=${(ev: Event) => {
                                                  this.webhook.usernamePassword ??= {};
                                                  this.webhook.usernamePassword.password = (ev.currentTarget as HTMLInputElement).value;
                                                  this.notifyWebhookUpdate();
                                              }}>
                        <or-translate slot="label" value="password"></or-translate>
                    </or-vaadin-password-field>
                </div>
            `
        } else {
            return html`
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <div style="display: flex; flex-direction: row; align-items: center; gap: 5px;">
                        <or-vaadin-text-field type="url" value=${authGrant.tokenEndpointUri} required style="flex: 1;"
                                              @change=${(ev: Event) => {
                                                  authGrant.tokenEndpointUri = (ev.currentTarget as HTMLInputElement).value;
                                                  this.notifyWebhookUpdate();
                                              }}>
                            <or-translate slot="label" value="tokenUrl"></or-translate>
                        </or-vaadin-text-field>
                    </div>
                    ${when(authGrant.grant_type != undefined, () => {
                        switch (authGrant.grant_type) {
                            case "client_credentials": {
                                const grant = authGrant as OAuthClientCredentialsGrant;
                                return html`
                                    <or-vaadin-text-field value=${grant.client_id}
                                                          @change=${(ev: Event) => {
                                                              grant.client_id = (ev.currentTarget as HTMLInputElement).value;
                                                              this.notifyWebhookUpdate();
                                                          }}>
                                        <or-translate slot="label" value="clientId"></or-translate>
                                    </or-vaadin-text-field>
                                    <or-vaadin-password-field value=${grant.client_secret}
                                                              @change=${(ev: Event) => {
                                                                  grant.client_secret = (ev.currentTarget as HTMLInputElement).value;
                                                                  this.notifyWebhookUpdate();
                                                              }}>
                                        <or-translate slot="label" value="clientSecret"></or-translate>
                                    </or-vaadin-password-field>
                                `;
                            }
                            case "password": {
                                const grant = authGrant as OAuthPasswordGrant;
                                return html`
                                    <or-vaadin-text-field value=${grant.username}
                                                          @change=${(ev: Event) => {
                                                              grant.username = (ev.currentTarget as HTMLInputElement).value;
                                                              this.notifyWebhookUpdate();
                                                          }}>
                                        <or-translate slot="label" value="username"></or-translate>
                                    </or-vaadin-text-field>
                                    <or-vaadin-password-field value=${grant.password}
                                                          @change=${(ev: Event) => {
                                                              grant.password = (ev.currentTarget as HTMLInputElement).value;
                                                              this.notifyWebhookUpdate();
                                                          }}>
                                        <or-translate slot="label" value="password"></or-translate>
                                    </or-vaadin-password-field>
                                `;
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
