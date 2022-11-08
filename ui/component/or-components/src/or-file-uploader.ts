import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("or-file-uploader")
export class OrFileUploader extends LitElement {

    @property({attribute: false})
    public src: string = "";

    public imageContent: string = "";

    static get styles() {
        return css`
            .image-container{
                position: relative;
            }
            .image-container img{
                position: absolute;
                width: 200px;
                height: 200px;
                background-color: red;
                cursor: pointer;
            }
            .image-container or-mwc-input{
                position: absolute;
                bottom: 0;
                right: 0;
            }
            input{
                //display: none;
            }
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        this.imageContent = this.src
        const fileInput = this.shadowRoot?.getElementById('fileInput')
        const imageContainer = this.shadowRoot?.getElementById('imageContainer')
        fileInput?.addEventListener("change", (e) => {
            this._onChange(e)
        })
        imageContainer?.addEventListener("click", (e) => {
            fileInput?.click()
        })
    }

    _onChange(e: any) {
        if (e.target) {
            if (!!e.target.files) {
                const file = e.target.files[0]
                if (file.type.startsWith("image/")){
                    this._convertToBase64(file)
                      .then((base64) => {
                          console.log(base64)
                          this.src = base64 as string
                          this.dispatchEvent(new CustomEvent('change', {
                              detail: {value: this.src}
                          }))
                      })
                }
            }
        }
    }

    _convertToBase64(file:any){
        return new Promise((resolve, reject) => {
            const fileReader = new FileReader();
            fileReader.readAsDataURL(file);

            fileReader.onload = () => {
                resolve(fileReader.result);
            };

            fileReader.onerror = (error) => {
                console.error(error)
                reject(error);
            };
        });
    }


    render() {
        return html`
        <div class="container">
            <div class="image-container">
                <img src="${this.src}" alt="OR-File-Uploader" id="imageContainer">
<!--                <or-mwc-input type="button" icon="pencil"></or-mwc-input>-->
            </div>
            <input type="file" id="fileInput">
        </div>
   `;
    }
}
