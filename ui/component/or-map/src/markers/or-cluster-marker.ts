import { html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";

type Slice = [name: string, color: string, count: number]

@customElement("or-cluster-marker")
export class OrClusterMarker extends LitElement {

    protected slices: Slice[] = [];

    constructor(slices: Slice[]) {
        super();
        this.slices = slices;
    }

    protected render() {
        return html`${this.createDonutChart(this.slices)}`;
    }

    /**
     * Methods from maplibre example for Cluster with Donut Chart
     */
    protected createDonutChart(slices: Slice[]): HTMLElement | undefined {
        let total = 0;
        for (const slice of slices) {
            slice.push(total);
            total += slice[2];
        }

        const highTreshold = 100;
        const midTreshold: number = highTreshold / 10;
        const lowTreshold: number = midTreshold / 10;

        const fontSize = total >= highTreshold ? 18 : total >= midTreshold ? 16 : total >= lowTreshold ? 14 : 12;
        const r = total >= highTreshold ? 32 : total >= midTreshold ? 26 : total >= lowTreshold ? 16 : 12;
        const r2 = total >= highTreshold ? 28 : total >= midTreshold ? 24 : total >= lowTreshold ? 16 : 8;
        const dR = r - r2;
        const r0: number = Math.round(r * 0.6);
        const w: number = r * 2;

        let html =
            `<div><svg width="${
                w
            }" height="${
                w
            }" viewbox="0 0 ${
                w
            } ${
                w
            }" text-anchor="middle" style="font: ${
                fontSize
            }px Helvetica Neue,Arial,Helvetica,sans-serif; display: block">`;

        for (const [,color,count,offset] of slices as [] as [name: string, color: string, count: number, offset: number][]) {
            html += this.donutSegment(
                offset / total,
                (offset + count) / total,
                r2,
                r0,
                color, false, dR
            );
        }

        html +=
            `<circle cx="${
                r
            }" cy="${
                r
            }" r="${
                r0
            }" fill="white" /><text dominant-baseline="central" transform="translate(${
                r
            }, ${
                r
            })">${
                (total > 99 ? 99 : total).toLocaleString() + (total > 99 ? "+" : "")
            }</text></svg></div>`;

        const el = document.createElement('div');
        el.innerHTML = html;
        return el.firstChild as HTMLElement;
    }

    /**
     * Methods from maplibre example for Cluster with Donut Chart
     */
    protected donutSegment(start: number, end: number, r: number, r0: number, color: string, sub: boolean, dR: number): string {
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

        return [
            '<path d="M',
            r + r0 * x0,
            r + r0 * y0,
            'L',
            r + r * x0,
            r + r * y0,
            'A',
            r,
            r,
            0,
            largeArc,
            1,
            r + r * x1,
            r + r * y1,
            'L',
            r + r0 * x1,
            r + r0 * y1,
            'A',
            r0,
            r0,
            0,
            largeArc,
            0,
            r + r0 * x0,
            r + r0 * y0,
            `" fill="#${color}" ${!sub ? `transform="translate(${dR}, ${dR})"` : ""} />`
        ].join(' ');
    }

}
