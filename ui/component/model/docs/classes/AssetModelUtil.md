[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / AssetModelUtil

# Class: AssetModelUtil

## Constructors

### new AssetModelUtil()

> **new AssetModelUtil**(): [`AssetModelUtil`](AssetModelUtil.md)

#### Returns

[`AssetModelUtil`](AssetModelUtil.md)

## Properties

### \_assetTypeInfos

> `static` **\_assetTypeInfos**: [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)[] = `[]`

#### Source

[util.ts:12](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L12)

***

### \_metaItemDescriptors

> `static` **\_metaItemDescriptors**: [`MetaItemDescriptor`](../interfaces/MetaItemDescriptor.md)[] = `[]`

#### Source

[util.ts:13](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L13)

***

### \_valueDescriptors

> `static` **\_valueDescriptors**: [`ValueDescriptor`](../interfaces/ValueDescriptor.md)[] = `[]`

#### Source

[util.ts:14](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L14)

## Methods

### getAssetDescriptor()

> `static` **getAssetDescriptor**(`type`?): `undefined` \| [`AssetDescriptor`](../interfaces/AssetDescriptor.md)

#### Parameters

• **type?**: `string` \| [`AssetDescriptor`](../interfaces/AssetDescriptor.md) \| [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)

#### Returns

`undefined` \| [`AssetDescriptor`](../interfaces/AssetDescriptor.md)

#### Source

[util.ts:50](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L50)

***

### getAssetDescriptorColour()

> `static` **getAssetDescriptorColour**(`typeOrDescriptor`, `fallbackColor`?): `undefined` \| `string`

#### Parameters

• **typeOrDescriptor**: `undefined` \| `string` \| [`AssetDescriptor`](../interfaces/AssetDescriptor.md) \| [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)

• **fallbackColor?**: `string`

#### Returns

`undefined` \| `string`

#### Source

[util.ts:213](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L213)

***

### getAssetDescriptorIcon()

> `static` **getAssetDescriptorIcon**(`typeOrDescriptor`, `fallbackIcon`?): `undefined` \| `string`

#### Parameters

• **typeOrDescriptor**: `undefined` \| `string` \| [`AssetDescriptor`](../interfaces/AssetDescriptor.md) \| [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)

• **fallbackIcon?**: `string`

#### Returns

`undefined` \| `string`

#### Source

[util.ts:218](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L218)

***

### getAssetDescriptors()

> `static` **getAssetDescriptors**(): [`AssetDescriptor`](../interfaces/AssetDescriptor.md)[]

#### Returns

[`AssetDescriptor`](../interfaces/AssetDescriptor.md)[]

#### Source

[util.ts:16](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L16)

***

### getAssetTypeInfo()

> `static` **getAssetTypeInfo**(`type`): `undefined` \| [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)

#### Parameters

• **type**: `string` \| [`AssetDescriptor`](../interfaces/AssetDescriptor.md) \| [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)

#### Returns

`undefined` \| [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)

#### Source

[util.ts:32](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L32)

***

### getAssetTypeInfos()

> `static` **getAssetTypeInfos**(): [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)[]

#### Returns

[`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)[]

#### Source

[util.ts:28](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L28)

***

### getAttributeAndValueDescriptors()

> `static` **getAttributeAndValueDescriptors**(`assetType`, `attributeNameOrDescriptor`, `attribute`?): [`undefined` \| [`AttributeDescriptor`](../interfaces/AttributeDescriptor.md), `undefined` \| [`ValueDescriptor`](../interfaces/ValueDescriptor.md)]

#### Parameters

• **assetType**: `undefined` \| `string`

• **attributeNameOrDescriptor**: `undefined` \| `string` \| [`AttributeDescriptor`](../interfaces/AttributeDescriptor.md)

• **attribute?**: [`Attribute`](../interfaces/Attribute.md)\<`any`\>

#### Returns

[`undefined` \| [`AttributeDescriptor`](../interfaces/AttributeDescriptor.md), `undefined` \| [`ValueDescriptor`](../interfaces/ValueDescriptor.md)]

#### Source

[util.ts:171](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L171)

***

### getAttributeDescriptor()

> `static` **getAttributeDescriptor**(`attributeName`, `assetTypeOrDescriptor`): `undefined` \| [`AttributeDescriptor`](../interfaces/AttributeDescriptor.md)

#### Parameters

• **attributeName**: `string`

• **assetTypeOrDescriptor**: `string` \| [`AssetDescriptor`](../interfaces/AssetDescriptor.md) \| [`AssetTypeInfo`](../interfaces/AssetTypeInfo.md)

#### Returns

`undefined` \| [`AttributeDescriptor`](../interfaces/AttributeDescriptor.md)

#### Source

[util.ts:69](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L69)

***

### getMetaItemDescriptor()

> `static` **getMetaItemDescriptor**(`name`?): `undefined` \| [`MetaItemDescriptor`](../interfaces/MetaItemDescriptor.md)

#### Parameters

• **name?**: `string`

#### Returns

`undefined` \| [`MetaItemDescriptor`](../interfaces/MetaItemDescriptor.md)

#### Source

[util.ts:204](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L204)

***

### getMetaItemDescriptors()

> `static` **getMetaItemDescriptors**(): [`MetaItemDescriptor`](../interfaces/MetaItemDescriptor.md)[]

#### Returns

[`MetaItemDescriptor`](../interfaces/MetaItemDescriptor.md)[]

#### Source

[util.ts:20](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L20)

***

### getValueDescriptor()

> `static` **getValueDescriptor**(`name`?): `undefined` \| [`ValueDescriptor`](../interfaces/ValueDescriptor.md)

#### Parameters

• **name?**: `string`

#### Returns

`undefined` \| [`ValueDescriptor`](../interfaces/ValueDescriptor.md)

#### Source

[util.ts:83](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L83)

***

### getValueDescriptors()

> `static` **getValueDescriptors**(): [`ValueDescriptor`](../interfaces/ValueDescriptor.md)[]

#### Returns

[`ValueDescriptor`](../interfaces/ValueDescriptor.md)[]

#### Source

[util.ts:24](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L24)

***

### resolveValueDescriptor()

> `static` **resolveValueDescriptor**(`valueHolder`, `descriptorOrValueType`): `undefined` \| [`ValueDescriptor`](../interfaces/ValueDescriptor.md)

#### Parameters

• **valueHolder**: `undefined` \| [`ValueHolder`](../interfaces/ValueHolder.md)\<`any`\>

• **descriptorOrValueType**: `undefined` \| `string` \| [`ValueDescriptorHolder`](../interfaces/ValueDescriptorHolder.md) \| [`ValueDescriptor`](../interfaces/ValueDescriptor.md)

#### Returns

`undefined` \| [`ValueDescriptor`](../interfaces/ValueDescriptor.md)

#### Source

[util.ts:107](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L107)

***

### resolveValueTypeFromValue()

> `static` **resolveValueTypeFromValue**(`value`): `undefined` \| `string`

#### Parameters

• **value**: `any`

#### Returns

`undefined` \| `string`

#### Source

[util.ts:130](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/model/src/util.ts#L130)
