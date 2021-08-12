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
package org.openremote.agent.protocol.velbus.device;

import org.openremote.model.util.EnumUtil;

import java.util.Optional;

/**
 * Abstract processor for channel related operations
 */
public abstract class OutputChannelProcessor extends ChannelProcessor {

    public enum ChannelSetting {
            NORMAL(0x00),
            INHIBITED(0x01),
            FORCED(0x02),
            LOCKED(0x03, 0x04);

            private final int code;
            private final int code2;

            ChannelSetting(int code) {
                this(code, 0);
            }

            ChannelSetting(int code, int code2) {
                this.code = code;
                this.code2 = code2;
            }

            public int getCode() {
                return this.code;
            }

            public static ChannelSetting fromCode(int code) {
                for (ChannelSetting type : ChannelSetting.values()) {
                    if (type.getCode() == code || type.code2 == code) {
                        return type;
                    }
                }

                return NORMAL;
            }

            public static Optional<ChannelSetting> fromValue(Object value) {
                if (value == null) {
                    return Optional.empty();
                }

                return EnumUtil.enumFromValue(ChannelSetting.class, value);
            }
    }

    protected OutputChannelProcessor() {}


}
