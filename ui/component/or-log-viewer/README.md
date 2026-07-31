# @openremote/or-log-viewer  \<or-log-viewer\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying system logs.

## Install
```bash
npm i @openremote/or-log-viewer
yarn add @openremote/or-log-viewer
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

With no properties set the viewer queries the manager's syslog and renders it with its own controls:

```html
<or-log-viewer></or-log-viewer>
```

Set `live` to keep the view following new events as they arrive rather than showing a fixed window.

### Query
`level`, `categories` and `filter` narrow which events are shown, and `limit` caps how many are fetched. The window
is the `interval` ending at `timestamp`, which defaults to now.

```html
<or-log-viewer level="WARN" .categories="${["RULES"]}" interval="HOUR" limit="500"></or-log-viewer>
```

### Configuration
`config` sets the initial state of the controls and hides the ones an app does not want the user to change.

```typescript
const config: ViewerConfig = {
    allowedCategories: ["RULES", "PROTOCOL"],
    initialCategories: ["RULES"],
    initialLevel: "INFO",
    initialFilter: "",
    hideCategories: false,
    hideFilter: true,
    hideLevel: false
};
```

### Styling
```css
--or-log-viewer-background-color (default: var(--or-app-color2))
--or-log-viewer-text-color (default: var(--or-app-color3))
--or-log-viewer-controls-margin (default: 0)
```

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-log-viewer.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-log-viewer
