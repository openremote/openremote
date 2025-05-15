import { MapConfig } from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { when } from "lit/directives/when.js"
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import { StyleSpecification } from "maplibre-gl";

@customElement("or-conf-map-global")
export class OrConfMapGlobal extends LitElement {

    static styles = css`
        .subheader {
            padding: 10px 0 4px;
            font-weight: bolder;
        }

        .map-tile-settings {
            display: flex;
        }

        .map-tile-server-group {
            flex-direction: column;
            width: 50%;
        }

        .map-tile-upload-group {
            flex-direction: column;
            padding-left: 12px;
            width: 50%;
        }

        .map-style-settings {
            display: flex;
        }

        .map-style-server-group {
            flex-direction: column;
            width: 50%;
        }

        .map-style-layers-group {
            flex-direction: column;
            padding-left: 12px;
            width: 50%;
        }

        @media screen and (max-width: 768px) {
            .map-tile-server-group, .map-tile-upload-group,
            .map-style-server-group, .map-style-layers-group  {
                width: 100%;
                padding: unset;
            }
            .map-tile-settings, .map-style-settings {
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
            font-style: italic;
            color: rgba(0, 0, 0, 0.6);
        }

        or-file-uploader {
            width: 108px;
            height: 108px;
        }

        .d-inline-flex {
            display: inline-flex;
        }
        
        .fit-content {
            width: fit-content;
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
        const hasExternalSource = Object.keys(this.config.sources).length > 1;
        return html`
            <div class="map-tile-settings">
                <div class="map-tile-server-group">
                    <div class="subheader"><or-translate value="configuration.global.mapTileServer"></or-translate></div>
                    <span>
                        <or-translate value="configuration.global.mapTileServerDescription"></or-translate><br>
                        <or-translate class="note" value="configuration.global.mapTileServerNote"></or-translate>
                    </span>
                    <or-mwc-input class="input"
                        .value="${isCustom ? this.config.sources?.vector_tiles?.tiles?.[0] : undefined}"
                        .type="${InputType.URL}"
                        .label="${i18next.t("configuration.global.mapTileServerPlaceholder")}"
                        placeholder="https://api.example.com/tileset/{z}/{x}/{y}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this.config.sources.vector_tiles.tiles = e.detail.value ? [e.detail.value] : undefined;
                            this.config.sources.vector_tiles.custom = !!e.detail.value;
                            this.notifyConfigChange(this.config);
                        }}"
                    ></or-mwc-input>
                </div>
                <div class="map-tile-upload-group">
                    <div class="subheader"><or-translate value="configuration.global.mapTiles"></or-translate></div>
                    <span>
                        <or-translate value="configuration.global.uploadMapTiles"></or-translate><br>
                        <or-translate class="note" value="configuration.global.uploadMapTilesNote"
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
            <div class="map-style-settings">
                <div class="map-style-server-group">
                    <span>
                        <div class="subheader"><or-translate value="configuration.global.mapStyleJsonUrl"></or-translate></div>
                        <or-translate value="configuration.global.mapStyleJsonUrlDescription"></or-translate><br>
                        <or-translate class="note" value="configuration.global.mapStyleJsonUrlNote"></or-translate>
                    </span>
                    <div style="display: flex; height: 56px; align-items: center">
                        <or-mwc-input class="input"
                            .value="${this.config.override}"
                            .type="${InputType.URL}"
                            .label="${i18next.t("configuration.global.mapStyleJsonUrlPlaceholder")}"
                            placeholder="https://api.example.com/tileset/style.json"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                this.config.override = e.detail.value || undefined
                                this.notifyConfigChange(this.config)
                                this.requestUpdate();
                            }}"
                        ></or-mwc-input>
                    </div>
                </div>
                <div class="map-style-layers-group">
                    <span>
                        <div class="subheader"><or-translate value="configuration.global.mapLayers"></or-translate></div>
                        <or-translate value="configuration.global.mapImportStyle"></or-translate>
                        <or-translate class="note" value="configuration.global.mapImportStyleNote"></or-translate>
                    </span>
                    <div style="display: flex; gap: 12px; align-items: center">
                        <or-mwc-input class="input fit-content" type="button" outlined icon="import" 
                            .label="${i18next.t("configuration.global.import")}"
                            .disabled="${!this.config.override}" @or-mwc-input-changed="${this.importMapSettings}"
                        ></or-mwc-input>
                        <or-conf-json class="input fit-content hide-mobile" .heading="${i18next.t("configuration.global.mapLayers")}"
                            .config="${hasExternalSource ? this.config.layers.filter(({ id }) => !id.startsWith("or:")) : this.config.layers}"
                            @saveLocalConfig="${(ev: CustomEvent) => {
                                if (Array.isArray(ev.detail.value)) {
                                    if (hasExternalSource) {
                                        // Set default layers
                                        this.config.layers =  this.config.layers.filter(({ id }) => id.startsWith("or:"))
                                        // Push changed layers
                                        this.config.layers.push(...ev.detail.value);
                                    } else {
                                        this.config.layers = ev.detail.value;
                                    }
                                    this.notifyConfigChange(this.config);
                                    this.requestUpdate();
                                }
                        }}"
                        ></or-conf-json>
                        ${when(hasExternalSource, () => html`
                            <or-mwc-input class="input fit-content" outlined type="button" icon="undo"
                                .label="${i18next.t("configuration.global.reset")}"
                                @or-mwc-input-changed="${this.resetMapSettings}"
                            ></or-mwc-input>
                        `)}
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
        }
        this.dispatchEvent(new CustomEvent("map-file-changed", { detail: file }));
    }

    protected resetMapSettings() {
        this.config.glyphs = undefined;
        this.config.sprite = undefined;
        this.config.sources = { vector_tiles: this.config.sources.vector_tiles };
        this.config.layers = this.config.layers
            .filter(({ id }) => id.startsWith("or:"))
            .map(({ id, ...layer }) => ({ ...layer, id: id.replace("or:", "") }));
        this.notifyConfigChange(this.config);
        this.requestUpdate();
    }

    protected async importMapSettings() {
      if (this.config.override) {
          const data = await (await fetch(this.config.override)).json() as StyleSpecification
          if (Object.keys(this.config.sources).length > 1) {
              // Set default layers
              this.config.layers =  this.config.layers.filter(({ id }) => id.startsWith("or:"))
              // Push new layers
              this.config.layers.push(...data.layers);
          } else {
              this.config.layers = this.config.layers.map(({ id, ...layer }) => ({ ...layer, id: "or:" + id }));
              this.config.layers.push(...data.layers);
          }
          Object.assign(this.config.sources, data.sources)
          this.config.sprite = data.sprite as string;
          this.config.glyphs = data.glyphs;
          delete this.config.override;
          this.notifyConfigChange(this.config);
          this.requestUpdate();
      }
    }

    private humanReadableBytes(bytes: number) {
        const unit = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'];
        const exponent = Math.floor(Math.log(bytes) / Math.log(1000));
        return (bytes / Math.pow(1000, exponent)).toFixed(2) + " " + unit[exponent];
    }
}
