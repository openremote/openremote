import { TemplateResult } from "lit";
import "@openremote/or-asset-tree";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-mwc-components/or-mwc-list";
import { OrAssetTreeSelectionEvent } from "@openremote/or-asset-tree";
import { OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import { Asset, Attribute, AttributeRef } from "@openremote/model";
import { DialogAction, DialogActionBase, OrMwcDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
export declare class OrAttributePickerPickedEvent extends CustomEvent<AttributeRef[]> {
    static readonly NAME = "or-attribute-picker-picked";
    constructor(detail: AttributeRef[]);
}
declare global {
    export interface HTMLElementEventMap {
        [OrAttributePickerPickedEvent.NAME]: OrAttributePickerPickedEvent;
    }
}
export declare class OrAttributePicker extends OrMwcDialog {
    showOnlyDatapointAttrs?: boolean;
    showOnlyRuleStateAttrs?: boolean;
    attributeFilter?: (attribute: Attribute<any>) => boolean;
    multiSelect?: boolean;
    selectedAttributes: AttributeRef[];
    selectedAssets: string[];
    protected assetAttributes?: (Attribute<any>)[];
    protected asset?: Asset;
    protected addBtn: OrMwcInput;
    constructor();
    setShowOnlyDatapointAttrs(showOnlyDatapointAttrs: boolean | undefined): OrAttributePicker;
    setShowOnlyRuleStateAttrs(showOnlyRuleStateAttrs: boolean | undefined): OrAttributePicker;
    setAttributeFilter(attributeFilter: ((attribute: Attribute<any>) => boolean) | undefined): OrAttributePicker;
    setMultiSelect(multiSelect: boolean | undefined): OrAttributePicker;
    setSelectedAttributes(selectedAttributes: AttributeRef[]): OrAttributePicker;
    setSelectedAssets(selectedAssets: string[]): OrAttributePicker;
    setOpen(isOpen: boolean): OrAttributePicker;
    setHeading(heading: TemplateResult | string | undefined): OrAttributePicker;
    setContent(content: TemplateResult | (() => TemplateResult) | undefined): OrAttributePicker;
    setActions(actions: DialogAction[] | undefined): OrAttributePicker;
    setDismissAction(action: DialogActionBase | null | undefined): OrAttributePicker;
    setStyles(styles: string | TemplateResult | undefined): OrAttributePicker;
    setAvatar(avatar: boolean | undefined): OrAttributePicker;
    protected setDialogActions(): void;
    protected setDialogContent(): void;
    protected _onAssetSelectionChanged(event: OrAssetTreeSelectionEvent): Promise<void>;
    protected _onAttributeSelectionChanged(attributeRefs: AttributeRef[]): void;
}
