import {html} from "@polymer/polymer";
import {customElement, property} from "@polymer/decorators";
import {OrMapMarker} from "./or-map-marker";


/**
 * `or-map-marker`
 * Displays marker on OpenRemote Map
 *
 * @customElement
 * @polymer
 * @demo demo/index.html
 */

@customElement('or-map-marker-basic')
export class OrMapMarkerBasic extends OrMapMarker {
    @property({type: Number})
    lat: number = 0;

    @property({type: Number})
    lng: number = 0;

    @property({type: String})
    icon: string = '';

    ready() {
       super.ready();
       this._lat = this.lat;
       this._lng = this.lng;
       const div = document.createElement('div');
       div.innerHTML = `<style>
                            .marker {
                                position: absolute;
                                width: 32px;
                                height: 32px;
                                background-size: 32px;
                            }
                        </style>
                        <div style="background-image: url('${this.icon}');" class="marker"></div>`;
        this._html = div;
    }

}

