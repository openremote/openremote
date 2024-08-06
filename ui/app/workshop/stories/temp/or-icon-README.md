# @openremote/or-icon \<or-icon\>
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

Web Component for displaying an icon from a loaded iconset.

## Install
```bash
npm i @openremote/or-icon
yarn add @openremote/or-icon
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Icons are defined in iconsets, there are two iconsets provided by default and new ones can be created as required (see
[demo-core](../../demo/demo-core)), The `mdi` iconset is quite large and it is possible to prevent loading of this
when initialising the OpenRemote `Manager` via the `ManagerConfig`:

* `mdi` - [Material Design Icons](https://materialdesignicons.com/)
* `or` - OpenRemote icons (see [here](./or-iconset.ts))

The default iconset is `mdi` but this can be changed by setting `OrIcon.DEFAULT_ICONSET`, to load an icon use the
following HTML: 

```$html
<or-icon icon="mdi:access-point" />
```

If using the default iconset then the iconset prefix can be omitted:
```$html
<or-icon icon="access-point" />
```

Styling is done through CSS, the following CSS variables can be used:

```$css
--or-icon-fill (default: currentcolor)
--or-icon-stroke (default: none)
--or-icon-height (default: 24px)
--or-icon-width (default: 24px)
--or-icon-pointer-events (default: none)
```

When an iconset is added then any `or-icon` components in the DOM will be notified and refresh as required.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/live-xxx.svg
[npm-url]: https://npmjs.org/package/@openremote/or-icon
[travis-image]: https://img.shields.io/travis/live-js/live-xxx/master.svg
[travis-url]: https://travis-ci.org/live-js/live-xxx
[coveralls-image]: https://img.shields.io/coveralls/live-js/live-xxx/master.svg
[coveralls-url]: https://coveralls.io/r/live-js/live-xxx?branch=master
