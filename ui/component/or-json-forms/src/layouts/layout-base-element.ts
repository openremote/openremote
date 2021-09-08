import {Layout, LayoutProps, mapStateToJsonFormsRendererProps, OwnPropsOfLayout, OwnPropsOfRenderer,
    Resolve, UISchemaElement} from "@jsonforms/core";
import {property} from "lit/decorators.js";
import {BaseElement} from "../base-element";

export abstract class LayoutBaseElement<T extends Layout> extends BaseElement<T, LayoutProps> implements OwnPropsOfLayout {

    @property({type: String})
    public direction: "row" | "column" = "column";

    protected getChildProps(): OwnPropsOfRenderer[]  {
        return (this.uischema && this.uischema.elements ? this.uischema.elements : []).map(
            (el: UISchemaElement) => {
                const props: OwnPropsOfRenderer = {
                    renderers: this.renderers,
                    uischema: el,
                    schema: this.schema,
                    path: this.path
                }
                return props;
            }
        );
    }
}
