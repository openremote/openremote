import customElements from "../../../component/docs/custom-elements.json";

export function getComponentDocs(tagName?: string, customElementsJson = customElements): string[] {
    if(tagName) {
        const modules = (customElementsJson.modules as any[]);
        const tagNameModule = modules.find(m => m.declarations.map(d => d.tagName).includes(tagName));
        return tagNameModule.declarations.find(d => d.tagName === tagName);
    } else {
        return [];
    }
}

export function getPackageTotalSizeKb(statsJson: any): string {
    return ((statsJson.assets?.find(x => x.name === "index.js")?.size || 0) / 1000).toFixed(2);
}

export function switchInstallationTab(installer: string) {
    const tabs = document.getElementsByClassName("tab-item") as HTMLCollectionOf<HTMLElement>;
    for (let i = 0; i < tabs.length; i++) {
        tabs[i].style.display = "none";
    }
    document.getElementById("installation-" + installer).style.display = "block";
}
