import {OrTreeMenu, OrTreeNode, TreeMenuSelection, TreeNode} from "@openremote/or-tree-menu";
import {customElement, property, state} from "lit/decorators.js";
import {RealmRuleset, RulesetLang, RulesetStatus, RulesetUnion} from "@openremote/model";
import {css, html, PropertyValues, TemplateResult} from "lit";
import manager, {Util} from "@openremote/core";
import {i18next} from "@openremote/or-translate";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {
    OrRules,
    OrRulesAddEvent,
    OrRulesRequestAddEvent,
    OrRulesRequestDeleteEvent, OrRulesRequestGroupEvent, OrRulesRequestSelectionEvent,
    OrRulesSelectionEvent,
    RulesConfig,
    RulesetGroupNode,
    RulesetNode,
    RuleViewInfoMap
} from "./index";
import {ifDefined} from "lit/directives/if-defined.js";
import {when} from "lit/directives/when.js";

const styling = css`
    .iconfill-gray {
        --or-icon-fill: var(--internal-or-rules-list-icon-color-ok);
    }

    .iconfill-red {
        --or-icon-fill: var(--internal-or-rules-list-icon-color-error);
    }

    #rules-tree-global-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 15px;
        min-height: 48px;
        background: var(--or-app-color3, #4c4c4c);
        color: var(--or-app-color7, white);
        --or-icon-fill: var(--or-app-color7, white);
    }
`;

export interface RuleTreeNode extends TreeNode {
    ruleset?: RulesetUnion;
}

export enum RuleTreeSorting {
    NAME = "name", CREATED_ON = "created_on"
}

@customElement("or-rule-tree")
export class OrRuleTree extends OrTreeMenu {

    public static DEFAULT_ALLOWED_LANGUAGES = [RulesetLang.JSON, RulesetLang.GROOVY, RulesetLang.JAVASCRIPT, RulesetLang.FLOW];

    /**
     * List of rules visible in the tree menu
     */
    @property({type: Array})
    public rules?: RulesetUnion[];

    /**
     * Rules configuration object, for example to only allow certain ruleset types
     */
    @property({type: Object})
    public config?: RulesConfig;

    /**
     * Defines whether only global rulesets should be visible.
     */
    @property({type: Boolean})
    public global = false;

    /**
     * Sets every element in the tree to readonly mode
     */
    @property({type: Boolean})
    public readonly = false;

    nodes: RuleTreeNode[] = [];
    draggable = true;
    selection = TreeMenuSelection.MULTI;
    sortOptions: any[] = [RuleTreeSorting.NAME, RuleTreeSorting.CREATED_ON];
    sortBy: any = RuleTreeSorting.NAME;
    groupFirst = true;
    menuTitle = "rules";

    @state()
    protected _loadingPromises: Promise<boolean>[] = [];

    @state()
    protected _anySelected = false;

    static get styles(): any[] {
        return [...super.styles, styling];
    }

    protected willUpdate(changedProps: PropertyValues) {
        if (changedProps.has("rules")) {
            if (!this.rules) {
                this._loadRulesets(this.global).then(rulesets => this.rules = rulesets);
            } else {
                this.nodes = this._getRuleNodes(this.rules);
            }
        }
        return super.willUpdate(changedProps);
    }

    protected firstUpdated(changedProps: PropertyValues) {
        if (!this.rules) {
            this._loadRulesets(this.global).then(rulesets => {
                this.rules = rulesets;
            });
        }
    }

    protected _getSingleNodeSlotTemplate(node: RuleTreeNode): TemplateResult {
        return html`
            <or-icon slot="prefix" icon="${this._getRulesetLangIcon(node.ruleset?.lang)}"></or-icon>
            <span>${node.label}</span>
            <or-icon slot="suffix" icon=${ifDefined(this._getRulesetStatusIcon(node.ruleset?.status))}
                     class=${this._getRulesetStatusColorClass(node.ruleset?.status)}
            ></or-icon>
        `;
    }

    protected _getGroupNodeSlotTemplate(node: RuleTreeNode): TemplateResult {
        let icon;
        const childrenStatus = (node.children as RuleTreeNode[] | undefined)?.map(child => child.ruleset?.status).filter(status => status) as RulesetStatus[];
        const hasError = childrenStatus?.find(c => [RulesetStatus.LOOP_ERROR, RulesetStatus.EXECUTION_ERROR, RulesetStatus.COMPILATION_ERROR].includes(c)) || false;
        if (hasError) {
            const errorCounts = [RulesetStatus.LOOP_ERROR, RulesetStatus.EXECUTION_ERROR, RulesetStatus.COMPILATION_ERROR].map(err => [err, childrenStatus.filter(s => s === err).length] as [RulesetStatus, number]);
            const mostCommonError = errorCounts.reduce((prev, curr) => curr[1] > prev[1] ? curr : prev)[0];
            icon = this._getRulesetStatusIcon(mostCommonError);
        }
        return html`
            <or-icon slot="prefix" icon="folder"></or-icon>
            <span>${node.label}</span>
            <or-icon slot="suffix" icon=${ifDefined(icon)} class="iconfill-red"></or-icon>
        `;
    }

    protected _getHeaderTemplate(): TemplateResult {
        return html`
            ${when(manager.isSuperUser(), () => html`
                <div id="rules-tree-global-header">
                    <or-translate value="realmRules"></or-translate>
                    <or-mwc-input type=${InputType.SWITCH} @or-mwc-input-changed=${this._onGlobalSwitch}></or-mwc-input>
                    <or-translate value="globalRules"></or-translate>
                </div>
            `)}
            <div id="tree-header">
                <h3 id="tree-header-title">
                    <or-translate value=${this.menuTitle}></or-translate>
                </h3>
                <div id="tree-header-actions">
                    ${when(this._anySelected && !this.readonly, () => html`
                        <or-mwc-input type=${InputType.BUTTON} icon="content-copy" @or-mwc-input-changed=${this._onCopyClicked}></or-mwc-input>
                        <or-mwc-input type=${InputType.BUTTON} icon="delete" @or-mwc-input-changed=${this._onDeleteClicked}></or-mwc-input>
                    `)}
                    ${this._getAddActionTemplate()}
                    ${this._getSortActionTemplate(this.sortBy, this.sortOptions)}
                </div>
            </div>
        `;
    }

    /**
     * HTML callback on when the global switch is toggled.
     * It normally toggles between 'global' and 'realm' rulesets when the user is a superuser.
     */
    protected _onGlobalSwitch(ev: OrInputChangedEvent) {
        this.global = ev.detail.value;
        this.refresh();
    }

    /**
     * HTML callback on when the 'copy' menu option is pressed.
     * The function duplicates the current selected rule, and shows it inside the viewer window.
     * @protected
     */
    protected _onCopyClicked(_ev: OrInputChangedEvent) {
        const selected = this._findSelectedTreeNodes() as RuleTreeNode[];
        if (selected.length !== 1) {
            return;
        }

        const node = selected[0];
        const ruleset = JSON.parse(JSON.stringify(node.ruleset)) as RulesetUnion;
        delete ruleset.lastModified;
        delete ruleset.createdOn;
        delete ruleset.status;
        delete ruleset.error;
        delete ruleset.id;
        ruleset.name = ruleset.name + " copy";

        if (this.config && this.config.rulesetCopyHandler && !this.config.rulesetCopyHandler(ruleset)) {
            return;
        }

        Util.dispatchCancellableEvent(this, new OrRulesRequestAddEvent({
            ruleset: ruleset,
            sourceRuleset: node.ruleset
        })).then((detail) => {
            if (detail.allow) {
                this.dispatchEvent(new OrRulesAddEvent(detail.detail));
            }
        });
    }

    /**
     * HTML callback on when the 'delete' menu option is pressed.
     * The function deletes the rule that is affiliated with selected tree node,
     * after prompting a modal that the user confirms this action.
     * @protected
     */
    protected _onDeleteClicked(_ev: OrInputChangedEvent) {
        const selected = this._findSelectedTreeNodes() as RuleTreeNode[];
        if (selected.length > 0) {
            const selectedRules = selected.map(node => node.ruleset).filter(x => x) as RulesetUnion[];
            const selectedRuleNodes = selectedRules.map(ruleset => ({ ruleset: ruleset, selected: true }) as RulesetNode);

            Util.dispatchCancellableEvent(this, new OrRulesRequestDeleteEvent(selectedRuleNodes))
                .then((detail) => {
                    if (detail.allow) {
                        const names = selectedRules.map(r => r.name);
                        showOkCancelDialog(i18next.t("deleteRulesets"), i18next.t("deleteRulesetsConfirm", { ruleNames: names }), i18next.t("delete"))
                            .then((ok) => {
                                if (ok) {
                                    this._deselectAllNodes();
                                    this._deleteRulesets(selectedRules)
                                        .catch(e => console.error(e))
                                        .finally(() => this.refresh());
                                }
                            });
                    }
                });
        }
    }

    /**
     * Utility function that returns a promise for deleting a list of rulesets, based on their type.
     * @param rulesets - List of rulesets to delete.
     * @protected
     */
    protected async _deleteRulesets(rulesets: RulesetUnion[]) {
        const promises: Promise<any>[] = [];
        rulesets.forEach(ruleset => {
            switch (ruleset.type) {
                case "asset":
                    promises.push(manager.rest.api.RulesResource.deleteAssetRuleset(ruleset.id!))
                    break;
                case "realm":
                    promises.push(manager.rest.api.RulesResource.deleteRealmRuleset(ruleset.id!));
                    break;
                case "global":
                    promises.push(manager.rest.api.RulesResource.deleteGlobalRuleset(ruleset.id!));
                    break;
            }
        });
        return Promise.all(promises);
    }

    /**
     * Public function that allows consumers to select a ruleset in the tree.
     * It finds the tree node in the UI, and selects it.
     * @param ruleset - ruleset in the tree to be selected
     * @param silent - skips the modal of 'discard changes' and does not dispatch an event
     */
    public selectRuleset(ruleset: RulesetUnion, silent = false) {
        const rulesetNode = this.nodes.find(n => n.ruleset === ruleset);
        if(rulesetNode) {
            const uiNode = this._getUiNodeFromTree(rulesetNode);
            if(uiNode) this._selectNode(uiNode, !silent);
        }
    }

    protected _selectNode(node?: OrTreeNode, notify = true) {
        super._selectNode(node, notify);
        this._anySelected = this._findSelectedTreeNodes().length > 0;
    }

    protected _dispatchSelectEvent(selectedNodes?: RuleTreeNode[]): boolean {

        const lastSelected = this._lastSelectedNode ? this._getTreeNodeFromTree(this._lastSelectedNode) as RuleTreeNode : undefined;

        // Only select if ONE is selected
        const newSelected = (!selectedNodes || selectedNodes.length > 1) ? undefined : selectedNodes[0];

        const isGroupNode = (node?: RuleTreeNode) => !!node?.children;
        const oldNodes = lastSelected ? isGroupNode(lastSelected) ? [{type: "group", groupId: lastSelected.id} as RulesetGroupNode] : [{type: "rule", ruleset: lastSelected.ruleset!, selected: false} as RulesetNode] : [];
        const newNodes = newSelected ? isGroupNode(newSelected) ? [{type: "group", groupId: newSelected.id} as RulesetGroupNode] : [{type: "rule", ruleset: newSelected.ruleset!, selected: true} as RulesetNode] : [];

        // Ask for consumers if selection is allowed. If event.preventDefault() is called, we cancel the selection
        const success = this.dispatchEvent(new OrRulesRequestSelectionEvent({oldNodes: oldNodes, newNodes: newNodes}));
        if(!success) return false;

        // If succeeded, actually sent the "select" event.
        this.dispatchEvent(new OrRulesSelectionEvent({oldNodes: oldNodes, newNodes: newNodes}));
        return super._dispatchSelectEvent(selectedNodes);
    }

    /**
     * Gets the list of {@link RuleTreeNode[]} (tree nodes) based on a list of rulesets.
     * It maps the ruleset to the correct tree node format. For example, when a groupId is set in the Ruleset meta,
     * we group them together as children of a new group node.
     * @param rules - List of rulesets
     * @protected
     */
    protected _getRuleNodes(rules: RulesetUnion[]): RuleTreeNode[] {
        const groupList = new Set(rules.map(r => r.meta?.groupId).filter(x => x));
        const groupedNodes = Array.from(groupList).map(group => ({
            id: group,
            label: group,
            children: rules.filter(r => (r.meta as any)?.groupId === group).map(rule => ({
                id: rule.id?.toString(),
                ruleset: rule,
                label: rule.name
            }) as RuleTreeNode)
        }) as RuleTreeNode);

        const ungroupedRules = rules.filter(r => !r.meta?.groupId);
        const ungroupedNodes = ungroupedRules.map(rule => ({
            id: rule.id?.toString(),
            ruleset: rule,
            label: rule.name
        } as RuleTreeNode));

        return [...ungroupedNodes, ...groupedNodes];
    }

    /**
     * Utility function that returns a promise for loading the rulesets, based on the {@link global} and {@link realm} parameters
     * @param global - Whether only global rulesets should be loaded instead.
     * @param realm - Name of the realm to load (if global is `false` or `undefined`)
     * @protected
     */
    protected async _loadRulesets(global = false, realm = manager.displayRealm): Promise<RulesetUnion[]> {
        if (global) {
            const promise = manager.rest.api.RulesResource.getGlobalRulesets({ fullyPopulate: true });
            return (await promise).data;
        } else {
            const promise = manager.rest.api.RulesResource.getRealmRulesets(realm, { fullyPopulate: true });
            return (await promise).data;
        }
    }

    /**
     * Refreshes the tree content, and re-fetches the list of rules.
     */
    public async refresh() {
        await this.getUpdateComplete();
        const promise = this._loadRulesets(this.global);
        promise.then(rulesets => {
            this.rules = rulesets;
        });
        return promise;
    }

    /**
     * Generates a HTML {@link TemplateResult} for the "add" button in the controls menu.
     * @protected
     */
    protected _getAddActionTemplate(): TemplateResult {
        const menuOptions: (ListItem | null)[] = (this._getAllowedLanguages() || []).map(l =>
            ({value: l, text: i18next.t("rulesLanguages." + l)} as ListItem)
        );
        menuOptions.push(null);
        menuOptions.push({value: "group", text: i18next.t("group")} as ListItem);

        const onValueChange = (value: string) => {
            value === "group" ? this._onGroupAddClick() : this._onRulesetAddClick(value as RulesetLang);
        };

        return getContentWithMenuTemplate(
            html`
                <or-mwc-input type=${InputType.BUTTON} icon="plus"></or-mwc-input>`,
            menuOptions,
            undefined,
            value => onValueChange(String(value))
        );
    }

    /**
     * HTML callback function for clicking a ruleset language in the "add rule" menu.
     * @param lang - The language to be used for adding a rule.
     * @param global - Whether to create a global ruleset or not.
     * @protected
     */
    protected _onRulesetAddClick(lang: RulesetLang, global = this.global) {
        const type = global ? "global" : "realm";
        const realm = manager.isSuperUser() ? manager.displayRealm : manager.config.realm;
        const ruleset: RulesetUnion = {
            id: 0,
            type: type,
            name: OrRules.DEFAULT_RULESET_NAME,
            lang: lang,
            realm: realm,
            rules: undefined
        };

        if (this.config && this.config.rulesetAddHandler && !this.config.rulesetAddHandler(ruleset)) {
            return;
        }

        if (this.config && this.config.rulesetTemplates && this.config.rulesetTemplates[lang]) {
            ruleset.rules = this.config.rulesetTemplates[lang];
        } else {
            ruleset.rules = RuleViewInfoMap[lang].viewRulesetTemplate;
        }

        // Ensure config hasn't messed with certain values
        if (type === "realm") {
            (ruleset as RealmRuleset).realm = realm;
        }

        const detail = {
            ruleset: ruleset,
            isCopy: false
        };

        Util.dispatchCancellableEvent(this, new OrRulesRequestAddEvent(detail))
            .then((detail) => {
                if (detail.allow) {
                    this.dispatchEvent(new OrRulesAddEvent(detail.detail));
                }
            });
    }

    /**
     * HTML callback function for clicking "add group" in the controls menu.
     * @protected
     */
    protected _onGroupAddClick() {
        this._deselectAllNodes();
        const groupName = i18next.t('rulesEditorDefaultGroup') || "Group name"
        this.dispatchEvent(new OrRulesRequestGroupEvent(groupName));
    }

    /**
     * Utility function for getting the list of allowed languages based on {@link config} and {@link global} parameters.
     * @param config - Rules configuration to use
     * @param global - Whether only to include global rulesets
     * @protected
     */
    protected _getAllowedLanguages(config = this.config, global = this.global): RulesetLang[] | undefined {
        const languages = config?.controls?.allowedLanguages ? [...config.controls.allowedLanguages] : OrRuleTree.DEFAULT_ALLOWED_LANGUAGES;
        const groovyIndex = languages.indexOf(RulesetLang.GROOVY);
        const flowIndex = languages.indexOf(RulesetLang.FLOW);
        if (!manager.isSuperUser()) {
            if (groovyIndex > 0) languages.splice(groovyIndex, 1);
        } else if (groovyIndex < 0) {
            languages.push(RulesetLang.GROOVY);
        }
        if (global) {
            if (flowIndex > 0) languages.splice(flowIndex, 1);
        }
        return languages;
    }

    /**
     * Utility function that gets an Material Design icon based on the ruleset language
     * @param lang - The ruleset language to get an icon of.
     * @protected
     */
    protected _getRulesetLangIcon(lang?: RulesetLang): string {
        switch (lang) {
            case (RulesetLang.JSON):
                return "ray-start-arrow";
            case (RulesetLang.FLOW):
                return "transit-connection-variant";
            case (RulesetLang.GROOVY):
                return "alpha-g-box-outline";
            case (RulesetLang.JAVASCRIPT):
                return "language-javascript";
            default:
                return "mdi-state-machine";
        }
    }

    /**
     * Utility function that gets an Material Design icon based on the ruleset status
     * @param status - The ruleset status to get an icon of.
     * @protected
     */
    protected _getRulesetStatusIcon(status?: RulesetStatus): string | undefined {
        switch (status) {
            case RulesetStatus.DEPLOYED:
                return;
            case RulesetStatus.READY:
                return "check";
            case RulesetStatus.COMPILATION_ERROR:
            case RulesetStatus.LOOP_ERROR:
            case RulesetStatus.EXECUTION_ERROR:
                return "alert-octagon";
            case RulesetStatus.DISABLED:
                return "minus-circle";
            case RulesetStatus.PAUSED:
                return "calendar-arrow-right";
            case RulesetStatus.EXPIRED:
                return "calendar-remove";
            case RulesetStatus.REMOVED:
                return "close";
            default:
                return "stop";
        }
    }

    /**
     * Utility function that returns a CSS class for the icon element, based on the ruleset status
     * @param status - The ruleset status to get a CSS class of.
     * @protected
     */
    protected _getRulesetStatusColorClass(status?: RulesetStatus): string {
        switch (status) {
            case RulesetStatus.DEPLOYED:
            case RulesetStatus.READY:
                return "iconfill-gray";
            case RulesetStatus.COMPILATION_ERROR:
            case RulesetStatus.LOOP_ERROR:
            case RulesetStatus.EXECUTION_ERROR:
                return "iconfill-red";
            case RulesetStatus.DISABLED:
            case RulesetStatus.PAUSED:
            case RulesetStatus.EXPIRED:
            case RulesetStatus.REMOVED:
                return "iconfill-gray";
            default:
                return "iconfill-gray";
        }
    }
}
