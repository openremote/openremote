import { svg as html /** Aliased for syntax highlighting */, LitElement, TemplateResult, SVGTemplateResult } from "lit";
import { customElement } from "lit/decorators.js";

/**
 * Slice of the donut chart for cluster markers
 */
export type Slice = [name: string, color: string, count: number]

type SliceWithOffset = [...Slice, offset: number]

@customElement("or-cluster-marker")
export class OrClusterMarker extends LitElement {

    protected _slices: Slice[] = [];

    protected _highThreshold = 99;
    protected _midTreshold = this._highThreshold / 10;
    protected _lowTreshold = this._midTreshold / 10;

    constructor(slices: Slice[]) {
        super();
        this._slices = slices;
    }

    protected render() {
        return html`${this._createDonutChart(this._slices)}`;
    }

    /**
     * Based on https://github.com/maplibre/maplibre-gl-js/blob/3aa2de2e3a365c07ac2792847c242a7c36e625ea/test/examples/display-html-clusters-with-custom-properties.html#L147
     */
    protected _createDonutChart(slices: Slice[]): TemplateResult {
        let total = 0;
        for (const slice of slices) {
            slice.push(total);
            total += slice[2];
        }

        const fontSize = total >= this._highThreshold ? 18 : total >= this._midTreshold ? 16 : total >= this._lowTreshold ? 14 : 12;
        const r = total >= this._highThreshold ? 32 : total >= this._midTreshold ? 26 : total >= this._lowTreshold ? 16 : 12;
        const r2 = total >= this._highThreshold ? 28 : total >= this._midTreshold ? 24 : total >= this._lowTreshold ? 16 : 8;
        const dR = r - r2;
        const innerRadius: number = Math.round(r * 0.6);
        const w: number = r * 2;

        const isClusterCountlimitSurpassed = total > this._highThreshold

        return html`
            <svg 
                width="${w}"
                height="${w}"
                viewbox="0 0 ${w} ${w}"
                text-anchor="middle"
                style="font: ${fontSize}px Helvetica Neue,Arial,Helvetica,sans-serif; display: block">
                    ${(slices as [] as SliceWithOffset[]).map(([,color,count,offset]) => this._donutSegment(
                        offset / total,
                        (offset + count) / total,
                        r2,
                        innerRadius,
                        color, dR
                    ))}
                    <circle cx="${r}" cy="${r}" r="${innerRadius}" fill="white" />
                    <text dominant-baseline="central" transform="translate(${r}, ${r})">
                        ${(isClusterCountlimitSurpassed ? this._highThreshold : total).toLocaleString() + (isClusterCountlimitSurpassed ? "+" : "")}
                    </text>
            </svg>
        `;
    }

    /**
     * Based on https://github.com/maplibre/maplibre-gl-js/blob/3aa2de2e3a365c07ac2792847c242a7c36e625ea/test/examples/display-html-clusters-with-custom-properties.html#L209
     */
    protected _donutSegment(start: number, end: number, r: number, r0: number, color: string, dR: number): SVGTemplateResult {
        if (end - start === 1) {
            end -= 0.00001;
        }
        const a0: number = 2 * Math.PI * (start - 0.25);
        const a1: number = 2 * Math.PI * (end - 0.25);
        const x0: number = Math.cos(a0);
        const y0: number = Math.sin(a0);
        const x1: number = Math.cos(a1) 
        const y1: number = Math.sin(a1);
        const largeArc: 1 | 0 = end - start > 0.5 ? 1 : 0;

        return html`<path fill="#${color}" transform="translate(${dR}, ${dR})" d="
            M ${r + r0 * x0} ${r + r0 * y0}
            L ${r + r * x0} ${r + r * y0}
            A ${r} ${r} 0 ${largeArc} 1 ${r + r * x1} ${r + r * y1}
            L ${r + r0 * x1} ${r + r0 * y1}
            A ${r0} ${r0} 0 ${largeArc} 0 ${r + r0 * x0} ${r + r0 * y0}
            "
        />`;
    }
}
