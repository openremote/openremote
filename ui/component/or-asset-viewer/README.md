# @openremote/or-asset-viewer  \<or-asset-viewer\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying an asset viewer. This component requires an OpenRemote Manager to query and save assets.

## Install
```bash
npm i @openremote/or-asset-viewer
yarn add @openremote/or-asset-viewer
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Point the viewer at an asset with `assetId`, or pass a loaded `asset` directly. Set `editMode` to expose the attribute
editor and `readonly` to prevent any writes.

```html
<or-asset-viewer assetId="4bK9K8j1Y8SQ1zZY4Zx0kN"></or-asset-viewer>
```

### Configuration
Without a `config` the viewer falls back to `DEFAULT_VIEWER_CONFIG`. A `ViewerConfig` holds a `default` config plus
per asset type overrides; an asset type entry replaces the default panels rather than merging with them.

```typescript
const config: ViewerConfig = {
    default: { ... },
    assetTypes: {
        ThingAsset: { ... }
    }
};
```

### Panels
`AssetViewerConfig.panels` is an ordered array of panel configs. Panels are laid out in two fixed columns; `column: 0`
(the default) places a panel in the left column and `column: 1` in the right, in array order.

```typescript
const config: ViewerConfig = {
    default: {
        panels: [
            {
                title: "attributes",
                type: "info",
                properties: {include: []},
                attributes: {exclude: ["location"]}
            },
            {
                type: "history",
                column: 1
            }
        ]
    }
};
```

Every panel supports `type`, `title`, `hide`, `column`, `hideOnMobile` and `panelStyles`. The available types are
`info`, `setup`, `history`, `group`, `linkedUsers` and `alarm.linkedAlarms`; `info`, `setup`, `history` and `group`
each add their own options on top, such as the `properties` and `attributes` include/exclude lists shown above.

`viewerStyles` applies inline styles to the container that holds both columns.

### View providers
Rendering can be overridden per panel, property or attribute by supplying `panelViewProvider`, `propertyViewProvider`
or `attributeViewProvider`. Each is called before the built in rendering and returning `undefined` falls back to it.

```typescript
const config: ViewerConfig = {
    default: {
        attributeViewProvider: (asset, attribute, hostElement, viewerConfig, panelConfig) => {
            if (attribute.name !== "notes") {
                return undefined;
            }
            return html`<my-notes-editor .asset="${asset}"></my-notes-editor>`;
        }
    }
};
```

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-asset-viewer.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-asset-viewer
