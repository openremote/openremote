import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import { i18next } from "@openremote/or-translate";
import { convertBase64 } from "@openremote/core/lib/util";

@customElement("or-file-uploader")
export class OrFileUploader extends LitElement {

    @property({ attribute: false })
    public src: string = "";

    @property()
    public title: string = "";

    @property({ attribute: false })
    public accept: string = "image/png,image/jpeg,image/vnd.microsoft.icon,image/svg+xml";

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

            #imageContainer:hover .pencil-container{
                display: flex;
            }

            #imageContainer .pencil-container {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 0;
                right: 0;
                background: rgba(0,0,0,.4);
                color: var(--or-app-color4);
                display: none;
                justify-content: center;
                align-items: center;
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
            this.src = await convertBase64(files[0]) as string;
            this.dispatchEvent(new CustomEvent("change", {
                detail: { value: files },
            }));
        }
        this.loading = false;
        this.requestUpdate();
    }

    render() {
        return html`
            <div class="container">
                ${this.loading ? html`
                    <or-loader .overlay="${true}"></or-loader>` : ""}
                <div class="title">${this.title}</div>
                <div id="imageContainer">

                    ${!!this.src ?
                      html`<img .src="${this.src}" alt="OR-File-Uploader">
                      <div class="pencil-container">
                          <or-icon icon="pencil-circle"></or-icon>
                      </div>
                      `
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
