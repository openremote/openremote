import "@polymer/iron-demo-helpers/demo-pages-shared-styles";
import "@polymer/iron-demo-helpers/demo-snippet";
import "@openremote/or-map";
import manager, {Auth} from "@openremote/core";
import {OrMap, OrMapMarkerClickedEvent} from "@openremote/or-map";
import {getBuildingAsset} from "../../demo-core/src/util";

manager.init({
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    keycloakUrl: "http://localhost:8080/auth",
    managerUrl: "http://localhost:8080",
    realm: "smartcity"
})
    .then(getBuildingAsset)
    .then((apartment1) => {

        const maps = document.querySelectorAll("or-map");
        const rasterMap = maps[0] as OrMap;
        const vectorMap = maps[1] as OrMap;

        rasterMap.addEventListener(OrMapMarkerClickedEvent.NAME, (evt) => {
            console.log("RASTER MAP: Marker clicked: " + JSON.stringify(evt.detail));
        });
        rasterMap.addEventListener("click", (evt) => {
            console.log("RASTER MAP: Map clicked");
        });

        vectorMap.addEventListener(OrMapMarkerClickedEvent.NAME, (evt) => {
            console.log("VECTOR MAP: Marker clicked: " + JSON.stringify(evt.detail));
        });
        vectorMap.addEventListener("click", (evt) => {
            console.log("VECTOR MAP: Map clicked");
        });

        if (apartment1) {
            // Add an or-map-marker-asset to the vector map
            const assetMarker = document.createElement("or-map-marker-asset");
            assetMarker.setAttribute("assetid", apartment1.id!);
            vectorMap.appendChild(assetMarker);
        }
});
