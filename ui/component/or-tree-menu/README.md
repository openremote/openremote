# @openremote/or-tree-menu  \<or-tree-menu\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a tree menu of items with a predefined hierarchy.

## Install
```bash
npm i @openremote/or-tree-menu
yarn add @openremote/or-tree-menu
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The menu renders a list of `TreeNode` objects; a node with `children` becomes a group. It holds no domain knowledge,
so consumers subclass it to render their own node content.

```html
<or-tree-menu menu-title="Assets" .nodes="${this.nodes}"
              @or-tree-select="${(e) => this.selected = e.detail}">
</or-tree-menu>
```

```typescript
const nodes: TreeNode[] = [
    {
        id: "1",
        label: "Ground floor",
        children: [
            {id: "2", label: "Living room"}
        ]
    }
];
```

### Selection
`selection` sets what may be selected: `LEAF` allows a single node but not a group, `SINGLE` allows a single node
including groups, and `MULTI` allows several using control and shift. `deselectAllNodes()` clears the selection and
`expandGroup(groupId)` opens a group.

### Sorting and layout
`sort-options` lists the sort choices offered in the header, so leaving it unset leaves the sort menu empty. `sort-by`
is the active choice and defaults to alphabetical. Set `group-first` to float groups above the other nodes, and
`no-header` to drop the header including its title and sort control.

### Dragging
Setting `draggable` lets the user drag nodes into and out of groups. The move fires a cancelable `or-tree-drag` first,
so a consumer can persist the change or reject it:

```typescript
onDrag(event: OrTreeDragEvent) {
    if (!this._canMove(event.detail.nodes, event.detail.groupNode)) {
        event.preventDefault();
    }
}
```

`moveNodesToGroup(nodes, groupNode)` performs the same move programmatically.

### Custom nodes
Subclass `OrTreeMenu` and override `_getSingleNodeSlotTemplate` or `_getGroupNodeSlotTemplate` to change what a node
shows. Each renders into the `prefix`, default and `suffix` slots of `or-tree-node`.

```typescript
@customElement("my-tree-menu")
export class MyTreeMenu extends OrTreeMenu {

    protected _getSingleNodeSlotTemplate(node: TreeNode): TemplateResult {
        return html`
            <or-icon slot="prefix" icon="lightbulb"></or-icon>
            <span>${node.label}</span>
            <span slot="suffix"></span>
        `;
    }
}
```

### Events
* `or-tree-select` (`OrTreeSelectEvent`) - The selection changed; detail is the list of selected nodes
* `or-tree-drag` (`OrTreeDragEvent`) - Nodes were dragged into or out of a group; cancelable, detail contains the
moved `nodes`, the target `groupNode` and the resulting `newNodes`

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.

## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-tree-menu.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-tree-menu
