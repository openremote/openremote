# @openremote/or-map  \<or-map\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a MapLibre map. This component requires an OpenRemote Manager to retrieve map settings
and tiles.

## Install
```bash
npm i @openremote/or-map
yarn add @openremote/or-map
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

```html
<or-map center="5.454250,51.445990" zoom="5" style="height: 500px; width: 100%;"></or-map>
```

The center and zoom are optional overrides for the defaults that will be defined in the `mapsettings` loaded from the
OpenRemote Manager; each realm can have different `mapsettings` with a fallback to the default. The `center` attribute
is parsed as `lng,lat`.

Other properties on `or-map`:

* `cluster` - a `ClusterConfig` (`cluster`, `clusterRadius`, `clusterMaxZoom`) overriding how registered assets are
clustered; clustering is on by default
* `geoJson` and `showGeoJson` - overlay a GeoJSON layer
* `boundary` and `showBoundaryBoxControl` - restrict and edit the visible area
* `filters` - a list of `MapFilter` presets; adds a control for picking one and limits the registered assets to those
it matches
* `showGeoCodingControl` - add the search control, `showLegend` - add the asset type legend once at least two types
are registered
* `useZoomControl` - apply the zoom limits from the `mapsettings`
* `controls` - replace the default navigation and geolocate controls with custom `IControl` instances

Markers can be added via markup as children:

```html
<or-map id="map" center="5.454250,51.445990" zoom="5" style="height: 500px; width: 100%;">
    <or-map-marker lat="51.445990" lng="5.454250" icon="or:logo-plain"></or-map-marker>
</or-map>
```

Or programmatically:

```typescript
const map = document.getElementById("map");
const assetMarker = document.createElement("or-map-marker-asset");
assetMarker.assetId = apartment1.id!;
map.appendChild(assetMarker);
```

### Assets
Markers added as children are drawn as they are and take no part in clustering, filtering or the legend. Assets that
should do so are registered with the map instead, which owns the decision of what is worth drawing at the current
viewport:

```typescript
map.addAssets(assets);
map.updateAttribute(attributeEvent);
map.removeAssets([assetId]);
map.removeAllAssets();
```

Each registered asset needs an `id`, a `type` and a `location` attribute. The map then reports the assets that survive
clustering and filtering through `or-map-markers-changed`, and the consumer renders a marker per asset:

```html
<or-map @or-map-markers-changed="${(e) => this._assetsOnScreen = e.detail}">
    ${this._assetsOnScreen.map(asset => html`
        <or-map-marker-asset .asset="${asset}" .config="${this.markerConfig}"></or-map-marker-asset>
    `)}
</or-map>
```

There are two types of built in markers:

### \<or-map-marker\>
This is a basic marker and the base class for any other markers and it has the following properties:

* `lat`* and `lng`*
* `visible` (show/hide the marker)
* `icon` (uses `or-icon` to render an icon inside the default marker)
* `color` and `activeColor` (override the marker colour, the latter while `active` is set)
* `active` (renders the marker in its larger, highlighted form)
* `interactive` (sets pointer events for the marker)
* `radius` (draws a circle of the given radius in metres around the marker; only one marker at a time can show one)
* `displayValue` (text rendered next to the marker)
* `direction` (rotates the marker, in degrees)

*required

The visual content of the marker can be controlled by adding child content to the `or-map-marker` element; any child
content is rendered inside a `div`. If no children are specified then the default marker will be used. Subclasses can
override the `createMarkerContent()` method to control the look of the marker.

### \<or-map-marker-asset\>
This links the marker to an Asset in the OpenRemote Manager by using the `asset-mixin` and adds the following
properties:

* `assetId`* (ID of the Asset to link) or `asset` (an already loaded Asset)
* `config` (per asset type overrides for the icon and colour, including thresholds on an attribute value)
* `assetTypeAsIcon` (default: `true`)

*required

The Asset must be valid, accessible and must have a valid `location` attribute otherwise no marker will be shown. By
default the asset type is used to set the icon of the marker but this can be controlled by setting the
`assetTypeAsIcon` property, which is read while the asset resolves and so has to be set up front.

### Styling
All styling is done through CSS, the following CSS variables can be used:

```css
--or-map-width (default: 100%)
--or-map-min-height (default: 300px)

--or-map-marker-color (default: var(--or-app-color3))
--or-map-marker-stroke (default: none)
--or-map-marker-width (default: 32px)
--or-map-marker-height (default: 32px)
--or-map-marker-transform (default: translate(-16px, -29px))

--or-map-marker-icon-color (default: var(--or-app-color1))
--or-map-marker-icon-stroke (default: none)
--or-map-marker-icon-width (default: 16px)
--or-map-marker-icon-height (default: 16px)
--or-map-marker-icon-transform (default: translate(-50%, -14px))
```

Each of the marker variables has an `active` counterpart that applies while the marker is active, for example
`--or-map-marker-active-color` and `--or-map-marker-icon-active-width`.

### Events
The following DOM events may be fired by the component and markers:

* `or-map-loaded` (`OrMapLoadedEvent`) - The underlying map has finished loading
* `or-map-clicked` (`OrMapClickedEvent`) - The map itself was clicked; detail contains the clicked coordinates
* `or-map-long-press` (`OrMapLongPressEvent`) - The map was long pressed; detail contains the coordinates
* `or-map-markers-changed` (`OrMapMarkersChangedEvent`) - The registered assets in view changed; detail is the list of
assets to render a marker for
* `or-map-marker-clicked` (`OrMapMarkerClickedEvent`) - A marker was clicked; detail contains the clicked `marker`
* `or-map-marker-changed` (`OrMapMarkerChangedEvent`) - A marker was modified; detail contains the changed `marker`
and the name of the changed `property`
* `or-map-geocoder-change` (`OrMapGeocoderChangeEvent`) - A search result was picked; detail contains the `geocode`

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-map.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-map
