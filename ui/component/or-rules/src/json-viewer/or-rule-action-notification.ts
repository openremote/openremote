import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {until} from "lit/directives/until.js";
import {ActionType, OrRulesRuleUnsupportedEvent, RulesConfig} from "../index";
import {
    AssetQuery,
    AssetQueryOrderBy$Property,
    AssetTypeInfo,
    JsonRule,
    RuleAction,
    RuleActionNotification,
    UserQuery,
    WellknownAssets,
    NotificationTargetType, PushNotificationMessage, EmailNotificationMessage, LocalizedNotificationMessage
} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {getTargetTypeMap, OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import "./modals/or-rule-notification-modal";
import "./forms/or-rule-form-email-message";
import "./forms/or-rule-form-push-notification";
import "./forms/or-rule-form-localized";
import "./or-rule-action-attribute";
import {i18next} from "@openremote/or-translate";
import manager, {Util} from "@openremote/core";
import {OrRulesNotificationModalCancelEvent, OrRulesNotificationModalOkEvent} from "./modals/or-rule-notification-modal";

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

    protected _initialAction?: RuleActionNotification;

    connectedCallback() {
        this.addEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
        this._initialAction = structuredClone(this.action);
        return super.connectedCallback();
    }

    willUpdate(changedProps: PropertyValues) {

        // If the rule property changes, we assume it is a "new rule".
        // For example when the SAVE button is pressed in the JSON editor (which triggers an update of this 'rule' property),
        // we want to reset the _initialAction cache variable.
        if(changedProps.has("rule") && changedProps.get("rule") !== undefined) {
            this._initialAction = structuredClone(this.action);
        }

        return super.willUpdate(changedProps);
    }

    disconnectedCallback() {
        this.removeEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
        return super.disconnectedCallback();
    }

    protected _onJsonRuleChanged() {

        // Upon rule change, we update the name of the "Notification action" to a sensible value, for example with the subject of an email
        // This is to prevent the NAME (for example showing up in the logs) being NULL or not identifiable.
        if(this.action.notification) {
            const message = this.action.notification.message;
            if(message?.type === "localized") {
                const locale = this.config?.notifications?.[manager.displayRealm]?.defaultLanguage || this.config?.notifications?.default?.defaultLanguage || manager.config.defaultLanguage!;
                const msg = message.languages?.[locale]; // if localized, we use the default language
                if(msg?.type === "push") {
                    this.action.notification.name = msg.title;
                } else if(msg?.type === "email") {
                    this.action.notification.name = msg.subject;
                }
            } else if (message?.type === "push") {
                this.action.notification.name = message.title;
            } else if (message?.type === "email") {
                this.action.notification.name = message.subject;
            }
        }
    }

    protected static getActionTargetTemplate(targetTypeMap: [string, string?][], action: RuleAction, actionType: ActionType, readonly: boolean, config: RulesConfig | undefined, baseAssetQuery: AssetQuery | undefined, onTargetTypeChangedCallback: (type: NotificationTargetType) => void, onTargetChangedCallback: (type: NotificationTargetType, value: string | undefined) => void): PromiseLike<TemplateResult> | undefined {

        let allowedTargetTypes: [NotificationTargetType, string][] = [
            [NotificationTargetType.USER, i18next.t("user_plural")],
            [NotificationTargetType.ASSET, i18next.t("asset_plural")],
            [NotificationTargetType.REALM, i18next.t("realm_plural")],
            [NotificationTargetType.CUSTOM, i18next.t("custom")]
        ];

        if (config && config.controls && config.controls.allowedActionTargetTypes) {
            let configTypes: string[] | undefined;

            if (config.controls.allowedActionTargetTypes.actions) {
                configTypes = (config.controls.allowedActionTargetTypes.actions as any)[actionType] as NotificationTargetType[];
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

        let targetType: NotificationTargetType | undefined = NotificationTargetType.ASSET;

        if (action.target) {
            if (action.target.users && !action.target.conditionAssets && !action.target.matchedAssets && !action.target.assets) {
                targetType = NotificationTargetType.USER;
            } else if (action.target.linkedUsers) {
                targetType = NotificationTargetType.USER;
            } else if (action.target.custom !== undefined && !action.target.conditionAssets && !action.target.matchedAssets && !action.target.assets) {
                targetType = NotificationTargetType.CUSTOM;
            }
        }

        let targetValueTemplate: PromiseLike<TemplateResult>;

        if (!allowedTargetTypes.find((allowedTargetType) => allowedTargetType[0] === targetType)) {
            targetType = undefined;
        }

        if (targetType === NotificationTargetType.CUSTOM) {

            const template = html`
                <or-mwc-input class="min-width" .type="${InputType.TEXT}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => onTargetChangedCallback(targetType!, e.detail.value)}" ?readonly="${readonly}" .value="${action.target!.custom}" ></or-mwc-input>            
            `;
            targetValueTemplate = Promise.resolve(template);

        } else {

            let targetValuesGenerator: PromiseLike<[string, string][]>;
            let label: string | undefined;
            let value: string | undefined;

            if (targetType === NotificationTargetType.USER) {
                // Get users excluding system accounts and service users
                const query: UserQuery = {
                    realmPredicate: {name: manager.displayRealm},
                    select: {basic: true},
                    serviceUsers: false,
                    attributes: [{name: {value: "systemAccount", predicateType: "string"}, negated: true}]
                }
                targetValuesGenerator = manager.rest.api.UserResource.query(query).then(
                    async (usersResponse) => {
                        const linkedLabel = i18next.t("linked");

                        // Get realm roles and add as options
                        const realm = await manager.rest.api.RealmResource.get(manager.displayRealm);
                        let realmRoleOpts: [string, string][] = realm.data.realmRoles!.filter(r => Util.realmRoleFilter(r)).map(r => ["linked-" + r.name!,  linkedLabel + ": " + i18next.t("realmRole." + r.name, Util.camelCaseToSentenceCase(r.name!.replace("_", " ").replace("-", " ")))]);
                        let values: [string, string][] = usersResponse.data.map((user) => [user.id!, user.username!]);
                        return [["linkedUsers", linkedLabel], ...realmRoleOpts, ...values.sort(Util.sortByString(user => user[1]))];
                    }
                );
                label = i18next.t("user_plural");
                const userQuery = action.target!.users!;

                if (action.target!.linkedUsers) {
                    if (userQuery && userQuery.realmRoles) {
                        if (userQuery.realmRoles.length > 1) {
                            console.warn("Rule action user target query is unsupported: " + JSON.stringify(userQuery, null, 2));
                            return;
                        }
                        value = "linked-" + userQuery.realmRoles[0].value;
                    } else {
                        value = "linkedUsers";
                    }
                } else if (userQuery) {
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
                } else {
                    console.warn("Rule action user target query is unsupported: " + JSON.stringify(userQuery, null, 2));
                    return;
                }

            } else {
                const assetQuery = baseAssetQuery ? {...baseAssetQuery} : {};
                assetQuery.orderBy = {
                    property: AssetQueryOrderBy$Property.NAME
                };

                targetValuesGenerator = manager.rest.api.AssetResource.queryAssets(assetQuery).then(
                    (response) => {
                        let values: [string, string][] = response.data.map((asset) => [asset.id!, asset.name! + " (" + asset.id! + ")"]);

                        // Add additional options for assets
                        const additionalValues: [string, string][] = [["allMatched", i18next.t("matched")]];
                        if (targetTypeMap && targetTypeMap.length > 1) {
                            targetTypeMap.forEach((typeAndTag) => {
                                if (!additionalValues.find((av) => av[0] === typeAndTag[0])) {
                                    additionalValues.push([typeAndTag[0], i18next.t("matchedOfType", {type: Util.getAssetTypeLabel(typeAndTag[0])})]);
                                }
                            });
                        }
                        return [...additionalValues, ...values];
                    }
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
                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => onTargetTypeChangedCallback(e.detail.value as NotificationTargetType)}" 
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

        // When 'cancel' is pressed, reset ACTION to the initial state (all changes get removed)
        const onModalCancel = (ev: OrRulesNotificationModalCancelEvent) => {
            if(this._initialAction && this.action.notification) {
                const newAction = structuredClone(this._initialAction);

                // Check if anything in the message has changed
                if(JSON.stringify(this.action.notification.message) !== JSON.stringify(newAction.notification?.message)) {
                    console.debug("Rolling back the notification to former state...");
                    this.action.notification.message = newAction.notification?.message;
                    this.requestUpdate('action');
                } else {
                    console.debug("Rolling back was not necessary, as no changes have been done.");
                }

            } else {
                console.warn("Could not rollback notification form.");
            }
        };

        const onModalOk = (ev: OrRulesNotificationModalOkEvent) => {
            this._initialAction = structuredClone(this.action); // update initial action for opening the modal in the future
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        };

        if (message) {
            if (messageType === "push") {
                modalTemplate = html`
                    <or-rule-notification-modal title="push-notification" .action="${this.action}"
                                                @or-rules-notification-modal-cancel="${onModalCancel}"
                                                @or-rules-notification-modal-ok="${onModalOk}">
                        <or-rule-form-push-notification .message="${message as PushNotificationMessage}"></or-rule-form-push-notification>
                    </or-rule-notification-modal>
                `;
            }
            
            else if (messageType === "email") {
                modalTemplate = html`
                    <or-rule-notification-modal title="email" .action="${this.action}"
                                                @or-rules-notification-modal-cancel="${onModalCancel}"
                                                @or-rules-notification-modal-ok="${onModalOk}">
                        <or-rule-form-email-message .message="${message as EmailNotificationMessage}"></or-rule-form-email-message>
                    </or-rule-notification-modal>
                `;
            }

            else if(messageType === "localized") {
                const notificationConfig = this.config?.notifications?.[manager.displayRealm] || this.config?.notifications?.["default"];
                const languages = [...new Set([
                    ...(notificationConfig?.languages || []),
                    ...(Object.keys((message as LocalizedNotificationMessage).languages || {}) || [])
                ])] as string[];
                const defaultLang = notificationConfig?.defaultLanguage || manager.config.defaultLanguage;
                if(languages.length === 0 && defaultLang) {
                    languages.push(defaultLang);
                }
                const defaultLangHasChanged = defaultLang !== (message as LocalizedNotificationMessage).defaultLanguage;
                const type = this.actionType === ActionType.EMAIL_LOCALIZED ? "email" : "push";
                const title = this.actionType === ActionType.EMAIL_LOCALIZED ? "email" : "push-notification";
                modalTemplate = html`
                    <or-rule-notification-modal title="${title}" .action="${this.action}"
                                                @or-rules-notification-modal-cancel="${onModalCancel}"
                                                @or-rules-notification-modal-ok="${onModalOk}">
                        <or-rule-form-localized .message="${message}" .type="${type}" .languages="${languages}" .defaultLang="${defaultLang}" 
                                                .wrongLanguage="${defaultLangHasChanged}"
                        ></or-rule-form-localized>
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

    protected _onTargetTypeChanged(targetType: NotificationTargetType) {
        if (targetType === NotificationTargetType.ASSET) {
            delete this.action.target;
        } else if (targetType === NotificationTargetType.USER) {
            this.action.target = {
                users: {
                    ids: []
                }
            };
        } else if (targetType === NotificationTargetType.CUSTOM) {
            this.action.target = {
                custom: ""
            }
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected _onTargetChanged(targetType: NotificationTargetType, value: string | undefined) {
        switch (targetType) {
            case NotificationTargetType.USER:
                if (value === "linkedUsers") {
                    this.action.target = {
                        linkedUsers: true
                    }
                } else if (value?.startsWith("linked-")) {
                    this.action.target = {
                        users: {
                            realmRoles: [
                                {
                                    predicateType: "string",
                                    value: value?.substring(7)
                                }
                            ]
                        },
                        linkedUsers: true
                    }
                } else if (value) {
                    this.action.target = {
                        users: {ids: [value]}
                    }
                }
            break;
            case NotificationTargetType.CUSTOM:
                    this.action.target = {
                        custom: value
                    }
                break;
            case NotificationTargetType.ASSET:
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
