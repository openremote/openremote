[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / SubStringValueFilter

# Interface: SubStringValueFilter

Returns the substring beginning at the specified index (inclusive) and ending at the optional endIndex (exclusive); if endIndex is not supplied then the remainder of the string is returned; negative values can be used to indicate a backwards count from the length of the string e.g. -1 means length-1

## Extends

- [`ValueFilter`](ValueFilter.md)

## Properties

### beginIndex?

> `optional` **beginIndex**: `number`

#### Source

model.ts:1670

***

### endIndex?

> `optional` **endIndex**: `any`

#### Source

model.ts:1671

***

### type

> **type**: `"substring"`

#### Overrides

[`ValueFilter`](ValueFilter.md).[`type`](ValueFilter.md#type)

#### Source

model.ts:1669
