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

public class AllocatedSceneRange extends Range {

    private int firstScene;

    private int lastScene;

    @Override
    public final int getLowerBound() {
        return lowerBound;
    }

    @Override
    public final int getUpperBound() {
        return upperBound;
    }

    public List<Range> minus(final Range left, final Range right) {
        return null;
    }

    /**
     * Constructs {@link AllocatedSceneRange} for provisioner
     *
     * @param firstScene high address of group range
     * @param lastScene  low address of group range
     */
    public AllocatedSceneRange(final int firstScene, final int lastScene) {
        lowerBound = 0x0001;
        upperBound = 0xFFFF;
        if (firstScene < lowerBound || firstScene > upperBound)
            throw new IllegalArgumentException("firstScene value must range from 0x0000 to 0xFFFF");

        if (lastScene < lowerBound || lastScene > upperBound)
            throw new IllegalArgumentException("lastScene value must range from 0x0000 to 0xFFFF");

        /*if (firstScene > lastScene)
            throw new IllegalArgumentException("firstScene value must be lower than the lastScene value");*/

        this.firstScene = firstScene;
        this.lastScene = lastScene;
    }

    AllocatedSceneRange() {
    }

    /**
     * Returns the low address of the allocated group address
     *
     * @return low address
     */
    public int getLastScene() {
        return lastScene;
    }

    /**
     * Sets the low address of the allocated group address
     *
     * @param lastScene of the group range
     */
    public void setLastScene(final int lastScene) {
        this.lastScene = lastScene;
    }

    /**
     * Returns the high address of the allocated group range
     *
     * @return firstScene of the group range
     */
    public int getFirstScene() {
        return firstScene;
    }

    /**
     * Sets the high address of the group address
     *
     * @param firstScene of the group range
     */
    public void setFirstScene(final int firstScene) {
        this.firstScene = firstScene;
    }

    @Override
    public int range() {
        return lastScene - firstScene;
    }

    @Override
    public boolean overlaps(final Range otherRange) {
        if (otherRange instanceof AllocatedSceneRange) {
            final AllocatedSceneRange otherSceneRange = (AllocatedSceneRange) otherRange;
            return overlaps(firstScene, lastScene, otherSceneRange.getFirstScene(), otherSceneRange.getLastScene());
        }
        return false;
    }

    /**
     * Subtracts a range from a list of ranges
     *
     * @param ranges ranges to be subtracted
     * @param other  {@link AllocatedSceneRange} range
     * @return a resulting {@link AllocatedSceneRange} or null otherwise
     */
    public static List<AllocatedSceneRange> minus(final List<AllocatedSceneRange> ranges, final AllocatedSceneRange other) {
        List<AllocatedSceneRange> results = new ArrayList<>();
        for (AllocatedSceneRange range : ranges) {
            results.addAll(range.minus(other));
            results = mergeSceneRanges(results);
        }
        /*ranges.clear();
        ranges.addAll(results);*/
        return results;
    }

    /**
     * Deducts a range from another
     *
     * @param other right {@link AllocatedSceneRange}
     * @return a resulting {@link AllocatedSceneRange} or null otherwise
     */
    private List<AllocatedSceneRange> minus(final AllocatedSceneRange other) {
        final List<AllocatedSceneRange> results = new ArrayList<>();
        // Left:   |------------|                    |-----------|                 |---------|
        //                  -                              -                            -
        // Right:      |-----------------|   or                     |---|   or        |----|
        //                  =                              =                            =
        // Result: |---|                             |-----------|                 |--|
        if (other.firstScene > firstScene) {
            final AllocatedSceneRange leftSlice = new AllocatedSceneRange(firstScene, (Math.min(lastScene, other.firstScene - 1)));
            results.add(leftSlice);
        }

        // Left:                |----------|             |-----------|                     |--------|
        //                         -                          -                             -
        // Right:      |----------------|           or       |----|          or     |---|
        //                         =                          =                             =
        // Result:                      |--|                      |--|                     |--------|
        if (other.lastScene < lastScene) {
            final AllocatedSceneRange rightSlice = new AllocatedSceneRange(Math.max(other.lastScene + 1, firstScene), lastScene);
            results.add(rightSlice);
        }
        return results;
    }
}
