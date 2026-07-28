# @openremote/or-asset-tree  \<or-asset-tree\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying an asset tree. This component requires an OpenRemote Manager to query assets.

## Install
```bash
npm i @openremote/or-asset-tree
yarn add @openremote/or-asset-tree
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

With no properties set the tree queries the assets of the current realm and subscribes to asset events so that it
stays in step with the manager:

```html
<or-asset-tree></or-asset-tree>
```

The queried set can be narrowed with `rootAssets` or `rootAssetIds`, replaced outright by passing loaded `assets`, or
taken over entirely with a `dataProvider` that resolves a page of assets:

```html
<or-asset-tree .rootAssetIds="${["4bK9K8j1Y8SQ1zZY4Zx0kN"]}"></or-asset-tree>
```

```typescript
const dataProvider = (offset: number, limit: number, parentId?: string) => loadAssets(offset, limit, parentId);
```

Assets are loaded a page at a time using `queryLimit`; above `paginationThreshold` total assets the tree switches to
loading children on expand rather than up front. Set `disableSubscribe` to stop the tree tracking manager events.

### Selection
`selectedIds` reflects the current selection and can be set to select nodes programmatically. `checkboxes` renders a
checkbox per node for multi selection, and `expandAllNodes` expands the whole tree on load.

```html
<or-asset-tree checkboxes .selectedIds="${this.selectedIds}"
               @or-asset-tree-selection="${(e) => this.selectedIds = e.detail.newNodes.map((n) => n.asset.id)}">
</or-asset-tree>
```

### Controls
`readonly` hides the add, delete and move controls, while `disabled` blocks all interaction. The filter, deselect and
sort controls are toggled with `showFilter`, `showFilterIcon`, `showDeselectBtn` and `showSortBtn`, and `sortBy`
chooses the initial sort.

### Events
* `or-asset-tree-selection` (`OrAssetTreeSelectionEvent`) - The selection changed; detail contains the old and new nodes
* `or-asset-tree-expand` (`OrAssetTreeToggleExpandEvent`) - A node was expanded or collapsed
* `or-asset-tree-add` (`OrAssetTreeAddEvent`) - An asset was added
* `or-asset-tree-change-parent` (`OrAssetTreeChangeParentEvent`) - Assets were moved to a new parent
* `or-asset-tree-asset-event` (`OrAssetTreeAssetEvent`) - An asset event was received for a node in the tree

Selection, add and delete each fire a request event first (`or-asset-tree-request-selection`,
`or-asset-tree-request-add`, `or-asset-tree-request-delete`). Setting `detail.allow` to `false` on the handler vetoes
the action, which is how a consumer can prompt before discarding unsaved changes.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-asset-tree.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-asset-tree
