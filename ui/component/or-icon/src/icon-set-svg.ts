import {IconSet} from "./index";

export class IconSetSvg implements IconSet {
    public icons: { [icon: string]: string };
    public size: number;

    constructor(size: number, icons: { [icon: string]: string }) {
        this.size = size;
        this.icons = icons;
    }

    public getIcon(icon: string): Element | undefined {
        if (!icon) {
            return undefined;
        }

        if (!this.icons.hasOwnProperty(icon)) {
            return undefined;
        }

        const iconData = this.icons[icon];
        const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("viewBox", "0 0 " + this.size + " " + this.size);
        svg.style.cssText = "pointer-events: none; display: block; width: 100%; height: 100%;";
        svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
        svg.setAttribute("focusable", "false");
        if (iconData.startsWith("<")) {
            svg.innerHTML = iconData;
        } else {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", iconData);
            svg.appendChild(path);
        }
        return svg;
    }
}