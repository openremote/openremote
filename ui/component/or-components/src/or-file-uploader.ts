import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import { i18next } from "@openremote/or-translate";

@customElement("or-file-uploader")
export class OrFileUploader extends LitElement {

    @property({ attribute: false })
    public src: string = "";

    @property()
    public title: string = "";

    @property({ attribute: false })
    public accept: string = "image/png,image/jpeg,image/vnd.microsoft.icon";

    private loading: boolean = false;

    static get styles() {
        return css`
            #imageContainer {
                position: relative;
                max-width: 150px;
                background-color: whitesmoke;
                border: 1px solid var(--or-app-color5, #CCC);;
                cursor: pointer;
                border-radius: 4px;
                padding: 4px;
            }

            #imageContainer img {
                max-width: 100%;
                max-height: 100%;
                border-radius: 2px;
                margin: unset;
            }

            input {
                display: none;
            }

            .placeholder-container {
                text-align: center;
                width: 100%;
            }

            .placeholder-container or-icon {
                font-size: 24px;
                margin: 16px 0;
                max-height: 24px;
                width: 100%;
                text-align: center;
            }

            #imageContainer .edit-icon {
                position: absolute;
                right: 8px;
                bottom: 8px;
                font-size: 18px;
                color: var(--or-app-color4);
            }

            .container {
                position: relative;
            }
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        const fileInput = this.shadowRoot?.getElementById('fileInput')
        const imageContainer = this.shadowRoot?.getElementById('imageContainer')
        fileInput?.addEventListener("input", (e) => {
            this._onChange(e);
        });
        imageContainer?.addEventListener("click", (e) => {
            console.log("Click container");
            fileInput?.click();
        });
    }

    async _onChange(e: any) {
        const files = e?.target?.files;
        this.loading = true;
        this.requestUpdate();
        if (files.length > 0) {
            this.src = await this.convertBase64(files[0]) as string;
            this.dispatchEvent(new CustomEvent("change", {
                detail: { value: files },
            }));
        }
        this.loading = false;
        this.requestUpdate();
    }


    convertBase64 (file:any) {
        return new Promise((resolve, reject) => {
            if (file) {
                const fileReader = new FileReader();
                fileReader.readAsDataURL(file);

                fileReader.onload = () => {
                    resolve(fileReader.result);
                };

                fileReader.onerror = (error) => {
                    reject(error);
                };
            }
        });
    };


    render() {
        return html`
            <div class="container">
                ${this.loading ? html`
                    <or-loader .overlay="${true}"></or-loader>` : ""}
                <div class="title">${this.title}</div>
                <div id="imageContainer">

                    ${!!this.src ?
                      html`<img .src="${this.src?.startsWith("data") ? this.src : "/manager" + this.src}"
                                alt="OR-File-Uploader">
                      <or-icon class="edit-icon" icon="pencil"></or-icon>`
                      :
                      html`
                          <div class="placeholder-container">
                              <or-icon icon="upload"></or-icon>
                              ${i18next.t('uploadFile')}
                          </div>
                  ` 
        }
            </div>
            <input type="file" id="fileInput" accept="${this.accept}">
        </div>
   `;
    }
}
