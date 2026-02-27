import {css, html} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import "@openremote/or-rules";
import {OrRules, OrRulesSelectionEvent, RulesConfig, RulesetNode} from "@openremote/or-rules";
import {NotificationTargetType, RulesetLang, WellknownAssets} from "@openremote/model";
import {Store} from "@reduxjs/toolkit";
import {Page, PageProvider, router} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import manager from "@openremote/core";
import {createSelector} from "reselect";

export interface PageRulesConfig {
    rules: RulesConfig;
}

export function pageRulesProvider(store: Store<AppStateKeyed>, config: PageRulesConfig = PAGE_RULES_CONFIG_DEFAULT): PageProvider<AppStateKeyed> {
    return {
        name: "rules",
        routes: [
            "rules",
            "rules/:id"
        ],
        pageCreator: () => {
            const page = new PageRules(store);
            page.config = {
                rules: {...PAGE_RULES_CONFIG_DEFAULT.rules, ...config.rules}
            };
            return page;
        }
    };
}

export const PAGE_RULES_CONFIG_DEFAULT: PageRulesConfig = {

    rules: {
        controls: {
            allowedLanguages: [RulesetLang.JSON, RulesetLang.FLOW, RulesetLang.GROOVY],
            allowedActionTargetTypes: {
                actions: {
                    wait: [],
                    attribute: [],
                    webhook: [],
                    email: [NotificationTargetType.USER, NotificationTargetType.CUSTOM],
                    email_localized: [NotificationTargetType.USER, NotificationTargetType.CUSTOM],
                    push: [NotificationTargetType.USER, NotificationTargetType.ASSET],
                    push_localized: [NotificationTargetType.USER, NotificationTargetType.ASSET]
                }
            }
        },
        descriptors: {
            all: {
                excludeAssets: [
                    WellknownAssets.TRADFRILIGHTASSET,
                    WellknownAssets.TRADFRIPLUGASSET,
                    WellknownAssets.ARTNETLIGHTASSET
                ]
            }
        },
        json: {}
    }
};

@customElement("page-rules")
export class PageRules extends Page<AppStateKeyed>  {

    static get styles() {
        // language=CSS
        return css`
            :host {
            
            }
            
            or-rules {
                width: 100%;
                height: 100%;
            }
        `;
    }

    @property()
    public config?: PageRulesConfig;

    @property()
    protected _ruleId?: number;

    @query("#rules")
    protected _orRules!: OrRules;

    protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.displayRealm;

    protected getRealmState = createSelector(
        [this._realmSelector],
        async (realm) => {
            if (this._orRules) {
                this._orRules.refresh();
            }
        }
    )

    get name(): string {
        return "rules";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
        // Listening to the rule selection event to update internal state
        this.addEventListener(OrRulesSelectionEvent.NAME, this._onRuleSelection.bind(this));
    }

    protected render() {
        return html`
            <or-rules id="rules"
                      .config="${this.config && this.config.rules ? this.config.rules : PAGE_RULES_CONFIG_DEFAULT.rules}"
                      .selectedRuleId="${this._ruleId}"
            ></or-rules>
        `;
    }

    public stateChanged(state: AppStateKeyed) {
        this.getRealmState(state);

        // Extracting the id from state and saving internally
        this._ruleId = parseInt(state.app.params?.id);
    }

    /**
     * Handles the selection event from the rule tree.
     *
     * If exactly one node is selected and it is of type 'rule', updates the internal `_ruleId`
     * and updates the route to reflect the newly selected rule.
     *
     * @param event - The custom event containing the newly selected nodes.
     */
    protected _onRuleSelection(event: OrRulesSelectionEvent) {
        const selectedNodes = event.detail.newNodes;
        let newRuleId: number | undefined = undefined;
        if (selectedNodes.length === 1 && selectedNodes[0].type === 'rule') {
            const ruleNode = selectedNodes[0] as RulesetNode;
            newRuleId = ruleNode.ruleset.id;

            if (this._ruleId !== newRuleId) {
                this._ruleId = newRuleId;
                this._updateRoute(newRuleId.toString());
            }
        } else {
            this._ruleId = undefined;
            this._updateRoute();
        }
    }

    /**
     * Updates the browser route to reflect the currently selected rule.
     *
     * Navigates to either `rules/<ruleId>` or `rules` if no rule ID is provided.
     * Navigation is performed without triggering route hooks or handlers.
     *
     * @param ruleId - Optional string ID of the selected rule.
     */
    protected _updateRoute(ruleId?: string) {
        const path = ruleId ? `rules/${ruleId}` : 'rules';
        router.navigate(path, { callHooks: false, callHandler: false });
    }
}
