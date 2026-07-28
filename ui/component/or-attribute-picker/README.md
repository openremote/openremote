# @openremote/or-attribute-picker  \<or-attribute-picker\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a dialog to select an attribute.

## Install
```bash
npm i @openremote/or-attribute-picker
yarn add @openremote/or-attribute-picker
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The pickers are dialogs, so they are constructed and handed to `showDialog` rather than placed in markup. Every
setter returns the picker, so configuration chains. The selection is delivered by an event when the user confirms;
dismissing the dialog fires nothing.

There are two pickers. `or-asset-attribute-picker` selects attributes of specific assets and reports `AttributeRef`s:

```typescript
import {OrAssetAttributePicker, OrAssetAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";

const dialog = showDialog(new OrAssetAttributePicker()
    .setMultiSelect(true)
    .setSelectedAttributes(this.attributeRefs)
    .setShowOnlyDatapointAttrs(true));

dialog.addEventListener(OrAssetAttributePickerPickedEvent.NAME, (event) => {
    this.attributeRefs = event.detail;
});
```

`or-assettype-attribute-picker` selects attributes of asset types instead, and reports a map of asset type name to
the chosen `AttributeDescriptor`s:

```typescript
const dialog = showDialog(new OrAssetTypeAttributePicker().setMultiSelect(true));

dialog.addEventListener(OrAssetTypeAttributePickerPickedEvent.NAME, (event) => {
    this.descriptorsByAssetType = event.detail;
});
```

### Filtering
`setShowOnlyDatapointAttrs`, `setShowOnlyRuleStateAttrs` and `setShowPredictedDataAttrs` restrict the list to
attributes carrying the corresponding meta item; the predicted flag composes additively with the other two.
`setAttributeFilter` takes a predicate for anything else, and the asset type picker also accepts
`setAssetTypeFilter`.

```typescript
new OrAssetAttributePicker().setAttributeFilter((attribute) => attribute.name !== "location");
```

`setSelectedAssets` opens the asset picker with an asset already selected, so the user lands on its attribute list.

Note that `or-attribute-picker` and `OrAttributePickerPickedEvent` are deprecated aliases of the asset picker and its
event.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-attribute-picker.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-attribute-picker
