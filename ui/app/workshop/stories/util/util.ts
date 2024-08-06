export function getMarkdownString(file: any): string {
    return file.toString();
}

export function splitLines(value: string, amount = 1) {
    const strArray = value.split("\n");
    strArray.splice(0, amount);
    return strArray.join("\n");
}
