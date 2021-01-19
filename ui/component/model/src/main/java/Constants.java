/*
 * Copyright 2021, OpenRemote Inc.
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

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.value.*;

import java.util.Arrays;
import java.util.List;

public final class Constants {
    protected Constants() {}

    public static final List<Class<?>> IGNORE_TYPE_PARAMS_ON_CLASSES = Arrays.asList(
        Asset.class,
        Agent.class,
        AssetFilter.class,
        AssetDescriptor.class,
        AgentDescriptor.class,
        AgentLink.class,
        Protocol.class,
        AttributeDescriptor.class,
        MetaItemDescriptor.class,
        ValueDescriptor.class,
        ValueDescriptorHolder.class,
        AbstractNameValueDescriptorHolder.class
    );
}
