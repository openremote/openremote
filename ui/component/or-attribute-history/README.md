# @openremote/or-attribute-history  \<or-attribute-history\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying historical values of an attribute.

## Install
```bash
npm i @openremote/or-attribute-history
yarn add @openremote/or-attribute-history
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Identify the attribute with either an `attributeRef`, or an `attribute` together with the `assetId` and `assetType`
it belongs to. Number and boolean attributes are drawn as a chart and everything else as a table.

```html
<or-attribute-history .attributeRef="${{id: asset.id, name: "temperature"}}"
                      .assetType="${asset.type}"
                      style="height: 400px">
</or-attribute-history>
```

The window shown is the `period` (a moment unit such as `hour`, `day` or `week`) ending at `toTimestamp`, which
defaults to now.

### Configuration
`config` holds separate `chart` and `table` sections. The chart section only sets the axis labels; the table section
resolves a column layout for the attribute, looking for a match by asset type, then attribute name, then attribute
value type, and falling back to `default`.

```typescript
const config: HistoryConfig = {
    chart: {xLabel: "time", yLabel: "value"},
    table: {
        default: {
            timestampFormat: "L HH:mm:ss",
            columns: [
                {type: "timestamp", header: "time"},
                {type: "prop", path: "$.value.status", header: "status"}
            ]
        }
    }
};
```

A column of type `prop` reads its value from the datapoint using the JSONPath in `path`, and `contentProvider` can
replace the rendering of an individual cell. Set `autoColumns` to derive the columns from the keys of object values
instead of listing them.

### Events
* `or-attribute-history-event` (`OrAttributeHistoryEvent`) - The value changed; detail contains `value` and
`previousValue`

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-attribute-history.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-attribute-history
