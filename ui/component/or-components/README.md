# @openremote/or-components
[![NPM Version][npm-image]][npm-url]

Contains basic UI components

## Install
```bash
npm i @openremote/or-components
yarn add @openremote/or-components
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Each element is a separate entry point, so import only the ones being used:

```typescript
import "@openremote/or-components/or-panel";
```

### \<or-panel\>
A titled container; `heading` accepts a string or a template, and the content goes in the default slot.

```html
<or-panel heading="settings">
    <div>...</div>
</or-panel>
```

### \<or-collapsible-panel\>
A panel that expands and collapses, with `header`, `header-description` and `content` slots. Set `expanded` for the
initial state, or `expandable` to `false` to drop the chevron and keep the content collapsed.

```html
<or-collapsible-panel expanded>
    <span slot="header">Details</span>
    <span slot="header-description">3 items</span>
    <div slot="content">...</div>
</or-collapsible-panel>
```

Pass `lazycontent` instead of the `content` slot to defer building the content until the panel is first expanded.

### \<or-loading-indicator\> and \<or-loading-wrapper\>
The indicator is a standalone spinner; setting `overlay` centres it over the parent. The wrapper hides its slotted
content while `loading` is set. Set `fadeContent` to fade rather than hide, and `loadDom` to `false` to keep the
content out of the DOM entirely until loading finishes.

```html
<or-loading-wrapper .loading="${this._loading}" fadeContent>
    <div>...</div>
</or-loading-wrapper>
```

### \<or-iframe\>
An iframe with loading and error states. `timeout` is how long to wait before reporting failure, and `preventCache`
appends a cache busting parameter to `src`.

```html
<or-iframe src="https://example.com" .timeout="${20000}"></or-iframe>
```

### \<or-file-uploader\>
A file input that previews the current file at `src` and fires a `change` event whose detail `value` is the selected
`FileList`. `accept` restricts the file types and defaults to the image formats used for logos and favicons.

```html
<or-file-uploader .src="${this.logoUrl}" .accept="${"image/png,image/svg+xml"}"
                  @change="${(e) => this._upload(e.detail.value[0])}">
</or-file-uploader>
```

### \<or-ace-editor\>
An [Ace](https://ace.c9.io) editor. `mode` selects the syntax (`ace/mode/json` by default), `value` holds the content
and `readonly` disables editing. Use `getValue()` and `validate()` to read and check the content, or listen for
`or-ace-editor-changed`, which reports the new value and whether it is valid.

```html
<or-ace-editor .value="${this.rules}" .mode="${"ace/mode/javascript"}"
               @or-ace-editor-changed="${(e) => this._onChanged(e.detail)}">
</or-ace-editor>
```

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-components.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-components
