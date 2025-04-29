import { MapConfig } from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { when } from "lit/directives/when.js"
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";

@customElement("or-conf-map-global")
export class OrConfMapGlobal extends LitElement {

    static styles = css`
        .subheader {
            padding: 10px 0 4px;
            font-weight: bolder;
        }

        .settings-container {
            display: flex;
        }

        .settings-misc {
            display: flex;
            flex-direction: column;
            padding-left: 12px;
            width: 50%;
        }

        .server-group {
            flex-direction: column;
            width: 50%;
        }

        @media screen and (max-width: 768px) {
            .server-group, .settings-misc {
                width: 100%;
                padding: unset;
            }
            .settings-container {
                display: block;
            }
        }

        .input {
            width: 100%;
            max-width: 800px;
            padding: 10px 0;
        }

        .input or-mwc-input:not([icon]) {
            width: 80%;
        }

        .note {
            color: rgba(0, 0, 0, 0.6);
        }

        or-file-uploader {
            width: 108px;
            height: 108px;
        }

        .d-inline-flex {
            display: inline-flex;
        }
    `;

    @property()
    public config?: MapConfig = {};

    @property()
    protected filename: string;

    @property()
    protected limit = 30e+6;

    /* -------------- */

    protected render() {
        const isCustom = this.config.sources?.vector_tiles?.custom;
        return html`
            <div class="settings-container">
                <div class="server-group">
                    <div class="subheader"><or-translate value="configuration.global.mapTileJsonUrl"></or-translate></div>
                    <div style="display: flex;">
                        <span>
                            <or-translate value="configuration.global.mapTileJsonUrlDescription"></or-translate><br>
                            <or-translate style="font-style: italic;" class="note" value="configuration.global.mapTileJsonUrlNote"></or-translate>
                        </span>
                        <or-mwc-input class="input" style="width: fit-content; margin-left: auto; padding: 0"
                            .value="${isCustom}"
                            .type="${InputType.CHECKBOX}"
                            .label="${i18next.t("configuration.global.configureMap")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                this.config.sources.vector_tiles.custom = e.detail.value
                                this.notifyConfigChange(this.config)
                                this.requestUpdate();
                            }}"
                        ></or-mwc-input>
                    </div>
                    <or-mwc-input class="input"
                        .disabled="${!isCustom}"
                        .required="${isCustom}"
                        .value="${isCustom ? this.config.sources.vector_tiles.url : undefined}"
                        .type="${InputType.URL}"
                        .label="${i18next.t("configuration.global.mapTileJsonUrlPlaceholder")}"
                        placeholder="https://api.example.com/tiles/tile.json"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this.config.sources.vector_tiles.url = e.detail.value;
                            this.notifyConfigChange(this.config);
                        }}"
                    ></or-mwc-input>
                    <or-mwc-input class="input"
                        .disabled="${!isCustom}"
                        .required="${isCustom}"
                        .value="${isCustom ? this.config.glyphs : undefined}"
                        .type="${InputType.URL}"
                        .label="${i18next.t("configuration.global.mapGlyphUrlPlaceholder")}"
                        placeholder="https://api.example.com/fonts/{fontstack}/{range}.pbf"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this.config.glyphs = e.detail.value;
                            this.notifyConfigChange(this.config);
                        }}"
                    ></or-mwc-input>
                    <or-mwc-input class="input"
                        .disabled="${!isCustom}"
                        .value="${isCustom ? this.config.sprite : undefined}"
                        .type="${InputType.URL}"
                        .label="${i18next.t("configuration.global.mapSpriteUrlPlaceholder")}"
                        placeholder="https://api.example.com/maps/tileset/sprite"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this.config.sprite = e.detail.value;
                            this.notifyConfigChange(this.config);
                        }}"
                    ></or-mwc-input>
                    <or-mwc-input class="input"
                        .disabled="${!isCustom}"
                        .value="${isCustom ? this.config.sources?.vector_tiles?.tiles?.[0] : undefined}"
                        .type="${InputType.URL}"
                        .label="${i18next.t("configuration.global.mapTileServerPlaceholder")}"
                        placeholder="https://api.example.com/tileset/{z}/{x}/{y}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this.config.sources.vector_tiles.tiles = [e.detail.value];
                            this.notifyConfigChange(this.config);
                        }}"
                    ></or-mwc-input>
                </div>
                <div class="settings-misc">
                    <div class="subheader"><or-translate value="configuration.global.mapStyleJsonUrl"></or-translate></div>
                    <span>
                        <or-translate value="configuration.global.mapStyleJsonUrlDescription"></or-translate><br>
                        <or-translate style="font-style: italic;" class="note" value="configuration.global.mapStyleJsonUrlNote"></or-translate>
                    </span>
                    <or-mwc-input class="input"
                        .value="${this.config.override}"
                        .type="${InputType.URL}"
                        .label="${i18next.t("configuration.global.mapStyleJsonUrlPlaceholder")}"
                        placeholder="https://api.example.com/tileset/style.json"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this.config.override = e.detail.value || undefined
                            this.notifyConfigChange(this.config)
                        }}"
                    ></or-mwc-input>
                    <div class="custom-tile-group">
                        <div class="subheader"><or-translate value="configuration.global.mapTiles"></or-translate></div>
                        <span>
                            <or-translate value="configuration.global.uploadMapTiles"></or-translate><br>
                            <or-translate style="font-style: italic;" class="note" value="configuration.global.uploadMapTilesNote"
                                .options=${{limit: this.humanReadableBytes(this.limit)}}
                            ></or-translate>
                        </span>
                        <div class="input d-inline-flex" style="height: 56px">
                            <div id="fileupload" style="display: flex; align-items: center">
                                <or-mwc-input outlined label="selectFile" style="width: fit-content; padding-right: 12px;" .type="${InputType.BUTTON}" @or-mwc-input-changed="${
                                    () => this.shadowRoot.getElementById('fileupload-elem').click()
                                }">
                                    <input id="fileupload-elem" name="configfile" type="file" accept=".mbtiles" @change="${this.notifyMapFileChange}"/>
                                </or-mwc-input>
                                <or-mwc-input id="filename-elem" style="width: unset" .value="${this.filename}" .label="${i18next.t("file")}" .type="${InputType.TEXT}" disabled>
                                </or-mwc-input>
                                ${when(this.filename, () => html`<or-mwc-input type="${InputType.BUTTON}" iconColor="black" icon="delete"
                                    @or-mwc-input-changed="${this.notifyMapFileChange}"
                                ></or-mwc-input>`)}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `
    }

    protected notifyConfigChange(config: MapConfig) {
        this.dispatchEvent(new CustomEvent("change", { detail: config }));
    }

    protected notifyMapFileChange(e: OrInputChangedEvent) {
        const file = (e.target as HTMLInputElement)?.files?.[0];
        if (file) {
            if (file.size > this.limit) {
                showSnackbar(undefined, "configuration.global.uploadMapTilesError")
                return;
            }
            const filenameEl = this.shadowRoot.getElementById('filename-elem') as HTMLInputElement
            if (filenameEl) {
                filenameEl.value = file.name;
            }
            this.dispatchEvent(new CustomEvent("upload", { detail: file }));
        } else {
            this.dispatchEvent(new CustomEvent("delete"));
        }
    }

    private humanReadableBytes(bytes: number) {
        const unit = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'];
        const exponent = Math.floor(Math.log(bytes) / Math.log(1000));
        return (bytes / Math.pow(1000, exponent)).toFixed(2) + " " + unit[exponent];
    }
}
