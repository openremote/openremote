var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import { css, html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { when } from 'lit/directives/when.js';
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
//language=css
const styling = css `
    .divider {
        border-bottom: 1px solid rgba(0, 0, 0, 12%);
    }
`;
let OrRuleFormWebhook = class OrRuleFormWebhook extends LitElement {
    constructor() {
        super(...arguments);
        this.loading = false;
        this.httpMethodOptions = ["GET" /* HTTPMethod.GET */, "POST" /* HTTPMethod.POST */, "PUT" /* HTTPMethod.PUT */, "DELETE" /* HTTPMethod.DELETE */];
        this.authMethodOptions = new Map([
            ["username_password", (i18next.t('username') + " & " + i18next.t('password'))],
            ["client_credentials", "oauth Client Credentials Grant"],
            ["password", "oauth Password Grant"]
        ]);
    }
    static get styles() {
        return [styling];
    }
    /* --------------------------- */
    // Lifecycle methods
    shouldUpdate(changedProperties) {
        if (changedProperties.has('webhook')) {
            if (this.webhook.headers == undefined) {
                this.webhook.headers = {};
            }
        }
        return super.shouldUpdate(changedProperties);
    }
    /* --------------- */
    // Util
    getAuthMethod(webhook) {
        if (webhook.oAuthGrant != undefined) {
            return this.authMethodOptions.get(webhook.oAuthGrant.grant_type);
        }
        else {
            return this.authMethodOptions.get("username_password");
        }
    }
    getOAuthGrant(authMethodKey) {
        if (authMethodKey == 'client_credentials' || authMethodKey == 'password') {
            return { grant_type: authMethodKey };
        }
        else {
            return undefined;
        }
    }
    reloadHeaders() {
        this.loading = true;
        this.updateComplete.then(() => this.loading = false);
        this.notifyWebhookUpdate(false);
    }
    notifyWebhookUpdate(requestUpdate = true) {
        if (requestUpdate) {
            this.requestUpdate("webhook");
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }
    /* ---------------- */
    // Template rendering
    render() {
        return when(!this.webhook, () => html `
            ${i18next.t('errorOccurred')}
        `, () => html `
            <form style="display: flex; flex-direction: column; min-width: 520px;">
                <!-- HTTP Method & URL -->
                <div style="display: flex; flex-direction: row; align-items: center; gap: 5px; margin-bottom: 28px;">
                    <or-mwc-input style="flex: 0;" type="${InputType.SELECT}" .value="${this.webhook.httpMethod}"
                                  .options="${this.httpMethodOptions}"
                                  @or-mwc-input-changed="${(ev) => {
            this.webhook.httpMethod = ev.detail.value;
            this.notifyWebhookUpdate();
        }}"
                    ></or-mwc-input>
                    <or-mwc-input style="flex: 1;" type="${InputType.URL}" required label="${i18next.t('webUrl')}"
                                  .value="${this.webhook.url}" helperPersistent
                                  @or-mwc-input-changed="${(e) => {
            this.webhook.url = e.detail.value;
            this.notifyWebhookUpdate();
        }}"></or-mwc-input>
                </div>
                <!-- Headers -->
                <div style="display: flex; flex-direction: column; gap: 5px; margin-bottom: 28px;">
                    <span>Headers</span>
                    ${when(this.loading, () => html `
                        ${this.getHeadersTemplate(this.webhook.headers, true)}
                    `, () => html `
                        ${this.getHeadersTemplate(this.webhook.headers, false)}
                    `)}
                    <or-mwc-input type="${InputType.BUTTON}" icon="plus" label="addRequestHeader"
                                  @or-mwc-input-changed="${(ev) => {
            if ((this.webhook.headers ? this.webhook.headers[''] : undefined) != undefined) {
                this.webhook.headers[''].push('');
            }
            else {
                this.webhook.headers[''] = [''];
            }
            this.reloadHeaders();
        }}"></or-mwc-input>
                </div>
                <!-- Authorization -->
                <div style="display: flex; flex-direction: column; gap: 10px; margin-bottom: ${this.webhook.oAuthGrant || this.webhook.usernamePassword ? '28px' : '0'};">
                    <or-mwc-input type="${InputType.SWITCH}" fullwidth label="${i18next.t('requiresAuthorization')}"
                                  .value="${this.webhook.oAuthGrant || this.webhook.usernamePassword}"
                                  @or-mwc-input-changed="${(ev) => {
            this.webhook.usernamePassword = ev.detail.value ? {
                username: 'admin',
                password: 'secret'
            } : undefined;
            this.notifyWebhookUpdate();
        }}"></or-mwc-input>
                    ${when(this.webhook.oAuthGrant || this.webhook.usernamePassword, () => {
            const values = Array.from(this.authMethodOptions.values());
            return html `
                            <or-mwc-input type="${InputType.SELECT}" label="${i18next.t('method')}"
                                          .value="${this.getAuthMethod(this.webhook)}"
                                          .options="${values}"
                                          @or-mwc-input-changed="${(ev) => {
                const entry = [...this.authMethodOptions.entries()].find((entry) => entry[1] == ev.detail.value);
                this.webhook.oAuthGrant = this.getOAuthGrant(entry[0]);
                this.notifyWebhookUpdate();
            }}"></or-mwc-input>
                            ${this.getAuthSettingsTemplate(this.webhook)}
                        `;
        })}
                </div>
                <!-- Payload -->
                <div style="display: flex; flex-direction: column; gap: 5px;">
                    ${when(this.webhook.httpMethod != "GET" /* HTTPMethod.GET */ && this.webhook.httpMethod != "DELETE" /* HTTPMethod.DELETE */, () => html `
                        <or-mwc-input type="${InputType.SWITCH}" fullwidth label="${i18next.t('includeBodyInRequest')}"
                                      .value="${this.webhook.payload != undefined}"
                                      @or-mwc-input-changed="${(ev) => {
            this.webhook.payload = ev.detail.value ? JSON.stringify({
                rule: "%RULESET_NAME%",
                assets: "%TRIGGER_ASSETS%"
            }, null, 4) : undefined;
            this.notifyWebhookUpdate();
        }}"
                        ></or-mwc-input>
                        ${when(this.webhook.payload != undefined, () => {
            return html `
                                <or-mwc-input type="${InputType.TEXTAREA}" .value="${this.webhook.payload}"
                                              @or-mwc-input-changed="${(ev) => {
                this.webhook.payload = ev.detail.value;
                this.notifyWebhookUpdate();
            }}"></or-mwc-input>
                            `;
        })}
                    `)}
                </div>
            </form>
        `);
    }
    getHeadersTemplate(headers, loading) {
        return Object.keys(this.webhook.headers)
            .sort((a, b) => -(a.localeCompare(b)))
            .map((key, keyIndex, keys) => {
            const values = this.webhook.headers[key];
            return values.map((value, valueIndex) => html `
                    <div style="display: flex; gap: 5px;">
                        <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('header')}" value="${key}"
                                      style="flex: 1;" .disabled="${loading}"
                                      @or-mwc-input-changed="${(ev) => {
                values.length > 0 ? values.splice(valueIndex, 1) : delete this.webhook.headers[key];
                const newValues = this.webhook.headers[ev.detail.value];
                if (newValues && newValues.length > 0) {
                    newValues.push(value);
                }
                else {
                    this.webhook.headers[ev.detail.value] = [value];
                }
                this.reloadHeaders();
            }}"
                        ></or-mwc-input>
                        <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('value')}" value="${value}"
                                      style="flex: 1;" .disabled="${loading}"
                                      @or-mwc-input-changed="${(ev) => {
                this.webhook.headers[key][valueIndex] = ev.detail.value;
                this.notifyWebhookUpdate();
            }}"
                        ></or-mwc-input>
                        <or-mwc-input type="${InputType.BUTTON}" icon="delete" .disabled="${loading}"
                                      @or-mwc-input-changed="${() => {
                values.splice(valueIndex, 1);
                this.reloadHeaders();
            }}"></or-mwc-input>
                    </div>
                `);
        });
    }
    getAuthSettingsTemplate(webhook) {
        var _a, _b;
        const authGrant = webhook.oAuthGrant;
        if (authGrant == undefined) {
            return html `
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('username')}"
                                  .value="${(_a = webhook.usernamePassword) === null || _a === void 0 ? void 0 : _a.username}"
                                  @or-mwc-input-changed="${(ev) => {
                if (!this.webhook.usernamePassword) {
                    this.webhook.usernamePassword = {};
                }
                this.webhook.usernamePassword.username = ev.detail.value;
                this.notifyWebhookUpdate();
            }}"></or-mwc-input>
                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('password')}"
                                  .value="${(_b = this.webhook.usernamePassword) === null || _b === void 0 ? void 0 : _b.password}"
                                  @or-mwc-input-changed="${(ev) => {
                if (!this.webhook.usernamePassword) {
                    this.webhook.usernamePassword = {};
                }
                this.webhook.usernamePassword.password = ev.detail.value;
                this.notifyWebhookUpdate();
            }}"></or-mwc-input>
                </div>
            `;
        }
        else {
            return html `
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <div style="display: flex; flex-direction: row; align-items: center; gap: 5px;">
                        <or-mwc-input style="flex: 1;" type="${InputType.URL}" required
                                      label="${i18next.t('tokenUrl')}" .value="${authGrant.tokenEndpointUri}"
                                      @or-mwc-input-changed="${(ev) => {
                authGrant.tokenEndpointUri = ev.detail.value;
                this.notifyWebhookUpdate();
            }}"></or-mwc-input>
                    </div>
                    ${when(authGrant.grant_type != undefined, () => {
                switch (authGrant.grant_type) {
                    case "client_credentials": {
                        const grant = authGrant;
                        return html `
                                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('clientId')}"
                                                  .value="${grant.client_id}"
                                                  @or-mwc-input-changed="${(ev) => {
                            grant.client_id = ev.detail.value;
                            this.notifyWebhookUpdate();
                        }}"></or-mwc-input>
                                    <or-mwc-input type="${InputType.PASSWORD}" label="${i18next.t('clientSecret')}"
                                                  .value="${grant.client_secret}"
                                                  @or-mwc-input-changed="${(ev) => {
                            grant.client_secret = ev.detail.value;
                            this.notifyWebhookUpdate();
                        }}"></or-mwc-input>
                                `;
                    }
                    case "password": {
                        const grant = authGrant;
                        return html `
                                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('username')}"
                                                  .value="${grant.username}"
                                                  @or-mwc-input-changed="${(ev) => {
                            grant.username = ev.detail.value;
                            this.notifyWebhookUpdate();
                        }}"></or-mwc-input>
                                    <or-mwc-input type="${InputType.TEXT}" label="${i18next.t('password')}"
                                                  .value="${grant.password}"
                                                  @or-mwc-input-changed="${(ev) => {
                            grant.password = ev.detail.value;
                            this.notifyWebhookUpdate();
                        }}"></or-mwc-input>
                                `;
                    }
                    default:
                        return html `${i18next.t('errorOccurred')}`;
                }
            })}
                </div>
            `;
        }
    }
};
__decorate([
    property({ type: Object })
], OrRuleFormWebhook.prototype, "webhook", void 0);
__decorate([
    state()
], OrRuleFormWebhook.prototype, "loading", void 0);
OrRuleFormWebhook = __decorate([
    customElement("or-rule-form-webhook")
], OrRuleFormWebhook);
export { OrRuleFormWebhook };
//# sourceMappingURL=or-rule-form-webhook.js.map