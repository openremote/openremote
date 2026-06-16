import { IControl, LngLat, Map as MapGL } from "maplibre-gl";
import "@openremote/or-vaadin-components/or-vaadin-text-field";
import "@openremote/or-vaadin-components/or-vaadin-icon";

export const CoordinatesRegexPattern = "^[ ]*(?:Lat: )?(-?\\d+\\.?\\d*)[, ]+(?:Lng: )?(-?\\d+\\.?\\d*)[ ]*$";

export function getCoordinatesInputKeyHandler(valueChangedHandler: (value: LngLat | undefined) => void) {
    return (e: KeyboardEvent) => {
        if (e.code === "Enter" || e.code === "NumpadEnter") {
            const valStr = (e.target as any).value as string;
            let value: LngLat | undefined = !valStr ? undefined : {} as LngLat;

            if (valStr) {
                const lngLatArr = valStr.split(/[ ,]/).filter(v => !!v);
                if (lngLatArr.length === 2) {
                    value = new LngLat(
                        Number.parseFloat(lngLatArr[0]),
                        Number.parseFloat(lngLatArr[1])
                    );
                }
            }
            valueChangedHandler(value);
        }
    };
}

export class CoordinatesControl implements IControl {
    protected map?: MapGL;
    protected elem?: HTMLElement;
    protected input?: HTMLElement;
    protected _readonly = false;
    protected _value: any;
    protected _valueChangedHandler: (value: LngLat | undefined) => void;

    constructor(disabled = false, valueChangedHandler: (value: LngLat | undefined) => void) {
        this._readonly = disabled;
        this._valueChangedHandler = valueChangedHandler;
    }

    onAdd(map: MapGL): HTMLElement {
        this.map = map;
        const control = document.createElement("div");
        control.classList.add("maplibregl-ctrl");
        control.classList.add("maplibregl-ctrl-group");

        const input = document.createElement("or-vaadin-text-field") as HTMLElement;
        input.setAttribute("theme", "small");
        if (this._readonly) input.setAttribute("readonly", "");
        if (this._value != null) (input as any).value = this._value;
        input.setAttribute("pattern", CoordinatesRegexPattern);
        input.addEventListener("keyup", getCoordinatesInputKeyHandler(this._valueChangedHandler) as EventListener);

        const icon = document.createElement("or-vaadin-icon") as HTMLElement;
        icon.setAttribute("icon", "vaadin:crosshairs");
        icon.setAttribute("slot", "prefix");
        icon.style.cssText = "width: 14px; height: 14px;";
        input.appendChild(icon);

        control.appendChild(input);
        this.elem = control;
        this.input = input;
        return control;
    }

    onRemove(_map: MapGL) {
        this.map = undefined;
        this.elem = undefined;
        this.input = undefined;
    }

    public set readonly(readonly: boolean) {
        this._readonly = readonly;
        if (this.input) {
            if (readonly) {
                this.input.setAttribute("readonly", "");
            } else {
                this.input.removeAttribute("readonly");
            }
        }
    }

    public set value(value: any) {
        this._value = value;
        if (this.input) {
            (this.input as any).value = value;
        }
    }
}
