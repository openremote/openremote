import { OAuthGrant, Webhook } from "@openremote/model";
import { LitElement, TemplateResult } from "lit";
export declare class OrRuleFormWebhook extends LitElement {
    protected webhook: Webhook;
    protected loading: boolean;
    private httpMethodOptions;
    private authMethodOptions;
    static get styles(): import("lit").CSSResult[];
    shouldUpdate(changedProperties: Map<string, any>): boolean;
    getAuthMethod(webhook: Webhook): string | undefined;
    getOAuthGrant(authMethodKey: string): OAuthGrant | undefined;
    reloadHeaders(): void;
    notifyWebhookUpdate(requestUpdate?: boolean): void;
    render(): TemplateResult<1>;
    getHeadersTemplate(headers: {
        [p: string]: string[];
    }, loading: boolean): TemplateResult<1>[][];
    getAuthSettingsTemplate(webhook: Webhook): TemplateResult | undefined;
}
