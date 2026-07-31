# @openremote/or-gauge  \<or-gauge\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a single value on a gauge.

## Install
```bash
npm i @openremote/or-gauge
yarn add @openremote/or-gauge
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The gauge shows one attribute value. Give it an `attrRef` and it queries the asset itself:

```html
<or-gauge .attrRef="${{id: asset.id, name: "temperature"}}" style="height: 200px"></or-gauge>
```

Pass an already loaded `asset` with its `assetAttribute` instead to avoid the extra query, or set `value` directly to
drive the gauge from data of your own. The value is read once rather than subscribed to, so a consumer that needs it
to track live changes should update `value` itself. The element has no intrinsic height, so give it one.

### Scale and formatting
`min` and `max` bound the scale, defaulting to 0 and 100, and `decimals` sets the precision. `unit` labels the value
and is resolved from the attribute descriptor whenever the gauge loads an attribute.

`thresholds` colours the arc, as ascending pairs of the value at which each band starts and its colour:

```html
<or-gauge .attrRef="${attrRef}" .min="${0}" .max="${100}"
          .thresholds="${[[0, "#4caf50"], [60, "#ff9800"], [85, "#ef5350"]]}">
</or-gauge>
```

`config.options` is passed through to the underlying gauge library for anything the properties do not cover.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.

## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-gauge.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-gauge
