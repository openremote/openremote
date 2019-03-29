import "@polymer/iron-demo-helpers/demo-pages-shared-styles";
import "@polymer/iron-demo-helpers/demo-snippet";
import "@openremote/or-map";
import "@openremote/or-map/dist/markers/or-map-marker";
import openremote, {Auth} from "@openremote/core";
import {OrMapMarkerEvent} from "@openremote/or-map/dist/markers/or-map-marker";
import {OrMap} from "@openremote/or-map";

openremote.init({
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    keycloakUrl: "http://localhost:8080/auth",
    managerUrl: "http://localhost:8080",
    realm: "tenantA"
}).then(() => {

    let maps = document.querySelectorAll("or-map");
    let rasterMap = maps[0] as OrMap;
    let vectorMap = maps[1] as OrMap;
    rasterMap.addEventListener(OrMapMarkerEvent.CLICKED, (evt) => {
        console.log("RASTER MAP: Marker clicked: " + JSON.stringify(evt.detail));
    });
    rasterMap.addEventListener("click", (evt) => {
        console.log("RASTER MAP: Map clicked");
    });

    vectorMap.addEventListener(OrMapMarkerEvent.CLICKED, (evt) => {
        console.log("VECTOR MAP: Marker clicked: " + JSON.stringify(evt.detail));
    });
    vectorMap.addEventListener("click", (evt) => {
        console.log("VECTOR MAP: Map clicked");
    });
});
