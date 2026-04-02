/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {customElement, property, query} from "lit/decorators.js";
import {LitElement, PropertyValues, css, html} from "lit";
import {Rive, RuntimeLoader, ViewModelInstance} from "@rive-app/webgl2";
import debounce from "lodash.debounce";

// Import WebAssembly required for running Rive
// @ts-ignore
import riveWASMResource from "@rive-app/webgl2/rive.wasm";

RuntimeLoader.setWasmUrl(riveWASMResource);

@customElement("or-rive-renderer")
export class SxRiveRenderer extends LitElement {

    static get styles() {
        return [css`
            :host {
                display: block;
                position: relative;
                width: 100%;
                height: 100%;
                overflow: hidden;
                min-height: var(--solyx-size-sm); /* Prevent 0-height collapses */
            }
            canvas {
                display: block;
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
            }
        `];
    }

    /**
     * A string representing the URL or path to the public asset .riv file.
     * For more details, refer to Rive Parameters on how to properly use this property.
     */
    @property({type: String})
    public url?: string;

    /**
     * The string representing the artboard you want to display.
     * When not supplied, the default artboard from the .riv file is selected.
     * Docs: https://rive.app/docs/runtimes/web/artboards
     */
    @property({type: String})
    public artboard?: string;

    /**
     * A list of strings (`["My State Machine"]`) you wish to display.
     * Look up the Rive documentation for more info: https://rive.app/docs/runtimes/web/state-machines.
     * By default, the state machine will autoplay.
     */
    @property({type: Array})
    public stateMachines = ["State Machine 1"];

    @query("canvas")
    protected _canvas?: HTMLCanvasElement;

    protected _rive?: Rive;
    protected _resizeObserver?: ResizeObserver;
    protected _valueQueue: (() => void)[] = [];

    /**
     * Public function for updating data within the animation using "View Models".
     * For example, if the animation has a state property named "myValue", you can use this function to update the value to "100".
     * Full documentation can be found here: https://rive.app/docs/runtimes/web/data-binding
     * @param name - Name of the state property inside the .riv animation
     * @param value - Value to set. It will auto-detect the object {@link type}.
     * @param type - (optional) the value type to set. Currently, the supported types are: string, boolean, number, color and enum. Everything else will be converted to a trigger.
     */
    public async setValue(name: string, value?: any, type: string = typeof value): Promise<void> {
        await this.updateComplete;
        const func = () => {
            const vmi = this._rive!.viewModelInstance as ViewModelInstance;
            console.debug(`Setting ${name} ${type} value of Rive animation ${this.artboard} to`, value);
            if(type === "string") {
                vmi.string(name)!.value = value;
            } else if(type === "boolean") {
                vmi.boolean(name)!.value = value;
            } else if(type === "number") {
                vmi.number(name)!.value = value;
            } else if(type === "color") {
                vmi.color(name)!.value = value;
            } else if(type === "enum") {
                vmi.enum(name)!.value = value;
            } else {
                vmi.trigger(name);
            }
        };
        // If Rive has not been loaded yet, cache the function until it does.
        if(!this._rive?.viewModelInstance) {
            this._valueQueue.push(func);
            return;
        }
        func();
    }

    connectedCallback() {
        super.connectedCallback();
        this._resizeObserver = new ResizeObserver(entries => this._onResize(entries));
        this._resizeObserver.observe(this);
    }

    disconnectedCallback() {
        this._resizeObserver?.disconnect();
        this._onResize.cancel();
        super.disconnectedCallback();
    }

    updated(changedProps: PropertyValues) {
        if (!this.url || !this._canvas) {
            console.warn("Could not render Rive animation; some details were missing");
        } else {
            this._rive?.deleteRiveRenderer();
            console.debug("Loading Rive animation of artboard", this.artboard);
            this._rive = new Rive({
                artboard: this.artboard,
                src: this.url,
                canvas: this._canvas,
                autoplay: true,
                autoBind: true,
                stateMachines: this.stateMachines,
                onLoad: () => {
                    this._rive?.resizeDrawingSurfaceToCanvas();
                    if(this._valueQueue) {
                        this._valueQueue.forEach(func => func());
                    }
                }
            });
        }
        return super.updated(changedProps);
    }

    render() {
        return html`
            <canvas></canvas>
        `;
    }

    /**
     * Callback function when this web component changes in size.
     * In this case, it is used for calling a Rive JS function to calibrate using the new canvas size.
     * @protected
     */
    protected _onResize = debounce((entries: ResizeObserverEntry[]) => {
        if(this._canvas && entries?.length > 0) {
            this._canvas.width = entries[0].contentRect.width;
            this._canvas.height = entries[0].contentRect.height;
        }
        this._rive?.resizeDrawingSurfaceToCanvas();
    }, 150);

}
