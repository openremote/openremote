# @openremote/or-attribute-barchart  \<or-attribute-barchart\>
[![NPM Version][npm-image]][npm-url]

Web Component for showing aggregated values at intervals of attribute datapoints.

## Install
```bash
npm i @openremote/or-attribute-barchart
yarn add @openremote/or-attribute-barchart
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Pass the loaded `assets` together with `assetAttributes`, which pairs each attribute with the index of its asset in
that array, an interval `datapointQuery`, and an `attributeConfig` naming the aggregation to apply. The element has no
intrinsic height, so give it one.

```html
<or-attribute-barchart .assets="${assets}"
                       .assetAttributes="${[[0, assets[0].attributes.power]]}"
                       .attributeConfig="${{methodAvgAttributes: [{id: assets[0].id, name: "power"}]}}"
                       .datapointQuery="${{type: "interval"}}"
                       interval="hour"
                       style="height: 400px">
</or-attribute-barchart>
```

The chart derives the query's `interval`, `formula`, `gapFill` and timestamps from its own properties on every load,
so only the query `type` needs supplying.

### Intervals and time range
`interval` selects a bucket size from `intervalOptions` (`auto`, `1Minute` through `year`); replace that map to offer
a different set. The visible window is either an explicit `timeframe` of two dates, or a preset built from
`timePrefixKey` (`this`, `last` or `next`) and `timeWindowKey`. Set `timestampControls` to let the user change it.

### Appearance
`colors` sets the palette and `attributeColors` overrides it per attribute. `attributeConfig` moves attributes to the
right axis and chooses the aggregation formula per attribute:

```typescript
const attributeConfig: BarChartAttributeConfig = {
    rightAxisAttributes: [{id: assetId, name: "cost"}],
    methodMaxAttributes: [{id: assetId, name: "power"}]
};
```

An attribute is only plotted if it appears in at least one `method*Attributes` list, and each list it appears in
produces its own series.

`showLegend`, `denseLegend` and `stacked` toggle the surrounding chart furniture, `decimals` controls value
formatting, and `chartOptions` is merged into the underlying ECharts option object.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-attribute-barchart.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-attribute-barchart
