import { AssetDescriptor, AssetTypeInfo, Attribute, AttributeDescriptor, MetaItemDescriptor, ValueDescriptor, ValueDescriptorHolder, ValueHolder } from "./index";
export declare class AssetModelUtil {
    static _assetTypeInfos: AssetTypeInfo[];
    static _metaItemDescriptors: MetaItemDescriptor[];
    static _valueDescriptors: ValueDescriptor[];
    static getAssetDescriptors(): AssetDescriptor[];
    static getMetaItemDescriptors(): MetaItemDescriptor[];
    static getValueDescriptors(): ValueDescriptor[];
    static getAssetTypeInfos(): AssetTypeInfo[];
    static getAssetTypeInfo(type: string | AssetDescriptor | AssetTypeInfo): AssetTypeInfo | undefined;
    static getAssetDescriptor(type?: string | AssetDescriptor | AssetTypeInfo): AssetDescriptor | undefined;
    static getAttributeDescriptor(attributeName: string, assetTypeOrDescriptor: string | AssetDescriptor | AssetTypeInfo): AttributeDescriptor | undefined;
    static getValueDescriptor(name?: string): ValueDescriptor | undefined;
    static resolveValueDescriptor(valueHolder: ValueHolder<any> | undefined, descriptorOrValueType: ValueDescriptorHolder | ValueDescriptor | string | undefined): ValueDescriptor | undefined;
    static resolveValueTypeFromValue(value: any): string | undefined;
    static getAttributeAndValueDescriptors(assetType: string | undefined, attributeNameOrDescriptor: string | AttributeDescriptor | undefined, attribute?: Attribute<any>): [AttributeDescriptor | undefined, ValueDescriptor | undefined];
    static getMetaItemDescriptor(name?: string): MetaItemDescriptor | undefined;
    static getAssetDescriptorColour(typeOrDescriptor: string | AssetTypeInfo | AssetDescriptor | undefined, fallbackColor?: string): string | undefined;
    static getAssetDescriptorIcon(typeOrDescriptor: string | AssetTypeInfo | AssetDescriptor | undefined, fallbackIcon?: string): string | undefined;
}
