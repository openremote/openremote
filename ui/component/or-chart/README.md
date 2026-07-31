# @openremote/or-chart  \<or-chart\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying chart values.

## Install
```bash
npm i @openremote/or-chart
yarn add @openremote/or-chart
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The chart plots datapoints for one or more attributes. Pass the loaded `assets` together with `assetAttributes`, which
pairs each attribute with the index of its asset in that array, and a `datapointQuery` describing how the manager
should aggregate the stored datapoints. The element has no intrinsic height, so give it one.

```html
<or-chart .assets="${assets}"
          .assetAttributes="${[[0, assets[0].attributes.temperature]]}"
          .datapointQuery="${{type: "lttb", amountOfPoints: 100}}"
          style="height: 400px">
</or-chart>
```

Set `attributeControls` to let the user pick attributes from an asset tree, and `timestampControls` to expose the
time range controls.

### Time range
The visible window is either an explicit `timeframe` of two dates, or a preset built from `timePrefixKey`
(`this`, `last` or `next`) and `timeWindowKey` (`Hour`, `24Hours`, `Week`, `Month`, `Year` and others). Replace
`timePrefixOptions` or `timeWindowOptions` to offer a different set.

```html
<or-chart timePrefixKey="last" timeWindowKey="24Hours" ...></or-chart>
```

### Appearance
`colors` sets the palette and `attributeColors` overrides it per attribute. `attributeConfig` moves attributes to the
right axis or changes how their series is drawn:

```typescript
const attributeConfig: ChartAttributeConfig = {
    rightAxisAttributes: [{id: assetId, name: "humidity"}],
    areaAttributes: [{id: assetId, name: "temperature"}],
    smoothAttributes: [{id: assetId, name: "temperature"}]
};
```

`showLegend`, `denseLegend`, `stacked` and `showZoomBar` toggle the surrounding chart furniture, and `chartOptions`
is merged into the underlying ECharts option object for anything not covered by a property.

Setting `panelName` makes the chart persist the user's attribute and time selections per realm in console storage.

### Data
`dataProvider` replaces the manager query entirely and is called with the start and end of the visible period:

```typescript
const dataProvider = async (startOfPeriod: number, endOfPeriod: number) => loadSeries(startOfPeriod, endOfPeriod);
```

### Events
* `or-chart-event` (`OrChartEvent`) - The chart value changed; detail contains `value` and `previousValue`

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-chart.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-chart
