# @openremote/or-translate  \<or-translate\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a translated string based on the `i18next` library. This component requires an OpenRemote Manager to retrieve the locale files.

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

* Language: console preference, then user preference, then `ManagerConfig.defaultLanguage`, then `en`
* Fallback language: `en`
* Default Namespace: `app`
* Fallback namespace: `or`
* Available namespaces: `ManagerConfig.loadTranslations`
* OR Namespace path: `managerURL` + `/shared/locales/{{lng}}/{{ns}}.json`
* Namespace path: `ManagerConfig.translationsLoadPath` or fallback to `locales/{{lng}}/{{ns}}.json`

Pass `ManagerConfig.configureTranslationsOptions` to amend the `InitOptions` before `i18next` is initialised.

There is an `or` namespace which is used for OpenRemote related translations; apps can use any other namespace(s) it is
recommended to use `app` as this is set as the default as described above. To translate a string use the following HTML:

```html
<or-translate value="app:asset"></or-translate>
```

If using the default namespace then the namespace prefix can be omitted:
```html
<or-translate value="asset"></or-translate>
```

It is also possible to pass a `TOptions<InitOptions>` object to the `i18next.t` method by setting the
`options` property, for example to interpolate values:

```html
<or-translate value="assetCount" .options="${{count: 5}}"></or-translate>
```

Interpolation also accepts a format, where `uppercase` and any `moment` format string for `Date` values are supported.


### Translate mixin (`@openremote/or-translate/translate-mixin`)
Exports a `translate` function/mixin that can be used by any web component to hook into the `i18next` `initialized` and
`languageChanged` events; if the web component is a `LitElement` an update of the component will be automatically
requested when either event fires; otherwise the `initCallback` and/or `langChangedCallback` should be overridden as
required.

```typescript
import {i18next, translate} from "@openremote/or-translate";

@customElement("my-element")
export class MyElement extends translate(i18next)(LitElement) {

    protected render() {
        return html`<span>${i18next.t("asset")}</span>`;
    }
}
```


## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-translate.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-translate
