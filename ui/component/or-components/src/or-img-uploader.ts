import {css, html, LitElement, PropertyValues } from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {DefaultColor2} from "@openremote/core";
import { InputType,OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { ManagerConfRealm } from "@openremote/model";

@customElement("or-img-uploader")
export class OrImgUploader extends LitElement {

    @property({attribute: false})
    public src: string = "";

    static get styles() {
        return css`
            img{
                max-width: 150px;
                max-height: 150px;
            }
            .container{
                border: 2px solid red;
            }
        `;
    }

    async _onChange(e: any) {
        if (e.target) {
            if (!!e.target.files) {
                const file = e.target.files[0];
                this.src = await this.convertBase64(file) as string;

                this.dispatchEvent(new CustomEvent('change', {
                    detail: {value: this.src}
                }))
            }
        }
    }


    convertBase64 (file:any) {
        return new Promise((resolve, reject) => {
            const fileReader = new FileReader();
            fileReader.readAsDataURL(file);

            fileReader.onload = () => {
                resolve(fileReader.result);
            };

            fileReader.onerror = (error) => {
                reject(error);
            };
        });
    };


    render() {
        return html`
        <div class="container">
            <img src="${this.src}" alt="${this.src}" width="150px">
            <input type="file" accept="image/png,image/jpeg" @change="${(e:any) => this._onChange(e)}">
        </div>
   `;
    }
}
