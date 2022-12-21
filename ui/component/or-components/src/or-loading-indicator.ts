/*
 * Copyright 2022, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("or-loading-indicator")
export class OrLoadingIndicator extends LitElement {

  @property({ attribute: true })
  public overlay: boolean = false;

  static get styles() {
    return css`
      .loader-container {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: rgba(0, 0, 0, .2);
        z-index: 1000;
      }

      .loader {
        position: absolute;
        left: 50%;
        transform: translate(-50%, -50%);
        top: 50%;
        z-index: 1010;
      }

      .loader svg {
        height: 60px;
        width: 60px;
      }

      @-webkit-keyframes rotate-right {
        from {
          -webkit-transform: rotate(0deg);
        }
        to {
          -webkit-transform: rotate(360deg);
        }
      }
      @-webkit-keyframes rotate-left {
        from {
          -webkit-transform: rotate(0deg);
        }
        to {
          -webkit-transform: rotate(-360deg);
        }
      }

      .loader #circle1 {
        -webkit-transform: translate3d(0, 0, 0);
        -webkit-transform-origin: 54.1px 53.6px;
        -webkit-animation: rotate-right 7s linear 0s infinite;
      }

      .loader #circle2 {
        -webkit-transform: translate3d(0, 0, 0);
        -webkit-transform-origin: 54.1px 53.6px;
        -webkit-animation: rotate-left 6s linear 0s infinite;
      }

      .loader #circle3 {
        -webkit-transform: translate3d(0, 0, 0);
        -webkit-transform-origin: 54.1px 53.6px;
        -webkit-animation: rotate-right 5s linear 0s infinite;
      }
    `;
  }


  render() {
    const loader = html`
      <div class="loader">
        <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 110 110"
             xml:space="preserve">
                    <path fill="#d2d2d2" id="circle1"
                          d="M53.648,107.296C24.068,107.296,0,83.236,0,53.646h11.234c0,23.391,19.025,42.42,42.414,42.42   c23.385,0,42.416-19.029,42.416-42.42c0-23.382-19.031-42.408-42.416-42.408V0c29.582,0,53.65,24.068,53.65,53.646   C107.299,83.236,83.23,107.296,53.648,107.296L53.648,107.296z" />
          <path fill="#a5a5a5" id="circle2"
                d="M45.525,92.57c-10.395-2.166-19.324-8.262-25.145-17.137c-5.814-8.884-7.826-19.511-5.654-29.906   c2.174-10.399,8.258-19.325,17.141-25.145c8.889-5.815,19.506-7.825,29.906-5.655c21.463,4.479,35.281,25.582,30.803,47.041   L81.58,59.478c3.207-15.397-6.703-30.539-22.105-33.751c-7.461-1.56-15.078-0.119-21.455,4.06   c-6.369,4.169-10.736,10.58-12.299,18.039c-1.555,7.458-0.113,15.075,4.064,21.453c4.17,6.37,10.576,10.744,18.041,12.297   L45.525,92.57L45.525,92.57z" />
          <path fill="#878787" id="circle3"
                d="M53.682,79.428c-0.432,0-0.871-0.012-1.309-0.032c-6.869-0.342-13.205-3.344-17.83-8.439   c-4.621-5.108-6.982-11.705-6.639-18.582l11.215,0.553c-0.188,3.879,1.141,7.609,3.75,10.488c2.604,2.879,6.186,4.568,10.059,4.761   c3.869,0.179,7.607-1.142,10.48-3.748c2.887-2.603,4.576-6.179,4.773-10.057c0.391-8.012-5.803-14.854-13.816-15.248l0.559-11.222   c14.201,0.71,25.178,12.823,24.475,27.021c-0.344,6.883-3.336,13.212-8.441,17.831C66.174,77.086,60.084,79.428,53.682,79.428   L53.682,79.428z" />
                  </svg>
      </div>`;


    if (this.overlay) {
      return html`
        <div class="loader-container">
          ${loader}
        </div>`;
    }
    return loader;
  }
}
