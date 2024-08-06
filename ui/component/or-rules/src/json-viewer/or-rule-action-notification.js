var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var OrRuleActionNotification_1;
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { until } from "lit/directives/until.js";
import { OrRulesRuleUnsupportedEvent } from "../index";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { getTargetTypeMap, OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
import "./modals/or-rule-notification-modal";
import "./forms/or-rule-form-email-message";
import "./forms/or-rule-form-push-notification";
import "./or-rule-action-attribute";
import { i18next } from "@openremote/or-translate";
import manager, { Util } from "@openremote/core";
// language=CSS
const style = css `
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
let OrRuleActionNotification = OrRuleActionNotification_1 = class OrRuleActionNotification extends LitElement {
    static get styles() {
        return style;
    }
    static getActionTargetTemplate(targetTypeMap, action, actionType, readonly, config, baseAssetQuery, onTargetTypeChangedCallback, onTargetChangedCallback) {
        let allowedTargetTypes = [
            ["USER" /* NotificationTargetType.USER */, i18next.t("user_plural")],
            ["ASSET" /* NotificationTargetType.ASSET */, i18next.t("asset_plural")],
            ["REALM" /* NotificationTargetType.REALM */, i18next.t("realm_plural")],
            ["CUSTOM" /* NotificationTargetType.CUSTOM */, i18next.t("custom")]
        ];
        if (config && config.controls && config.controls.allowedActionTargetTypes) {
            let configTypes;
            if (config.controls.allowedActionTargetTypes.actions) {
                configTypes = config.controls.allowedActionTargetTypes.actions[actionType];
            }
            else {
                configTypes = config.controls.allowedActionTargetTypes.default;
            }
            if (configTypes) {
                allowedTargetTypes = allowedTargetTypes.filter((allowedType) => configTypes === null || configTypes === void 0 ? void 0 : configTypes.includes(allowedType[0]));
            }
        }
        if (allowedTargetTypes.length === 0) {
            console.warn("Rule action config doesn't allow any action target types for this type of action");
            return;
        }
        let targetType = "ASSET" /* NotificationTargetType.ASSET */;
        if (action.target) {
            if (action.target.users && !action.target.conditionAssets && !action.target.matchedAssets && !action.target.assets) {
                targetType = "USER" /* NotificationTargetType.USER */;
            }
            else if (action.target.linkedUsers) {
                targetType = "USER" /* NotificationTargetType.USER */;
            }
            else if (action.target.custom !== undefined && !action.target.conditionAssets && !action.target.matchedAssets && !action.target.assets) {
                targetType = "CUSTOM" /* NotificationTargetType.CUSTOM */;
            }
        }
        let targetValueTemplate;
        if (!allowedTargetTypes.find((allowedTargetType) => allowedTargetType[0] === targetType)) {
            targetType = undefined;
        }
        if (targetType === "CUSTOM" /* NotificationTargetType.CUSTOM */) {
            const template = html `
                <or-mwc-input class="min-width" .type="${InputType.TEXT}" @or-mwc-input-changed="${(e) => onTargetChangedCallback(targetType, e.detail.value)}" ?readonly="${readonly}" .value="${action.target.custom}" ></or-mwc-input>            
            `;
            targetValueTemplate = Promise.resolve(template);
        }
        else {
            let targetValuesGenerator;
            let label;
            let value;
            if (targetType === "USER" /* NotificationTargetType.USER */) {
                // Get users excluding system accounts and service users
                const query = {
                    realmPredicate: { name: manager.displayRealm },
                    select: { basic: true },
                    serviceUsers: false,
                    attributes: [{ name: { value: "systemAccount", predicateType: "string" }, negated: true }]
                };
                targetValuesGenerator = manager.rest.api.UserResource.query(query).then((usersResponse) => __awaiter(this, void 0, void 0, function* () {
                    const linkedLabel = i18next.t("linked");
                    // Get realm roles and add as options
                    const realm = yield manager.rest.api.RealmResource.get(manager.displayRealm);
                    let realmRoleOpts = realm.data.realmRoles.filter(r => Util.realmRoleFilter(r)).map(r => ["linked-" + r.name, linkedLabel + ": " + i18next.t("realmRole." + r.name, Util.camelCaseToSentenceCase(r.name.replace("_", " ").replace("-", " ")))]);
                    let values = usersResponse.data.map((user) => [user.id, user.username]);
                    return [["linkedUsers", linkedLabel], ...realmRoleOpts, ...values];
                }));
                label = i18next.t("user_plural");
                const userQuery = action.target.users;
                if (action.target.linkedUsers) {
                    if (userQuery && userQuery.realmRoles) {
                        if (userQuery.realmRoles.length > 1) {
                            console.warn("Rule action user target query is unsupported: " + JSON.stringify(userQuery, null, 2));
                            return;
                        }
                        value = "linked-" + userQuery.realmRoles[0].value;
                    }
                    else {
                        value = "linkedUsers";
                    }
                }
                else if (userQuery) {
                    if ((userQuery.ids && userQuery.ids.length > 1)
                        || userQuery.usernames
                        || userQuery.assets
                        || userQuery.limit
                        || userQuery.pathPredicate
                        || userQuery.realmPredicate) {
                        console.warn("Rule action user target query is unsupported: " + JSON.stringify(userQuery, null, 2));
                        return;
                    }
                    if (userQuery.ids && userQuery.ids.length === 1) {
                        value = userQuery.ids[0];
                    }
                }
                else {
                    console.warn("Rule action user target query is unsupported: " + JSON.stringify(userQuery, null, 2));
                    return;
                }
            }
            else {
                const assetQuery = baseAssetQuery ? Object.assign({}, baseAssetQuery) : {};
                assetQuery.orderBy = {
                    property: "NAME" /* AssetQueryOrderBy$Property.NAME */
                };
                targetValuesGenerator = manager.rest.api.AssetResource.queryAssets(assetQuery).then((response) => {
                    let values = response.data.map((asset) => [asset.id, asset.name + " (" + asset.id + ")"]);
                    // Add additional options for assets
                    const additionalValues = [["allMatched", i18next.t("matched")]];
                    if (targetTypeMap && targetTypeMap.length > 1) {
                        targetTypeMap.forEach((typeAndTag) => {
                            if (!additionalValues.find((av) => av[0] === typeAndTag[0])) {
                                additionalValues.push([typeAndTag[0], i18next.t("matchedOfType", { type: Util.getAssetTypeLabel(typeAndTag[0]) })]);
                            }
                        });
                    }
                    return [...additionalValues, ...values];
                });
                label = i18next.t("asset_plural");
                if (!action.target) {
                    value = "allMatched";
                }
                else {
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
                    }
                    else if (action.target.assets) {
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
                return html `
                    <or-mwc-input type="${InputType.SELECT}" 
                        class="min-width"
                        .options="${values}"
                        .label="${label}"
                        .value="${value}"
                        @or-mwc-input-changed="${(e) => onTargetChangedCallback(targetType, e.detail.value)}" 
                        ?readonly="${readonly}"></or-mwc-input>
                `;
            });
        }
        targetValueTemplate = targetValueTemplate.then((valueTemplate) => {
            return html `
                <or-mwc-input type="${InputType.SELECT}" 
                    class="min-width"
                    .options="${allowedTargetTypes}"
                    .value="${targetType}"
                    .label="${i18next.t("recipients")}"
                    @or-mwc-input-changed="${(e) => onTargetTypeChangedCallback(e.detail.value)}" 
                    ?readonly="${readonly}"></or-mwc-input>
                ${valueTemplate}
            `;
        });
        return targetValueTemplate;
    }
    render() {
        if (!this.action.notification || !this.action.notification.message) {
            return html ``;
        }
        const message = this.action.notification.message;
        const messageType = message.type;
        let baseAssetQuery;
        if (messageType === "push") {
            baseAssetQuery = {
                types: [
                    "ConsoleAsset" /* WellknownAssets.CONSOLEASSET */
                ]
            };
        }
        else {
            baseAssetQuery = {
                attributes: {
                    items: [
                        {
                            name: { "predicateType": "string", "value": "email" },
                            value: { "predicateType": "value-empty", negate: true }
                        }
                    ]
                }
            };
        }
        let targetTemplate = OrRuleActionNotification_1.getActionTargetTemplate(getTargetTypeMap(this.rule), this.action, this.actionType, !!this.readonly, this.config, baseAssetQuery, (type) => this._onTargetTypeChanged(type), (type, value) => this._onTargetChanged(type, value));
        let modalTemplate = ``;
        if (!targetTemplate) {
            this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
            return ``;
        }
        if (message) {
            if (messageType === "push") {
                modalTemplate = html `
                    <or-rule-notification-modal title="push-notification" .action="${this.action}">
                        <or-rule-form-push-notification .action="${this.action}"></or-rule-form-push-notification>
                    </or-rule-notification-modal>
                `;
            }
            if (messageType === "email") {
                modalTemplate = html `
                    <or-rule-notification-modal title="email" .action="${this.action}">
                        <or-rule-form-email-message .action="${this.action}"></or-rule-form-email-message>
                    </or-rule-notification-modal>
                `;
            }
        }
        targetTemplate = targetTemplate.then((targetTemplate) => html `
                ${targetTemplate}
                ${modalTemplate}
            `);
        return html `${until(targetTemplate, html ``)}`;
    }
    _onTargetTypeChanged(targetType) {
        if (targetType === "ASSET" /* NotificationTargetType.ASSET */) {
            delete this.action.target;
        }
        else if (targetType === "USER" /* NotificationTargetType.USER */) {
            this.action.target = {
                users: {
                    ids: []
                }
            };
        }
        else if (targetType === "CUSTOM" /* NotificationTargetType.CUSTOM */) {
            this.action.target = {
                custom: ""
            };
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    _onTargetChanged(targetType, value) {
        switch (targetType) {
            case "USER" /* NotificationTargetType.USER */:
                if (value === "linkedUsers") {
                    this.action.target = {
                        linkedUsers: true
                    };
                }
                else if (value === null || value === void 0 ? void 0 : value.startsWith("linked-")) {
                    this.action.target = {
                        users: {
                            realmRoles: [
                                {
                                    predicateType: "string",
                                    value: value === null || value === void 0 ? void 0 : value.substring(7)
                                }
                            ]
                        },
                        linkedUsers: true
                    };
                }
                else if (value) {
                    this.action.target = {
                        users: { ids: [value] }
                    };
                }
                break;
            case "CUSTOM" /* NotificationTargetType.CUSTOM */:
                this.action.target = {
                    custom: value
                };
                break;
            case "ASSET" /* NotificationTargetType.ASSET */:
                if (!value || value === "allMatched") {
                    delete this.action.target;
                }
                else if (value.endsWith("Asset")) {
                    // This is an asset type
                    this.action.target = {
                        matchedAssets: {
                            types: [
                                value
                            ]
                        }
                    };
                }
                else {
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
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleActionNotification.prototype, "rule", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrRuleActionNotification.prototype, "action", void 0);
__decorate([
    property({ type: String, attribute: false })
], OrRuleActionNotification.prototype, "actionType", void 0);
__decorate([
    property({ type: Object })
], OrRuleActionNotification.prototype, "assetInfos", void 0);
__decorate([
    property({ type: Object })
], OrRuleActionNotification.prototype, "config", void 0);
OrRuleActionNotification = OrRuleActionNotification_1 = __decorate([
    customElement("or-rule-action-notification")
], OrRuleActionNotification);
export { OrRuleActionNotification };
//# sourceMappingURL=or-rule-action-notification.js.map