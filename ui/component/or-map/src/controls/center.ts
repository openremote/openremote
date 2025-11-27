import { LngLatLike, Map as MapGL } from "maplibre-gl";

export class CenterControl {
    protected map?: MapGL;
    protected elem?: HTMLElement;
    public pos?: LngLatLike;

    onAdd(map: MapGL): HTMLElement {
        this.map = map;
        const control = document.createElement("div");
        control.classList.add("maplibregl-ctrl");
        control.classList.add("maplibregl-ctrl-group");
        const button = document.createElement("button");
        button.className = "maplibregl-ctrl-compass";
        button.addEventListener("click", (ev) =>
            map.flyTo({
                center: this.pos,
                zoom: map.getZoom(),
            })
        );
        const buttonIcon = document.createElement("span");
        buttonIcon.className = "maplibregl-ctrl-icon";
        button.appendChild(buttonIcon);
        control.appendChild(button);
        this.elem = control;
        return control;
    }

    onRemove(map: MapGL) {
        this.map = undefined;
        this.elem = undefined;
    }
}
