# @openremote/or-map  \<or-map\>
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

Web Component for displaying a Mapbox map; either raster or vector (default). This component requires an OpenRemote
Manager to retrieve map settings and tiles.

## Install
```bash
npm i @openremote/or-map
yarn add @openremote/or-map
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

```$html
<or-map center="5.454250, 51.445990" zoom="5" style="height: 500px; width: 100%;" />
```

For a raster map (raster tile serving in the OpenRemote Manager must be properly configured):

```$html
<or-map type="RASTER" center="5.454250, 51.445990" zoom="5" style="height: 500px; width: 100%;" />
```

The center and zoom are optional overrides for the defaults that will be defined in the `mapsettings` loaded from the
OpenRemote Manager; each realm can have different `mapsettings` with a fallback to the default.

Markers can be added via markup as children:

```$html
<or-map id="vectorMap" center="5.454250, 51.445990" zoom="5" style="height: 500px; width: 100%;">
    <or-map-marker id="demo-marker" lng="5.454250" class="marker" icon="or:logo-plain"></or-map-marker>
</or-map>
```

Or programmatically:

```$typescript
const vectorMap = docuemnt.getElementById("vectorMap");
const assetMarker = document.createElement("or-map-marker-asset");
assetMarker.setAttribute("asset", apartment1.id!);
vectorMap.appendChild(assetMarker);
```

There are two types of built in markers:

### \<or-map-marker\>
This is a basic marker and the base class for any other markers and it has the following attributes:

* lat*
* lng*
* visible (show/hide the marker)
* icon (uses `or-icon` to render an icon inside the default marker)
* interactive (sets pointer events for the marker)

*required

The visual content of the marker can be controlled by adding child content to the `or-map-marker` element; any child
content is rendered inside a `div`. If no children are specified then the default marker will be used. Subclasses can
override the `createMarkerContent()` method to control the look of the marker.

### \<or-map-marker-asset\>
This links the marker to an Asset in the OpenRemote Manager by using the `asset-mixin` and adds the following attribute:

* asset* (ID of the Asset to link) 

*required 

The Asset must be valid, accessible and must have a valid `location` attribute otherwise no marker will be shown. By
default the `AssetType` is used to set the icon of the marker but this can be controlled by setting the `assetTypeAsIcon`
property.
  
### Styling
All styling is done through CSS, the following CSS variables can be used:

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
 
* `click` - Standard click event is fired when the map itself is clicked
* `OrMapMarkerEvent.CLICKED` - A map marker has been clicked; details contains the clicked `marker`
* `OrMapMarkerEvent.CHANGED` - A map marker has been modified; details contains the changed `marker` and name of changed
`property`

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