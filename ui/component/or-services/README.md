# @openremote/or-services  \<or-services\>
[![NPM Version][npm-image]][npm-url]

Web component for displaying registered services from the OpenRemote Manager. The listed services are displayed in a sidebar menu and can be selected to display the service in an iframe.

## Install
```bash
npm i @openremote/or-services
yarn add @openremote/or-services
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The component renders the services it is given; loading them and tracking the selection is the consumer's job.

```html
<or-services .realmName="${this.realmName}"
             .services="${this.services}"
             .selectedService="${this.selectedService}"
             .loading="${this._loading}"
             @or-service-selected="${(e) => this.selectedService = e.detail}">
</or-services>
```

`realmName` defaults to the manager's display realm and is substituted into the `{realm}` placeholder of a service's
`homepageUrl`, as a path segment for super users and as a query parameter otherwise. Set `loading` while fetching to
show the loading state.

Call `refreshIframe()` on the element to reload the currently displayed service, for example after its status
changes.

### Events
* `or-service-selected` (`OrServiceSelectedEvent`) - A service was selected in the sidebar; detail is the
`ExternalService`

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-services.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-services
