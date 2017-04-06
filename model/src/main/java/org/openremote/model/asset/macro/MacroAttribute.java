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
package org.openremote.model.asset.macro;

import elemental.json.JsonObject;
import org.openremote.model.Meta;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AbstractAssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.util.JsonUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

public class MacroAttribute extends ProtocolConfiguration {
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":macro";

    public MacroAttribute(String name) {
        super(name, PROTOCOL_NAME);
    }

    public MacroAttribute(String assetId, String name) {
        super(assetId, name, PROTOCOL_NAME);
    }

    public MacroAttribute(String assetId, String name, JsonObject jsonObject) {
        super(assetId, name, jsonObject);
    }

    public MacroAttribute(ProtocolConfiguration protocolConfiguration) {
        super(protocolConfiguration);
    }

    public List<MacroAction> getActions() {
        return Arrays.stream(getMeta().all())
                .filter(metaItem -> metaItem.getName().equals(AssetMeta.MACRO_ACTION.getName()))
                .map(metaItem -> new MacroAction(metaItem.getValueAsObject()))
                .collect(Collectors.toList());
    }

    public void setActions(List<MacroAction> actions) {
        Meta meta = getMeta() != null ? getMeta() : new Meta();
        meta = meta.removeAll(AssetMeta.MACRO_ACTION);
        Meta finalMeta = meta;
        actions.forEach(action -> finalMeta.add(action.asMetaItem()));
    }

    public void addAction(MacroAction action) {
        Meta meta = getMeta() != null ? getMeta() : new Meta();
        meta.add(action.asMetaItem());
        setMeta(meta);
    }

    public void removeAction(MacroAction action) {
        if (!hasMeta()) {
            return;
        }

        MetaItem[] metaItems = getMeta().all();
        IntStream.range(0, metaItems.length)
                .filter(i -> metaItems[i].getName().equals(AssetMeta.MACRO_ACTION.getName()))
                .filter(i -> JsonUtil.equals(metaItems[i].getValueAsObject(), action.asJsonValue()))
                .forEach(i -> getMeta().remove(i));
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }
}
