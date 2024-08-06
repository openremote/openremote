import manager from "@openremote/core";
import { project, modal } from "../components/flow-editor";
export class Exporter {
    flowToJson(collection) {
        return JSON.stringify(collection);
    }
    jsonToFlow(json) {
        return JSON.parse(json);
    }
    collectionToRuleset(collection, type) {
        const ruleset = {
            lang: "FLOW" /* RulesetLang.FLOW */,
            name: collection.name,
            type,
            rules: this.flowToJson(collection),
        };
        return ruleset;
    }
    rulesetToCollection(ruleset) {
        return this.jsonToFlow(ruleset.rules);
    }
    exportAsNew(collection) {
        const json = this.flowToJson(collection);
        const ruleApi = manager.rest.api.RulesResource;
        const rs = {
            lang: "FLOW" /* RulesetLang.FLOW */,
            name: collection.name,
            type: "global",
            rules: json
        };
        ruleApi.createGlobalRuleset(rs).then((e) => {
            if (e.status === 200) {
                project.setCurrentProject(e.data, collection.name, collection.description);
                console.log("Successfully saved new ruleset");
            }
            else {
                console.log("Something went wrong while saving NEW ruleset\nHTTP status " + e.status);
                modal.notification("Failure", `Something went wrong while saving ${rs.name}`);
            }
        }).catch((e) => {
            console.log("Something went wrong while saving NEW ruleset\nHTTP status " + e);
            modal.notification("Failure", `Something went wrong while saving ${rs.name}`);
        });
    }
    exportAsExisting(id, collection) {
        const json = this.flowToJson(collection);
        const ruleApi = manager.rest.api.RulesResource;
        ruleApi.getGlobalRuleset(project.existingFlowRuleId).then((response) => {
            const existing = response.data;
            existing.rules = json;
            ruleApi.updateGlobalRuleset(existing.id, existing).then((e) => {
                if (e.status === 204) {
                    project.unsavedState = false;
                    console.log("Successfully saved ruleset");
                }
                else {
                    console.log("Something went wrong while saving EXISTING ruleset\n" + e.status);
                    modal.notification("Failure", `Something went wrong while saving ${collection.name}`);
                }
            });
        }).catch((e) => {
            console.log("Something went wrong while saving EXISTING ruleset\n" + e);
            modal.notification("Failure", `Something went wrong while saving ${collection.name}`);
        });
    }
}
//# sourceMappingURL=exporter.js.map