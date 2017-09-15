/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.widget;

import com.google.gwt.user.client.ui.FlowPanel;
import org.openremote.model.attribute.AttributeRef;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AttributeRefEditor extends FlowPanel {

    public static class AssetInfo {
        String name;
        String id;

        public AssetInfo(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }
    }

    public static class AttributeInfo {
        String name;
        String label;

        public AttributeInfo(String name, String label) {
            this.name = name;
            this.label = label;
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }
    }

    protected final String[] currentAssetAttribute;
    protected final FormListBox assetList;
    protected final FormListBox attributeList;
    protected final String attributeWatermark;
    protected Map<AssetInfo, List<AttributeInfo>> assetAttributeMap;
    protected final Consumer<AttributeRef> onValueModified;

    public AttributeRefEditor(AttributeRef currentValue,
                              Consumer<AttributeRef> onValueModified,
                              boolean readOnly,
                              Consumer<Consumer<Map<AssetInfo, List<AttributeInfo>>>> assetAttributeSupplier,
                              String assetWatermark,
                              String attributeWatermark,
                              String styleName) {

        setStyleName("flex layout horizontal center or-ValueEditor");
        setStyleName(styleName, true);

        FlowPanel widgetWrapper = new FlowPanel();
        widgetWrapper.setStyleName("flex layout horizontal center");
        add(widgetWrapper);

        this.onValueModified = onValueModified;
        this.attributeWatermark = attributeWatermark;
        assetList = new FormListBox();
        assetList.setEnabled(!readOnly);
        attributeList = new FormListBox();
        attributeList.setEnabled(!readOnly);

        currentAssetAttribute = currentValue != null ?
            new String[]{currentValue.getEntityId(), currentValue.getAttributeName()} :
            new String[2];

        assetList.addItem(assetWatermark);
        attributeList.addItem(attributeWatermark);
        attributeList.setVisible(false);

        assetList.addChangeHandler(event -> onAssetListChanged());
        attributeList.addChangeHandler(event -> onAttributeListChanged());

        // Populate asset list
        // TODO: indicate loading state

        assetAttributeSupplier.accept(retrievedAssetsAndAttributes -> {
            assetAttributeMap = retrievedAssetsAndAttributes;
            // Update asset list box

            // Current index and selected index
            final int[] indexes = {0,0};

            retrievedAssetsAndAttributes.forEach((assetInfo, attributeNameLabel) -> {
                assetList.addItem(assetInfo.getName(), assetInfo.getId());
                if (assetInfo.getId().equals(currentAssetAttribute[0])) {
                    indexes[1] = indexes[0] + 1;
                }
                indexes[0]++;
            });

            assetList.setSelectedIndex(indexes[1]);
            onAssetListChanged();
        });

        widgetWrapper.add(assetList);
        widgetWrapper.add(attributeList);
    }

    protected void onAssetListChanged() {
        int selectedIndex = assetList.getSelectedIndex();
        attributeList.clear();
        attributeList.addItem(attributeWatermark);
        final int[] attributeSelectedIndex = {0};

        if (selectedIndex == 0) {
            attributeList.setVisible(false);
            currentAssetAttribute[0] = null;
            currentAssetAttribute[1] = null;
        } else {
            if (!assetList.getSelectedValue().equals(currentAssetAttribute[0])) {
                currentAssetAttribute[0] = assetList.getSelectedValue();
                currentAssetAttribute[1] = null;
            }

            // Populate attribute list
            List<AttributeInfo> attributeInfos = assetAttributeMap.entrySet().stream()
                .filter(assetAndAttributes -> assetAndAttributes.getKey().getId().equals(assetList.getSelectedValue()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(Collections.emptyList());

            IntStream.range(0, attributeInfos.size())
                .forEach(i -> {
                    AttributeInfo attributeInfo = attributeInfos.get(i);
                    attributeList.addItem(attributeInfo.getLabel(), attributeInfo.getName());
                    if (attributeInfo.getName().equals(currentAssetAttribute[1])) {
                        attributeSelectedIndex[0] = i + 1;
                    }
                });

            attributeList.setVisible(true);
        }
        attributeList.setSelectedIndex(attributeSelectedIndex[0]);
        onAttributeListChanged();
    }

    protected void onAttributeListChanged() {
        int selectedIndex = attributeList.getSelectedIndex();
        if (selectedIndex == 0) {
            currentAssetAttribute[1] = null;
        } else {
            currentAssetAttribute[1] = attributeList.getSelectedValue();
        }

        if (onValueModified != null) {
            onValueModified.accept(!isNullOrEmpty(currentAssetAttribute[0]) && !isNullOrEmpty(currentAssetAttribute[1]) ? new AttributeRef(currentAssetAttribute[0], currentAssetAttribute[1]) : null);
        }
    }
}
