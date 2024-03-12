import { css, html, LitElement, TemplateResult } from "lit";
import { customElement, property } from "lit/decorators.js";
import { ActionType, RulesConfig } from "../index";
import {
    JsonRule,
    RuleActionAlarm,
    User,
    UserQuery,
} from "@openremote/model";
import "./modals/or-rule-alarm-modal";
import "./forms/or-rule-form-alarm";
import manager from "@openremote/core";


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

@customElement("or-rule-action-alarm")
export class OrRuleActionAlarm extends LitElement {

    static get styles() {
        return style;
    }

    @property({ type: Object, attribute: false })
    public rule!: JsonRule;

    @property({ type: Object, attribute: false })
    public action!: RuleActionAlarm;

    @property({ type: String, attribute: false })
    public actionType!: ActionType;

    public readonly?: boolean;

    @property({ type: Object })
    public config?: RulesConfig;

    protected _loadedUsers: User[] = [];

    async connectedCallback(): Promise<void> {
        await this.loadUsers();
        super.connectedCallback();
    }

    protected async loadUsers() {
        const usersResponse = await manager.rest.api.UserResource.query({
            realmPredicate: { name: manager.displayRealm },
        } as UserQuery);

        if (usersResponse.status !== 200) {
            return;
        }

        this._loadedUsers = usersResponse.data.filter((user) => user.enabled && !user.serviceAccount);
    }


    protected render() {
        if (!this.action.alarm || !this.action.alarm.title) {
            return html``;
        }

        const alarm = this.action.alarm;

        let modalTemplate: TemplateResult | string = ``;

        if (alarm) {
            modalTemplate = html`
                        <div style="display: flex;">
                            <or-rule-alarm-modal title="alarm." .action="${this.action}">
                                <or-rule-form-alarm .users="${this._loadedUsers}" .action="${this.action}"></or-rule-form-alarm>
                            </or-rule-alarm-modal>
                        </div>
                    `;
        }

        return html`${modalTemplate}`;
    }
}
