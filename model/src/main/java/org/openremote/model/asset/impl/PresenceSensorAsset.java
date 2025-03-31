/*
 * Copyright 2020, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.asset.impl;

import java.util.Optional;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;

@Entity
public class PresenceSensorAsset extends Asset<PresenceSensorAsset> {

  public static final AttributeDescriptor<Boolean> PRESENCE =
      new AttributeDescriptor<>(
          "presence", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

  public static final AssetDescriptor<PresenceSensorAsset> DESCRIPTOR =
      new AssetDescriptor<>("eye-circle", "5a20cc", PresenceSensorAsset.class);

  /** For use by hydrators (i.e. JPA/Jackson) */
  protected PresenceSensorAsset() {}

  public PresenceSensorAsset(String name) {
    super(name);
  }

  public Optional<Boolean> getPresence() {
    return getAttributes().getValue(PRESENCE);
  }
}
