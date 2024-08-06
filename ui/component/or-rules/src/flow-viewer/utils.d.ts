export declare class Utilities {
    static getCenter(bounds: ClientRect): {
        x: number;
        y: number;
    };
    static isPointInsideBox(x: number, y: number, box: {
        x: number;
        y: number;
        width: number;
        height: number;
    }): boolean;
    static isBoxInsideBox(a: {
        x: number;
        y: number;
        width: number;
        height: number;
    }, b: {
        x: number;
        y: number;
        width: number;
        height: number;
    }): boolean;
    static humanLike(input: string): string;
    static ellipsis(input: string, maxLength?: number, ellipsis?: string): string;
}
