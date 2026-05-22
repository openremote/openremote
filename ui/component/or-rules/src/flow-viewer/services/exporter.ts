/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { NodeCollection, GlobalRuleset, RulesetLang, Ruleset } from "@openremote/model";
import manager from "@openremote/core";
import { project, modal } from "../components/flow-editor";

export class Exporter {
    public flowToJson(collection: NodeCollection) {
        return JSON.stringify(collection);
    }

    public jsonToFlow(json: string): NodeCollection {
        return JSON.parse(json);
    }

    public collectionToRuleset(collection: NodeCollection, type: "global" | "asset" | "realm") {
        const ruleset: Ruleset = {
            lang: RulesetLang.FLOW,
            name: collection.name,
            type,
            rules: this.flowToJson(collection),
        };
        return ruleset;
    }

    public rulesetToCollection(ruleset: Ruleset) {
        return this.jsonToFlow(ruleset.rules!);
    }

    public exportAsNew(collection: NodeCollection) {
        const json = this.flowToJson(collection);
        const ruleApi = manager.rest.api.RulesResource;

        const rs: GlobalRuleset = {
            lang: RulesetLang.FLOW,
            name: collection.name,
            type: "global",
            rules: json
        };

        ruleApi.createGlobalRuleset(rs).then((e) => {
            if (e.status === 200) {
                project.setCurrentProject(e.data, collection.name!, collection.description!);
                console.log("Successfully saved new ruleset");
            } else {
                console.log("Something went wrong while saving NEW ruleset\nHTTP status " + e.status);
                modal.notification("Failure", `Something went wrong while saving ${rs.name}`);
            }
        }).catch((e) => {
            console.log("Something went wrong while saving NEW ruleset\nHTTP status " + e);
            modal.notification("Failure", `Something went wrong while saving ${rs.name}`);
        });
    }

    public exportAsExisting(id: number, collection: NodeCollection) {
        const json = this.flowToJson(collection);
        const ruleApi = manager.rest.api.RulesResource;
        ruleApi.getGlobalRuleset(project.existingFlowRuleId).then((response) => {
            const existing = response.data;
            existing.rules = json;
            ruleApi.updateGlobalRuleset(existing.id!, existing).then((e: any) => {
                if (e.status === 204) {
                    project.unsavedState = false;
                    console.log("Successfully saved ruleset");
                } else {
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
