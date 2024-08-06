[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / AgentLink

# Interface: AgentLink

## Properties

### id?

> `optional` **id**: `any`

#### Source

model.ts:163

***

### messageMatchFilters?

> `optional` **messageMatchFilters**: [`ValueFilterUnion`](../type-aliases/ValueFilterUnion.md)[]

ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate

#### Source

model.ts:187

***

### messageMatchPredicate?

> `optional` **messageMatchPredicate**: [`ValuePredicateUnion`](../type-aliases/ValuePredicateUnion.md)

The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.

#### Source

model.ts:183

***

### type

> **type**: `string`

#### Source

model.ts:162

***

### updateOnWrite?

> `optional` **updateOnWrite**: `any`

Don't expect a response from the protocol just update the attribute immediately on write

#### Source

model.ts:191

***

### valueConverter?

> `optional` **valueConverter**: `object`

Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute

#### Index signature

 \[`index`: `string`\]: `any`

#### Source

model.ts:171

***

### valueFilters?

> `optional` **valueFilters**: [`ValueFilterUnion`](../type-aliases/ValueFilterUnion.md)[]

Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order

#### Source

model.ts:167

***

### writeValue?

> `optional` **writeValue**: `any`

String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol (particularly useful for executable attributes)

#### Source

model.ts:179

***

### writeValueConverter?

> `optional` **writeValueConverter**: `object`

Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion

#### Index signature

 \[`index`: `string`\]: `any`

#### Source

model.ts:175
