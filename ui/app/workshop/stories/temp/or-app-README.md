# @openremote/or-app  \<or-app\>
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

## Install
```bash
npm i @openremote/or-app
yarn add @openremote/or-app
```

## Usage
```javascript
import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {OrApp, appReducer} from "@openremote/or-app";
import "@openremote/or-app"; // this is necessary

const rootReducer = combineReducers({
    app: appReducer
});
const store = configureStore({
    reducer: rootReducer
});
const orApp = new OrApp(store);
document.body.appendChild(orApp);
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
