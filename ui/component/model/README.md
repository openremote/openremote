# @openremote/model
[![NPM Version][npm-image]][npm-url]

Typescript type definitions for the OpenRemote object model.

## Install
```bash
npm i @openremote/model
yarn add @openremote/model 
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The package is generated from the manager's Java model, so the types mirror the JSON returned by the REST API and the
event bus. It is types only, with no runtime behaviour beyond the enums and `AssetModelUtil`.

```typescript
import {Asset, WellknownAssets} from "@openremote/model";

const asset: Asset = {
    name: "Living room",
    type: WellknownAssets.ROOMASSET
};
```

Names that exist as strings in the manager, such as asset types, attribute names, value types and meta items, have a
matching `Wellknown*` enum, which avoids spreading string literals through an app.

### AssetModelUtil
`AssetModelUtil` resolves descriptors from the asset model that `@openremote/core` downloads during initialisation,
so it only returns results once the `Manager` `init` method has completed.

```typescript
import {AssetModelUtil} from "@openremote/model";

const descriptor = AssetModelUtil.getAssetDescriptor(asset.type);
const icon = AssetModelUtil.getAssetDescriptorIcon(descriptor);
const [attributeDescriptor, valueDescriptor] =
    AssetModelUtil.getAttributeAndValueDescriptors(asset.type, "temperature");
```


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/model.svg
[npm-url]: https://www.npmjs.com/package/@openremote/model
