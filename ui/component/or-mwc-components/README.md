# @openremote/or-mwc-* \<or-mwc-*\>
[![NPM Version][npm-image]][npm-url]

Web components wrapper for `MDC` web UI components, the standard `MWC` components are not complete, and they have a
horrible icon font dependency (hopefully this will removed) whereas we use `SVG` based icon sets.

## Install
```bash
npm i @openremote/or-mwc-components
yarn add @openremote/or-mwc-components
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Each element is a separate entry point, so import only the ones being used:

```typescript
import "@openremote/or-mwc-components/or-mwc-table";
```

Note that `or-mwc-input` and the menu helpers in `or-mwc-menu` are deprecated in favour of
[@openremote/or-vaadin-components](../or-vaadin-components).

### \<or-mwc-dialog\>
Dialogs are usually constructed and passed to `showDialog`, which attaches them to the document and removes them once
dismissed. `OrMwcDialog.DialogHostElement` sets where in the DOM they are attached.

```typescript
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";

showDialog(new OrMwcDialog()
    .setHeading("confirmDelete")
    .setContent(html`<p>...</p>`)
    .setActions([
        {actionName: "cancel", content: "cancel"},
        {actionName: "ok", content: "ok", action: () => this._delete()}
    ]));
```

An action's `action` callback runs when it is picked, and `dismissAction` is what runs when the dialog is closed any
other way. `showOkCancelDialog` wraps the common confirmation case and resolves to a boolean.

```typescript
if (await showOkCancelDialog("confirmDelete", "areYouSure")) {
    // Confirmed
}
```

### \<or-mwc-table\>
Takes `columns` and `rows`, either as plain strings or as objects that control alignment, sorting and mobile
visibility.

```html
<or-mwc-table .columns="${[{title: "name", isSortable: true}, {title: "count", isNumeric: true}]}"
              .rows="${rows}"
              .config="${{stickyFirstColumn: true, pagination: {enable: true}}}"
              @or-mwc-table-row-click="${(e) => this._onRowClick(e.detail)}">
</or-mwc-table>
```

`config.multiSelect` adds row checkboxes and makes the table fire `or-mwc-table-row-select`, with the selection
readable from `selectedRows`. With pagination enabled, set `totalCount` when the rows are fetched a page at a time.

### \<or-mwc-snackbar\>
`showSnackbar` displays a transient message with an optional action, and does not require the element in markup.

```typescript
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";

showSnackbar(undefined, "saveFailed", "retry", () => this._save());
```

### \<or-mwc-tabs\> and \<or-mwc-drawer\>
`or-mwc-tabs` renders a tab bar over a set of panels, and `or-mwc-drawer` is a slide out panel with `header` and
default slots, toggled through its `open` property.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-mwc-components.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-mwc-components
