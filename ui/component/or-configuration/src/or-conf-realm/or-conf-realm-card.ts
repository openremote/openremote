import { html, LitElement, css } from "lit";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { customElement } from "lit/decorators.js";


@customElement("or-conf-realm-card")
export class OrConfRealmCard extends LitElement {

  static styles = css`
    or-panel img{
      max-width: 16px;
    }
    .btn-add-realm{
      margin-top: 48px;
      width: 100%;
      text-align: center;
    }
    .content{
      padding: 10px;
    }
    or-mwc-input{
      width: 100%;
      margin: 10px 0;
    }
    .row{
      display: flex;
      -ms-flex-wrap: wrap;
      flex-wrap: wrap;
      margin-right: -15px;
      margin-left: -15px;
    }
    .col{
      -ms-flex: 0 0 50%;
      flex: 0 0 50%;
      max-width: 50%;
    }
    .col or-mwc-input{
      margin: 10px;
    }
    
    or-collapsible-panel{
      border-radius: 4px;
    }

    .panels>.panels:not(:last-child)>or-collapsible-panel, .panels>or-collapsible-panel:not(:last-child) {
      border-bottom-right-radius: 0;
      border-bottom-left-radius: 0;
    }

    .panels>.panels:not(:first-child)>or-collapisble-panel, .panels>or-collapsible-panel:not(:first-child) {
      border-top-left-radius: 0;
      border-top-right-radius: 0;
    }
  `;

  render() {
    return html`
      <or-panel>
        <img src="data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjxzdmcgdmlld0JveD0iMCAwIDEwNy4zNzIgMTA3LjQ1MyIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4NCiAgPGc+DQogICAgPHBhdGggZmlsbD0iI0M0RDYwMCIgZD0iTTUzLjY0OCwxMDcuMjk2QzI0LjA2OCwxMDcuMjk2LDAsODMuMjM2LDAsNTMuNjQ2aDExLjIzNGMwLDIzLjM5MSwxOS4wMjUsNDIuNDIsNDIuNDE0LDQyLjQyIGMyMy4zODUsMCw0Mi40MTYtMTkuMDI5LDQyLjQxNi00Mi40MmMwLTIzLjM4Mi0xOS4wMzEtNDIuNDA4LTQyLjQxNi00Mi40MDhWMGMyOS41ODIsMCw1My42NSwyNC4wNjgsNTMuNjUsNTMuNjQ2IEMxMDcuMjk5LDgzLjIzNiw4My4yMywxMDcuMjk2LDUzLjY0OCwxMDcuMjk2TDUzLjY0OCwxMDcuMjk2eiIvPg0KICAgIDxwYXRoIGZpbGw9IiM0RTlEMkQiIGQ9Ik00NS41MjUsOTIuNTdjLTEwLjM5NS0yLjE2Ni0xOS4zMjQtOC4yNjItMjUuMTQ1LTE3LjEzN2MtNS44MTQtOC44ODQtNy44MjYtMTkuNTExLTUuNjU0LTI5LjkwNiBjMi4xNzQtMTAuMzk5LDguMjU4LTE5LjMyNSwxNy4xNDEtMjUuMTQ1YzguODg5LTUuODE1LDE5LjUwNi03LjgyNSwyOS45MDYtNS42NTVjMjEuNDYzLDQuNDc5LDM1LjI4MSwyNS41ODIsMzAuODAzLDQ3LjA0MSBMODEuNTgsNTkuNDc4YzMuMjA3LTE1LjM5Ny02LjcwMy0zMC41MzktMjIuMTA1LTMzLjc1MWMtNy40NjEtMS41Ni0xNS4wNzgtMC4xMTktMjEuNDU1LDQuMDYgYy02LjM2OSw0LjE2OS0xMC43MzYsMTAuNTgtMTIuMjk5LDE4LjAzOWMtMS41NTUsNy40NTgtMC4xMTMsMTUuMDc1LDQuMDY0LDIxLjQ1M2M0LjE3LDYuMzcsMTAuNTc2LDEwLjc0NCwxOC4wNDEsMTIuMjk3IEw0NS41MjUsOTIuNTdMNDUuNTI1LDkyLjU3eiIvPg0KICAgIDxwYXRoIGZpbGw9IiMxRDU2MzIiIGQ9Ik01My42ODIsNzkuNDI4Yy0wLjQzMiwwLTAuODcxLTAuMDEyLTEuMzA5LTAuMDMyYy02Ljg2OS0wLjM0Mi0xMy4yMDUtMy4zNDQtMTcuODMtOC40MzkgYy00LjYyMS01LjEwOC02Ljk4Mi0xMS43MDUtNi42MzktMTguNTgybDExLjIxNSwwLjU1M2MtMC4xODgsMy44NzksMS4xNDEsNy42MDksMy43NSwxMC40ODhjMi42MDQsMi44NzksNi4xODYsNC41NjgsMTAuMDU5LDQuNzYxIGMzLjg2OSwwLjE3OSw3LjYwNy0xLjE0MiwxMC40OC0zLjc0OGMyLjg4Ny0yLjYwMyw0LjU3Ni02LjE3OSw0Ljc3My0xMC4wNTdjMC4zOTEtOC4wMTItNS44MDMtMTQuODU0LTEzLjgxNi0xNS4yNDhsMC41NTktMTEuMjIyIGMxNC4yMDEsMC43MSwyNS4xNzgsMTIuODIzLDI0LjQ3NSwyNy4wMjFjLTAuMzQ0LDYuODgzLTMuMzM2LDEzLjIxMi04LjQ0MSwxNy44MzFDNjYuMTc0LDc3LjA4Niw2MC4wODQsNzkuNDI4LDUzLjY4Miw3OS40MjggTDUzLjY4Miw3OS40Mjh6Ii8+DQogIDwvZz4NCjwvc3ZnPg==">
        <strong>Default</strong>
        <div class="panels">
          <or-collapsible-panel>
                            <div slot="header">
                                Name
                            </div>
                            <div slot="content" class="content">
                                <or-mwc-input .type="${InputType.TEXT}" value="Test" label="Name"></or-mwc-input>
                                <or-mwc-input .type="${InputType.TEXT}" value="Test" label="App Title"></or-mwc-input>
                                <or-mwc-input .type="${InputType.SELECT}" value="Test" label="Default language"></or-mwc-input>
                            </div>
                        </or-collapsible-panel>
          <or-collapsible-panel>
                            <div slot="header">
                                Images
                            </div>
                            <div slot="content" class="content">
                            </div>
                        </or-collapsible-panel>
          <or-collapsible-panel>
                            <div slot="header">
                                Headers
                            </div>
                            <div slot="content" class="content row">
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Map"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Assets"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Rules"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Insights"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Language"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Logs"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Account"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Users"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Roles"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Realms"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Configuration"></or-mwc-input>
                                <or-mwc-input id="name-input" .type="${InputType.CHECKBOX}" class="col" label="Logout"></or-mwc-input>
                            </div>
                        </or-collapsible-panel>
          <or-collapsible-panel>
                            <div slot="header">
                                Colors
                            </div>
                            <div slot="content" class="content row">
                                <div class="col">
                                    <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
                                </div>
                                <div class="col">
                                    <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
                                </div>
                                <div class="col">
                                    <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
                                </div>
                                <div class="col">
                                    <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
                                </div>
                                <div class="col">
                                    <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
                                </div>
                                <div class="col">
                                    <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
                                </div>
                                
                            </div>
                        </or-collapsible-panel>
        </div>
        <or-mwc-input id="name-input" .type="${InputType.BUTTON}" label="Save"></or-mwc-input>
      </or-panel>
`;
  }
}
