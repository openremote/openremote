import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("or-file-uploader")
export class OrFileUploader extends LitElement {

    @property({attribute: false})
    public src: string = "";

    @property()
    public title: string = "";

    @property({attribute: false})
    public accept: string = "image/png,image/jpeg,image/vnd.microsoft.icon";

    static get styles() {
        return css`
            #imageContainer{
                position: relative;
                max-width: 150px;
                max-height: 150px;
                background-color: whitesmoke;
                border: 1px solid var(--or-app-color5);
                cursor: pointer;
                border-radius: 2px;
                padding: 4px;
            }
            #imageContainer img{
                width: 100%;
                border-radius: 2px;
            }
            input{
                display: none;
            }
            .placeholder-container{
                text-align: center;
                width: 100%;
            }
            .placeholder-container or-icon{
                font-size: 24px;
                margin: 16px 0;
                max-height: 24px;
                width: 100%;
                text-align: center;
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
            console.log("Click container")
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
            <div class="title">${this.title}</div>
            <div id="imageContainer">
                ${ !!this.src ? 
                  html`<img .src="${this.src?.startsWith('data') ? this.src : '/manager' + this.src}" alt="OR-File-Uploader">` 
                  : 
                  html`
                  <div class="placeholder-container">
                      <or-icon icon="upload"></or-icon>
                      Upload your file...
                  </div>
                  ` 
        }
            </div>
            <input type="file" id="fileInput" accept="${this.accept}">
        </div>
   `;
    }
}
