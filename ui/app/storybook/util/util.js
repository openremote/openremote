/**
 * Retrieves the custom element definition for the given tag name from the customElements.json file.
 * @param tagName - The tag name of the custom element. Such as `or-asset-tree`
 * @param customElementsJson - JSON object containing the custom elements definitions.
 * @returns Object - The custom element definition.
 */
export function getCustomElements(tagName, customElementsJson) {
  if(tagName) {
    const modules = (customElementsJson.modules || []);
    const tagNameModule = modules.find(m => m.declarations?.map(d => d.tagName).includes(tagName));
    return tagNameModule.declarations?.find(d => d.tagName === tagName);
  } else {
    return [];
  }
}
