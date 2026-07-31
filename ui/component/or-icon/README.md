# @openremote/or-icon \<or-icon\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying an icon from a loaded iconset.

## Install
```bash
npm i @openremote/or-icon
yarn add @openremote/or-icon
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Icons are defined in iconsets. Two are registered by the OpenRemote `Manager` during initialisation, and loading them
can be prevented with `ManagerConfig.loadIcons`:

* `mdi` - [Material Design Icons](https://materialdesignicons.com/)
* `or` - OpenRemote icons

The `icon` attribute takes an `<iconset>:<name>` reference:

```html
<or-icon icon="mdi:access-point"></or-icon>
```

The first registered iconset becomes the default, so the prefix can be omitted for it:

```html
<or-icon icon="access-point"></or-icon>
```

Styling is done through CSS, the following CSS variables can be used:

```css
--or-icon-fill (default: currentColor)
--or-icon-stroke-width (default: 0)
--or-icon-height (default: 24px)
--or-icon-width (default: 24px)
```

### Custom iconsets
Register an iconset with `IconSets.addIconSet`. `createSvgIconSet` builds one from a viewbox size and a map of names
to SVG path data or markup:

```typescript
import {createSvgIconSet, IconSets} from "@openremote/or-icon";

IconSets.addIconSet("my-icons", createSvgIconSet(24, {
    "my-icon": "M12 2 L22 22 L2 22 Z"
}));
```

```html
<or-icon icon="my-icons:my-icon"></or-icon>
```

Adding an iconset dispatches `or-iconset-added` (`IconSetAddedEvent`) on `window`, and any `or-icon` in the DOM
refreshes itself in response.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-icon.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-icon
