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
package org.openremote.agent.protocol.bluetooth.mesh.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Logger;

public enum OutputOOBAction {

    /**
     * Output OOB Actions
     */
    NO_OUTPUT((short) 0x0000),
    BLINK((short) 0x0001),
    BEEP((short) 0x0002),
    VIBRATE((short) 0x0004),
    OUTPUT_NUMERIC((short) 0x0008),
    OUTPUT_ALPHA_NUMERIC((short) 0x0010);

    private static final Logger LOG = java.util.logging.Logger.getLogger(OutputOOBAction.class.getName());
    private short outputOOBAction;

    OutputOOBAction(final short outputOOBAction) {
        this.outputOOBAction = outputOOBAction;
    }

    public short getOutputOOBAction() {
        return outputOOBAction;
    }

    /**
     * Returns the oob method used for authentication
     *
     * @param method auth method used
     */
    public static OutputOOBAction fromValue(final short method) {
        switch (method) {
            default:
            case 0x0000:
                return NO_OUTPUT;
            case 0x0001:
                return BLINK;
            case 0x0002:
                return VIBRATE;
            case 0x0004:
                return VIBRATE;
            case 0x0008:
                return OUTPUT_NUMERIC;
            case 0x0010:
                return OUTPUT_ALPHA_NUMERIC;
        }
    }

    /**
     * Returns the Output OOB Action description
     *
     * @param type Output OOB type
     * @return Input OOB type descrption
     */
    public static String getOutputOOBActionDescription(final OutputOOBAction type) {
        switch (type) {
            case NO_OUTPUT:
                return "Not Supported";
            case BLINK:
                return "Blink";
            case BEEP:
                return "Beep";
            case VIBRATE:
                return "Vibrate";
            case OUTPUT_NUMERIC:
                return "Output Numeric";
            case OUTPUT_ALPHA_NUMERIC:
                return "Output Alpha Numeric";
            default:
                return "Unknown";
        }
    }

    /**
     * Parses the output oob action value
     *
     * @param outputAction type of output action
     * @return selected output action type
     */
    public static ArrayList<OutputOOBAction> parseOutputActionsFromBitMask(final int outputAction) {
        final OutputOOBAction[] outputActions = {BLINK, BEEP, VIBRATE, OUTPUT_NUMERIC, OUTPUT_ALPHA_NUMERIC};
        final ArrayList<OutputOOBAction> supportedActionValues = new ArrayList<>();
        for (OutputOOBAction action : outputActions) {
            if ((outputAction & action.outputOOBAction) == action.outputOOBAction) {
                supportedActionValues.add(action);
                LOG.info("Supported output oob action type: " + getOutputOOBActionDescription(action));
            }
        }
        return supportedActionValues;
    }

    /**
     * Returns the Output OOB Action value
     *
     * @param type output OOB type
     */
    public static int getOutputOOBActionValue(final OutputOOBAction type) {
        switch (type) {
            case BLINK:
                return 0;
            case BEEP:
                return 1;
            case VIBRATE:
                return 2;
            case OUTPUT_NUMERIC:
                return 3;
            case OUTPUT_ALPHA_NUMERIC:
                return 4;
            case NO_OUTPUT:
            default:
                return 0;
        }
    }

    /**
     * Returns the Output OOB Action value
     *
     * @param actionType output OOB action type
     */
    public static int getOutputOOBActionValue(final short actionType) {
        switch (fromValue(actionType)) {
            case BLINK:
                return 0;
            case BEEP:
                return 1;
            case VIBRATE:
                return 2;
            case OUTPUT_NUMERIC:
                return 3;
            case OUTPUT_ALPHA_NUMERIC:
                return 4;
            case NO_OUTPUT:
            default:
                return 0;
        }
    }

    /**
     * Creates 128-bit authentication value
     *
     * @param outputActionType selected {@link OutputOOBAction}
     * @param input            input
     */
    public static byte[] generateOutputOOBAuthenticationValue(final OutputOOBAction outputActionType, final String input) {
        final int authLength = 16;
        final ByteBuffer buffer = ByteBuffer.allocate(authLength).order(ByteOrder.BIG_ENDIAN);
        switch (outputActionType) {
            case BLINK:
            case BEEP:
            case VIBRATE:
            case OUTPUT_NUMERIC:
                buffer.position(8);
                final long intValue = Long.valueOf(input);
                buffer.putLong(intValue);
                return buffer.array();
            case OUTPUT_ALPHA_NUMERIC:
                buffer.put(input.toUpperCase(Locale.US).getBytes());
                return buffer.array();
            default:
                return null;
        }
    }
}