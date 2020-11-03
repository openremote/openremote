# @openremote/or-asset-viewer  \<or-asset-viewer\>
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

Web Component for displaying an asset tree. This component requires an OpenRemote Manager to retrieve, save and query assets.

## Install
```bash
npm i @openremote/or-asset-viewer
yarn add @openremote/or-asset-viewer
```

## Usage
By default the or-asset-viewer is using a 2 columns grid. This can be changed by using a different config.

4 column grid, 25% for each column:
```javascript
const viewerConfig = {
    viewerStyles: {
        gridTemplateColumns: "repeat(auto-fill, minmax(calc(25%),1fr))";
    }
};
<or-asset-viewer .config="${viewerConfig}"></or-asset-viewer>
```


The position of a panel can also be changed by changing the config of or-asset-viewer

To change the width of a panel use gridColumn:
```javascript
const viewerConfig = {
    panels: {
      "info": {
          type: "property",
          panelStyles: {
            gridColumn: "1 / -1" // same as 1 / 3 in a 2 column grid: Start on column 1, End on column 3
          }
      }
    }
};
```

gridColumn can also be used to change the position horizontaly.
```javascript
const viewerConfig = {
    panels: {
      "info": {
          type: "property",
          panelStyles: {
            gridColumnStart: "2" // start the panel in the second column
          }
      }
    }
};
```

To change the vertical position of a panel use gridRowStart. To start the panel on the first row set gridRowStart to 1:
```javascript
const viewerConfig = {
    panels: {
      "info": {
          type: "property",
          panelStyles: {
            gridRowStart: "1"
          }
      }
    }
};
```

For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPLv3](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/live-xxx.svg
[npm-url]: https://npmjs.org/package/@openremote/or-asset-list
[travis-image]: https://img.shields.io/travis/live-js/live-xxx/master.svg
[travis-url]: https://travis-ci.org/live-js/live-xxx
[coveralls-image]: https://img.shields.io/coveralls/live-js/live-xxx/master.svg
[coveralls-url]: https://coveralls.io/r/live-js/live-xxx?branch=master
