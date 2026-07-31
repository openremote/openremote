# @openremote/or-attribute-card  \<or-attribute-card\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a card that shows details about an attribute.

## Install
```bash
npm i @openremote/or-attribute-card
yarn add @openremote/or-attribute-card
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The card shows a single attribute's value over a period alongside the change against the preceding period. Pass the
loaded `assets` together with `assetAttributes`, which pairs the attribute with the index of its asset in that array.

```html
<or-attribute-card .assets="${assets}"
                   .assetAttributes="${[[0, assets[0].attributes.power]]}"
                   period="month"
                   style="height: 200px">
</or-attribute-card>
```

`period` is a moment unit such as `hour`, `day`, `week`, `month` or `year`.

### Appearance
`mainValueDecimals` sets the precision of the headline value and `deltaFormat` shows the change as either `absolute`
or `percentage`. `showControls` exposes the period controls and `hideAttributePicker` removes the button that lets
the user swap the attribute.

Setting `panelName` makes the card persist the user's attribute and period choice per realm in console storage,
sharing that storage with `or-chart`.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-attribute-card.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-attribute-card
