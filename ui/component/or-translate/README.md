# @openremote/or-translate  \<or-translate\>
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

Web Component for displaying a translated string based on the `18next` library.

## Install
```bash
npm i @openremote/or-translate
yarn add @openremote/or-translate
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

For a full list of `i18next` functionality refer to that project's [documentation](https://www.i18next.com/). 

If used in conjunction with `@openremote/core` and the `Manager` `init` method has been called then the `i18next`
default export will be ready to use and would have been configured with the following settings:

* Language: `en`
* Fallback language: `en`
* Default Namespace: `app`
* Fallback namespace: `or`
* Available namespaces: `ManagerConfig.loadTranslations`
* OR Namespace path: `managerURL` + `/shared/locales/{{lng}}/{{ns}}.json`
* Namespace path: `ManagerConfig.loadTranslations` or fallback to `locales/{{lng}}/{{ns}}.json`

There is an `or` namespace which is used for OpenRemote related translations; apps can use any other namespace(s) it is
recommended to use `app` as this is set as the default as described above. To translate a string use the following HTML:

```$html
<or-translate value="app:asset" />
```

If using the default namespace then the namespace prefix can be omitted:
```$html
<or-translate value="asset" />
```

It is also possible to pass an `TOptions<InitOptions>` object to the `18next.t` method by setting the
`options` attribute.
 

### Translate mixin (`dist/translate-mixin`)
Exports a `translate` function/mixin that can be used by any web component to hook into the `i18next` `initialized` and
`languageChanged` events; if the web component is a `LitElement` an update of the component will be automatically 
requested when either event fires; otherwise the `initCallback` and/or `langChangedCallback` should be overridden as
required. For usage example see the [or-translate source code](./src/index.ts).


## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/live-xxx.svg
[npm-url]: https://npmjs.org/package/@openremote/or-translate
[travis-image]: https://img.shields.io/travis/live-js/live-xxx/master.svg
[travis-url]: https://travis-ci.org/live-js/live-xxx
[coveralls-image]: https://img.shields.io/coveralls/live-js/live-xxx/master.svg
[coveralls-url]: https://coveralls.io/r/live-js/live-xxx?branch=master
