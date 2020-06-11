import {css, customElement, html, LitElement, property, PropertyValues, TemplateResult} from "lit-element";
import {CalendarEvent, ClientRole, RulesetLang, TenantRuleset} from "@openremote/model";
import "@openremote/or-translate";
import manager, {EventCallback, OREvent} from "@openremote/core";
import "@openremote/or-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {style as OrAssetTreeStyle} from "@openremote/or-asset-tree";
import {
    OrRulesRequestAddEvent,
    OrRulesRequestCopyEvent,
    OrRulesRequestDeleteEvent,
    OrRulesRequestSelectEvent,
    OrRulesSelectionChangedEvent,
    RequestEventDetail,
    RulesConfig,
    RulesetNode
} from "./index";
import "@openremote/or-mwc-components/dist/or-mwc-menu";
import {MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import {translate} from "@openremote/or-translate";
import i18next from "i18next";
import {getContentWithMenuTemplate} from "../../or-mwc-components/dist/or-mwc-menu";

// language=CSS
const style = css`
    :host {
        flex-grow: 1;
        font-size: var(--internal-or-rules-list-text-size);
        
        /*Override asset tree styles*/
        --internal-or-asset-tree-header-height: var(--internal-or-rules-list-header-height);
    }
    
    #wrapper {
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 100%;
    }

    #wrapper[data-disabled] {
        opacity: 0.3;
        pointer-events: none;
    }

    .node-container {
        align-items: center;
        padding-left: 10px;
    }
    
    .header-ruleset-type {
        display: flex;
        align-items: center;
        height: var(--internal-or-asset-tree-header-height);
        line-height: var(--internal-or-asset-tree-header-height);
        color: var(--internal-or-asset-tree-header-text-color);
    }
    
    .header-ruleset-type p {
        margin: 0 15px;
    }

    .node-status {
        width: 10px;
        height: 10px;
        border-radius: 5px;
        margin-right: 10px;
        display: none;
    }

    .node-language{
        padding-left: 10px;
        opacity: 50%;
    }

    .bg-green {
        background-color: #28b328;
    }

    .bg-red {
        background-color: red;
    }
    
    .bg-blue {
        background-color: #3e92dc;
    }

    .bg-grey {
        background-color: #b7b7b7;
    }
`;

@customElement("or-rule-list")
export class OrRuleList extends translate(i18next)(LitElement) {

    public static DEFAULT_ALLOWED_LANGUAGES = [RulesetLang.JSON, RulesetLang.GROOVY, RulesetLang.JAVASCRIPT, RulesetLang.FLOW];

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public multiSelect: boolean = true;

    @property({type: Array})
    public selectedIds?: number[];

    @property({type: String})
    public sortBy?: string;

    @property({type: String})
    public language?: RulesetLang;

    @property({attribute: false})
    protected _nodes?: RulesetNode[];

    @property({attribute: false})
    protected _showLoading: boolean = true;

    @property({type: Boolean})
    protected _globalRulesets: boolean = false;

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

    public disconnectedCallback() {
        super.disconnectedCallback();
        manager.removeListener(this.onManagerEvent);
    }

    protected get _allowedLanguages(): RulesetLang[] | undefined {
        const languages = this.config && this.config.controls && this.config.controls.allowedLanguages ? [...this.config.controls.allowedLanguages] : OrRuleList.DEFAULT_ALLOWED_LANGUAGES;
        if(!manager.isSuperUser() || !this._globalRulesets) {
            const index = languages.indexOf(RulesetLang.GROOVY);
            if(index > 0) languages.splice(index)
        }
        return languages;
    }

    protected _updateLanguage() {
        const rulesetLangs = this._allowedLanguages;
        if (!rulesetLangs || rulesetLangs.length === 0) {
            this.language = undefined;
        } else if (!this.language || rulesetLangs.indexOf(this.language) < 0) {
            this.language = rulesetLangs[0];
        }
    }

    protected _onReady() {
        this._ready = true;
        this._loadRulesets();
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        manager.addListener(this.onManagerEvent);
        if (manager.ready) {
            this._onReady();
        }
    }

    protected onManagerEvent = (event: OREvent) => {
        switch (event) {
            case OREvent.READY:
                if (!manager.ready) {
                    this._onReady();
                }
                break;
            case OREvent.DISPLAY_REALM_CHANGED:
                this._nodes = undefined;
                break;
        }
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        const result = super.shouldUpdate(_changedProperties);

        if (!this.sortBy) {
            this.sortBy = "name";
        }

        if (_changedProperties.has("language")) {
            this._updateLanguage();
        }

        if (_changedProperties.has("_globalRulesets")) {
            this._updateLanguage(); 
            this.refresh();
        }
        
        if (manager.ready) {
            if (!this._nodes) {
                this._loadRulesets();
                return true;
            }

            if (_changedProperties.has("selectedIds")) {
                let changed = true;
                const oldValue = _changedProperties.get("selectedIds") as number[];
                if (this.selectedIds && oldValue && this.selectedIds.length === oldValue.length) {
                    changed = !!this.selectedIds.find((oldSelected) => oldValue.indexOf(oldSelected) < 0);
                }
                if (changed) {
                    this._updateSelectedNodes();
                }
            }

            if (_changedProperties.has("sortBy")) {
                OrRuleList._updateSort(this._nodes!, this._getSortFunction());
            }
        }

        return result;
    }

    protected render() {
        if (!this.language) {
            this._updateLanguage();
        }

        const allowedLanguages = this._allowedLanguages;
        const sortOptions = ["name", "createdOn"]
        if (allowedLanguages && allowedLanguages.length > 1) sortOptions.push("lang")
        
        let addTemplate: TemplateResult | string = ``;

        if (!this._isReadonly()) {
            if (allowedLanguages && allowedLanguages.length > 1) {
                addTemplate = getContentWithMenuTemplate(
                    html`<or-input type="${InputType.BUTTON}" icon="plus"></or-input>`,
                    allowedLanguages.map((l) => {
                        return {value: l, text: i18next.t(l)} as MenuItem;
                    }),
                    this.language,
                    (v) => this._onAddClicked(v as RulesetLang));
            } else {
                addTemplate = html`<or-input type="${InputType.BUTTON}" icon="plus" @click="${() => this._onAddClicked(this.language!)}"></or-input>`;
            }
        }
        return html`
            <div id="wrapper" ?data-disabled="${this.disabled}">
                ${manager.isSuperUser() ? html`
                    <div class="header-ruleset-type bg-grey">
                        <p>${i18next.t("realmRules")}</p>
                        
                        <div style="flex: 1 1 0; display: flex;">
                            <or-input style="margin: auto;" type="${InputType.SWITCH}" @or-input-changed="${(evt: OrInputChangedEvent) => this._globalRulesets = evt.detail.value}"></or-input>
                        </div>

                        <p>${i18next.t("globalRules")}</p>
                    </div>
                ` : ``}

                <div id="header">
                    <div id="title-container">
                        <or-translate id="title" value="rule_plural"></or-translate>
                    </div>
        
                    <div id="header-btns">
                        <or-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length === 0}" type="${InputType.BUTTON}" icon="content-copy" @click="${() => this._onCopyClicked()}"></or-input>
                        <or-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length === 0}" type="${InputType.BUTTON}" icon="delete" @click="${() => this._onDeleteClicked()}"></or-input>
                        ${addTemplate}
                        <or-input hidden type="${InputType.BUTTON}" icon="magnify" @click="${() => this._onSearchClicked()}"></or-input>
                        
                        ${getContentWithMenuTemplate(
            html`<or-input type="${InputType.BUTTON}" icon="sort-variant"></or-input>`,
            sortOptions.map((sort) => { return { value: sort, text: i18next.t(sort) } as MenuItem; }),
            this.sortBy,
            (v) => this._onSortClicked(v as string))}
                    </div>
                </div>
        
                ${!this._nodes || this._showLoading
                ? html`
                        <span id="loading"><or-translate value="loading"></or-translate></span>`
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

    protected _isReadonly() {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_RULES);
    }

    protected _nodeTemplate(node: RulesetNode): TemplateResult | string {
        return html`
            <li ?data-selected="${node.selected}" @click="${(evt: MouseEvent) => this._onNodeClicked(evt, node)}">
                <div class="node-container">
                    <span class="node-status ${OrRuleList._getNodeStatusClasses(node.ruleset)}"></span>
                    <span class="node-name">${node.ruleset.name}<span class="node-language">${node.ruleset.lang !== RulesetLang.JSON ? node.ruleset.lang : ""}</span></span>
                </div>
            </li>
        `;
    }

    protected static _getNodeStatusClasses(ruleset: TenantRuleset): string {
        let status = ruleset.enabled ? "bg-green" : "bg-red";

        if (ruleset.enabled) {

            // Look at validity meta
            if (ruleset.meta && ruleset.meta.hasOwnProperty("urn:openremote:rule:meta:validity")) {
                let calendarEvent = ruleset.meta["urn:openremote:rule:meta:validity"] as CalendarEvent;
                let now = new Date().getTime();

                if (calendarEvent.start) {
                    if (now < calendarEvent.start) {
                        // before startDate, show blue
                        status = "bg-blue";
                    } else if (calendarEvent.end && now > calendarEvent.end) {
                        // after endDate, show grey
                        status = "bg-grey";
                    } else if (calendarEvent.recurrence) {
                        // TODO: Implement RRule support
                    }
                }
            }
        }

        return status;
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
        this.dispatchEvent(new OrRulesSelectionChangedEvent(this._selectedNodes.map((node) => node.ruleset)));
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

        this._doRequest(new OrRulesRequestSelectEvent(selectRulesets), (detail) => {
            this.selectedIds = detail.map((ruleset) => ruleset.id!);
        });
    }

    protected _onCopyClicked() {
        if (this._selectedNodes.length == 1) {
            this.dispatchEvent(new OrRulesRequestCopyEvent(this._selectedNodes[0].ruleset));
        }
    }

    protected _onDeleteClicked() {
        if (this._selectedNodes.length > 0) {
            this.dispatchEvent(new OrRulesRequestDeleteEvent(this._selectedNodes.map((node) => node.ruleset)));
        }
    }

    protected _onAddClicked(lang: RulesetLang) {
        const type = this._globalRulesets ? "global": "tenant";
        this.dispatchEvent(new OrRulesRequestAddEvent(lang, type));
    }

    protected _onSearchClicked() {

    }

    protected _onSortClicked(sortBy: string) {
        this.sortBy = sortBy;
    }

    protected _getSortFunction(): (a: RulesetNode, b: RulesetNode) => number {
        return (a, b) => { return (a.ruleset as any)![this.sortBy!] < (b.ruleset as any)![this.sortBy!] ? -1 : (a.ruleset as any)![this.sortBy!] > (b.ruleset as any)![this.sortBy!] ? 1 : 0 };
    }

    protected _getRealm(): string | undefined {
        if (manager.isSuperUser()) {
            return manager.displayRealm;
        }

        return manager.getRealm();
    }

    protected _doRequest<T>(event: CustomEvent<RequestEventDetail<T>>, handler: (detail: T) => void) {
        this.dispatchEvent(event);
        window.setTimeout(() => {
            if (event.detail.allow) {
                handler(event.detail.detail);
            }
        });
    }

    protected _loadRulesets() {
        const sortFunction = this._getSortFunction();

        if(this._globalRulesets) {
            manager.rest.api.RulesResource.getGlobalRulesets({fullyPopulate: true }).then((response: any) => {
                if (response && response.data) {
                    this._buildTreeNodes(response.data, sortFunction);
                }
            }).catch((reason: any) => {
                console.error("Error: " + reason);
            });
        } else {
            const params = {
                fullyPopulate: true,
                language:  this._allowedLanguages
            }
            manager.rest.api.RulesResource.getTenantRulesets(this._getRealm() || manager.config.realm, params).then((response: any) => {
                if (response && response.data) {
                    this._buildTreeNodes(response.data, sortFunction);
                }
            }).catch((reason: any) => {
                console.error("Error: " + reason);
            });
        }
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