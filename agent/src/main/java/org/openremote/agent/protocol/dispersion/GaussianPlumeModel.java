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

public final class GaussianPlumeModel {

    private GaussianPlumeModel() {
    }

    public static double concentration(double emissionRate,
                                       double windSpeedMs,
                                       double downwindMeters,
                                       double crosswindMeters,
                                       double receptorHeightMeters,
                                       double sourceHeightMeters,
                                       DispersionStabilityClass stabilityClass) {

        if (!Double.isFinite(emissionRate)
            || !Double.isFinite(windSpeedMs)
            || !Double.isFinite(downwindMeters)
            || !Double.isFinite(crosswindMeters)
            || !Double.isFinite(receptorHeightMeters)
            || !Double.isFinite(sourceHeightMeters)
            || emissionRate <= 0d
            || windSpeedMs <= 0d
            || downwindMeters <= 0d) {
            return 0d;
        }

        double xMeters = Math.max(1d, downwindMeters);
        double sigmaY = sigmaY(stabilityClass, xMeters);
        double sigmaZ = sigmaZ(stabilityClass, xMeters);

        if (sigmaY <= 0d || sigmaZ <= 0d) {
            return 0d;
        }

        double denominator = 2d * Math.PI * windSpeedMs * sigmaY * sigmaZ;
        double lateral = Math.exp(-(crosswindMeters * crosswindMeters) / (2d * sigmaY * sigmaY));
        double verticalOne = Math.exp(-Math.pow(receptorHeightMeters - sourceHeightMeters, 2d) / (2d * sigmaZ * sigmaZ));
        double verticalTwo = Math.exp(-Math.pow(receptorHeightMeters + sourceHeightMeters, 2d) / (2d * sigmaZ * sigmaZ));

        double concentration = (emissionRate / denominator) * lateral * (verticalOne + verticalTwo);

        if (!Double.isFinite(concentration) || concentration < 0d) {
            return 0d;
        }

        return concentration;
    }

    public static double sigmaY(DispersionStabilityClass stabilityClass, double x) {
        return switch (stabilityClass) {
            case A -> 0.22d * x * Math.pow(1d + 0.0001d * x, -0.5d);
            case B -> 0.16d * x * Math.pow(1d + 0.0001d * x, -0.5d);
            case C -> 0.11d * x * Math.pow(1d + 0.0001d * x, -0.5d);
            case D -> 0.08d * x * Math.pow(1d + 0.0001d * x, -0.5d);
            case E -> 0.06d * x * Math.pow(1d + 0.0001d * x, -0.5d);
            case F -> 0.04d * x * Math.pow(1d + 0.0001d * x, -0.5d);
        };
    }

    public static double sigmaZ(DispersionStabilityClass stabilityClass, double x) {
        return switch (stabilityClass) {
            case A -> 0.20d * x;
            case B -> 0.12d * x;
            case C -> 0.08d * x * Math.pow(1d + 0.0002d * x, -0.5d);
            case D -> 0.06d * x * Math.pow(1d + 0.0015d * x, -0.5d);
            case E -> 0.03d * x * Math.pow(1d + 0.0003d * x, -1d);
            case F -> 0.016d * x * Math.pow(1d + 0.0003d * x, -1d);
        };
    }
}
