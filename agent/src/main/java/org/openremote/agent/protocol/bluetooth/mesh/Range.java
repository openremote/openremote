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
package org.openremote.agent.protocol.bluetooth.mesh;

import java.util.ArrayList;
import java.util.List;

public abstract class Range {

    protected int lowerBound;

    protected int upperBound;

    /**
     * Returns the lower bound of the Range
     */
    public abstract int getLowerBound();

    /**
     * Returns the upper bound of the range
     */
    public abstract int getUpperBound();

    /**
     * Checks if two ranges overlaps
     *
     * @param otherRange other range
     * @return true if overlaps or false otherwise
     */
    public abstract boolean overlaps(final Range otherRange);

    /**
     * Returns the address range as a int
     */
    public abstract int range();

    protected boolean overlaps(final int rLow, final int rHigh, final int oLow, final int oHigh) {
        // Are the ranges are equal
        if (rLow == oLow && rHigh == oHigh) {
            return true;
        }
        // Is the range greater than the other range?
        else if (rLow < oLow && rHigh > oHigh) {
            return true;
        }
        // Is the range within the other range?
        else if (rLow > oLow && rHigh < oHigh) {
            return true;
        }
        // Is the range's lower address lower than the other range's low address
        else if (rLow <= oLow &&
            rHigh >= oLow && rHigh <= oHigh) {
            return true;
        }
        // Is the range's higher address greater than the other range's high address
        else return rHigh >= oHigh &&
                rLow >= oLow && rLow <= oHigh;
    }


    /**
     * Returns a list of merged unicast ranges
     *
     * @param ranges list of{@link AllocatedUnicastRange} to merge with
     */
    public static List<AllocatedUnicastRange> mergeUnicastRanges(final List<AllocatedUnicastRange> ranges) {
        AllocatedUnicastRange accumulator = new AllocatedUnicastRange();
        final List<AllocatedUnicastRange> result = new ArrayList<>();
        for (AllocatedUnicastRange range : ranges) {
            if (accumulator.getLowAddress() == 0 && accumulator.getHighAddress() == 0) {
                accumulator = range;
            }

            // Is the range already in accumulator's range?
            //noinspection StatementWithEmptyBody
            if (accumulator.getHighAddress() >= range.getHighAddress()) {
                // Do nothing.
            }

            // Does the range start inside the accumulator, or just after the accumulator?
            else if (accumulator.getHighAddress() + 1 >= range.getLowAddress()) {
                accumulator = new AllocatedUnicastRange(accumulator.getLowAddress(), range.getHighAddress());
            }

            // There must have been a gap, the accumulator can be appended to result array.
            else {
                result.add(accumulator);
                // Initialize the new accumulator as the new range.
                accumulator = range;
            }
        }

        // Add the last accumulator if it was set above.
        if (accumulator.getLowAddress() != 0 && accumulator.getHighAddress() != 0) {
            result.add(accumulator);
        }
        return result;
    }

    /**
     * Returns a list of merged group ranges
     *
     * @param ranges list of{@link AllocatedGroupRange} to merge with
     */
    public static List<AllocatedGroupRange> mergeGroupRanges(final List<AllocatedGroupRange> ranges) {
        AllocatedGroupRange accumulator = new AllocatedGroupRange();
        final List<AllocatedGroupRange> results = new ArrayList<>();
        for (AllocatedGroupRange range : ranges) {
            if (accumulator.getLowAddress() == 0 && accumulator.getHighAddress() == 0) {
                accumulator = range;
            }

            // Is the range already in accumulator's range?
            //noinspection StatementWithEmptyBody
            if (accumulator.getHighAddress() >= range.getHighAddress()) {
                // Do nothing.
            }

            // Does the range start inside the accumulator, or just after the accumulator?
            else if (accumulator.getHighAddress() + 1 >= range.getLowAddress()) {
                accumulator = new AllocatedGroupRange(accumulator.getLowAddress(), range.getHighAddress());
            }

            // There must have been a gap, the accumulator can be appended to result array.
            else {
                results.add(accumulator);
                // Initialize the new accumulator as the new range.
                accumulator = range;
            }
        }

        // Add the last accumulator if it was set above.
        if (accumulator.getLowAddress() != 0 && accumulator.getHighAddress() != 0) {
            results.add(accumulator);
        }
        return results;
    }

    /**
     * Returns a list of merged scene ranges
     *
     * @param ranges list of{@link AllocatedSceneRange} to merge with
     */
    public static List<AllocatedSceneRange> mergeSceneRanges(final List<AllocatedSceneRange> ranges) {
        AllocatedSceneRange accumulator = new AllocatedSceneRange();
        final List<AllocatedSceneRange> result = new ArrayList<>();
        for (AllocatedSceneRange range : ranges) {
            if (accumulator.getFirstScene() == 0 && accumulator.getLastScene() == 0) {
                accumulator = range;
            }

            // Is the range already in accumulator's range?
            //noinspection StatementWithEmptyBody
            if (accumulator.getLastScene() >= range.getLastScene()) {
                // Do nothing.
            }

            // Does the range start inside the accumulator, or just after the accumulator?
            else if (accumulator.getLastScene() + 1 >= range.getFirstScene()) {
                accumulator = new AllocatedSceneRange(accumulator.getFirstScene(), range.getLastScene());
            }

            // There must have been a gap, the accumulator can be appended to result array.
            else {
                result.add(accumulator);
                // Initialize the new accumulator as the new range.
                accumulator = range;
            }
        }

        // Add the last accumulator if it was set above.
        if (accumulator.getFirstScene() != 0 && accumulator.getLastScene() != 0) {
            result.add(accumulator);
        }
        return result;
    }
}
