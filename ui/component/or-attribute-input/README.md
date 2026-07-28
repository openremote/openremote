# @openremote/or-attribute-input  \<or-attribute-input\>
[![NPM Version][npm-image]][npm-url]

Web Component to dynamically display fitting inputs/forms for attributes. This component requires an OpenRemote Manager to retrieve JSON Schemas for complex Value Types.

## Install
```bash
npm i @openremote/or-attribute-input
yarn add @openremote/or-attribute-input
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Give the component an `attribute` and the `assetId` it belongs to. It resolves the attribute and value descriptors
from the asset model, picks a matching input, subscribes to attribute events for the live value, and writes back on
change.

```html
<or-attribute-input .attribute="${asset.attributes.targetTemperature}" .assetId="${asset.id}"></or-attribute-input>
```

Without an `assetId` the component is unbound: it renders the same input for the given `attribute` but neither
subscribes nor writes, and reports changes through its event instead. `disableSubscribe` and `disableWrite` switch off
either half individually.

An attribute is not required. Passing an `attributeDescriptor` or `valueDescriptor` with an `assetType` renders an
input for a value of that type, which is how forms for attributes that do not exist yet are built.

```html
<or-attribute-input .valueDescriptor="${valueDescriptor}" .value="${value}" .assetType="${assetType}"></or-attribute-input>
```

### Writing
A write is sent when the input commits a value, and the component shows a pending state until the manager echoes the
new value back or `writeTimeout` elapses. `disableButton` drops the separate send button for inputs that have one.
Set `readonly` to render the current value without an editor, or `disabled` to grey the input out.

### Appearance
`label` overrides the descriptor derived label and `inputType` overrides the automatically chosen input. `compact`,
`comfortable`, `fullWidth`, `rounded`, `outlined`, `resizeVertical` and `hasHelperText` adjust the layout.

`customProvider` replaces the input template for cases the built in providers do not cover. It takes over for every
value type, including the ones with dedicated providers such as GeoJSON points, so it has to handle them itself.

```typescript
const customProvider: ValueInputProviderGenerator = (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {
    return {
        templateFunction: (value, focused, loading, sending, error, helperText) => html`...`,
        supportsHelperText: false,
        supportsLabel: true,
        supportsSendButton: false
    };
};
```

### Events
* `or-attribute-input-changed` (`OrAttributeInputChangedEvent`) - The value changed; detail contains `value` and
`previousValue`

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-attribute-input.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-attribute-input
