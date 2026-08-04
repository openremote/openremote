import { CoordinatesControl } from "@openremote/or-map/controls/coordinates";

export class CoordinatesControlFixture extends HTMLElement {
    commit(value, event, readonly) {
        let result;
        const control = new CoordinatesControl(readonly, (lngLat) => {
            result = lngLat ? {lng: lngLat.lng, lat: lngLat.lat} : null;
        });
        const input = control.onAdd()
            .querySelector("or-vaadin-text-field");
        input.value = value;
        input.dispatchEvent(event === "change"
            ? new Event("change", {bubbles: true})
            : new KeyboardEvent("keyup", {code: "Enter", bubbles: true}));
        control.onRemove();
        return result;
    }
}

customElements.define("coordinates-control-fixture", CoordinatesControlFixture);
