import { Layout, LayoutProps, OwnPropsOfLayout, OwnPropsOfRenderer } from "@jsonforms/core";
import { BaseElement } from "../base-element";
export declare abstract class LayoutBaseElement<T extends Layout> extends BaseElement<T, LayoutProps> implements OwnPropsOfLayout {
    direction: "row" | "column";
    protected getChildProps(): OwnPropsOfRenderer[];
}
