import { ComplexAttributeConverter } from "lit";
import { Node } from "@openremote/model";
import { project } from "../components/flow-editor";

export const nodeConverter: ComplexAttributeConverter<unknown, unknown> = {
    fromAttribute: (value: string) => {
        return project.nodes.find((n) => n.id === value);
    },

    toAttribute: (node: Node) => {
        return node.id;
    },
};
