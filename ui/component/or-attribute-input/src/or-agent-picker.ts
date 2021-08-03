import {html, LitElement, PropertyValues} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {manager, OREvent} from "@openremote/core";
import {Agent} from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";

export class OrAgentPickerLoadedEvent extends CustomEvent<Agent[]> {

    public static readonly NAME = "or-agent-picker-loaded";

    constructor(value: Agent[]) {
        super(OrAgentPickerLoadedEvent.NAME, {
            detail: value,
            bubbles: true,
            composed: true
        });
    }
}

export class OrAgentPickerChangedEvent extends CustomEvent<Agent> {

    public static readonly NAME = "or-agent-picker-changed";

    constructor(value?: Agent) {
        super(OrAgentPickerChangedEvent.NAME, {
            detail: value,
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAgentPickerChangedEvent.NAME]: OrAgentPickerChangedEvent;
        [OrAgentPickerLoadedEvent.NAME]: OrAgentPickerLoadedEvent;
    }
}

function getAgents(): Promise<Agent[]> {
    return manager.rest.api.AssetResource.queryAssets({
        tenant: {
            realm: manager.displayRealm
        },
        types: [
            "Agent"
        ],
        select: {
            excludeParentInfo: true,
            excludePath: true,
            excludeAttributes: true
        }
    }).then(response => response.data as Agent[]);
}

/**
 * This function creates a short lived cache for loading the list of agents; this is useful when multiple instances
 * of this control are used in a single UI
 */
let agents: Agent[] | undefined;
let loadingPromise: Promise<Agent[]> | undefined;
const timeout = 2000;
function loadAgents(): PromiseLike<Agent[]> {

    if (agents) {
        return Promise.resolve(agents);
    }

    if (loadingPromise) {
        return loadingPromise;
    }

    loadingPromise = getAgents()
        .then(agnts => {
            agents = agnts;
            manager.addListener((ev: OREvent) => {
                switch (ev) {
                    case OREvent.DISPLAY_REALM_CHANGED:
                        agents = undefined;
                        loadingPromise = undefined;
                        break;
                }
            });
            manager.events!.subscribeAssetEvents(undefined, false, undefined, (assetEvent) => {
                if (assetEvent.asset && assetEvent.asset.type!.endsWith("Agent")) {
                    agents = undefined;
                    loadingPromise = undefined;
                }
            })

            return agnts;
        });

    return loadingPromise;
}

@customElement("or-agent-picker")
export class OrAgentPicker extends LitElement {

    @property({type: String})
    public agentId?: string;

    @property({type: Boolean})
    public disabled: boolean = false;

    @state()
    protected agents?: Agent[];

    connectedCallback() {
        super.connectedCallback();
        loadAgents().then((agents) => {
            this.agents = agents;
            this.dispatchEvent(new OrAgentPickerLoadedEvent(agents));
        });
    }

    render() {

        if (!this.agents) {
            return html`
                <or-mwc-input .disabled="${true}" .type="${InputType.SELECT}"></or-mwc-input>
            `;
        }

        const agentOptions: [string ,string][] = this.agents ? this.agents.map(agent => [agent.id!, agent.name + " (" + agent.id + ")"]) : [];

        return html`
            <or-mwc-input .disabled="${this.disabled}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onAgentChanged(ev.detail.value as string)}" type="${InputType.SELECT}" .value="${this.agentId}" .placeholder="${agentOptions.length > 0 ? "" : i18next.t("loading")}" .options="${agentOptions}"></or-mwc-input>
        `;
    }

    onAgentChanged(id?: string) {
        this.dispatchEvent(new OrAgentPickerChangedEvent(this.agents?.find(agent => agent.id === id)));
    }
}
