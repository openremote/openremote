import { LitElement, html, css } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { OrMwcInput, InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import manager from "@openremote/core";
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
        console.log('Target selection event:', e.detail);
        console.log('Previous selected target:', this._selectedTarget);
        const select = e.detail.value;
        console.log('New selected value:', select);
        if (!select) return;

        this._selectedTarget = select;
        console.log('Update selected target:', this._selectedTarget);
        
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

    protected render() {
        console.log('Render state:', {
            targetOptions: this._targetOptions,
            selectedTarget: this._selectedTarget,
            mappedOptions: this._targetOptions.map(o => [o.value, o.text])
        });
        return html`
            <div class="form-container">
                <or-mwc-input 
                    label="${i18next.t("Name")}"
                    type="${InputType.TEXT}"
                    style="width: 100%;"
                    ?disabled="${this.disabled}"
                    required
                    id="notificationName">
                </or-mwc-input>
                
                <or-mwc-input 
                    label="${i18next.t("Title")}"
                    type="${InputType.TEXT}"
                    style="width: 100%;"
                    ?disabled="${this.disabled}"
                    required
                    id="notificationTitle">
                </or-mwc-input>
                
                <or-mwc-input 
                    label="${i18next.t("Body")}"
                    type="${InputType.TEXTAREA}"
                    rows="4"
                    style="width: 100%;"
                    ?disabled="${this.disabled}"
                    required
                    id="notificationBody">
                </or-mwc-input>

                <or-mwc-input 
                    label="${i18next.t("Priority")}"
                    type="${InputType.SELECT}"
                    .options="${["NORMAL", "HIGH"]}"
                    ?disabled="${this.disabled}"
                    required
                    style="width: 100%;"
                    id="notificationPriority">
                </or-mwc-input>

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

                <!-- <or-mwc-input 
                    label="${i18next.t("Target")}"
                    type="${InputType.SELECT}"
                    style="width: 100%;"
                    ?disabled="${this.disabled || !this._targetOptions}"
                    required
                    id="target"
                    .value="${this._selectedTarget ?? ''}"
                    .options="${this._targetOptions.map(o => [o.value, o.text])}"
                    @or-mwc-input-changed="${this._onTargetSelected}">
                </or-mwc-input> -->

                <div class="select-container">
                    <label for="target">${i18next.t("Target")}</label>
                    <select
                        id="target"
                        style="width: 100%;"
                        ?disabled="${this.disabled || !this._targetOptions}"
                        required
                        @change="${(e: Event) => {
                            const select = e.target as HTMLSelectElement;
                            this._onTargetSelected({
                                detail: { value: select.value }
                            } as OrInputChangedEvent);
                        }}">
                        <option value="" disabled selected=${!this._selectedTarget}>
                            ${i18next.t("Select a target")}
                        </option>
                        ${this._targetOptions.map(option => html`
                            <option 
                                value="${option.value}"
                                ?selected="${this._selectedTarget === option.value}">
                                ${option.text}
                            </option>
                        `)}
                    </select>
                </div>  
            </div>
        `;
    }
}