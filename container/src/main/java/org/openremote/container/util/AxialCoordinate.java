/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.container.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Axial coordinates for hex grids (q, r).
 */
public class AxialCoordinate {
    public final int q;
    public final int r;

    public AxialCoordinate(int q, int r) {
        this.q = q;
        this.r = r;
    }

    /**
     * Returns the axial-to-cartesian X component (in hex units) for a pointy-top hex grid.
     */
    public double xHex() {
        return q + r / 2d;
    }

    /**
     * Returns the axial-to-cartesian Y component (in hex units) for a pointy-top hex grid.
     */
    public double yHex() {
        return r * Math.sqrt(3d) / 2d;
    }

    /**
     * Returns all axial coordinates in a hex ring at the given radius.
     */
    public static List<AxialCoordinate> ring(int radius) {
        List<AxialCoordinate> results = new ArrayList<>(radius * 6);
        int q = radius;
        int r = 0;
        int[][] directions = new int[][] {
            {-1, 1}, {-1, 0}, {0, -1}, {1, -1}, {1, 0}, {0, 1}
        };
        for (int[] direction : directions) {
            for (int i = 0; i < radius; i++) {
                results.add(new AxialCoordinate(q, r));
                q += direction[0];
                r += direction[1];
            }
        }
        return results;
    }
}
