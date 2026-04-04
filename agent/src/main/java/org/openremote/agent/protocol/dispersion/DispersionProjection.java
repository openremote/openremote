/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.agent.protocol.dispersion;

public final class DispersionProjection {

    public record PlumeCoordinates(double downwindMeters, double crosswindMeters) {
    }

    private DispersionProjection() {
    }

    public static PlumeCoordinates projectToPlume(double eastMeters, double northMeters, double windDirectionFromDeg) {
        double downwindDirectionDeg = normaliseDegrees(windDirectionFromDeg + 180d);
        double downwindDirectionRad = Math.toRadians(downwindDirectionDeg);
        double windEast = Math.sin(downwindDirectionRad);
        double windNorth = Math.cos(downwindDirectionRad);

        double downwind = (eastMeters * windEast) + (northMeters * windNorth);
        double crosswind = (eastMeters * -windNorth) + (northMeters * windEast);

        return new PlumeCoordinates(downwind, crosswind);
    }

    public static double normaliseDegrees(double degrees) {
        double normalized = degrees % 360d;
        return normalized < 0d ? normalized + 360d : normalized;
    }
}
