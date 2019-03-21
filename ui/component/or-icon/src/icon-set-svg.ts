import {IconSet} from "./index";

export class IconSetSvg implements IconSet {
    icons: { [icon: string]: string };
    size: number;

    constructor(size: number, icons: { [icon: string]: string }) {
        this.size = size;
        this.icons = icons;
    }

    getIcon(icon: string): Element | undefined {
        if (!icon) {
            return undefined;
        }

        if (!this.icons.hasOwnProperty(icon)) {
            return undefined;
        }

        let iconData = this.icons[icon];
        let svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("viewBox", "0 0 " + this.size + " " + this.size);
        svg.style.cssText = "pointer-events: none; display: block; width: 100%; height: 100%;";
        svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
        svg.setAttribute("focusable", "false");
        if (iconData.startsWith("<path")) {
            svg.innerHTML = iconData;
        } else {
            let path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", iconData);
            svg.appendChild(path);
        }
        return svg;
    }
}