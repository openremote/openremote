import { project } from "../components/flow-editor";
export const nodeConverter = {
    fromAttribute: (value) => {
        return project.nodes.find((n) => n.id === value);
    },
    toAttribute: (node) => {
        return node.id;
    },
};
//# sourceMappingURL=node-converter.js.map