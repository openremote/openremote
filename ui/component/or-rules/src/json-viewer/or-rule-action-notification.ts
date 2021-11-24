import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {until} from "lit/directives/until.js";
import {ActionTargetType, ActionType, OrRulesRuleUnsupportedEvent, RulesConfig} from "../index";
import {
    AssetQuery,
    AssetQueryOrderBy$Property,
    AssetTypeInfo,
    JsonRule,
    RuleAction,
    RuleActionNotification,
    UserQuery,
    WellknownAssets
} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {getTargetTypeMap, OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import "./modals/or-rule-notification-modal";
import "./forms/or-rule-form-email-message";
import "./forms/or-rule-form-push-notification";
import "./or-rule-action-attribute";
import {i18next} from "@openremote/or-translate";
import manager, {Util} from "@openremote/core";

// language=CSS
const style = css`
    :host {
        display: flex;
        align-items: center;
    }

    :host > * {
        margin: 0 3px 6px;
    }

    .min-width {
        min-width: 200px;
    }
`;

@customElement("or-rule-action-notification")
export class OrRuleActionNotification extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public rule!: JsonRule;

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;

    @property({type: String, attribute: false})
    public actionType!: ActionType;

    public readonly?: boolean;

    @property({type: Object})
    public assetInfos?: AssetTypeInfo[];

    @property({type: Object})
    public config?: RulesConfig;

    protected static getActionTargetTemplate(targetTypeMap: [string, string?][], action: RuleAction, actionType: ActionType, readonly: boolean, config: RulesConfig | undefined, baseAssetQuery: AssetQuery | undefined, onTargetTypeChangedCallback: (type: ActionTargetType) => void, onTargetChangedCallback: (type: ActionTargetType, value: string | undefined) => void): PromiseLike<TemplateResult> | undefined {

        let allowedTargetTypes: [ActionTargetType, string][] = [
            [ActionTargetType.USER, i18next.t("user_plural")],
            [ActionTargetType.ASSET, i18next.t("asset_plural")],
            [ActionTargetType.TENANT, i18next.t("tenant_plural")],
            [ActionTargetType.CUSTOM, i18next.t("custom")]
        ];

        if (config && config.controls && config.controls.allowedActionTargetTypes) {
            let configTypes: string[] | undefined;

            if (config.controls.allowedActionTargetTypes.actions) {
                configTypes = (config.controls.allowedActionTargetTypes.actions as any)[actionType] as ActionTargetType[];
            } else {
                configTypes = config.controls.allowedActionTargetTypes.default;
            }

            if (configTypes) {
                allowedTargetTypes = allowedTargetTypes.filter((allowedType) => configTypes?.includes(allowedType[0]));
            }
        }

        if (allowedTargetTypes.length === 0) {
            console.warn("Rule action config doesn't allow any action target types for this type of action");
            return;
        }

        let targetType: ActionTargetType | undefined = ActionTargetType.ASSET;

        if (action.target) {
            if (action.target.users && !action.target.conditionAssets && !action.target.matchedAssets && !action.target.assets) {
                targetType = ActionTargetType.USER;
            } else if (action.target.custom !== undefined && !action.target.conditionAssets && !action.target.matchedAssets && !action.target.assets) {
                targetType = ActionTargetType.CUSTOM;
            }
        }

        let targetValueTemplate: PromiseLike<TemplateResult>;

        if (!allowedTargetTypes.find((allowedTargetType) => allowedTargetType[0] === targetType)) {
            targetType = undefined;
        }

        if (targetType === ActionTargetType.CUSTOM) {

            const template = html`
                <or-mwc-input class="min-width" .type="${InputType.TEXT}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => onTargetChangedCallback(targetType!, e.detail.value)}" ?readonly="${readonly}" .value="${action.target!.custom}" ></or-mwc-input>            
            `;
            targetValueTemplate = Promise.resolve(template);

        } else {

            let targetValuesGenerator: PromiseLike<[string, string][]>;
            let label: string | undefined;
            let value: string | undefined;

            if (targetType === ActionTargetType.USER) {
                targetValuesGenerator = manager.rest.api.UserResource.query({tenant: manager.displayRealm} as UserQuery).then(
                    (usersResponse) => usersResponse.data.map((user) => [user.id!, user.username!])
                );
                label = i18next.t("user_plural");

                const userQuery = action.target!.users!;

                if ((userQuery.ids && userQuery.ids.length > 1)
                    || userQuery.usernames
                    || userQuery.assetPredicate
                    || userQuery.limit
                    || userQuery.pathPredicate
                    || userQuery.tenantPredicate) {
                    console.warn("Rule action user target query is unsupported: " + JSON.stringify(userQuery, null, 2));
                    return;
                }

                if (userQuery.ids && userQuery.ids.length === 1) {
                    value = userQuery.ids[0];
                }
            } else {
                const assetQuery = baseAssetQuery ? {...baseAssetQuery} : {};
                assetQuery.select = {
                    excludeParentInfo: true,
                    excludePath: true
                };
                assetQuery.orderBy = {
                    property: AssetQueryOrderBy$Property.NAME
                };

                targetValuesGenerator = manager.rest.api.AssetResource.queryAssets(assetQuery).then(
                    (response) => response.data.map((asset) => [asset.id!, asset.name! + " (" + asset.id! + ")"])
                );
                label = i18next.t("asset_plural");
                if (!action.target) {
                    value = "allMatched";
                } else {
                    if (action.target.conditionAssets) {
                        console.warn("Rule action asset target, conditionAssets is unsupported: " + JSON.stringify(action.target, null, 2));
                        return;
                    }
                    if (action.target.matchedAssets) {
                        if (action.target.matchedAssets.types && action.target.matchedAssets.types.length > 1) {
                            console.warn("Rule action asset target, matchedAssets query unsupported: " + JSON.stringify(action.target, null, 2));
                            return;
                        }
                        if (action.target.matchedAssets.types && action.target.matchedAssets.types.length === 1) {
                            value = action.target.matchedAssets.types[0];
                        }
                    } else if (action.target.assets) {
                        if (action.target.assets.ids && action.target.assets.ids.length > 1) {
                            console.warn("Rule action asset target, assets query unsupported: " + JSON.stringify(action.target, null, 2));
                            return;
                        }
                        if (action.target.assets.ids && action.target.assets.ids.length === 1) {
                            value = action.target.assets.ids[0];
                        }
                    }
                }
            }

            targetValueTemplate = targetValuesGenerator.then((values) => {

                // Add additional options for assets
                if (targetType === ActionTargetType.ASSET) {
                    const additionalValues: [string, string][] = [["allMatched", i18next.t("matched")]];
                    if (targetTypeMap && targetTypeMap.length > 1) {
                        targetTypeMap.forEach((typeAndTag) => {
                            if (!additionalValues.find((av) => av[0] === typeAndTag[0])) {
                                additionalValues.push([typeAndTag[0], i18next.t("matchedOfType", {type: Util.getAssetTypeLabel(typeAndTag[0])})]);
                            }
                        });
                    }
                    values = [...additionalValues, ...values];
                }

                return html`
                    <or-mwc-input type="${InputType.SELECT}" 
                        class="min-width"
                        .options="${values}"
                        .label="${label}"
                        .value="${value}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => onTargetChangedCallback(targetType!, e.detail.value)}" 
                        ?readonly="${readonly}"></or-mwc-input>
                `;
            });
        }

        targetValueTemplate = targetValueTemplate.then((valueTemplate) => {
            return html`
                <or-mwc-input type="${InputType.SELECT}" 
                    class="min-width"
                    .options="${allowedTargetTypes}"
                    .value="${targetType}"
                    .label="${i18next.t("recipients")}"
                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => onTargetTypeChangedCallback(e.detail.value as ActionTargetType)}" 
                    ?readonly="${readonly}"></or-mwc-input>
                ${valueTemplate}
            `;
        });

        return targetValueTemplate;
    }

    protected render() {

        if (!this.action.notification || !this.action.notification.message) {
            return html``;
        }

        const message = this.action.notification.message;
        const messageType = message.type!;
        let baseAssetQuery: AssetQuery;

        if (messageType === "push") {
            baseAssetQuery = {
                types: [
                    WellknownAssets.CONSOLEASSET
                ]};
        } else {
            baseAssetQuery = {
                attributes: {
                    items: [
                        {
                            name: { "predicateType": "string", "value": "email" },
                            value: { "predicateType": "value-empty", negate: true }
                        }
                    ]
                }
            }
        }

        let targetTemplate = OrRuleActionNotification.getActionTargetTemplate(getTargetTypeMap(this.rule), this.action, this.actionType, !!this.readonly, this.config, baseAssetQuery,(type) => this._onTargetTypeChanged(type), (type, value) => this._onTargetChanged(type, value));
        let modalTemplate: TemplateResult | string = ``;

        if (!targetTemplate) {
            this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
            return ``;
        }

        if (message) {
            if (messageType === "push") {
                modalTemplate = html`
                    <or-rule-notification-modal title="push-notification" .action="${this.action}">
                        <or-rule-form-push-notification .action="${this.action}"></or-rule-form-push-notification>
                    </or-rule-notification-modal>
                `;
            }
            
            if (messageType === "email") {
                modalTemplate = html`
                    <or-rule-notification-modal title="email" .action="${this.action}">
                        <or-rule-form-email-message .action="${this.action}"></or-rule-form-email-message>
                    </or-rule-notification-modal>
                `;
            }
        }

        targetTemplate = targetTemplate.then((targetTemplate) =>
            html`
                ${targetTemplate}
                ${modalTemplate}
            `
        );

        return html`${until(targetTemplate,html``)}`;
    }

    protected _onTargetTypeChanged(targetType: ActionTargetType) {
        if (targetType === ActionTargetType.ASSET) {
            delete this.action.target;
        } else if (targetType === ActionTargetType.USER) {
            this.action.target = {
                users: {
                    ids: []
                }
            };
        } else if (targetType === ActionTargetType.CUSTOM) {
            this.action.target = {
                custom: ""
            }
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected _onTargetChanged(targetType: ActionTargetType, value: string | undefined) {
        switch (targetType) {
            case ActionTargetType.USER:
                if(value){
                    const users:UserQuery = {ids: [value]}
                    this.action.target = {users: users}
                }
            break;
            case ActionTargetType.CUSTOM:
                    this.action.target = {
                        custom: value
                    }
                break;
            case ActionTargetType.ASSET:
                if (!value || value === "allMatched") {
                    delete this.action.target;
                } else if (value.endsWith("Asset")) {
                     // This is an asset type
                    this.action.target = {
                        matchedAssets: {
                            types: [
                                value
                            ]
                        }
                    };
                } else {
                    this.action.target = {
                        assets: {
                            ids: [
                                value
                            ]
                        }
                    };
                }
                break;
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }
}
