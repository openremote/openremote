# @openremote/or-json-forms  \<or-json-forms\>
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

Web Component for dynamically generating and validating forms based on JSONSchemas. This component can be used to render attribute configuration items through the or-attribute-input.

## Install
```bash
npm i @openremote/or-json-forms
yarn add @openremote/or-json-forms
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

```$html
<or-json-forms ... />
```

### Architecture

The `or-json-forms` component renders inputs based on the JSONSchema that you provide.

Inside the util
The component recursively walks over the Schema using the `getTemplateFromProps` function to determine ...

Standard-renderers

```
or-json-forms recursive structure
  index.ts -> (util.ts)getTemplateFromProps -> (standard-renderers.ts)*
    case (rank6)(schema.const || schema.enum.length === 1)                                                      constTester                 -> constRenderer
    case (rank4)(Control is schema.anyOf || schema.oneOf)                                                       anyOfOneOfControlTester     -> anyOfOneOfControlRenderer
    case (rank4)(Control is schema.allOf)                                                                       allOfControlTester          -> allOfControlRenderer
    case (rank3)(Control is String, Boolean, Number, Integer, Date, Time, DateTime, Enum, OneOfEnum, EnumArray) inputControlTester          -> inputControlRenderer
    case (rank2)(schema.type == array && typeof schema.items != array)                                          arrayControlTester          -> arrayControlRenderer
    case (rank2)(Control is object)                                                                             objectControlTester         -> objectControlRenderer
    case (rank1)(VerticalLayout || Group)                                                                       verticalOrGroupLayoutTester -> verticalLayoutRenderer
```


### Standard-renderers
This is a basic marker and the base class for any other markers and it has the following attributes:

### Styling
All styling is done through CSS, the following CSS variables can be used:

```$css
--or-app-color5 DefaultColor5
--or-app-color5 DefaultColor5
--or-app-color5 #CCC
--or-app-color3 DefaultColor3
--or-app-color5 DefaultColor5
--or-app-color4 DefaultColor4
--or-app-color4 DefaultColor4

border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
border-right: 1px solid var(--or-app-color5, #CCC);
color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
border-bottom-color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
border-top-color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
```


```$css
--or-map-marker-fill (default: #1D5632)
--or-map-marker-stroke (default: none)
--or-map-marker-width (default: 48px)
--or-map-marker-height (default: 48px)
--or-map-marker-transform (default: translate(-24px, -45px))

--or-map-marker-icon-fill (default: #FFF)
--or-map-marker-icon-stroke (default: none)
--or-map-marker-icon-width (default: 24px)
--or-map-marker-icon-height (default: 24px)
--or-map-marker-icon-transform (default: translate(-50%, -19px))
```


### Events
The following DOM events may be fired by the component and markers:

* `onChange` - The state of the form when changed can be tracked through this callback.

## Backend

Configuring Java models to be used by the `or-json-forms` can be done using Jackson annotations.

You can use `@NonNull` to make non-primitive types required in the generated JSONSchema.

> Note: all primitive types are inherently required, because they cannot be null. All properties on JSONSchemas are
> See [Nullable types](https://github.com/mbknor/mbknor-jackson-jsonSchema/?tab=readme-ov-file#code---using-java).

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.

## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/live-xxx.svg
[npm-url]: https://npmjs.org/package/@openremote/or-map
[travis-image]: https://img.shields.io/travis/live-js/live-xxx/master.svg
[travis-url]: https://travis-ci.org/live-js/live-xxx
[coveralls-image]: https://img.shields.io/coveralls/live-js/live-xxx/master.svg
[coveralls-url]: https://coveralls.io/r/live-js/live-xxx?branch=master
