import {customElement, html, LitElement, property, PropertyValues, query, TemplateResult, css} from "lit-element";
import {JsonRule, RuleCondition, RulesetLang, Tenant, TenantRuleset} from "@openremote/model";
import "@openremote/or-translate";
import manager, {EventCallback, OREvent} from "@openremote/core";
import moment from "moment";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {style as OrAssetTreeStyle} from "@openremote/or-asset-tree";
import {
    OrRulesEditorRequestAddEvent,
    OrRulesEditorRequestCopyEvent,
    OrRulesEditorRequestDeleteEvent,
    OrRulesEditorRequestSelectEvent,
    OrRulesEditorSelectionChangedEvent,
    RequestEventDetail,
    RulesetNode
} from "./index";

// language=CSS
const style = css`
    :host {
        overflow: auto;
        flex-grow: 1;
        font-size: var(--internal-or-rules-editor-list-text-size);
    }
    
    #wrapper[data-disabled] {
        opacity: 0.3;
        pointer-events: none;
    }
    
    .d-flex {
        display: -webkit-box;
        display: -moz-box;
        display: -ms-flexbox;
        display: -webkit-flex;
        display: flex;
    }

    .flex {
        -webkit-box-flex: 1;
        -moz-box-flex: 1;
        -webkit-flex: 1;
        -ms-flex: 1;
        flex: 1;
    }

    .list-title {
        display: block;
        padding: 30px 30px 5px 20px;
        text-transform: uppercase;
        /*color: var(, #808080);*/
        font-size:14px;
        font-weight: bold;
        text-align: left;
    }

    .list-item {
        text-decoration: none;
        padding: 13px 15px;
        border-left: 5px solid transparent;
        cursor: pointer;
        transition: all 200ms ease-in;
        opacity: 0.8;
    }

    .list-item:hover {
        border-left-color: var(--internal-or-rules-editor-list-selected-color);
        background-color: var(--internal-or-rules-editor-list-selected-color);
    }

    .list-item[selected] {
        border-left-color: var(--internal-or-rules-editor-button-color);
        background-color: var(--internal-or-rules-editor-list-selected-color);
        opacity: 1;
    }

    .list-item > span {
        width: 8px;
        height: 8px;
        border-radius: 8px;
        margin: 6px 10px 0 0;
    }

    .bg-green {
        background-color: #28b328;
    }

    .bg-red {
        background-color: red
    }
    
    .bg-blue {
        background-color: #3e92dc;
    }

    .bg-grey {
        background-color: #b7b7b7;
    }
`;

@customElement("or-rule-list")
export class OrRulesList extends LitElement {

    @property({type: String})
    public realm?: string;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public multiSelect: boolean = true;

    @property({type: Array})
    public selectedIds?: number[];

    @property({type: String})
    public sortBy?: string;

    @property({attribute: false})
    protected _realms?: Tenant[];

    @property({attribute: false})
    protected _nodes?: RulesetNode[];

    @property()
    protected _showLoading: boolean = true;

    @query("#sort-menu")
    protected _sortMenu!: HTMLDivElement;

    protected _selectedNodes: RulesetNode[] = [];
    protected _initCallback?: EventCallback;
    protected _ready = false;

    static get styles() {
        return [
            OrAssetTreeStyle,
            style
        ];
    }

    public refresh() {
        this._nodes = undefined;
    }

    protected _onReady() {
        this._ready = true;
        this._loadRealms();
        this._loadRulesets();
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);

        if (!manager.ready) {
            // Defer until openremote is initialised
            this._initCallback = (initEvent: OREvent) => {
                if (initEvent === OREvent.READY) {
                    this._onReady();
                    manager.removeListener(this._initCallback!);
                }
            };
            manager.addListener(this._initCallback);
        } else {
            this._onReady();
        }

        this.addEventListener("click", (evt) => {
            if (this._sortMenu.hasAttribute("data-visible") && !this._sortMenu.contains(evt.target as Node)) {
                this._sortMenu.toggleAttribute("data-visible");
            }
        });
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        const result = super.shouldUpdate(_changedProperties);

        if (manager.ready) {
            if (_changedProperties.has("realm")) {
                this._nodes = undefined;
            }

            if (!this._nodes) {
                this._loadRulesets();
                return true;
            }

            if (_changedProperties.has("selectedIds")) {
                this._updateSelectedNodes();
            }

            if (_changedProperties.has("sortBy")) {
                OrRulesList._updateSort(this._nodes!, this._getSortFunction());
            }
        }

        return result;
    }

    protected render() {

        return html`
            <div id="wrapper" ?data-disabled="${this.disabled}">
                <div id="header">
                    <div id="title-container">
                        <or-translate id="title" value="rule_plural"></or-translate>
                        ${manager.isSuperUser() ? html `<or-input id="realm-picker" type="${InputType.SELECT}" .value="${this._getRealm()}" .options="${this._realms ? this._realms.map((tenant) => [tenant.realm, tenant.displayName]) : []}" @or-input-changed="${(evt: OrInputChangedEvent) => this._onRealmChanged(evt)}"></or-input>` : ``}
                    </div>
        
                    <div id="header-btns">
                    
                        <button style="display:none;" ?hidden="${!manager.hasRole("write:rules") || !this.selectedIds || this.selectedIds.length === 0}" @click="${() => this._onCopyClicked()}"><or-icon icon="content-copy"></or-icon></button>
                        <button ?hidden="${!manager.hasRole("write:rules") || !this.selectedIds || this.selectedIds.length === 0}" @click="${() => this._onDeleteClicked()}"><or-icon icon="delete"></or-icon></button>
                        <button ?hidden="${!manager.hasRole("write:rules")}" @click="${() => this._onAddClicked()}"><or-icon icon="plus"></or-icon></button>
                        <button hidden @click="${() => this._onSearchClicked()}"><or-icon icon="magnify"></or-icon></button>
                        <button @click="${() => this._onSortClicked()}"><or-icon icon="sort-variant"></or-icon></button>
                        <div class="modal-container">
                            <div class="modal" id="sort-menu">
                                <div class="modal-content">
                                    <ul>
                                        <li @click="${() => this.sortBy = "name"}" ?data-selected="${!this.sortBy || this.sortBy === "name"}"><or-icon icon="check"></or-icon><or-translate value="name"></or-translate></li>
                                        <li @click="${() => this.sortBy = "created"}" ?data-selected="${this.sortBy === "created"}"><or-icon icon="check"></or-icon><or-translate value="creationDate"></or-translate></li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
        
                ${!this._nodes || this._showLoading
                    ? html`
                        <span id="loading">LOADING</span>` 
                    : html`
                        <div id="list-container">
                            <ol id="list">
                                ${this._nodes.map((treeNode) => this._nodeTemplate(treeNode))}
                            </ol>
                        </div>
                    `
                }
        
                <div id="footer">
                
                </div>
            </div>
        `;
    }

    protected _nodeTemplate(node: RulesetNode): TemplateResult | string {
        return html`
            <li ?data-selected="${node.selected}" @click="${(evt: MouseEvent) => this._onNodeClicked(evt, node)}">
                <div class="node-container">
                    <div class="node-name">
                        <span class="${OrRulesList._getNodeStatusClasses(node.ruleset)}"></span>
                        <div class="flex">
                            <span>${node.ruleset.name}</span>
                        </div>
                    </div>
                </div>
            </li>
        `;
    }

    protected static _getNodeStatusClasses(ruleset: TenantRuleset): string {
        let status = ruleset.enabled ? "bg-green" : "bg-red";

        if(ruleset.rules && ruleset.enabled) {
            const rule: JsonRule = JSON.parse(ruleset.rules).rules[0];

            // HACK/WIP: the status of a rule should be better thought over see issue #95
            // currently only checks the date of the first whenCondition
            if(rule && rule.when && rule.when.items && rule.when.items.length > 0) {
                const ruleCondition: RuleCondition = rule.when.items[0];
                if(ruleCondition.datetime) {
                    const today = moment();
                    const startDate = ruleCondition.datetime.value;
                    const endDate = ruleCondition.datetime.rangeValue;

                    if (today.diff(startDate) < 0) {
                        // before startDate, show blue
                        status = "bg-blue";
                    } else if (today.diff(endDate) > 0) {
                        // after endDate, show grey
                        status = "bg-grey";
                    }
                }
            }
        }

        return status;
    }

    protected _loadRealms() {
        if (manager.isSuperUser()) {
            manager.rest.api.TenantResource.getAll().then((response) => {
                this._realms = response.data;
            });
        }
    }

    protected _updateSelectedNodes() {
        const actuallySelectedIds: number[] = [];
        const selectedNodes: RulesetNode[] = [];
        if (this._nodes) {
            this._nodes.forEach((node) => {
                if (this.selectedIds && this.selectedIds.indexOf(node.ruleset!.id!) >= 0) {
                    actuallySelectedIds.push(node.ruleset!.id!);
                    selectedNodes.push(node);
                    node.selected = true;
                } else {
                    node.selected = false;
                }
            });
        }

        this.selectedIds = actuallySelectedIds;
        this._selectedNodes = selectedNodes;
        this.dispatchEvent(new OrRulesEditorSelectionChangedEvent(this._selectedNodes.map((node) => node.ruleset)));
    }

    protected static _updateSort(nodes: RulesetNode[], sortFunction: (a: RulesetNode, b: RulesetNode) => number) {
        if (!nodes) {
            return;
        }

        nodes.sort(sortFunction);
    }

    protected _onNodeClicked(evt: MouseEvent, node: RulesetNode) {
        if (evt.defaultPrevented) {
            return;
        }

        evt.preventDefault();

        let selectRulesets = this._selectedNodes.map((node) => node.ruleset);
        const index = selectRulesets.findIndex((ruleset) => ruleset.id === node.ruleset.id);
        let select = true;
        let deselectOthers = true;

        if (this.multiSelect) {
            if (evt.ctrlKey || evt.metaKey) {
                deselectOthers = false;
                if (index >= 0 && selectRulesets.length > 1) {
                    select = false;
                }
            }
        }

        if (deselectOthers) {
            selectRulesets = [node.ruleset];
        } else if (select) {
            if (index < 0) {
                selectRulesets.push(node.ruleset);
            }
        } else {
            if (index >= 0) {
                selectRulesets.splice(index, 1);
                selectRulesets = [...selectRulesets];
            }
        }

        this._doRequest(new OrRulesEditorRequestSelectEvent(selectRulesets), (detail) => {
            this.selectedIds = detail.map((ruleset) => ruleset.id!);
        });
    }

    protected _onCopyClicked() {
        if (this._selectedNodes.length == 1) {
            this.dispatchEvent(new OrRulesEditorRequestCopyEvent(this._selectedNodes[0].ruleset));
        }
    }

    protected _onDeleteClicked() {
        if (this._selectedNodes.length > 0) {
            this.dispatchEvent(new OrRulesEditorRequestDeleteEvent(this._selectedNodes.map((node) => node.ruleset)));
        }
    }

    protected _onAddClicked() {
        this.dispatchEvent(new OrRulesEditorRequestAddEvent());
    }

    protected _onSearchClicked() {

    }

    protected _onSortClicked() {
        // Do open on next task to prevent click handler closing it immediately
        if (!this._sortMenu.hasAttribute("data-visible")) {
            window.setTimeout(() => {
                this._sortMenu.toggleAttribute("data-visible");
            });
        }
    }

    protected _getSortFunction(): (a: RulesetNode, b: RulesetNode) => number {

        const nameSort = (a: RulesetNode, b: RulesetNode) => { return a.ruleset!.name! < b.ruleset!.name! ? -1 : a.ruleset!.name! > b.ruleset!.name! ? 1 : 0 };

        if (this.sortBy === "created") {
            return (a, b) => { return a.ruleset!.createdOn! < b.ruleset!.createdOn! ? -1 : a.ruleset!.createdOn! > b.ruleset!.createdOn! ? 1 : nameSort(a, b) };
        }

        return nameSort;
    }

    protected _getRealm(): string | undefined {
        if (manager.isSuperUser() && this.realm) {
            return this.realm;
        }

        return manager.getRealm();
    }

    protected _onRealmChanged(evt: OrInputChangedEvent) {
        this.realm = evt.detail.value;
    }

    protected _doRequest<T>(event: CustomEvent<RequestEventDetail<T>>, handler: (detail: T) => void) {
        this.dispatchEvent(event);
        window.setTimeout(() => {
            if (event.detail.allow) {
                handler(event.detail.detail);
            }
        })
    }

    protected _loadRulesets() {

        const sortFunction = this._getSortFunction();

        manager.rest.api.RulesResource.getTenantRulesets(manager.config.realm, {
            language: RulesetLang.JSON,
            fullyPopulate: true
        }).then((response: any) => {
            if (response && response.data) {
                this._buildTreeNodes(response.data, sortFunction);
            }
        }).catch((reason: any) => {
            console.error("Error: " + reason);
        });
    }

    protected _buildTreeNodes(rulesets: TenantRuleset[], sortFunction: (a: RulesetNode, b: RulesetNode) => number) {
        if (!rulesets || rulesets.length === 0) {
            this._nodes = [];
        } else {

            let nodes = rulesets.map((ruleset) => {
                return {
                    ruleset: ruleset
                } as RulesetNode;
            });

            nodes.sort(sortFunction);

            this._nodes = nodes;
        }
        if (this.selectedIds && this.selectedIds.length > 0) {
            this._updateSelectedNodes();
        }
        this._showLoading = false;
    }
}