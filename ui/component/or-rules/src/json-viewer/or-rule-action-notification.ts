import {customElement, html, LitElement, property, css, PropertyValues} from "lit-element";
import {RulesConfig, getAssetTypeFromQuery} from "../index";
import {
    RuleActionNotification,
    EmailNotificationMessageRecipient,
    EmailNotificationMessage,
    AssetDescriptor,
    AssetQueryOrderBy$Property,
    Asset,
    NotificationTargetType,
    User,
    Tenant,
    UserQuery,
    AssetQuery
} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import "./modals/or-rule-notification-modal";
import "./forms/or-rule-form-message";
import "./forms/or-rule-form-push-notification";
import "./or-rule-action-attribute";
import { i18next } from "@openremote/or-translate";
import manager from "@openremote/core";

// language=CSS
const style = css`
    :host {
        display: flex;
        align-items: center;
    }

    :host > * {
        margin-right: 10px;
    }
`;

@customElement("or-rule-action-notification")
export class OrRuleActionNotification extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;

    public readonly?: boolean;

    @property({type: Object})
    public assetDescriptors?: AssetDescriptor[];

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: Array, attribute: false})
    protected _listItems?: Asset[] | User[];

    @property({type: String})
    public type?: NotificationTargetType;

    protected render() {
        let value: string = "";
        const message = this.action.notification && this.action.notification.message ? this.action.notification.message : undefined;
        const messageType = message && message.type ? message.type : undefined;
        let valueTemplate;
        let targetTypeTemplate;
        const idOptions: [string, string] [] = [];
        let targetTypes = [[NotificationTargetType.USER, i18next.t("user_plural")], [NotificationTargetType.ASSET, i18next.t("asset_plural")], [NotificationTargetType.TENANT, i18next.t("tenant_plural")], [NotificationTargetType.CUSTOM, i18next.t("custom")]];

        const hideNotificationTargetType = this.config && this.config.controls ? this.config.controls.hideNotificationTargetType: {};
        if(hideNotificationTargetType && messageType && messageType in hideNotificationTargetType) {
            const hideTargets = hideNotificationTargetType[messageType];
            if(hideTargets) {
                targetTypes = targetTypes.filter(target => {
                    const nTarget = target[0] as NotificationTargetType;
                    return !hideTargets.includes(nTarget)
                });
            }
        }

        if(this.type === NotificationTargetType.ASSET) {
            idOptions.push(["*", i18next.t("matched")]);
            if(this._listItems) this._listItems.forEach((asset: Asset) => idOptions.push([asset.id!, asset.name!] as [string, string]));
        }

        if(this.type ===  NotificationTargetType.TENANT) {
            if(this._listItems) this._listItems.forEach((tenant: Tenant) => idOptions.push([tenant.id!, tenant.displayName!] as [string, string]));
        }
        
        if(this.type ===  NotificationTargetType.USER) {
            if(this._listItems) this._listItems.forEach((user: User) => idOptions.push([user.id!, user.username ? user.username : user.email] as [string, string]));
            
        }
        if(this.type){
            targetTypeTemplate = html`<or-input type="${InputType.SELECT}" 
                            .options="${targetTypes}"
                            value="${this.type}"
                            label="${i18next.t("recipients")}"
                            @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationType(e.detail.value)}" 
                            ?readonly="${this.readonly}"></or-input>
            `;
        } else if(targetTypes.length > 0 ){
            targetTypeTemplate = html`<or-input type="${InputType.SELECT}" 
                        .options="${targetTypes}"
                        label="${i18next.t("recipients")}"
                        @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationType(e.detail.value)}" 
                        ?readonly="${this.readonly}"></or-input>
            `;
        }

        if(idOptions.length > 0 && this._listItems) {
            valueTemplate = html`
                <or-input type="${InputType.SELECT}" 
                    .options="${idOptions}"
                    label="${this.type ? i18next.t(this.type.toLowerCase()+"_plural") : ""}"
                    .value="${this.getNotificationTargetId()}"
                    @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationValue(e.detail.value)}" 
                    ?readonly="${this.readonly}"></or-input>
            `;
        }

        if(this.type ===  NotificationTargetType.CUSTOM) {
            if(messageType === "email" && message) {
                const emailMessage:EmailNotificationMessage = message;
                value = message && emailMessage.to ? emailMessage.to.map(t => t.address).join(';') : "";
            }

            valueTemplate = html`<or-input .type="${InputType.TEXT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value)}" ?readonly="${this.readonly}" .value="${value}" ></or-input>`
        }

        let modalTemplate = html``;
        if(message) {
            if(messageType === "push") {
                modalTemplate = html`
                    <or-rule-notification-modal title="push-notification" .action="${this.action}">
                        <or-rule-form-push-notification .action="${this.action}"></or-rule-form-push-notification>
                    </or-rule-notification-modal>
                `;
            }
            
            if(messageType === "email") {
                modalTemplate = html`
                    <or-rule-notification-modal title="email" .action="${this.action}">
                        <or-rule-form-message .action="${this.action}"></or-rule-form-message>
                    </or-rule-notification-modal>
                `;
            }
        }

        return html`
            ${targetTypeTemplate}
            ${valueTemplate}
            ${modalTemplate}
        `;
    }
    
    protected _getAssetType() {
        if (!this.action.target) {
            return;
        }
        const query = this.action.target.assets ? this.action.target.assets : this.action.target.matchedAssets ? this.action.target.matchedAssets : undefined;
        return query ? getAssetTypeFromQuery(query) : undefined;
    }

    getNotificationTargetType() {
        if(this.action.target) {
            if(this.action.target.assets || this.action.target.matchedAssets) return NotificationTargetType.ASSET;
            if(this.action.target.users) return NotificationTargetType.USER;
        } else if(this.action.notification) {
            if(this.action.notification.message && this.action.notification.message.type === "email") return NotificationTargetType.CUSTOM;
        } else {
            return;
        }
        
    }


    getNotificationTargetId() {
        if(!this.action.target) return 
        
        switch (this.type) {
            case NotificationTargetType.ASSET:
                if( this.action.target.matchedAssets) {
                    return "*";
                }

                const assets = this.action.target.assets
                if(assets && assets.ids) {
                    return assets.ids[0]
                }
                break;
            case NotificationTargetType.USER:
                const users = this.action.target.users
                if(users && users.ids) {
                    return users.ids[0]
                }
                break;
            case NotificationTargetType.TENANT:
                break;
            case NotificationTargetType.CUSTOM:
                break;
        }
    }

    protected clearMessageTo() {
        if(this.action.notification && this.action.notification.message){
            const message:EmailNotificationMessage = this.action.notification.message
            delete message.to
        } 
    }
    protected setActionNotificationType(type: NotificationTargetType) {
        this.type = type;
        this.loadTypeData(type);
    }

    protected setActionNotificationValue(value: string) {
        this.clearMessageTo();
        switch (this.type) {
            case NotificationTargetType.ASSET:
                if (value === "*") {
                    const assetType = this._getAssetType();
                    if(this.action.target){
                        this.action.target.assets = undefined;
                    }
                    this.action.target = {
                        matchedAssets: {
                            types: [
                                {
                                    predicateType: "string",
                                    value: assetType
                                }
                            ]
                        }
                    };
                } else {
                    const assets:AssetQuery = {ids: [value]}
                    this.action.target = {assets: assets}
                }
                break;
            case NotificationTargetType.USER:
                const users:UserQuery = {ids: [value]}
                this.action.target = {users: users}
                break;
            case NotificationTargetType.TENANT:
                break;
            case NotificationTargetType.CUSTOM:
                break;
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        if(_changedProperties.has('action')) {
            const type = this.getNotificationTargetType();
            if(type) {
                this.setActionNotificationType(type);
            }
        }
    }

    protected loadTypeData(type: string | undefined) {
        if(this.action && this.action.notification) {
            switch (type) {
                case NotificationTargetType.ASSET:

                    const messageType = this.action.notification.message ? this.action.notification.message.type : undefined
                    if(messageType === "push") {
                        const query = {
                            types: [
                            {
                                predicateType: "string",
                                value: "urn:openremote:asset:console"
                            }
                        ]};
                        this.loadAssets(query)
                    } else {
                        const query = {
                            attributes: {
                                items: [
                                   {
                                      name: { "predicateType": "string", "value": "email" },
                                      value: { "predicateType": "value-not-empty" } 
                                   }
                                ]
                              }
                        }
                        this.loadAssets(query)
                    }
                    break;
                case NotificationTargetType.USER:
                    this.loadUsers()
                    break;
                case NotificationTargetType.TENANT:
                    this.loadTenants()
                    break;
                case NotificationTargetType.CUSTOM:
                    break;
                default:
                    break;
            }
        }
    }

    protected setActionNotificationName(emails: string | undefined) {
        delete this.action.target;
        if(emails && this.action.notification && this.action.notification.message){

            const arrayOfEmails = emails.split(';');
            const message:EmailNotificationMessage = this.action.notification.message;
            message.to = [];
            arrayOfEmails.forEach(email => {
                const messageRecipient:EmailNotificationMessageRecipient = {
                        address: email,
                        name: email
                };

                if(message && message.to){
                    message.to.push(messageRecipient);
                }
            });

            this.action.notification.message = message;
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    
    protected loadTenants() {
        manager.rest.api.TenantResource.getAll().then((response) => this._listItems = response.data);
    }

    protected loadUsers() {
        manager.rest.api.UserResource.getAll(manager.displayRealm).then((response) => this._listItems = response.data).then(() => {
            this.requestUpdate();
        });
    }

    protected loadAssets(query: object) {
        const baseQuery = {
            select: {
                excludeAttributeTimestamp: true,
                excludeAttributeValue: true,
                excludeParentInfo: true,
                excludePath: true
            },
            orderBy: {
                property: AssetQueryOrderBy$Property.NAME
            }
        };
        const assetQuery = {...query, ...baseQuery};

        manager.rest.api.AssetResource.queryAssets(assetQuery).then((response) => this._listItems = response.data);
    }
}