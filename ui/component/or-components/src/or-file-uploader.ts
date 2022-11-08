import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("or-file-uploader")
export class OrFileUploader extends LitElement {

    @property({attribute: false})
    public src: string = "";

    static get styles() {
        return css`
            .image-container{
                position: relative;
                width: 200px;
                height: 200px;
                background-color: red;
                cursor: pointer;
                border-radius: 2px;
            }
            .image-container img{
                max-height: 100%;
                max-width: 100%;
                position: absolute;
            }
            input{
                display: none;
            }
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        const fileInput = this.shadowRoot?.getElementById('fileInput')
        const imageContainer = this.shadowRoot?.getElementById('imageContainer')
        fileInput?.addEventListener("change", (e) => {
            this._onChange(e)
        })
        imageContainer?.addEventListener("click", (e) => {
            fileInput?.click()
        })
    }

    async _onChange(e: any) {
        const files = e?.target?.files
        if (files) {
            this.src = await this.convertBase64(files[0]) as string;
            this.requestUpdate()
            this.dispatchEvent(new CustomEvent('change', {
                detail: { value: files}
            }))
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
            <div class="image-container">
                <img .src="${this.src.startsWith('data') ? this.src : '/manager' + this.src}" alt="OR-File-Uploader" id="imageContainer">
            </div>
            <input type="file" id="fileInput">
        </div>
        ${this.src}
   `;
    }
}
