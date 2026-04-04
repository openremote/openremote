package org.openremote.agent.protocol.dispersion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DispersionProjectionTest {

    @Test
    public void shouldNormaliseDegrees() {
        assertEquals(270d, DispersionProjection.normaliseDegrees(-90d), 0.0d);
        assertEquals(10d, DispersionProjection.normaliseDegrees(370d), 0.0d);
        assertEquals(0d, DispersionProjection.normaliseDegrees(720d), 0.0d);
    }

    @Test
    public void shouldProjectEastPointAsDownwindForWestWind() {
        DispersionProjection.PlumeCoordinates coordinates = DispersionProjection.projectToPlume(100d, 0d, 270d);

        assertEquals(100d, coordinates.downwindMeters(), 1e-9);
        assertEquals(0d, coordinates.crosswindMeters(), 1e-9);
    }

    @Test
    public void shouldProjectNorthPointAsCrosswindForWestWind() {
        DispersionProjection.PlumeCoordinates coordinates = DispersionProjection.projectToPlume(0d, 100d, 270d);

        assertEquals(0d, coordinates.downwindMeters(), 1e-9);
        assertEquals(100d, coordinates.crosswindMeters(), 1e-9);
    }

    @Test
    public void shouldProjectWestPointAsUpwindForWestWind() {
        DispersionProjection.PlumeCoordinates coordinates = DispersionProjection.projectToPlume(-100d, 0d, 270d);

        assertEquals(-100d, coordinates.downwindMeters(), 1e-9);
        assertEquals(0d, coordinates.crosswindMeters(), 1e-9);
    }

    @Test
    public void shouldProjectSouthPointAsDownwindForNorthWind() {
        DispersionProjection.PlumeCoordinates coordinates = DispersionProjection.projectToPlume(0d, -100d, 0d);

        assertEquals(100d, coordinates.downwindMeters(), 1e-9);
        assertEquals(0d, coordinates.crosswindMeters(), 1e-9);
    }

    @Test
    public void shouldProjectDownwindAxisForNortheastWind() {
        DispersionProjection.PlumeCoordinates coordinates = DispersionProjection.projectToPlume(-100d, -100d, 45d);

        assertEquals(141.4213562373095d, coordinates.downwindMeters(), 1e-9);
        assertEquals(0d, coordinates.crosswindMeters(), 1e-9);
    }

    @Test
    public void shouldProjectPerpendicularAxisAsCrosswindForNortheastWind() {
        DispersionProjection.PlumeCoordinates coordinates = DispersionProjection.projectToPlume(100d, 0d, 45d);

        assertEquals(-70.71067811865474d, coordinates.downwindMeters(), 1e-9);
        assertEquals(70.71067811865477d, coordinates.crosswindMeters(), 1e-9);
    }

    @Test
    public void shouldPreserveVectorMagnitudeUnderProjectionRotation() {
        double eastMeters = 123.4d;
        double northMeters = -45.6d;
        DispersionProjection.PlumeCoordinates coordinates = DispersionProjection.projectToPlume(eastMeters, northMeters, 217d);

        double originalMagnitudeSquared = (eastMeters * eastMeters) + (northMeters * northMeters);
        double projectedMagnitudeSquared = (coordinates.downwindMeters() * coordinates.downwindMeters())
            + (coordinates.crosswindMeters() * coordinates.crosswindMeters());

        assertEquals(originalMagnitudeSquared, projectedMagnitudeSquared, 1e-9);
        assertTrue(projectedMagnitudeSquared > 0d);
    }
}
