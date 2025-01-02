import { LitElement, html, css, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { OrMwcInput, InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import manager, { DefaultColor3 } from "@openremote/core";
import { User, Asset } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { live } from "lit/directives/live.js";

export interface NotificationFormData {
    name: string;
    title: string;
    body: string;
    priority: string;
    targetType: string;
    target: string;
}

@customElement("notification-form")
export class NotificationForm extends LitElement {
    static styles = css`
        :host {
            display: block;
            width: 100%;
        }
        
        .form-container {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .select-container {
        display: flex;
        flex-direction: column;
        gap: 4px;

        }

        select {
            padding: 8px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 16px;
            background-color: white;
        }

        select:disabled {
            background-color: #f5f5f5;
            cursor: not-allowed;
        }

        label {
            font-size: 14px;
            color: #666;
        }

        /* Grid styles */

        .formGridContainer {
            display: grid; 
            grid-template-columns: 2fr 1fr; 
            grid-template-rows: 1fr 1fr 1fr 1fr; 
            gap: 8px 8px;  
            grid-template-areas: 
                "targetContainer actionButtonContainer"
                "messageContentContainer actionButtonContainer"
                "messageContentContainer propContainer"
                "messageContentContainer propContainer"; 
            }

        .targetContainer { grid-area: targetContainer; }
        .messageContentContainer { grid-area: messageContentContainer; }
        .actionButtonContainer { grid-area: actionButtonContainer; }
        .propContainer { grid-area: propContainer; }

        .panel-title {
            text-transform: uppercase;
            font-weight: bolder;
            color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
            line-height: 1em;
            /*margin-bottom: 10px;*/
            margin-top: 0;
            flex: 0 0 auto;
            letter-spacing: 0.025em;
            display: flex;
            align-items: center;
            min-height: 36px;
            
            }
    `;

    constructor() {
        super();
        this._selectedTargetType = "ASSET";
    }
    
    connectedCallback() {
        super.connectedCallback();
        console.log("NotificationForm connected");
    }

    async firstUpdated() {
        // load assets first since it's default
        await this._loadAssets();

        // set initial target options
        this._targetOptions = (this._assets || []).map(asset => ({
            text: asset.name,
            value: asset.id
        }));

        await Promise.all([
            this._loadUsers(),
            this._loadRealms()
        ]);
    }


    @state()
    protected _users?: User[];

    @state()
    protected _assets?: Asset[];

    @state()
    protected _realms?: string[];

    @state()
    protected _selectedTargetType?: string;

    @state()
    protected _targetOptions: { text: string, value: string }[] = [];

    @state()
    protected _selectedTarget?: string;

    @property({ type: Boolean })
    public disabled = false;

    protected async _loadUsers(): Promise<User[]> {
        try {
            const response = await manager.rest.api.UserResource.query({
                realmPredicate: { name: manager.displayRealm }
            });
            this._users = response.data.filter(user => user.enabled && !user.serviceAccount);
            return this._users;
        } catch (error) {
            console.error("Failed to load users:", error);
            showSnackbar(undefined, i18next.t("Loading users failed"));
            return [];
        }
    }

    protected async _loadAssets(): Promise<Asset[]> {
        try {
            const response = await manager.rest.api.AssetResource.queryAssets({
                realm: { name: manager.displayRealm }
            });
            this._assets = response.data;
            return response.data;
        } catch (error) {
            console.error("Failed to load assets:", error);
            showSnackbar(undefined, i18next.t("Loading assets failed"));
            return [];
        }
    }

    protected async _loadRealms(): Promise<string[]> {
        try {
            const response = await manager.rest.api.RealmResource.getAccessible();
            this._realms = response.data.map(realm => realm.name);
            return this._realms;
        } catch (error) {
            console.error("Failed to load realms:", error);
            showSnackbar(undefined, i18next.t("Loading realms failed"));
            return [];
        }
    }

    protected async _onTargetSelected(e: OrInputChangedEvent) {
        if (!e.detail || !e.detail.value) return;

        this._selectedTarget = e.detail.value;
        console.log('Selected target updated:', this._selectedTarget);
        await this.requestUpdate();
    }
    
    protected async _onTargetTypeChanged(e: OrInputChangedEvent) {
        const type = e.detail.value;
        if (!type) return;

        this._selectedTargetType = type;
        this._targetOptions = [];
        
        switch (type) {
            case "USER":
                if (!this._users) {
                    await this._loadUsers();
                }
                this._targetOptions = (this._users || []).map(user => ({
                    text: user.username,
                    value: user.id
                }));
                break;
                
            case "ASSET":
                if (!this._assets) {
                    await this._loadAssets();
                }
                this._targetOptions = (this._assets || []).map(asset => ({
                    text: asset.name,
                    value: asset.id
                }));
                if (this._targetOptions.length > 0) {
                    this._selectedTarget = this._targetOptions[0].value;
                }
                break;

            case "REALM":
                if (!this._realms) {
                    await this._loadRealms();
                }
                this._targetOptions = (this._realms || []).map(realm => ({
                    text: realm,
                    value: realm
                }));
                break;
        }
        
        await this.requestUpdate();
    }

    public getFormData(): NotificationFormData | null {
        const form = this.shadowRoot!;
        const inputs = {
            name: form.querySelector<OrMwcInput>("#notificationName"),
            title: form.querySelector<OrMwcInput>("#notificationTitle"),
            body: form.querySelector<OrMwcInput>("#notificationBody"),
            priority: form.querySelector<OrMwcInput>("#notificationPriority"),
            targetType: form.querySelector<OrMwcInput>("#targetType"),
            target: form.querySelector<OrMwcInput>("#target")
        };

        if (!inputs.name?.value || !inputs.title?.value || !inputs.body?.value) {
            return null;
        }

        return {
            name: inputs.name.value,
            title: inputs.title.value,
            body: inputs.body.value,
            priority: inputs.priority?.value || "NORMAL",
            targetType: inputs.targetType?.value,
            target: inputs.target?.value
        };
    }

    protected _getField(label, inputType, isDisabled, id, rows, isRequired) {
        return html`
            <or-mwc-input 
                    label="${i18next.t(label)}"
                    type="${inputType}"
                    rows="${rows}"
                    style="width: 100%;"
                    ?disabled="${isDisabled}"
                    ?required="${isRequired}"
                    id="${id}">
                </or-mwc-input>
        `;
    }

    protected render() {
        console.log('Render state:', {
            targetOptions: this._targetOptions,
            selectedTarget: this._selectedTarget,
            mappedOptions: this._targetOptions.map(o => [o.value, o.text])
        });
        const readonly = true;
        return html`
            <div class="form-container">

                <div class="formGridContainer">
                    <div class="targetContainer">
                        <div class="section-title">${i18next.t("Target")}</div>
                         <or-mwc-input 
                            label="${i18next.t("Target type")}"
                            type="${InputType.SELECT}"
                            .options="${["USER", "ASSET", "REALM"]}"
                            ?disabled="${this.disabled}"
                            required
                            style="width: 100%;"
                            id="targetType"
                            .value="${this._selectedTargetType}"
                            @or-mwc-input-changed="${this._onTargetTypeChanged}">
                        </or-mwc-input>

                        <or-mwc-input 
                            label="${i18next.t("Target")}"
                            type="${InputType.SELECT}"
                            
                            style="width: 100%;"
                            ?disabled="${this.disabled || !this._targetOptions}"
                            required
                            id="target"
                            .value="${this._selectedTarget}"
                            .options="${this._targetOptions.map(o => [o.value, o.text])}"
                            .searchProvider="${(search?: string) => {
                            // Filter options based on search text
                            if (!search) return Promise.resolve(this._targetOptions.map(o => [o.value, o.text]));
                            return Promise.resolve(
                                this._targetOptions
                                    .filter(o => o.text.toLowerCase().includes(search.toLowerCase()))
                                    .map(o => [o.value, o.text])
                            );
                        }}"
                        searchLabel="Search targets"
                            @or-mwc-input-changed="${this._onTargetSelected}">
                        </or-mwc-input>

                    </div>
               
                <div class="messageContentContainer">
                <div class="section-title">${i18next.t("Content")}</div>
                    ${this._getField("Name", InputType.TEXT, this.disabled, "notificationName", 1, true)}
                    ${this._getField("Title", InputType.TEXT, this.disabled, "notificationTitle", 1, true)}
                    ${this._getField("Body", InputType.TEXTAREA, this.disabled, "notificationBody", 4, true)}
                </div>
                  
                <div class="actionButtonContainer">
                <div class="section-title">${i18next.t("Actions")}</div>
                    ${this._getField("Website to be opened", InputType.TEXT, this.disabled, "notificationName", 1, false)}
                    ${this._getField("Text for action button", InputType.TEXT, this.disabled, "notificationTitle", 1, false)}
                    ${this._getField("Text for decline button", InputType.TEXT, this.disabled, "notificationTitle", 1, false)}
                </div>
               
                <div class="propContainer">
                <div class="section-title">${i18next.t("Props")}</div>
                    <or-mwc-input 
                        label="${i18next.t("Priority")}"
                        type="${InputType.SELECT}"
                        .options="${["NORMAL", "HIGH"]}"
                        ?disabled="${this.disabled}"
                        required
                        style="width: 100%;"
                        id="notificationPriority">
                    </or-mwc-input> 
                </div>
                </div>
            </div>
        `;
    }
}