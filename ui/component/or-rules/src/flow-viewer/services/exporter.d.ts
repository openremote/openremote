import { NodeCollection, Ruleset } from "@openremote/model";
export declare class Exporter {
    flowToJson(collection: NodeCollection): string;
    jsonToFlow(json: string): NodeCollection;
    collectionToRuleset(collection: NodeCollection, type: "global" | "asset" | "realm"): Ruleset;
    rulesetToCollection(ruleset: Ruleset): NodeCollection;
    exportAsNew(collection: NodeCollection): void;
    exportAsExisting(id: number, collection: NodeCollection): void;
}
