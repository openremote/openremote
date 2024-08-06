[**@openremote/or-attribute-card**](../README.md) • **Docs**

***

[@openremote/or-attribute-card](../globals.md) / OrAttributeCard

# Class: OrAttributeCard

## Extends

- `LitElement`

## Other

### new OrAttributeCard()

> **new OrAttributeCard**(): [`OrAttributeCard`](OrAttributeCard.md)

#### Returns

[`OrAttributeCard`](OrAttributeCard.md)

#### Overrides

`LitElement.constructor`

#### Source

[ui/component/or-attribute-card/src/index.ts:240](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L240)

***

### ATTRIBUTE\_NODE

> `readonly` **ATTRIBUTE\_NODE**: `2`

#### Inherited from

`LitElement.ATTRIBUTE_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16205

***

### CDATA\_SECTION\_NODE

> `readonly` **CDATA\_SECTION\_NODE**: `4`

node is a CDATASection node.

#### Inherited from

`LitElement.CDATA_SECTION_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16209

***

### COMMENT\_NODE

> `readonly` **COMMENT\_NODE**: `8`

node is a Comment node.

#### Inherited from

`LitElement.COMMENT_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16215

***

### DOCUMENT\_FRAGMENT\_NODE

> `readonly` **DOCUMENT\_FRAGMENT\_NODE**: `11`

node is a DocumentFragment node.

#### Inherited from

`LitElement.DOCUMENT_FRAGMENT_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16221

***

### DOCUMENT\_NODE

> `readonly` **DOCUMENT\_NODE**: `9`

node is a document.

#### Inherited from

`LitElement.DOCUMENT_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16217

***

### DOCUMENT\_POSITION\_CONTAINED\_BY

> `readonly` **DOCUMENT\_POSITION\_CONTAINED\_BY**: `16`

Set when other is a descendant of node.

#### Inherited from

`LitElement.DOCUMENT_POSITION_CONTAINED_BY`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16232

***

### DOCUMENT\_POSITION\_CONTAINS

> `readonly` **DOCUMENT\_POSITION\_CONTAINS**: `8`

Set when other is an ancestor of node.

#### Inherited from

`LitElement.DOCUMENT_POSITION_CONTAINS`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16230

***

### DOCUMENT\_POSITION\_DISCONNECTED

> `readonly` **DOCUMENT\_POSITION\_DISCONNECTED**: `1`

Set when node and other are not in the same tree.

#### Inherited from

`LitElement.DOCUMENT_POSITION_DISCONNECTED`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16224

***

### DOCUMENT\_POSITION\_FOLLOWING

> `readonly` **DOCUMENT\_POSITION\_FOLLOWING**: `4`

Set when other is following node.

#### Inherited from

`LitElement.DOCUMENT_POSITION_FOLLOWING`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16228

***

### DOCUMENT\_POSITION\_IMPLEMENTATION\_SPECIFIC

> `readonly` **DOCUMENT\_POSITION\_IMPLEMENTATION\_SPECIFIC**: `32`

#### Inherited from

`LitElement.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16233

***

### DOCUMENT\_POSITION\_PRECEDING

> `readonly` **DOCUMENT\_POSITION\_PRECEDING**: `2`

Set when other is preceding node.

#### Inherited from

`LitElement.DOCUMENT_POSITION_PRECEDING`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16226

***

### DOCUMENT\_TYPE\_NODE

> `readonly` **DOCUMENT\_TYPE\_NODE**: `10`

node is a doctype.

#### Inherited from

`LitElement.DOCUMENT_TYPE_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16219

***

### ELEMENT\_NODE

> `readonly` **ELEMENT\_NODE**: `1`

node is an element.

#### Inherited from

`LitElement.ELEMENT_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16204

***

### ENTITY\_NODE

> `readonly` **ENTITY\_NODE**: `6`

#### Inherited from

`LitElement.ENTITY_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16211

***

### ENTITY\_REFERENCE\_NODE

> `readonly` **ENTITY\_REFERENCE\_NODE**: `5`

#### Inherited from

`LitElement.ENTITY_REFERENCE_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16210

***

### NOTATION\_NODE

> `readonly` **NOTATION\_NODE**: `12`

#### Inherited from

`LitElement.NOTATION_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16222

***

### PROCESSING\_INSTRUCTION\_NODE

> `readonly` **PROCESSING\_INSTRUCTION\_NODE**: `7`

node is a ProcessingInstruction node.

#### Inherited from

`LitElement.PROCESSING_INSTRUCTION_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16213

***

### TEXT\_NODE

> `readonly` **TEXT\_NODE**: `3`

node is a Text node.

#### Inherited from

`LitElement.TEXT_NODE`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16207

***

### \_chart?

> `private` `optional` **\_chart**: `Chart`\<`"line"`, `ScatterDataPoint`[], `unknown`\>

#### Source

[ui/component/or-attribute-card/src/index.ts:229](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L229)

***

### \_chartElem

> `private` **\_chartElem**: `HTMLCanvasElement`

#### Source

[ui/component/or-attribute-card/src/index.ts:228](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L228)

***

### \_endOfPeriod?

> `protected` `optional` **\_endOfPeriod**: `number`

#### Source

[ui/component/or-attribute-card/src/index.ts:231](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L231)

***

### \_loading

> `protected` **\_loading**: `boolean` = `false`

#### Source

[ui/component/or-attribute-card/src/index.ts:218](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L218)

***

### \_startOfPeriod?

> `protected` `optional` **\_startOfPeriod**: `number`

#### Source

[ui/component/or-attribute-card/src/index.ts:230](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L230)

***

### \_style

> `protected` **\_style**: `CSSStyleDeclaration`

#### Source

[ui/component/or-attribute-card/src/index.ts:187](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L187)

***

### accessKey

> **accessKey**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/accessKey)

#### Inherited from

`LitElement.accessKey`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10053

***

### accessKeyLabel

> `readonly` **accessKeyLabel**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/accessKeyLabel)

#### Inherited from

`LitElement.accessKeyLabel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10055

***

### ariaAtomic

> **ariaAtomic**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaAtomic)

#### Inherited from

`LitElement.ariaAtomic`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2209

***

### ariaAutoComplete

> **ariaAutoComplete**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaAutoComplete)

#### Inherited from

`LitElement.ariaAutoComplete`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2211

***

### ariaBusy

> **ariaBusy**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaBusy)

#### Inherited from

`LitElement.ariaBusy`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2213

***

### ariaChecked

> **ariaChecked**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaChecked)

#### Inherited from

`LitElement.ariaChecked`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2215

***

### ariaColCount

> **ariaColCount**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaColCount)

#### Inherited from

`LitElement.ariaColCount`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2217

***

### ariaColIndex

> **ariaColIndex**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaColIndex)

#### Inherited from

`LitElement.ariaColIndex`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2219

***

### ariaColSpan

> **ariaColSpan**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaColSpan)

#### Inherited from

`LitElement.ariaColSpan`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2221

***

### ariaCurrent

> **ariaCurrent**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaCurrent)

#### Inherited from

`LitElement.ariaCurrent`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2223

***

### ariaDisabled

> **ariaDisabled**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaDisabled)

#### Inherited from

`LitElement.ariaDisabled`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2225

***

### ariaExpanded

> **ariaExpanded**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaExpanded)

#### Inherited from

`LitElement.ariaExpanded`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2227

***

### ariaHasPopup

> **ariaHasPopup**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaHasPopup)

#### Inherited from

`LitElement.ariaHasPopup`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2229

***

### ariaHidden

> **ariaHidden**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaHidden)

#### Inherited from

`LitElement.ariaHidden`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2231

***

### ariaInvalid

> **ariaInvalid**: `null` \| `string`

#### Inherited from

`LitElement.ariaInvalid`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2232

***

### ariaKeyShortcuts

> **ariaKeyShortcuts**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaKeyShortcuts)

#### Inherited from

`LitElement.ariaKeyShortcuts`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2234

***

### ariaLabel

> **ariaLabel**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaLabel)

#### Inherited from

`LitElement.ariaLabel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2236

***

### ariaLevel

> **ariaLevel**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaLevel)

#### Inherited from

`LitElement.ariaLevel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2238

***

### ariaLive

> **ariaLive**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaLive)

#### Inherited from

`LitElement.ariaLive`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2240

***

### ariaModal

> **ariaModal**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaModal)

#### Inherited from

`LitElement.ariaModal`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2242

***

### ariaMultiLine

> **ariaMultiLine**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaMultiLine)

#### Inherited from

`LitElement.ariaMultiLine`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2244

***

### ariaMultiSelectable

> **ariaMultiSelectable**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaMultiSelectable)

#### Inherited from

`LitElement.ariaMultiSelectable`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2246

***

### ariaOrientation

> **ariaOrientation**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaOrientation)

#### Inherited from

`LitElement.ariaOrientation`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2248

***

### ariaPlaceholder

> **ariaPlaceholder**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaPlaceholder)

#### Inherited from

`LitElement.ariaPlaceholder`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2250

***

### ariaPosInSet

> **ariaPosInSet**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaPosInSet)

#### Inherited from

`LitElement.ariaPosInSet`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2252

***

### ariaPressed

> **ariaPressed**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaPressed)

#### Inherited from

`LitElement.ariaPressed`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2254

***

### ariaReadOnly

> **ariaReadOnly**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaReadOnly)

#### Inherited from

`LitElement.ariaReadOnly`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2256

***

### ariaRequired

> **ariaRequired**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaRequired)

#### Inherited from

`LitElement.ariaRequired`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2258

***

### ariaRoleDescription

> **ariaRoleDescription**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaRoleDescription)

#### Inherited from

`LitElement.ariaRoleDescription`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2260

***

### ariaRowCount

> **ariaRowCount**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaRowCount)

#### Inherited from

`LitElement.ariaRowCount`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2262

***

### ariaRowIndex

> **ariaRowIndex**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaRowIndex)

#### Inherited from

`LitElement.ariaRowIndex`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2264

***

### ariaRowSpan

> **ariaRowSpan**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaRowSpan)

#### Inherited from

`LitElement.ariaRowSpan`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2266

***

### ariaSelected

> **ariaSelected**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaSelected)

#### Inherited from

`LitElement.ariaSelected`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2268

***

### ariaSetSize

> **ariaSetSize**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaSetSize)

#### Inherited from

`LitElement.ariaSetSize`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2270

***

### ariaSort

> **ariaSort**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaSort)

#### Inherited from

`LitElement.ariaSort`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2272

***

### ariaValueMax

> **ariaValueMax**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaValueMax)

#### Inherited from

`LitElement.ariaValueMax`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2274

***

### ariaValueMin

> **ariaValueMin**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaValueMin)

#### Inherited from

`LitElement.ariaValueMin`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2276

***

### ariaValueNow

> **ariaValueNow**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaValueNow)

#### Inherited from

`LitElement.ariaValueNow`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2278

***

### ariaValueText

> **ariaValueText**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/ariaValueText)

#### Inherited from

`LitElement.ariaValueText`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2280

***

### asset?

> `private` `optional` **asset**: `Asset`

#### Source

[ui/component/or-attribute-card/src/index.ts:224](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L224)

***

### assetAttributes

> `private` **assetAttributes**: [`number`, `Attribute`\<`any`\>][] = `[]`

#### Source

[ui/component/or-attribute-card/src/index.ts:193](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L193)

***

### assets

> **assets**: `Asset`[] = `[]`

#### Source

[ui/component/or-attribute-card/src/index.ts:190](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L190)

***

### assignedSlot

> `readonly` **assignedSlot**: `null` \| `HTMLSlotElement`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/assignedSlot)

#### Inherited from

`LitElement.assignedSlot`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:21296

***

### attributeStyleMap

> `readonly` **attributeStyleMap**: `StylePropertyMap`

#### Inherited from

`LitElement.attributeStyleMap`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7855

***

### attributes

> `readonly` **attributes**: `NamedNodeMap`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/attributes)

#### Inherited from

`LitElement.attributes`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7600

***

### autocapitalize

> **autocapitalize**: `string`

#### Inherited from

`LitElement.autocapitalize`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10056

***

### autofocus

> **autofocus**: `boolean`

#### Inherited from

`LitElement.autofocus`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:12045

***

### baseURI

> `readonly` **baseURI**: `string`

Returns node's node document's document base URL.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/baseURI)

#### Inherited from

`LitElement.baseURI`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16074

***

### childElementCount

> `readonly` **childElementCount**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/childElementCount)

#### Inherited from

`LitElement.childElementCount`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16763

***

### childNodes

> `readonly` **childNodes**: `NodeListOf`\<`ChildNode`\>

Returns the children.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/childNodes)

#### Inherited from

`LitElement.childNodes`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16080

***

### children

> `readonly` **children**: `HTMLCollection`

Returns the child elements.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/children)

#### Inherited from

`LitElement.children`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16769

***

### classList

> `readonly` **classList**: `DOMTokenList`

Allows for manipulation of element's class content attribute as a set of whitespace-separated tokens through a DOMTokenList object.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/classList)

#### Inherited from

`LitElement.classList`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7606

***

### className

> **className**: `string`

Returns the value of element's class content attribute. Can be set to change it.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/className)

#### Inherited from

`LitElement.className`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7612

***

### clientHeight

> `readonly` **clientHeight**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/clientHeight)

#### Inherited from

`LitElement.clientHeight`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7614

***

### clientLeft

> `readonly` **clientLeft**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/clientLeft)

#### Inherited from

`LitElement.clientLeft`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7616

***

### clientTop

> `readonly` **clientTop**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/clientTop)

#### Inherited from

`LitElement.clientTop`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7618

***

### clientWidth

> `readonly` **clientWidth**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/clientWidth)

#### Inherited from

`LitElement.clientWidth`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7620

***

### contentEditable

> **contentEditable**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/contentEditable)

#### Inherited from

`LitElement.contentEditable`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7862

***

### data?

> `private` `optional` **data**: `ValueDatapoint`\<`any`\>[] = `undefined`

#### Source

[ui/component/or-attribute-card/src/index.ts:196](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L196)

***

### dataset

> `readonly` **dataset**: `DOMStringMap`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/dataset)

#### Inherited from

`LitElement.dataset`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:12047

***

### delta?

> `private` `optional` **delta**: `object` = `undefined`

#### unit?

> `optional` **unit**: `string`

#### val?

> `optional` **val**: `number`

#### Source

[ui/component/or-attribute-card/src/index.ts:208](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L208)

***

### deltaFormat

> `private` **deltaFormat**: `"absolute"` \| `"percentage"` = `"absolute"`

#### Source

[ui/component/or-attribute-card/src/index.ts:212](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L212)

***

### deltaPlus

> `private` **deltaPlus**: `string` = `""`

#### Source

[ui/component/or-attribute-card/src/index.ts:210](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L210)

***

### dir

> **dir**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/dir)

#### Inherited from

`LitElement.dir`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10058

***

### draggable

> **draggable**: `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/draggable)

#### Inherited from

`LitElement.draggable`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10060

***

### enterKeyHint

> **enterKeyHint**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/enterKeyHint)

#### Inherited from

`LitElement.enterKeyHint`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7864

***

### error

> `private` **error**: `boolean` = `false`

#### Source

[ui/component/or-attribute-card/src/index.ts:220](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L220)

***

### firstChild

> `readonly` **firstChild**: `null` \| `ChildNode`

Returns the first child.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/firstChild)

#### Inherited from

`LitElement.firstChild`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16086

***

### firstElementChild

> `readonly` **firstElementChild**: `null` \| `Element`

Returns the first child that is an element, and null otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/firstElementChild)

#### Inherited from

`LitElement.firstElementChild`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16775

***

### formattedMainValue?

> `private` `optional` **formattedMainValue**: `object`

#### unit

> **unit**: `string`

#### value

> **value**: `undefined` \| `number`

#### Source

[ui/component/or-attribute-card/src/index.ts:225](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L225)

***

### hidden

> **hidden**: `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/hidden)

#### Inherited from

`LitElement.hidden`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10062

***

### id

> **id**: `string`

Returns the value of element's id content attribute. Can be set to change it.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/id)

#### Inherited from

`LitElement.id`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7626

***

### inert

> **inert**: `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/inert)

#### Inherited from

`LitElement.inert`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10064

***

### innerHTML

> **innerHTML**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/innerHTML)

#### Inherited from

`LitElement.innerHTML`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:14279

***

### innerText

> **innerText**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/innerText)

#### Inherited from

`LitElement.innerText`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10066

***

### inputMode

> **inputMode**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/inputMode)

#### Inherited from

`LitElement.inputMode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7866

***

### isConnected

> `readonly` **isConnected**: `boolean`

Returns true if node is connected and false otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/isConnected)

#### Inherited from

`LitElement.isConnected`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16092

***

### isContentEditable

> `readonly` **isContentEditable**: `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/isContentEditable)

#### Inherited from

`LitElement.isContentEditable`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7868

***

### lang

> **lang**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/lang)

#### Inherited from

`LitElement.lang`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10068

***

### lastChild

> `readonly` **lastChild**: `null` \| `ChildNode`

Returns the last child.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/lastChild)

#### Inherited from

`LitElement.lastChild`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16098

***

### lastElementChild

> `readonly` **lastElementChild**: `null` \| `Element`

Returns the last child that is an element, and null otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/lastElementChild)

#### Inherited from

`LitElement.lastElementChild`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16781

***

### localName

> `readonly` **localName**: `string`

Returns the local name.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/localName)

#### Inherited from

`LitElement.localName`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7632

***

### mainValue?

> `private` `optional` **mainValue**: `number`

#### Source

[ui/component/or-attribute-card/src/index.ts:202](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L202)

***

### mainValueDecimals

> `private` **mainValueDecimals**: `number` = `2`

#### Source

[ui/component/or-attribute-card/src/index.ts:204](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L204)

***

### mainValueSize

> `private` **mainValueSize**: `"s"` \| `"m"` \| `"l"` \| `"xs"` \| `"xl"` = `"m"`

#### Source

[ui/component/or-attribute-card/src/index.ts:206](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L206)

***

### namespaceURI

> `readonly` **namespaceURI**: `null` \| `string`

Returns the namespace.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/namespaceURI)

#### Inherited from

`LitElement.namespaceURI`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7638

***

### nextElementSibling

> `readonly` **nextElementSibling**: `null` \| `Element`

Returns the first following sibling that is an element, and null otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/CharacterData/nextElementSibling)

#### Inherited from

`LitElement.nextElementSibling`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16354

***

### nextSibling

> `readonly` **nextSibling**: `null` \| `ChildNode`

Returns the next sibling.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/nextSibling)

#### Inherited from

`LitElement.nextSibling`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16104

***

### nodeName

> `readonly` **nodeName**: `string`

Returns a string appropriate for the type of node.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/nodeName)

#### Inherited from

`LitElement.nodeName`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16110

***

### nodeType

> `readonly` **nodeType**: `number`

Returns the type of node.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/nodeType)

#### Inherited from

`LitElement.nodeType`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16116

***

### nodeValue

> **nodeValue**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/nodeValue)

#### Inherited from

`LitElement.nodeValue`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16118

***

### nonce?

> `optional` **nonce**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/nonce)

#### Inherited from

`LitElement.nonce`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:12049

***

### offsetHeight

> `readonly` **offsetHeight**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/offsetHeight)

#### Inherited from

`LitElement.offsetHeight`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10070

***

### offsetLeft

> `readonly` **offsetLeft**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/offsetLeft)

#### Inherited from

`LitElement.offsetLeft`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10072

***

### offsetParent

> `readonly` **offsetParent**: `null` \| `Element`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/offsetParent)

#### Inherited from

`LitElement.offsetParent`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10074

***

### offsetTop

> `readonly` **offsetTop**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/offsetTop)

#### Inherited from

`LitElement.offsetTop`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10076

***

### offsetWidth

> `readonly` **offsetWidth**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/offsetWidth)

#### Inherited from

`LitElement.offsetWidth`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10078

***

### onabort

> **onabort**: `null` \| (`this`, `ev`) => `any`

Fires when the user aborts the download.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/abort_event)

#### Inherited from

`LitElement.onabort`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8948

***

### onanimationcancel

> **onanimationcancel**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animationcancel_event)

#### Inherited from

`LitElement.onanimationcancel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8950

***

### onanimationend

> **onanimationend**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animationend_event)

#### Inherited from

`LitElement.onanimationend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8952

***

### onanimationiteration

> **onanimationiteration**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animationiteration_event)

#### Inherited from

`LitElement.onanimationiteration`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8954

***

### onanimationstart

> **onanimationstart**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animationstart_event)

#### Inherited from

`LitElement.onanimationstart`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8956

***

### onauxclick

> **onauxclick**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/auxclick_event)

#### Inherited from

`LitElement.onauxclick`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8958

***

### onbeforeinput

> **onbeforeinput**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/beforeinput_event)

#### Inherited from

`LitElement.onbeforeinput`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8960

***

### onblur

> **onblur**: `null` \| (`this`, `ev`) => `any`

Fires when the object loses the input focus.

#### Param

The focus event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/blur_event)

#### Inherited from

`LitElement.onblur`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8967

***

### oncancel

> **oncancel**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLDialogElement/cancel_event)

#### Inherited from

`LitElement.oncancel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8969

***

### oncanplay

> **oncanplay**: `null` \| (`this`, `ev`) => `any`

Occurs when playback is possible, but would require further buffering.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/canplay_event)

#### Inherited from

`LitElement.oncanplay`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8976

***

### oncanplaythrough

> **oncanplaythrough**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/canplaythrough_event)

#### Inherited from

`LitElement.oncanplaythrough`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8978

***

### onchange

> **onchange**: `null` \| (`this`, `ev`) => `any`

Fires when the contents of the object or selection have changed.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/change_event)

#### Inherited from

`LitElement.onchange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8985

***

### onclick

> **onclick**: `null` \| (`this`, `ev`) => `any`

Fires when the user clicks the left mouse button on the object

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/click_event)

#### Inherited from

`LitElement.onclick`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8992

***

### onclose

> **onclose**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLDialogElement/close_event)

#### Inherited from

`LitElement.onclose`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8994

***

### oncontextmenu

> **oncontextmenu**: `null` \| (`this`, `ev`) => `any`

Fires when the user clicks the right mouse button in the client area, opening the context menu.

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/contextmenu_event)

#### Inherited from

`LitElement.oncontextmenu`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9001

***

### oncopy

> **oncopy**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/copy_event)

#### Inherited from

`LitElement.oncopy`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9003

***

### oncuechange

> **oncuechange**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLTrackElement/cuechange_event)

#### Inherited from

`LitElement.oncuechange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9005

***

### oncut

> **oncut**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/cut_event)

#### Inherited from

`LitElement.oncut`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9007

***

### ondblclick

> **ondblclick**: `null` \| (`this`, `ev`) => `any`

Fires when the user double-clicks the object.

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/dblclick_event)

#### Inherited from

`LitElement.ondblclick`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9014

***

### ondrag

> **ondrag**: `null` \| (`this`, `ev`) => `any`

Fires on the source object continuously during a drag operation.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/drag_event)

#### Inherited from

`LitElement.ondrag`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9021

***

### ondragend

> **ondragend**: `null` \| (`this`, `ev`) => `any`

Fires on the source object when the user releases the mouse at the close of a drag operation.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/dragend_event)

#### Inherited from

`LitElement.ondragend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9028

***

### ondragenter

> **ondragenter**: `null` \| (`this`, `ev`) => `any`

Fires on the target element when the user drags the object to a valid drop target.

#### Param

The drag event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/dragenter_event)

#### Inherited from

`LitElement.ondragenter`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9035

***

### ondragleave

> **ondragleave**: `null` \| (`this`, `ev`) => `any`

Fires on the target object when the user moves the mouse out of a valid drop target during a drag operation.

#### Param

The drag event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/dragleave_event)

#### Inherited from

`LitElement.ondragleave`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9042

***

### ondragover

> **ondragover**: `null` \| (`this`, `ev`) => `any`

Fires on the target element continuously while the user drags the object over a valid drop target.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/dragover_event)

#### Inherited from

`LitElement.ondragover`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9049

***

### ondragstart

> **ondragstart**: `null` \| (`this`, `ev`) => `any`

Fires on the source object when the user starts to drag a text selection or selected object.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/dragstart_event)

#### Inherited from

`LitElement.ondragstart`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9056

***

### ondrop

> **ondrop**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/drop_event)

#### Inherited from

`LitElement.ondrop`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9058

***

### ondurationchange

> **ondurationchange**: `null` \| (`this`, `ev`) => `any`

Occurs when the duration attribute is updated.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/durationchange_event)

#### Inherited from

`LitElement.ondurationchange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9065

***

### onemptied

> **onemptied**: `null` \| (`this`, `ev`) => `any`

Occurs when the media element is reset to its initial state.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/emptied_event)

#### Inherited from

`LitElement.onemptied`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9072

***

### onended

> **onended**: `null` \| (`this`, `ev`) => `any`

Occurs when the end of playback is reached.

#### Param

The event

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/ended_event)

#### Inherited from

`LitElement.onended`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9079

***

### onerror

> **onerror**: `OnErrorEventHandler`

Fires when an error occurs during object loading.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/error_event)

#### Inherited from

`LitElement.onerror`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9086

***

### onfocus

> **onfocus**: `null` \| (`this`, `ev`) => `any`

Fires when the object receives focus.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/focus_event)

#### Inherited from

`LitElement.onfocus`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9093

***

### onformdata

> **onformdata**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLFormElement/formdata_event)

#### Inherited from

`LitElement.onformdata`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9095

***

### onfullscreenchange

> **onfullscreenchange**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/fullscreenchange_event)

#### Inherited from

`LitElement.onfullscreenchange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7640

***

### onfullscreenerror

> **onfullscreenerror**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/fullscreenerror_event)

#### Inherited from

`LitElement.onfullscreenerror`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7642

***

### ongotpointercapture

> **ongotpointercapture**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/gotpointercapture_event)

#### Inherited from

`LitElement.ongotpointercapture`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9097

***

### oninput

> **oninput**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/input_event)

#### Inherited from

`LitElement.oninput`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9099

***

### oninvalid

> **oninvalid**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLInputElement/invalid_event)

#### Inherited from

`LitElement.oninvalid`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9101

***

### onkeydown

> **onkeydown**: `null` \| (`this`, `ev`) => `any`

Fires when the user presses a key.

#### Param

The keyboard event

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/keydown_event)

#### Inherited from

`LitElement.onkeydown`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9108

***

### ~~onkeypress~~

> **onkeypress**: `null` \| (`this`, `ev`) => `any`

Fires when the user presses an alphanumeric key.

#### Param

The event.

#### Deprecated

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/keypress_event)

#### Inherited from

`LitElement.onkeypress`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9116

***

### onkeyup

> **onkeyup**: `null` \| (`this`, `ev`) => `any`

Fires when the user releases a key.

#### Param

The keyboard event

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/keyup_event)

#### Inherited from

`LitElement.onkeyup`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9123

***

### onload

> **onload**: `null` \| (`this`, `ev`) => `any`

Fires immediately after the browser loads the object.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/SVGElement/load_event)

#### Inherited from

`LitElement.onload`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9130

***

### onloadeddata

> **onloadeddata**: `null` \| (`this`, `ev`) => `any`

Occurs when media data is loaded at the current playback position.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/loadeddata_event)

#### Inherited from

`LitElement.onloadeddata`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9137

***

### onloadedmetadata

> **onloadedmetadata**: `null` \| (`this`, `ev`) => `any`

Occurs when the duration and dimensions of the media have been determined.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/loadedmetadata_event)

#### Inherited from

`LitElement.onloadedmetadata`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9144

***

### onloadstart

> **onloadstart**: `null` \| (`this`, `ev`) => `any`

Occurs when Internet Explorer begins looking for media data.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/loadstart_event)

#### Inherited from

`LitElement.onloadstart`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9151

***

### onlostpointercapture

> **onlostpointercapture**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/lostpointercapture_event)

#### Inherited from

`LitElement.onlostpointercapture`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9153

***

### onmousedown

> **onmousedown**: `null` \| (`this`, `ev`) => `any`

Fires when the user clicks the object with either mouse button.

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/mousedown_event)

#### Inherited from

`LitElement.onmousedown`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9160

***

### onmouseenter

> **onmouseenter**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/mouseenter_event)

#### Inherited from

`LitElement.onmouseenter`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9162

***

### onmouseleave

> **onmouseleave**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/mouseleave_event)

#### Inherited from

`LitElement.onmouseleave`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9164

***

### onmousemove

> **onmousemove**: `null` \| (`this`, `ev`) => `any`

Fires when the user moves the mouse over the object.

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/mousemove_event)

#### Inherited from

`LitElement.onmousemove`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9171

***

### onmouseout

> **onmouseout**: `null` \| (`this`, `ev`) => `any`

Fires when the user moves the mouse pointer outside the boundaries of the object.

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/mouseout_event)

#### Inherited from

`LitElement.onmouseout`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9178

***

### onmouseover

> **onmouseover**: `null` \| (`this`, `ev`) => `any`

Fires when the user moves the mouse pointer into the object.

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/mouseover_event)

#### Inherited from

`LitElement.onmouseover`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9185

***

### onmouseup

> **onmouseup**: `null` \| (`this`, `ev`) => `any`

Fires when the user releases a mouse button while the mouse is over the object.

#### Param

The mouse event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/mouseup_event)

#### Inherited from

`LitElement.onmouseup`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9192

***

### onpaste

> **onpaste**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/paste_event)

#### Inherited from

`LitElement.onpaste`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9194

***

### onpause

> **onpause**: `null` \| (`this`, `ev`) => `any`

Occurs when playback is paused.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/pause_event)

#### Inherited from

`LitElement.onpause`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9201

***

### onplay

> **onplay**: `null` \| (`this`, `ev`) => `any`

Occurs when the play method is requested.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/play_event)

#### Inherited from

`LitElement.onplay`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9208

***

### onplaying

> **onplaying**: `null` \| (`this`, `ev`) => `any`

Occurs when the audio or video has started playing.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/playing_event)

#### Inherited from

`LitElement.onplaying`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9215

***

### onpointercancel

> **onpointercancel**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointercancel_event)

#### Inherited from

`LitElement.onpointercancel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9217

***

### onpointerdown

> **onpointerdown**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointerdown_event)

#### Inherited from

`LitElement.onpointerdown`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9219

***

### onpointerenter

> **onpointerenter**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointerenter_event)

#### Inherited from

`LitElement.onpointerenter`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9221

***

### onpointerleave

> **onpointerleave**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointerleave_event)

#### Inherited from

`LitElement.onpointerleave`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9223

***

### onpointermove

> **onpointermove**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointermove_event)

#### Inherited from

`LitElement.onpointermove`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9225

***

### onpointerout

> **onpointerout**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointerout_event)

#### Inherited from

`LitElement.onpointerout`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9227

***

### onpointerover

> **onpointerover**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointerover_event)

#### Inherited from

`LitElement.onpointerover`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9229

***

### onpointerup

> **onpointerup**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/pointerup_event)

#### Inherited from

`LitElement.onpointerup`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9231

***

### onprogress

> **onprogress**: `null` \| (`this`, `ev`) => `any`

Occurs to indicate progress while downloading media data.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/progress_event)

#### Inherited from

`LitElement.onprogress`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9238

***

### onratechange

> **onratechange**: `null` \| (`this`, `ev`) => `any`

Occurs when the playback rate is increased or decreased.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/ratechange_event)

#### Inherited from

`LitElement.onratechange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9245

***

### onreset

> **onreset**: `null` \| (`this`, `ev`) => `any`

Fires when the user resets a form.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLFormElement/reset_event)

#### Inherited from

`LitElement.onreset`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9252

***

### onresize

> **onresize**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLVideoElement/resize_event)

#### Inherited from

`LitElement.onresize`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9254

***

### onscroll

> **onscroll**: `null` \| (`this`, `ev`) => `any`

Fires when the user repositions the scroll box in the scroll bar on the object.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/scroll_event)

#### Inherited from

`LitElement.onscroll`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9261

***

### onscrollend

> **onscrollend**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/scrollend_event)

#### Inherited from

`LitElement.onscrollend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9263

***

### onsecuritypolicyviolation

> **onsecuritypolicyviolation**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/securitypolicyviolation_event)

#### Inherited from

`LitElement.onsecuritypolicyviolation`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9265

***

### onseeked

> **onseeked**: `null` \| (`this`, `ev`) => `any`

Occurs when the seek operation ends.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/seeked_event)

#### Inherited from

`LitElement.onseeked`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9272

***

### onseeking

> **onseeking**: `null` \| (`this`, `ev`) => `any`

Occurs when the current playback position is moved.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/seeking_event)

#### Inherited from

`LitElement.onseeking`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9279

***

### onselect

> **onselect**: `null` \| (`this`, `ev`) => `any`

Fires when the current selection changes.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLInputElement/select_event)

#### Inherited from

`LitElement.onselect`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9286

***

### onselectionchange

> **onselectionchange**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/selectionchange_event)

#### Inherited from

`LitElement.onselectionchange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9288

***

### onselectstart

> **onselectstart**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/selectstart_event)

#### Inherited from

`LitElement.onselectstart`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9290

***

### onslotchange

> **onslotchange**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLSlotElement/slotchange_event)

#### Inherited from

`LitElement.onslotchange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9292

***

### onstalled

> **onstalled**: `null` \| (`this`, `ev`) => `any`

Occurs when the download has stopped.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/stalled_event)

#### Inherited from

`LitElement.onstalled`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9299

***

### onsubmit

> **onsubmit**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLFormElement/submit_event)

#### Inherited from

`LitElement.onsubmit`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9301

***

### onsuspend

> **onsuspend**: `null` \| (`this`, `ev`) => `any`

Occurs if the load operation has been intentionally halted.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/suspend_event)

#### Inherited from

`LitElement.onsuspend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9308

***

### ontimeupdate

> **ontimeupdate**: `null` \| (`this`, `ev`) => `any`

Occurs to indicate the current playback position.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/timeupdate_event)

#### Inherited from

`LitElement.ontimeupdate`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9315

***

### ontoggle

> **ontoggle**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLDetailsElement/toggle_event)

#### Inherited from

`LitElement.ontoggle`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9317

***

### ontouchcancel?

> `optional` **ontouchcancel**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/touchcancel_event)

#### Inherited from

`LitElement.ontouchcancel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9319

***

### ontouchend?

> `optional` **ontouchend**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/touchend_event)

#### Inherited from

`LitElement.ontouchend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9321

***

### ontouchmove?

> `optional` **ontouchmove**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/touchmove_event)

#### Inherited from

`LitElement.ontouchmove`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9323

***

### ontouchstart?

> `optional` **ontouchstart**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/touchstart_event)

#### Inherited from

`LitElement.ontouchstart`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9325

***

### ontransitioncancel

> **ontransitioncancel**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/transitioncancel_event)

#### Inherited from

`LitElement.ontransitioncancel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9327

***

### ontransitionend

> **ontransitionend**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/transitionend_event)

#### Inherited from

`LitElement.ontransitionend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9329

***

### ontransitionrun

> **ontransitionrun**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/transitionrun_event)

#### Inherited from

`LitElement.ontransitionrun`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9331

***

### ontransitionstart

> **ontransitionstart**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/transitionstart_event)

#### Inherited from

`LitElement.ontransitionstart`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9333

***

### onvolumechange

> **onvolumechange**: `null` \| (`this`, `ev`) => `any`

Occurs when the volume is changed, or playback is muted or unmuted.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/volumechange_event)

#### Inherited from

`LitElement.onvolumechange`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9340

***

### onwaiting

> **onwaiting**: `null` \| (`this`, `ev`) => `any`

Occurs when playback stops because the next frame of a video resource is not available.

#### Param

The event.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLMediaElement/waiting_event)

#### Inherited from

`LitElement.onwaiting`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9347

***

### ~~onwebkitanimationend~~

> **onwebkitanimationend**: `null` \| (`this`, `ev`) => `any`

#### Deprecated

This is a legacy alias of `onanimationend`.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animationend_event)

#### Inherited from

`LitElement.onwebkitanimationend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9353

***

### ~~onwebkitanimationiteration~~

> **onwebkitanimationiteration**: `null` \| (`this`, `ev`) => `any`

#### Deprecated

This is a legacy alias of `onanimationiteration`.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animationiteration_event)

#### Inherited from

`LitElement.onwebkitanimationiteration`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9359

***

### ~~onwebkitanimationstart~~

> **onwebkitanimationstart**: `null` \| (`this`, `ev`) => `any`

#### Deprecated

This is a legacy alias of `onanimationstart`.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animationstart_event)

#### Inherited from

`LitElement.onwebkitanimationstart`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9365

***

### ~~onwebkittransitionend~~

> **onwebkittransitionend**: `null` \| (`this`, `ev`) => `any`

#### Deprecated

This is a legacy alias of `ontransitionend`.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/transitionend_event)

#### Inherited from

`LitElement.onwebkittransitionend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9371

***

### onwheel

> **onwheel**: `null` \| (`this`, `ev`) => `any`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/wheel_event)

#### Inherited from

`LitElement.onwheel`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:9373

***

### outerHTML

> **outerHTML**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/outerHTML)

#### Inherited from

`LitElement.outerHTML`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7644

***

### outerText

> **outerText**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/outerText)

#### Inherited from

`LitElement.outerText`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10080

***

### ownerDocument

> `readonly` **ownerDocument**: `Document`

#### Inherited from

`LitElement.ownerDocument`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7645

***

### panelName?

> `optional` **panelName**: `string`

#### Source

[ui/component/or-attribute-card/src/index.ts:185](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L185)

***

### parentElement

> `readonly` **parentElement**: `null` \| `HTMLElement`

Returns the parent element.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/parentElement)

#### Inherited from

`LitElement.parentElement`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16130

***

### parentNode

> `readonly` **parentNode**: `null` \| `ParentNode`

Returns the parent.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/parentNode)

#### Inherited from

`LitElement.parentNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16136

***

### part

> `readonly` **part**: `DOMTokenList`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/part)

#### Inherited from

`LitElement.part`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7647

***

### period

> `private` **period**: `Base` = `"day"`

#### Source

[ui/component/or-attribute-card/src/index.ts:223](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L223)

***

### popover

> **popover**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/popover)

#### Inherited from

`LitElement.popover`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10082

***

### prefix

> `readonly` **prefix**: `null` \| `string`

Returns the namespace prefix.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/prefix)

#### Inherited from

`LitElement.prefix`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7653

***

### previousElementSibling

> `readonly` **previousElementSibling**: `null` \| `Element`

Returns the first preceding sibling that is an element, and null otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/CharacterData/previousElementSibling)

#### Inherited from

`LitElement.previousElementSibling`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16360

***

### previousSibling

> `readonly` **previousSibling**: `null` \| `ChildNode`

Returns the previous sibling.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/previousSibling)

#### Inherited from

`LitElement.previousSibling`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16142

***

### realm?

> `optional` **realm**: `string`

#### Source

[ui/component/or-attribute-card/src/index.ts:199](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L199)

***

### resizeObserver?

> `private` `optional` **resizeObserver**: `ResizeObserver`

#### Source

[ui/component/or-attribute-card/src/index.ts:232](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L232)

***

### role

> **role**: `null` \| `string`

#### Inherited from

`LitElement.role`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2281

***

### scrollHeight

> `readonly` **scrollHeight**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scrollHeight)

#### Inherited from

`LitElement.scrollHeight`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7655

***

### scrollLeft

> **scrollLeft**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scrollLeft)

#### Inherited from

`LitElement.scrollLeft`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7657

***

### scrollTop

> **scrollTop**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scrollTop)

#### Inherited from

`LitElement.scrollTop`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7659

***

### scrollWidth

> `readonly` **scrollWidth**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scrollWidth)

#### Inherited from

`LitElement.scrollWidth`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7661

***

### shadowRoot

> `readonly` **shadowRoot**: `null` \| `ShadowRoot`

Returns element's shadow root, if any, and if shadow root's mode is "open", and null otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/shadowRoot)

#### Inherited from

`LitElement.shadowRoot`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7667

***

### showControls

> **showControls**: `boolean` = `true`

#### Source

[ui/component/or-attribute-card/src/index.ts:214](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L214)

***

### showTitle

> **showTitle**: `boolean` = `true`

#### Source

[ui/component/or-attribute-card/src/index.ts:216](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L216)

***

### slot

> **slot**: `string`

Returns the value of element's slot content attribute. Can be set to change it.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/slot)

#### Inherited from

`LitElement.slot`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7673

***

### spellcheck

> **spellcheck**: `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/spellcheck)

#### Inherited from

`LitElement.spellcheck`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10084

***

### style

> `readonly` **style**: `CSSStyleDeclaration`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/style)

#### Inherited from

`LitElement.style`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7857

***

### tabIndex

> **tabIndex**: `number`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/tabIndex)

#### Inherited from

`LitElement.tabIndex`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:12051

***

### tagName

> `readonly` **tagName**: `string`

Returns the HTML-uppercased qualified name.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/tagName)

#### Inherited from

`LitElement.tagName`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7679

***

### textContent

> **textContent**: `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/textContent)

#### Inherited from

`LitElement.textContent`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16144

***

### title

> **title**: `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/title)

#### Inherited from

`LitElement.title`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10086

***

### translate

> **translate**: `boolean`

#### Inherited from

`LitElement.translate`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10087

***

### \_$litElement$

> `static` **\_$litElement$**: `boolean`

#### Inherited from

`LitElement._$litElement$`

#### Source

node\_modules/lit-element/lit-element.d.ts:93

***

### \_initializers?

> `static` `optional` **\_initializers**: `Initializer`[]

#### Inherited from

`LitElement._initializers`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:257

***

### finalized

> `static` `protected` **finalized**: `boolean`

Ensure this class is marked as `finalized` as an optimization ensuring
it will not needlessly try to `finalize`.

Note this property name is a string to prevent breaking Closure JS Compiler
optimizations. See @lit/reactive-element for more information.

#### Inherited from

`LitElement.finalized`

#### Source

node\_modules/lit-element/lit-element.d.ts:92

***

### styles

> `get` `static` **styles**(): `CSSResult`[]

#### Returns

`CSSResult`[]

#### Source

[ui/component/or-attribute-card/src/index.ts:234](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L234)

***

### \_cleanup()

> `protected` **\_cleanup**(): `void`

#### Returns

`void`

#### Source

[ui/component/or-attribute-card/src/index.ts:563](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L563)

***

### \_openDialog()

> `protected` **\_openDialog**(`dialogContent`?): `void`

#### Parameters

• **dialogContent?**: [`ContextMenuOption`](../type-aliases/ContextMenuOption.md)

#### Returns

`void`

#### Source

[ui/component/or-attribute-card/src/index.ts:462](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L462)

***

### \_setAttribute()

> `private` **\_setAttribute**(`attributeName`): `void`

#### Parameters

• **attributeName**: `string`

#### Returns

`void`

#### Source

[ui/component/or-attribute-card/src/index.ts:554](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L554)

***

### \_setPeriodOption()

> `protected` **\_setPeriodOption**(`value`): `void`

#### Parameters

• **value**: `any`

#### Returns

`void`

#### Source

[ui/component/or-attribute-card/src/index.ts:852](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L852)

***

### addEventListener()

#### addEventListener(type, listener, options)

> **addEventListener**\<`K`\>(`type`, `listener`, `options`?): `void`

##### Type parameters

• **K** *extends* keyof `HTMLElementEventMap`

##### Parameters

• **type**: `K`

• **listener**

• **options?**: `boolean` \| `AddEventListenerOptions`

##### Returns

`void`

##### Inherited from

`LitElement.addEventListener`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:10098

#### addEventListener(type, listener, options)

> **addEventListener**(`type`, `listener`, `options`?): `void`

##### Parameters

• **type**: `string`

• **listener**: `EventListenerOrEventListenerObject`

• **options?**: `boolean` \| `AddEventListenerOptions`

##### Returns

`void`

##### Inherited from

`LitElement.addEventListener`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:10099

***

### after()

> **after**(...`nodes`): `void`

Inserts nodes just after node, while replacing strings in nodes with equivalent Text nodes.

Throws a "HierarchyRequestError" DOMException if the constraints of the node tree are violated.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/CharacterData/after)

#### Parameters

• ...**nodes**: (`string` \| `Node`)[]

#### Returns

`void`

#### Inherited from

`LitElement.after`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:5599

***

### animate()

> **animate**(`keyframes`, `options`?): `Animation`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/animate)

#### Parameters

• **keyframes**: `null` \| `PropertyIndexedKeyframes` \| `Keyframe`[]

• **options?**: `number` \| `KeyframeAnimationOptions`

#### Returns

`Animation`

#### Inherited from

`LitElement.animate`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2431

***

### append()

> **append**(...`nodes`): `void`

Inserts nodes after the last child of node, while replacing strings in nodes with equivalent Text nodes.

Throws a "HierarchyRequestError" DOMException if the constraints of the node tree are violated.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/append)

#### Parameters

• ...**nodes**: (`string` \| `Node`)[]

#### Returns

`void`

#### Inherited from

`LitElement.append`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16789

***

### appendChild()

> **appendChild**\<`T`\>(`node`): `T`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/appendChild)

#### Type parameters

• **T** *extends* `Node`

#### Parameters

• **node**: `T`

#### Returns

`T`

#### Inherited from

`LitElement.appendChild`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16146

***

### attachInternals()

> **attachInternals**(): `ElementInternals`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/attachInternals)

#### Returns

`ElementInternals`

#### Inherited from

`LitElement.attachInternals`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10089

***

### attachShadow()

> **attachShadow**(`init`): `ShadowRoot`

Creates a shadow root for element and returns it.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/attachShadow)

#### Parameters

• **init**: `ShadowRootInit`

#### Returns

`ShadowRoot`

#### Inherited from

`LitElement.attachShadow`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7685

***

### before()

> **before**(...`nodes`): `void`

Inserts nodes just before node, while replacing strings in nodes with equivalent Text nodes.

Throws a "HierarchyRequestError" DOMException if the constraints of the node tree are violated.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/CharacterData/before)

#### Parameters

• ...**nodes**: (`string` \| `Node`)[]

#### Returns

`void`

#### Inherited from

`LitElement.before`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:5607

***

### blur()

> **blur**(): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/blur)

#### Returns

`void`

#### Inherited from

`LitElement.blur`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:12053

***

### checkVisibility()

> **checkVisibility**(`options`?): `boolean`

#### Parameters

• **options?**: `CheckVisibilityOptions`

#### Returns

`boolean`

#### Inherited from

`LitElement.checkVisibility`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7686

***

### click()

> **click**(): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/click)

#### Returns

`void`

#### Inherited from

`LitElement.click`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10091

***

### cloneNode()

> **cloneNode**(`deep`?): `Node`

Returns a copy of node. If deep is true, the copy also includes the node's descendants.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/cloneNode)

#### Parameters

• **deep?**: `boolean`

#### Returns

`Node`

#### Inherited from

`LitElement.cloneNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16152

***

### closest()

#### closest(selector)

> **closest**\<`K`\>(`selector`): `null` \| `HTMLElementTagNameMap`\[`K`\]

Returns the first (starting at element) inclusive ancestor that matches selectors, and null otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/closest)

##### Type parameters

• **K** *extends* keyof `HTMLElementTagNameMap`

##### Parameters

• **selector**: `K`

##### Returns

`null` \| `HTMLElementTagNameMap`\[`K`\]

##### Inherited from

`LitElement.closest`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7692

#### closest(selector)

> **closest**\<`K`\>(`selector`): `null` \| `SVGElementTagNameMap`\[`K`\]

##### Type parameters

• **K** *extends* keyof `SVGElementTagNameMap`

##### Parameters

• **selector**: `K`

##### Returns

`null` \| `SVGElementTagNameMap`\[`K`\]

##### Inherited from

`LitElement.closest`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7693

#### closest(selector)

> **closest**\<`K`\>(`selector`): `null` \| `MathMLElementTagNameMap`\[`K`\]

##### Type parameters

• **K** *extends* keyof `MathMLElementTagNameMap`

##### Parameters

• **selector**: `K`

##### Returns

`null` \| `MathMLElementTagNameMap`\[`K`\]

##### Inherited from

`LitElement.closest`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7694

#### closest(selectors)

> **closest**\<`E`\>(`selectors`): `null` \| `E`

##### Type parameters

• **E** *extends* `Element` = `Element`

##### Parameters

• **selectors**: `string`

##### Returns

`null` \| `E`

##### Inherited from

`LitElement.closest`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7695

***

### compareDocumentPosition()

> **compareDocumentPosition**(`other`): `number`

Returns a bitmask indicating the position of other relative to node.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/compareDocumentPosition)

#### Parameters

• **other**: `Node`

#### Returns

`number`

#### Inherited from

`LitElement.compareDocumentPosition`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16158

***

### computedStyleMap()

> **computedStyleMap**(): `StylePropertyMapReadOnly`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/computedStyleMap)

#### Returns

`StylePropertyMapReadOnly`

#### Inherited from

`LitElement.computedStyleMap`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7697

***

### connectedCallback()

> **connectedCallback**(): `void`

#### Returns

`void`

#### Overrides

`LitElement.connectedCallback`

#### Source

[ui/component/or-attribute-card/src/index.ts:244](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L244)

***

### contains()

> **contains**(`other`): `boolean`

Returns true if other is an inclusive descendant of node, and false otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/contains)

#### Parameters

• **other**: `null` \| `Node`

#### Returns

`boolean`

#### Inherited from

`LitElement.contains`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16164

***

### disconnectedCallback()

> **disconnectedCallback**(): `void`

#### Returns

`void`

#### Overrides

`LitElement.disconnectedCallback`

#### Source

[ui/component/or-attribute-card/src/index.ts:249](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L249)

***

### dispatchEvent()

> **dispatchEvent**(`event`): `boolean`

Dispatches a synthetic event event to target and returns true if either event's cancelable attribute value is false or its preventDefault() method was not invoked, and false otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/EventTarget/dispatchEvent)

#### Parameters

• **event**: `Event`

#### Returns

`boolean`

#### Inherited from

`LitElement.dispatchEvent`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:8217

***

### firstUpdated()

> **firstUpdated**(): `void`

#### Returns

`void`

#### Overrides

`LitElement.firstUpdated`

#### Source

[ui/component/or-attribute-card/src/index.ts:254](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L254)

***

### focus()

> **focus**(`options`?): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/focus)

#### Parameters

• **options?**: `FocusOptions`

#### Returns

`void`

#### Inherited from

`LitElement.focus`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:12055

***

### getAnimations()

> **getAnimations**(`options`?): `Animation`[]

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getAnimations)

#### Parameters

• **options?**: `GetAnimationsOptions`

#### Returns

`Animation`[]

#### Inherited from

`LitElement.getAnimations`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:2433

***

### getAttribute()

> **getAttribute**(`qualifiedName`): `null` \| `string`

Returns element's first attribute whose qualified name is qualifiedName, and null if there is no such attribute otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getAttribute)

#### Parameters

• **qualifiedName**: `string`

#### Returns

`null` \| `string`

#### Inherited from

`LitElement.getAttribute`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7703

***

### getAttributeNS()

> **getAttributeNS**(`namespace`, `localName`): `null` \| `string`

Returns element's attribute whose namespace is namespace and local name is localName, and null if there is no such attribute otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getAttributeNS)

#### Parameters

• **namespace**: `null` \| `string`

• **localName**: `string`

#### Returns

`null` \| `string`

#### Inherited from

`LitElement.getAttributeNS`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7709

***

### getAttributeNames()

> **getAttributeNames**(): `string`[]

Returns the qualified names of all element's attributes. Can contain duplicates.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getAttributeNames)

#### Returns

`string`[]

#### Inherited from

`LitElement.getAttributeNames`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7715

***

### getAttributeNode()

> **getAttributeNode**(`qualifiedName`): `null` \| `Attr`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getAttributeNode)

#### Parameters

• **qualifiedName**: `string`

#### Returns

`null` \| `Attr`

#### Inherited from

`LitElement.getAttributeNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7717

***

### getAttributeNodeNS()

> **getAttributeNodeNS**(`namespace`, `localName`): `null` \| `Attr`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getAttributeNodeNS)

#### Parameters

• **namespace**: `null` \| `string`

• **localName**: `string`

#### Returns

`null` \| `Attr`

#### Inherited from

`LitElement.getAttributeNodeNS`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7719

***

### getBoundingClientRect()

> **getBoundingClientRect**(): `DOMRect`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getBoundingClientRect)

#### Returns

`DOMRect`

#### Inherited from

`LitElement.getBoundingClientRect`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7721

***

### getClientRects()

> **getClientRects**(): `DOMRectList`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getClientRects)

#### Returns

`DOMRectList`

#### Inherited from

`LitElement.getClientRects`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7723

***

### getElementsByClassName()

> **getElementsByClassName**(`classNames`): `HTMLCollectionOf`\<`Element`\>

Returns a HTMLCollection of the elements in the object on which the method was invoked (a document or an element) that have all the classes given by classNames. The classNames argument is interpreted as a space-separated list of classes.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getElementsByClassName)

#### Parameters

• **classNames**: `string`

#### Returns

`HTMLCollectionOf`\<`Element`\>

#### Inherited from

`LitElement.getElementsByClassName`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7729

***

### getElementsByTagName()

#### getElementsByTagName(qualifiedName)

> **getElementsByTagName**\<`K`\>(`qualifiedName`): `HTMLCollectionOf`\<`HTMLElementTagNameMap`\[`K`\]\>

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getElementsByTagName)

##### Type parameters

• **K** *extends* keyof `HTMLElementTagNameMap`

##### Parameters

• **qualifiedName**: `K`

##### Returns

`HTMLCollectionOf`\<`HTMLElementTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.getElementsByTagName`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7731

#### getElementsByTagName(qualifiedName)

> **getElementsByTagName**\<`K`\>(`qualifiedName`): `HTMLCollectionOf`\<`SVGElementTagNameMap`\[`K`\]\>

##### Type parameters

• **K** *extends* keyof `SVGElementTagNameMap`

##### Parameters

• **qualifiedName**: `K`

##### Returns

`HTMLCollectionOf`\<`SVGElementTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.getElementsByTagName`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7732

#### getElementsByTagName(qualifiedName)

> **getElementsByTagName**\<`K`\>(`qualifiedName`): `HTMLCollectionOf`\<`MathMLElementTagNameMap`\[`K`\]\>

##### Type parameters

• **K** *extends* keyof `MathMLElementTagNameMap`

##### Parameters

• **qualifiedName**: `K`

##### Returns

`HTMLCollectionOf`\<`MathMLElementTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.getElementsByTagName`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7733

#### getElementsByTagName(qualifiedName)

> **getElementsByTagName**\<`K`\>(`qualifiedName`): `HTMLCollectionOf`\<`HTMLElementDeprecatedTagNameMap`\[`K`\]\>

##### Type parameters

• **K** *extends* keyof `HTMLElementDeprecatedTagNameMap`

##### Parameters

• **qualifiedName**: `K`

##### Returns

`HTMLCollectionOf`\<`HTMLElementDeprecatedTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.getElementsByTagName`

##### Deprecated

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7735

#### getElementsByTagName(qualifiedName)

> **getElementsByTagName**(`qualifiedName`): `HTMLCollectionOf`\<`Element`\>

##### Parameters

• **qualifiedName**: `string`

##### Returns

`HTMLCollectionOf`\<`Element`\>

##### Inherited from

`LitElement.getElementsByTagName`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7736

***

### getElementsByTagNameNS()

#### getElementsByTagNameNS(namespaceURI, localName)

> **getElementsByTagNameNS**(`namespaceURI`, `localName`): `HTMLCollectionOf`\<`HTMLElement`\>

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/getElementsByTagNameNS)

##### Parameters

• **namespaceURI**: `"http://www.w3.org/1999/xhtml"`

• **localName**: `string`

##### Returns

`HTMLCollectionOf`\<`HTMLElement`\>

##### Inherited from

`LitElement.getElementsByTagNameNS`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7738

#### getElementsByTagNameNS(namespaceURI, localName)

> **getElementsByTagNameNS**(`namespaceURI`, `localName`): `HTMLCollectionOf`\<`SVGElement`\>

##### Parameters

• **namespaceURI**: `"http://www.w3.org/2000/svg"`

• **localName**: `string`

##### Returns

`HTMLCollectionOf`\<`SVGElement`\>

##### Inherited from

`LitElement.getElementsByTagNameNS`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7739

#### getElementsByTagNameNS(namespaceURI, localName)

> **getElementsByTagNameNS**(`namespaceURI`, `localName`): `HTMLCollectionOf`\<`MathMLElement`\>

##### Parameters

• **namespaceURI**: `"http://www.w3.org/1998/Math/MathML"`

• **localName**: `string`

##### Returns

`HTMLCollectionOf`\<`MathMLElement`\>

##### Inherited from

`LitElement.getElementsByTagNameNS`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7740

#### getElementsByTagNameNS(namespace, localName)

> **getElementsByTagNameNS**(`namespace`, `localName`): `HTMLCollectionOf`\<`Element`\>

##### Parameters

• **namespace**: `null` \| `string`

• **localName**: `string`

##### Returns

`HTMLCollectionOf`\<`Element`\>

##### Inherited from

`LitElement.getElementsByTagNameNS`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7741

***

### getFirstKnownMeasurement()

> `protected` **getFirstKnownMeasurement**(`data`): `number`

#### Parameters

• **data**: `ValueDatapoint`\<`any`\>[]

#### Returns

`number`

#### Source

[ui/component/or-attribute-card/src/index.ts:792](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L792)

***

### getFormattedDelta()

> `protected` **getFormattedDelta**(`firstVal`, `lastVal`): `object`

#### Parameters

• **firstVal**: `number`

• **lastVal**: `number`

#### Returns

`object`

##### unit?

> `optional` **unit**: `string`

##### val?

> `optional` **val**: `number`

#### Source

[ui/component/or-attribute-card/src/index.ts:808](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L808)

***

### getFormattedValue()

> `protected` **getFormattedValue**(`value`): `undefined` \| `object`

#### Parameters

• **value**: `undefined` \| `number`

#### Returns

`undefined` \| `object`

#### Source

[ui/component/or-attribute-card/src/index.ts:774](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L774)

***

### getHighestValue()

> `protected` **getHighestValue**(`data`): `number`

#### Parameters

• **data**: `ValueDatapoint`\<`any`\>[]

#### Returns

`number`

#### Source

[ui/component/or-attribute-card/src/index.ts:770](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L770)

***

### getLastKnownMeasurement()

> `protected` **getLastKnownMeasurement**(`data`): `number`

#### Parameters

• **data**: `ValueDatapoint`\<`any`\>[]

#### Returns

`number`

#### Source

[ui/component/or-attribute-card/src/index.ts:800](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L800)

***

### getRootNode()

> **getRootNode**(`options`?): `Node`

Returns node's root.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/getRootNode)

#### Parameters

• **options?**: `GetRootNodeOptions`

#### Returns

`Node`

#### Inherited from

`LitElement.getRootNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16170

***

### getTotalValue()

> `protected` **getTotalValue**(`data`): `number`

#### Parameters

• **data**: `ValueDatapoint`\<`any`\>[]

#### Returns

`number`

#### Source

[ui/component/or-attribute-card/src/index.ts:764](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L764)

***

### handleMenuSelect()

> `protected` **handleMenuSelect**(`value`): `void`

#### Parameters

• **value**: [`ContextMenuOption`](../type-aliases/ContextMenuOption.md)

#### Returns

`void`

#### Source

[ui/component/or-attribute-card/src/index.ts:827](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L827)

***

### hasAttribute()

> **hasAttribute**(`qualifiedName`): `boolean`

Returns true if element has an attribute whose qualified name is qualifiedName, and false otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/hasAttribute)

#### Parameters

• **qualifiedName**: `string`

#### Returns

`boolean`

#### Inherited from

`LitElement.hasAttribute`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7747

***

### hasAttributeNS()

> **hasAttributeNS**(`namespace`, `localName`): `boolean`

Returns true if element has an attribute whose namespace is namespace and local name is localName.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/hasAttributeNS)

#### Parameters

• **namespace**: `null` \| `string`

• **localName**: `string`

#### Returns

`boolean`

#### Inherited from

`LitElement.hasAttributeNS`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7753

***

### hasAttributes()

> **hasAttributes**(): `boolean`

Returns true if element has attributes, and false otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/hasAttributes)

#### Returns

`boolean`

#### Inherited from

`LitElement.hasAttributes`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7759

***

### hasChildNodes()

> **hasChildNodes**(): `boolean`

Returns whether node has children.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/hasChildNodes)

#### Returns

`boolean`

#### Inherited from

`LitElement.hasChildNodes`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16176

***

### hasPointerCapture()

> **hasPointerCapture**(`pointerId`): `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/hasPointerCapture)

#### Parameters

• **pointerId**: `number`

#### Returns

`boolean`

#### Inherited from

`LitElement.hasPointerCapture`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7761

***

### hidePopover()

> **hidePopover**(): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/hidePopover)

#### Returns

`void`

#### Inherited from

`LitElement.hidePopover`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10093

***

### insertAdjacentElement()

> **insertAdjacentElement**(`where`, `element`): `null` \| `Element`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/insertAdjacentElement)

#### Parameters

• **where**: `InsertPosition`

• **element**: `Element`

#### Returns

`null` \| `Element`

#### Inherited from

`LitElement.insertAdjacentElement`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7763

***

### insertAdjacentHTML()

> **insertAdjacentHTML**(`position`, `text`): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/insertAdjacentHTML)

#### Parameters

• **position**: `InsertPosition`

• **text**: `string`

#### Returns

`void`

#### Inherited from

`LitElement.insertAdjacentHTML`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7765

***

### insertAdjacentText()

> **insertAdjacentText**(`where`, `data`): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/insertAdjacentText)

#### Parameters

• **where**: `InsertPosition`

• **data**: `string`

#### Returns

`void`

#### Inherited from

`LitElement.insertAdjacentText`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7767

***

### insertBefore()

> **insertBefore**\<`T`\>(`node`, `child`): `T`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/insertBefore)

#### Type parameters

• **T** *extends* `Node`

#### Parameters

• **node**: `T`

• **child**: `null` \| `Node`

#### Returns

`T`

#### Inherited from

`LitElement.insertBefore`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16178

***

### isDefaultNamespace()

> **isDefaultNamespace**(`namespace`): `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/isDefaultNamespace)

#### Parameters

• **namespace**: `null` \| `string`

#### Returns

`boolean`

#### Inherited from

`LitElement.isDefaultNamespace`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16180

***

### isEqualNode()

> **isEqualNode**(`otherNode`): `boolean`

Returns whether node and otherNode have the same properties.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/isEqualNode)

#### Parameters

• **otherNode**: `null` \| `Node`

#### Returns

`boolean`

#### Inherited from

`LitElement.isEqualNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16186

***

### isSameNode()

> **isSameNode**(`otherNode`): `boolean`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/isSameNode)

#### Parameters

• **otherNode**: `null` \| `Node`

#### Returns

`boolean`

#### Inherited from

`LitElement.isSameNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16188

***

### loadData()

> `protected` **loadData**(): `Promise`\<`void`\>

#### Returns

`Promise`\<`void`\>

#### Source

[ui/component/or-attribute-card/src/index.ts:688](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L688)

***

### loadSettings()

> `protected` **loadSettings**(`reset`): `Promise`\<`void`\>

#### Parameters

• **reset**: `boolean`= `false`

#### Returns

`Promise`\<`void`\>

#### Source

[ui/component/or-attribute-card/src/index.ts:570](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L570)

***

### lookupNamespaceURI()

> **lookupNamespaceURI**(`prefix`): `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/lookupNamespaceURI)

#### Parameters

• **prefix**: `null` \| `string`

#### Returns

`null` \| `string`

#### Inherited from

`LitElement.lookupNamespaceURI`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16190

***

### lookupPrefix()

> **lookupPrefix**(`namespace`): `null` \| `string`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/lookupPrefix)

#### Parameters

• **namespace**: `null` \| `string`

#### Returns

`null` \| `string`

#### Inherited from

`LitElement.lookupPrefix`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16192

***

### matches()

> **matches**(`selectors`): `boolean`

Returns true if matching selectors against element's root yields element, and false otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/matches)

#### Parameters

• **selectors**: `string`

#### Returns

`boolean`

#### Inherited from

`LitElement.matches`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7773

***

### normalize()

> **normalize**(): `void`

Removes empty exclusive Text nodes and concatenates the data of remaining contiguous exclusive Text nodes into the first of their nodes.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/normalize)

#### Returns

`void`

#### Inherited from

`LitElement.normalize`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16198

***

### prepend()

> **prepend**(...`nodes`): `void`

Inserts nodes before the first child of node, while replacing strings in nodes with equivalent Text nodes.

Throws a "HierarchyRequestError" DOMException if the constraints of the node tree are violated.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/prepend)

#### Parameters

• ...**nodes**: (`string` \| `Node`)[]

#### Returns

`void`

#### Inherited from

`LitElement.prepend`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16797

***

### querySelector()

#### querySelector(selectors)

> **querySelector**\<`K`\>(`selectors`): `null` \| `HTMLElementTagNameMap`\[`K`\]

Returns the first element that is a descendant of node that matches selectors.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/querySelector)

##### Type parameters

• **K** *extends* keyof `HTMLElementTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`null` \| `HTMLElementTagNameMap`\[`K`\]

##### Inherited from

`LitElement.querySelector`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16803

#### querySelector(selectors)

> **querySelector**\<`K`\>(`selectors`): `null` \| `SVGElementTagNameMap`\[`K`\]

##### Type parameters

• **K** *extends* keyof `SVGElementTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`null` \| `SVGElementTagNameMap`\[`K`\]

##### Inherited from

`LitElement.querySelector`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16804

#### querySelector(selectors)

> **querySelector**\<`K`\>(`selectors`): `null` \| `MathMLElementTagNameMap`\[`K`\]

##### Type parameters

• **K** *extends* keyof `MathMLElementTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`null` \| `MathMLElementTagNameMap`\[`K`\]

##### Inherited from

`LitElement.querySelector`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16805

#### querySelector(selectors)

> **querySelector**\<`K`\>(`selectors`): `null` \| `HTMLElementDeprecatedTagNameMap`\[`K`\]

##### Type parameters

• **K** *extends* keyof `HTMLElementDeprecatedTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`null` \| `HTMLElementDeprecatedTagNameMap`\[`K`\]

##### Inherited from

`LitElement.querySelector`

##### Deprecated

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16807

#### querySelector(selectors)

> **querySelector**\<`E`\>(`selectors`): `null` \| `E`

##### Type parameters

• **E** *extends* `Element` = `Element`

##### Parameters

• **selectors**: `string`

##### Returns

`null` \| `E`

##### Inherited from

`LitElement.querySelector`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16808

***

### querySelectorAll()

#### querySelectorAll(selectors)

> **querySelectorAll**\<`K`\>(`selectors`): `NodeListOf`\<`HTMLElementTagNameMap`\[`K`\]\>

Returns all element descendants of node that match selectors.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/querySelectorAll)

##### Type parameters

• **K** *extends* keyof `HTMLElementTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`NodeListOf`\<`HTMLElementTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.querySelectorAll`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16814

#### querySelectorAll(selectors)

> **querySelectorAll**\<`K`\>(`selectors`): `NodeListOf`\<`SVGElementTagNameMap`\[`K`\]\>

##### Type parameters

• **K** *extends* keyof `SVGElementTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`NodeListOf`\<`SVGElementTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.querySelectorAll`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16815

#### querySelectorAll(selectors)

> **querySelectorAll**\<`K`\>(`selectors`): `NodeListOf`\<`MathMLElementTagNameMap`\[`K`\]\>

##### Type parameters

• **K** *extends* keyof `MathMLElementTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`NodeListOf`\<`MathMLElementTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.querySelectorAll`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16816

#### querySelectorAll(selectors)

> **querySelectorAll**\<`K`\>(`selectors`): `NodeListOf`\<`HTMLElementDeprecatedTagNameMap`\[`K`\]\>

##### Type parameters

• **K** *extends* keyof `HTMLElementDeprecatedTagNameMap`

##### Parameters

• **selectors**: `K`

##### Returns

`NodeListOf`\<`HTMLElementDeprecatedTagNameMap`\[`K`\]\>

##### Inherited from

`LitElement.querySelectorAll`

##### Deprecated

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16818

#### querySelectorAll(selectors)

> **querySelectorAll**\<`E`\>(`selectors`): `NodeListOf`\<`E`\>

##### Type parameters

• **E** *extends* `Element` = `Element`

##### Parameters

• **selectors**: `string`

##### Returns

`NodeListOf`\<`E`\>

##### Inherited from

`LitElement.querySelectorAll`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:16819

***

### releasePointerCapture()

> **releasePointerCapture**(`pointerId`): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/releasePointerCapture)

#### Parameters

• **pointerId**: `number`

#### Returns

`void`

#### Inherited from

`LitElement.releasePointerCapture`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7775

***

### remove()

> **remove**(): `void`

Removes node.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/CharacterData/remove)

#### Returns

`void`

#### Inherited from

`LitElement.remove`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:5613

***

### removeAttribute()

> **removeAttribute**(`qualifiedName`): `void`

Removes element's first attribute whose qualified name is qualifiedName.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/removeAttribute)

#### Parameters

• **qualifiedName**: `string`

#### Returns

`void`

#### Inherited from

`LitElement.removeAttribute`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7781

***

### removeAttributeNS()

> **removeAttributeNS**(`namespace`, `localName`): `void`

Removes element's attribute whose namespace is namespace and local name is localName.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/removeAttributeNS)

#### Parameters

• **namespace**: `null` \| `string`

• **localName**: `string`

#### Returns

`void`

#### Inherited from

`LitElement.removeAttributeNS`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7787

***

### removeAttributeNode()

> **removeAttributeNode**(`attr`): `Attr`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/removeAttributeNode)

#### Parameters

• **attr**: `Attr`

#### Returns

`Attr`

#### Inherited from

`LitElement.removeAttributeNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7789

***

### removeChild()

> **removeChild**\<`T`\>(`child`): `T`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/removeChild)

#### Type parameters

• **T** *extends* `Node`

#### Parameters

• **child**: `T`

#### Returns

`T`

#### Inherited from

`LitElement.removeChild`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16200

***

### removeEventListener()

#### removeEventListener(type, listener, options)

> **removeEventListener**\<`K`\>(`type`, `listener`, `options`?): `void`

##### Type parameters

• **K** *extends* keyof `HTMLElementEventMap`

##### Parameters

• **type**: `K`

• **listener**

• **options?**: `boolean` \| `EventListenerOptions`

##### Returns

`void`

##### Inherited from

`LitElement.removeEventListener`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:10100

#### removeEventListener(type, listener, options)

> **removeEventListener**(`type`, `listener`, `options`?): `void`

##### Parameters

• **type**: `string`

• **listener**: `EventListenerOrEventListenerObject`

• **options?**: `boolean` \| `EventListenerOptions`

##### Returns

`void`

##### Inherited from

`LitElement.removeEventListener`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:10101

***

### render()

> `protected` **render**(): `TemplateResult`\<`1`\>

#### Returns

`TemplateResult`\<`1`\>

#### Overrides

`LitElement.render`

#### Source

[ui/component/or-attribute-card/src/index.ts:355](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L355)

***

### replaceChild()

> **replaceChild**\<`T`\>(`node`, `child`): `T`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Node/replaceChild)

#### Type parameters

• **T** *extends* `Node`

#### Parameters

• **node**: `Node`

• **child**: `T`

#### Returns

`T`

#### Inherited from

`LitElement.replaceChild`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16202

***

### replaceChildren()

> **replaceChildren**(...`nodes`): `void`

Replace all children of node with nodes, while replacing strings in nodes with equivalent Text nodes.

Throws a "HierarchyRequestError" DOMException if the constraints of the node tree are violated.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Document/replaceChildren)

#### Parameters

• ...**nodes**: (`string` \| `Node`)[]

#### Returns

`void`

#### Inherited from

`LitElement.replaceChildren`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:16827

***

### replaceWith()

> **replaceWith**(...`nodes`): `void`

Replaces node with nodes, while replacing strings in nodes with equivalent Text nodes.

Throws a "HierarchyRequestError" DOMException if the constraints of the node tree are violated.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/CharacterData/replaceWith)

#### Parameters

• ...**nodes**: (`string` \| `Node`)[]

#### Returns

`void`

#### Inherited from

`LitElement.replaceWith`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:5621

***

### requestFullscreen()

> **requestFullscreen**(`options`?): `Promise`\<`void`\>

Displays element fullscreen and resolves promise when done.

When supplied, options's navigationUI member indicates whether showing navigation UI while in fullscreen is preferred or not. If set to "show", navigation simplicity is preferred over screen space, and if set to "hide", more screen space is preferred. User agents are always free to honor user preference over the application's. The default value "auto" indicates no application preference.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/requestFullscreen)

#### Parameters

• **options?**: `FullscreenOptions`

#### Returns

`Promise`\<`void`\>

#### Inherited from

`LitElement.requestFullscreen`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7797

***

### requestPointerLock()

> **requestPointerLock**(): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/requestPointerLock)

#### Returns

`void`

#### Inherited from

`LitElement.requestPointerLock`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7799

***

### saveSettings()

> **saveSettings**(): `Promise`\<`void`\>

#### Returns

`Promise`\<`void`\>

#### Source

[ui/component/or-attribute-card/src/index.ts:647](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L647)

***

### scroll()

#### scroll(options)

> **scroll**(`options`?): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scroll)

##### Parameters

• **options?**: `ScrollToOptions`

##### Returns

`void`

##### Inherited from

`LitElement.scroll`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7801

#### scroll(x, y)

> **scroll**(`x`, `y`): `void`

##### Parameters

• **x**: `number`

• **y**: `number`

##### Returns

`void`

##### Inherited from

`LitElement.scroll`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7802

***

### scrollBy()

#### scrollBy(options)

> **scrollBy**(`options`?): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scrollBy)

##### Parameters

• **options?**: `ScrollToOptions`

##### Returns

`void`

##### Inherited from

`LitElement.scrollBy`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7804

#### scrollBy(x, y)

> **scrollBy**(`x`, `y`): `void`

##### Parameters

• **x**: `number`

• **y**: `number`

##### Returns

`void`

##### Inherited from

`LitElement.scrollBy`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7805

***

### scrollIntoView()

> **scrollIntoView**(`arg`?): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scrollIntoView)

#### Parameters

• **arg?**: `boolean` \| `ScrollIntoViewOptions`

#### Returns

`void`

#### Inherited from

`LitElement.scrollIntoView`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7807

***

### scrollTo()

#### scrollTo(options)

> **scrollTo**(`options`?): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/scrollTo)

##### Parameters

• **options?**: `ScrollToOptions`

##### Returns

`void`

##### Inherited from

`LitElement.scrollTo`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7809

#### scrollTo(x, y)

> **scrollTo**(`x`, `y`): `void`

##### Parameters

• **x**: `number`

• **y**: `number`

##### Returns

`void`

##### Inherited from

`LitElement.scrollTo`

##### Source

node\_modules/typescript/lib/lib.dom.d.ts:7810

***

### setAttribute()

> **setAttribute**(`qualifiedName`, `value`): `void`

Sets the value of element's first attribute whose qualified name is qualifiedName to value.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/setAttribute)

#### Parameters

• **qualifiedName**: `string`

• **value**: `string`

#### Returns

`void`

#### Inherited from

`LitElement.setAttribute`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7816

***

### setAttributeNS()

> **setAttributeNS**(`namespace`, `qualifiedName`, `value`): `void`

Sets the value of element's attribute whose namespace is namespace and local name is localName to value.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/setAttributeNS)

#### Parameters

• **namespace**: `null` \| `string`

• **qualifiedName**: `string`

• **value**: `string`

#### Returns

`void`

#### Inherited from

`LitElement.setAttributeNS`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7822

***

### setAttributeNode()

> **setAttributeNode**(`attr`): `null` \| `Attr`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/setAttributeNode)

#### Parameters

• **attr**: `Attr`

#### Returns

`null` \| `Attr`

#### Inherited from

`LitElement.setAttributeNode`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7824

***

### setAttributeNodeNS()

> **setAttributeNodeNS**(`attr`): `null` \| `Attr`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/setAttributeNodeNS)

#### Parameters

• **attr**: `Attr`

#### Returns

`null` \| `Attr`

#### Inherited from

`LitElement.setAttributeNodeNS`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7826

***

### setLabelSizeByLength()

> `protected` **setLabelSizeByLength**(`value`): `void`

#### Parameters

• **value**: `string`

#### Returns

`void`

#### Source

[ui/component/or-attribute-card/src/index.ts:837](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L837)

***

### setLabelSizeByWidth()

> `protected` **setLabelSizeByWidth**(`blockSize`): `void`

#### Parameters

• **blockSize**: `number`

#### Returns

`void`

#### Source

[ui/component/or-attribute-card/src/index.ts:845](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L845)

***

### setPointerCapture()

> **setPointerCapture**(`pointerId`): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/setPointerCapture)

#### Parameters

• **pointerId**: `number`

#### Returns

`void`

#### Inherited from

`LitElement.setPointerCapture`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7828

***

### shouldShowControls()

> **shouldShowControls**(): `boolean`

#### Returns

`boolean`

#### Source

[ui/component/or-attribute-card/src/index.ts:348](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L348)

***

### shouldShowTitle()

> **shouldShowTitle**(): `boolean`

#### Returns

`boolean`

#### Source

[ui/component/or-attribute-card/src/index.ts:351](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L351)

***

### showPopover()

> **showPopover**(): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/showPopover)

#### Returns

`void`

#### Inherited from

`LitElement.showPopover`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10095

***

### toggleAttribute()

> **toggleAttribute**(`qualifiedName`, `force`?): `boolean`

If force is not given, "toggles" qualifiedName, removing it if it is present and adding it if it is not present. If force is true, adds qualifiedName. If force is false, removes qualifiedName.

Returns true if qualifiedName is now present, and false otherwise.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/toggleAttribute)

#### Parameters

• **qualifiedName**: `string`

• **force?**: `boolean`

#### Returns

`boolean`

#### Inherited from

`LitElement.toggleAttribute`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7836

***

### togglePopover()

> **togglePopover**(`force`?): `void`

[MDN Reference](https://developer.mozilla.org/docs/Web/API/HTMLElement/togglePopover)

#### Parameters

• **force?**: `boolean`

#### Returns

`void`

#### Inherited from

`LitElement.togglePopover`

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:10097

***

### updated()

> **updated**(`changedProperties`): `void`

#### Parameters

• **changedProperties**: `Map`\<`PropertyKey`, `unknown`\> \| `PropertyValueMap`\<`any`\>

#### Returns

`void`

#### Overrides

`LitElement.updated`

#### Source

[ui/component/or-attribute-card/src/index.ts:258](https://github.com/openremote/openremote/blob/42cb01cba1a7d305ff5c67894c04424caf0a0b79/ui/component/or-attribute-card/src/index.ts#L258)

***

### ~~webkitMatchesSelector()~~

> **webkitMatchesSelector**(`selectors`): `boolean`

#### Parameters

• **selectors**: `string`

#### Returns

`boolean`

#### Inherited from

`LitElement.webkitMatchesSelector`

#### Deprecated

This is a legacy alias of `matches`.

[MDN Reference](https://developer.mozilla.org/docs/Web/API/Element/matches)

#### Source

node\_modules/typescript/lib/lib.dom.d.ts:7842

***

### addInitializer()

> `static` **addInitializer**(`initializer`): `void`

Adds an initializer function to the class that is called during instance
construction.

This is useful for code that runs against a `ReactiveElement`
subclass, such as a decorator, that needs to do work for each
instance, such as setting up a `ReactiveController`.

```ts
const myDecorator = (target: typeof ReactiveElement, key: string) => {
  target.addInitializer((instance: ReactiveElement) => {
    // This is run during construction of the element
    new MyController(instance);
  });
}
```

Decorating a field will then cause each instance to run an initializer
that adds a controller:

```ts
class MyElement extends LitElement {
  @myDecorator foo;
}
```

Initializers are stored per-constructor. Adding an initializer to a
subclass does not add it to a superclass. Since initializers are run in
constructors, initializers will run in order of the class hierarchy,
starting with superclasses and progressing to the instance's class.

#### Parameters

• **initializer**: `Initializer`

#### Returns

`void`

#### Inherited from

`LitElement.addInitializer`

#### Nocollapse

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:256

***

### finalize()

> `static` `protected` **finalize**(): `boolean`

Creates property accessors for registered properties, sets up element
styling, and ensures any superclasses are also finalized. Returns true if
the element was finalized.

#### Returns

`boolean`

#### Inherited from

`LitElement.finalize`

#### Nocollapse

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:423

## attributes

### observedAttributes

> `get` `static` **observedAttributes**(): `string`[]

Returns a list of attributes corresponding to the registered properties.

#### Nocollapse

#### Returns

`string`[]

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:347

***

### attributeChangedCallback()

> **attributeChangedCallback**(`name`, `_old`, `value`): `void`

Synchronizes property values when attributes change.

Specifically, when an attribute is set, the corresponding property is set.
You should rarely need to implement this callback. If this method is
overridden, `super.attributeChangedCallback(name, _old, value)` must be
called.

See [using the lifecycle callbacks](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_custom_elements#using_the_lifecycle_callbacks)
on MDN for more information about the `attributeChangedCallback`.

#### Parameters

• **name**: `string`

• **\_old**: `null` \| `string`

• **value**: `null` \| `string`

#### Returns

`void`

#### Inherited from

`LitElement.attributeChangedCallback`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:563

## controllers

### addController()

> **addController**(`controller`): `void`

Registers a `ReactiveController` to participate in the element's reactive
update cycle. The element automatically calls into any registered
controllers during its lifecycle callbacks.

If the element is connected when `addController()` is called, the
controller's `hostConnected()` callback will be immediately called.

#### Parameters

• **controller**: `ReactiveController`

#### Returns

`void`

#### Inherited from

`LitElement.addController`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:502

***

### removeController()

> **removeController**(`controller`): `void`

Removes a `ReactiveController` from the element.

#### Parameters

• **controller**: `ReactiveController`

#### Returns

`void`

#### Inherited from

`LitElement.removeController`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:507

## dev-mode

### disableWarning()?

> `static` `optional` **disableWarning**: (`warningKind`) => `void`

Disable the given warning category for this class.

This method only exists in development builds, so it should be accessed
with a guard like:

```ts
// Disable for all ReactiveElement subclasses
ReactiveElement.disableWarning?.('migration');

// Disable for only MyElement and subclasses
MyElement.disableWarning?.('migration');
```

#### Nocollapse

#### Parameters

• **warningKind**: `WarningKind`

#### Returns

`void`

#### Inherited from

`LitElement.disableWarning`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:222

***

### enableWarning()?

> `static` `optional` **enableWarning**: (`warningKind`) => `void`

Enable the given warning category for this class.

This method only exists in development builds, so it should be accessed
with a guard like:

```ts
// Enable for all ReactiveElement subclasses
ReactiveElement.enableWarning?.('migration');

// Enable for only MyElement and subclasses
MyElement.enableWarning?.('migration');
```

#### Nocollapse

#### Parameters

• **warningKind**: `WarningKind`

#### Returns

`void`

#### Inherited from

`LitElement.enableWarning`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:204

***

### enabledWarnings?

> `static` `optional` **enabledWarnings**: `WarningKind`[]

Read or set all the enabled warning categories for this class.

This property is only used in development builds.

#### Nocollapse

#### Inherited from

`LitElement.enabledWarnings`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:186

## properties

### elementProperties

> `static` **elementProperties**: `PropertyDeclarationMap`

Memoized list of all element properties, including any superclass properties.
Created lazily on user subclasses when finalizing the class.

#### Nocollapse

#### Inherited from

`LitElement.elementProperties`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:275

***

### properties

> `static` **properties**: `PropertyDeclarations`

User-supplied object that maps property names to `PropertyDeclaration`
objects containing options for configuring reactive properties. When
a reactive property is set the element will update and render.

By default properties are public fields, and as such, they should be
considered as primarily settable by element users, either via attribute or
the property itself.

Generally, properties that are changed by the element should be private or
protected fields and should use the `state: true` option. Properties
marked as `state` do not reflect from the corresponding attribute

However, sometimes element code does need to set a public property. This
should typically only be done in response to user interaction, and an event
should be fired informing the user; for example, a checkbox sets its
`checked` property when clicked and fires a `changed` event. Mutating
public properties should typically not be done for non-primitive (object or
array) properties. In other cases when an element needs to manage state, a
private property set with the `state: true` option should be used. When
needed, state properties can be initialized via public properties to
facilitate complex interactions.

#### Nocollapse

#### Inherited from

`LitElement.properties`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:301

***

### createProperty()

> `static` **createProperty**(`name`, `options`?): `void`

Creates a property accessor on the element prototype if one does not exist
and stores a PropertyDeclaration for the property with the
given options. The property setter calls the property's `hasChanged`
property option or uses a strict identity check to determine whether or not
to request an update.

This method may be overridden to customize properties; however,
when doing so, it's important to call `super.createProperty` to ensure
the property is setup correctly. This method calls
`getPropertyDescriptor` internally to get a descriptor to install.
To customize what properties do when they are get or set, override
`getPropertyDescriptor`. To customize the options for a property,
implement `createProperty` like this:

```ts
static createProperty(name, options) {
  options = Object.assign(options, {myOption: true});
  super.createProperty(name, options);
}
```

#### Parameters

• **name**: `PropertyKey`

• **options?**: `PropertyDeclaration`\<`unknown`, `unknown`\>

#### Returns

`void`

#### Inherited from

`LitElement.createProperty`

#### Nocollapse

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:373

***

### getPropertyDescriptor()

> `static` `protected` **getPropertyDescriptor**(`name`, `key`, `options`): `undefined` \| `PropertyDescriptor`

Returns a property descriptor to be defined on the given named property.
If no descriptor is returned, the property will not become an accessor.
For example,

```ts
class MyElement extends LitElement {
  static getPropertyDescriptor(name, key, options) {
    const defaultDescriptor =
        super.getPropertyDescriptor(name, key, options);
    const setter = defaultDescriptor.set;
    return {
      get: defaultDescriptor.get,
      set(value) {
        setter.call(this, value);
        // custom action.
      },
      configurable: true,
      enumerable: true
    }
  }
}
```

#### Parameters

• **name**: `PropertyKey`

• **key**: `string` \| `symbol`

• **options**: `PropertyDeclaration`\<`unknown`, `unknown`\>

#### Returns

`undefined` \| `PropertyDescriptor`

#### Inherited from

`LitElement.getPropertyDescriptor`

#### Nocollapse

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:401

***

### getPropertyOptions()

> `static` **getPropertyOptions**(`name`): `PropertyDeclaration`\<`unknown`, `unknown`\>

Returns the property options associated with the given property.
These options are defined with a `PropertyDeclaration` via the `properties`
object or the `@property` decorator and are registered in
`createProperty(...)`.

Note, this method should be considered "final" and not overridden. To
customize the options for a given property, override
[`createProperty`](OrAttributeCard.md#createproperty).

#### Parameters

• **name**: `PropertyKey`

#### Returns

`PropertyDeclaration`\<`unknown`, `unknown`\>

#### Inherited from

`LitElement.getPropertyOptions`

#### Nocollapse

#### Final

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:416

## rendering

### renderOptions

> `readonly` **renderOptions**: `RenderOptions`

#### Inherited from

`LitElement.renderOptions`

#### Source

node\_modules/lit-element/lit-element.d.ts:97

***

### renderRoot

> `readonly` **renderRoot**: `HTMLElement` \| `ShadowRoot`

Node or ShadowRoot into which element DOM should be rendered. Defaults
to an open shadowRoot.

#### Inherited from

`LitElement.renderRoot`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:455

***

### shadowRootOptions

> `static` **shadowRootOptions**: `ShadowRootInit`

Options used when calling `attachShadow`. Set this property to customize
the options for the shadowRoot; for example, to create a closed
shadowRoot: `{mode: 'closed'}`.

Note, these options are used in `createRenderRoot`. If this method
is customized, options should be respected if possible.

#### Nocollapse

#### Inherited from

`LitElement.shadowRootOptions`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:434

***

### createRenderRoot()

> `protected` **createRenderRoot**(): `Element` \| `ShadowRoot`

#### Returns

`Element` \| `ShadowRoot`

#### Inherited from

`LitElement.createRenderRoot`

#### Source

node\_modules/lit-element/lit-element.d.ts:102

## styles

### elementStyles

> `static` **elementStyles**: `CSSResultOrNative`[]

Memoized list of all element styles.
Created lazily on user subclasses when finalizing the class.

#### Nocollapse

#### Inherited from

`LitElement.elementStyles`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:308

***

### finalizeStyles()

> `static` `protected` **finalizeStyles**(`styles`?): `CSSResultOrNative`[]

Takes the styles the user supplied via the `static styles` property and
returns the array of styles to apply to the element.
Override this method to integrate into a style management system.

Styles are deduplicated preserving the _last_ instance in the list. This
is a performance optimization to avoid duplicated styles that can occur
especially when composing via subclassing. The last item is kept to try
to preserve the cascade order with the assumption that it's most important
that last added styles override previous styles.

#### Parameters

• **styles?**: `CSSResultGroup`

#### Returns

`CSSResultOrNative`[]

#### Inherited from

`LitElement.finalizeStyles`

#### Nocollapse

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:449

## updates

### hasUpdated

> **hasUpdated**: `boolean`

Is set to `true` after the first update. The element code cannot assume
that `renderRoot` exists before the element `hasUpdated`.

#### Inherited from

`LitElement.hasUpdated`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:474

***

### isUpdatePending

> **isUpdatePending**: `boolean`

True if there is a pending update as a result of calling `requestUpdate()`.
Should only be read.

#### Inherited from

`LitElement.isUpdatePending`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:468

***

### updateComplete

> `get` **updateComplete**(): `Promise`\<`boolean`\>

Returns a Promise that resolves when the element has completed updating.
The Promise value is a boolean that is `true` if the element completed the
update without triggering another update. The Promise result is `false` if
a property was set inside `updated()`. If the Promise is rejected, an
exception was thrown during the update.

To await additional asynchronous work, override the `getUpdateComplete`
method. For example, it is sometimes useful to await a rendered element
before fulfilling this Promise. To do this, first await
`super.getUpdateComplete()`, then any subsequent state.

#### Returns

`Promise`\<`boolean`\>

A promise of a boolean that resolves to true if the update completed
    without triggering another update.

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:659

***

### enableUpdating()

> `protected` **enableUpdating**(`_requestedUpdate`): `void`

Note, this method should be considered final and not overridden. It is
overridden on the element instance with a function that triggers the first
update.

#### Parameters

• **\_requestedUpdate**: `boolean`

#### Returns

`void`

#### Inherited from

`LitElement.enableUpdating`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:543

***

### getUpdateComplete()

> `protected` **getUpdateComplete**(): `Promise`\<`boolean`\>

Override point for the `updateComplete` promise.

It is not safe to override the `updateComplete` getter directly due to a
limitation in TypeScript which means it is not possible to call a
superclass getter (e.g. `super.updateComplete.then(...)`) when the target
language is ES5 (https://github.com/microsoft/TypeScript/issues/338).
This method should be overridden instead. For example:

```ts
class MyElement extends LitElement {
  override async getUpdateComplete() {
    const result = await super.getUpdateComplete();
    await this._myChild.updateComplete;
    return result;
  }
}
```

#### Returns

`Promise`\<`boolean`\>

A promise of a boolean that resolves to true if the update completed
    without triggering another update.

#### Inherited from

`LitElement.getUpdateComplete`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:683

***

### performUpdate()

> `protected` **performUpdate**(): `void` \| `Promise`\<`unknown`\>

Performs an element update. Note, if an exception is thrown during the
update, `firstUpdated` and `updated` will not be called.

Call `performUpdate()` to immediately process a pending update. This should
generally not be needed, but it can be done in rare cases when you need to
update synchronously.

Note: To ensure `performUpdate()` synchronously completes a pending update,
it should not be overridden. In LitElement 2.x it was suggested to override
`performUpdate()` to also customizing update scheduling. Instead, you should now
override `scheduleUpdate()`. For backwards compatibility with LitElement 2.x,
scheduling updates via `performUpdate()` continues to work, but will make
also calling `performUpdate()` to synchronously process updates difficult.

#### Returns

`void` \| `Promise`\<`unknown`\>

#### Inherited from

`LitElement.performUpdate`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:619

***

### requestUpdate()

> **requestUpdate**(`name`?, `oldValue`?, `options`?): `void`

Requests an update which is processed asynchronously. This should be called
when an element should update based on some state not triggered by setting
a reactive property. In this case, pass no arguments. It should also be
called when manually implementing a property setter. In this case, pass the
property `name` and `oldValue` to ensure that any configured property
options are honored.

#### Parameters

• **name?**: `PropertyKey`

name of requesting property

• **oldValue?**: `unknown`

old value of requesting property

• **options?**: `PropertyDeclaration`\<`unknown`, `unknown`\>

property options to use instead of the previously
    configured options

#### Returns

`void`

#### Inherited from

`LitElement.requestUpdate`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:579

***

### scheduleUpdate()

> `protected` **scheduleUpdate**(): `void` \| `Promise`\<`unknown`\>

Schedules an element update. You can override this method to change the
timing of updates by returning a Promise. The update will await the
returned Promise, and you should resolve the Promise to allow the update
to proceed. If this method is overridden, `super.scheduleUpdate()`
must be called.

For instance, to schedule updates to occur just before the next frame:

```ts
override protected async scheduleUpdate(): Promise<unknown> {
  await new Promise((resolve) => requestAnimationFrame(() => resolve()));
  super.scheduleUpdate();
}
```

#### Returns

`void` \| `Promise`\<`unknown`\>

#### Inherited from

`LitElement.scheduleUpdate`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:601

***

### shouldUpdate()

> `protected` **shouldUpdate**(`_changedProperties`): `boolean`

Controls whether or not `update()` should be called when the element requests
an update. By default, this method always returns `true`, but this can be
customized to control when to update.

#### Parameters

• **\_changedProperties**: `Map`\<`PropertyKey`, `unknown`\> \| `PropertyValueMap`\<`any`\>

Map of changed properties with old values

#### Returns

`boolean`

#### Inherited from

`LitElement.shouldUpdate`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:692

***

### update()

> `protected` **update**(`changedProperties`): `void`

Updates the element. This method reflects property values to attributes
and calls `render` to render DOM via lit-html. Setting properties inside
this method will *not* trigger another update.

#### Parameters

• **changedProperties**: `Map`\<`PropertyKey`, `unknown`\> \| `PropertyValueMap`\<`any`\>

Map of changed properties with old values

#### Returns

`void`

#### Inherited from

`LitElement.update`

#### Source

node\_modules/lit-element/lit-element.d.ts:110

***

### willUpdate()

> `protected` **willUpdate**(`_changedProperties`): `void`

Invoked before `update()` to compute values needed during the update.

Implement `willUpdate` to compute property values that depend on other
properties and are used in the rest of the update process.

```ts
willUpdate(changedProperties) {
  // only need to check changed properties for an expensive computation.
  if (changedProperties.has('firstName') || changedProperties.has('lastName')) {
    this.sha = computeSHA(`${this.firstName} ${this.lastName}`);
  }
}

render() {
  return html`SHA: ${this.sha}`;
}
```

#### Parameters

• **\_changedProperties**: `Map`\<`PropertyKey`, `unknown`\> \| `PropertyValueMap`\<`any`\>

#### Returns

`void`

#### Inherited from

`LitElement.willUpdate`

#### Source

node\_modules/@lit/reactive-element/reactive-element.d.ts:641
