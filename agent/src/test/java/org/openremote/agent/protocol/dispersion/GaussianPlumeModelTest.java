package org.openremote.agent.protocol.dispersion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GaussianPlumeModelTest {

    @Test
    public void shouldReturnZeroForNonPositiveDownwindDistance() {
        double concentration = GaussianPlumeModel.concentration(
            1200d,
            5d,
            0d,
            0d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );

        assertEquals(0d, concentration, 0d);
    }

    @Test
    public void shouldMatchReferenceSigmaCoefficientsForAllStabilityClasses() {
        double x = 1000d;

        assertEquals(209.7617696340303d, GaussianPlumeModel.sigmaY(DispersionStabilityClass.A, x), 1e-12);
        assertEquals(152.55401427929476d, GaussianPlumeModel.sigmaY(DispersionStabilityClass.B, x), 1e-12);
        assertEquals(104.88088481701514d, GaussianPlumeModel.sigmaY(DispersionStabilityClass.C, x), 1e-12);
        assertEquals(76.27700713964738d, GaussianPlumeModel.sigmaY(DispersionStabilityClass.D, x), 1e-12);
        assertEquals(57.20775535473553d, GaussianPlumeModel.sigmaY(DispersionStabilityClass.E, x), 1e-12);
        assertEquals(38.13850356982369d, GaussianPlumeModel.sigmaY(DispersionStabilityClass.F, x), 1e-12);

        assertEquals(200d, GaussianPlumeModel.sigmaZ(DispersionStabilityClass.A, x), 1e-12);
        assertEquals(120d, GaussianPlumeModel.sigmaZ(DispersionStabilityClass.B, x), 1e-12);
        assertEquals(73.02967433402215d, GaussianPlumeModel.sigmaZ(DispersionStabilityClass.C, x), 1e-12);
        assertEquals(37.94733192202055d, GaussianPlumeModel.sigmaZ(DispersionStabilityClass.D, x), 1e-12);
        assertEquals(23.076923076923073d, GaussianPlumeModel.sigmaZ(DispersionStabilityClass.E, x), 1e-12);
        assertEquals(12.307692307692307d, GaussianPlumeModel.sigmaZ(DispersionStabilityClass.F, x), 1e-12);
    }

    @Test
    public void shouldMatchReferenceCenterlineConcentrationForAllStabilityClasses() {
        double emissionRate = 1200d;
        double windSpeed = 5d;
        double downwind = 1000d;
        double crosswind = 0d;
        double receptorHeight = 1.5d;
        double sourceHeight = 5d;

        assertEquals(0.0018203592760946458d, GaussianPlumeModel.concentration(emissionRate, windSpeed, downwind, crosswind, receptorHeight, sourceHeight, DispersionStabilityClass.A), 1e-15);
        assertEquals(0.004169131761411366d, GaussianPlumeModel.concentration(emissionRate, windSpeed, downwind, crosswind, receptorHeight, sourceHeight, DispersionStabilityClass.B), 1e-15);
        assertEquals(0.009948477113896136d, GaussianPlumeModel.concentration(emissionRate, windSpeed, downwind, crosswind, receptorHeight, sourceHeight, DispersionStabilityClass.C), 1e-15);
        assertEquals(0.026144669673844313d, GaussianPlumeModel.concentration(emissionRate, windSpeed, downwind, crosswind, receptorHeight, sourceHeight, DispersionStabilityClass.D), 1e-15);
        assertEquals(0.05641054852486726d, GaussianPlumeModel.concentration(emissionRate, windSpeed, downwind, crosswind, receptorHeight, sourceHeight, DispersionStabilityClass.E), 1e-15);
        assertEquals(0.14893268273300683d, GaussianPlumeModel.concentration(emissionRate, windSpeed, downwind, crosswind, receptorHeight, sourceHeight, DispersionStabilityClass.F), 1e-15);
    }

    @Test
    public void shouldMatchReferenceConcentrationForClassD() {
        double concentration = GaussianPlumeModel.concentration(
            1200d,
            5d,
            1000d,
            0d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );

        assertEquals(0.026144669673844313d, concentration, 1e-12);
    }

    @Test
    public void shouldBeSymmetricAcrossCrosswindAxis() {
        double positiveCrosswind = GaussianPlumeModel.concentration(
            1200d,
            5d,
            1000d,
            160d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );
        double negativeCrosswind = GaussianPlumeModel.concentration(
            1200d,
            5d,
            1000d,
            -160d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );

        assertEquals(positiveCrosswind, negativeCrosswind, 1e-15);
    }

    @Test
    public void shouldScaleInverselyWithWindSpeed() {
        double concentrationSlowWind = GaussianPlumeModel.concentration(
            1200d,
            4d,
            1000d,
            80d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );
        double concentrationDoubleWind = GaussianPlumeModel.concentration(
            1200d,
            8d,
            1000d,
            80d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );

        assertEquals(concentrationSlowWind / 2d, concentrationDoubleWind, 1e-12);
    }

    @Test
    public void shouldClampSubMeterDownwindDistanceToOneMeter() {
        double concentrationHalfMeter = GaussianPlumeModel.concentration(
            1200d,
            5d,
            0.5d,
            0d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );
        double concentrationOneMeter = GaussianPlumeModel.concentration(
            1200d,
            5d,
            1d,
            0d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );

        assertEquals(concentrationOneMeter, concentrationHalfMeter, 1e-15);
    }

    @Test
    public void shouldScaleLinearlyWithEmissionRate() {
        double concentrationQ = GaussianPlumeModel.concentration(
            1000d,
            5d,
            800d,
            20d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );
        double concentrationDoubleQ = GaussianPlumeModel.concentration(
            2000d,
            5d,
            800d,
            20d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );

        assertEquals(concentrationQ * 2d, concentrationDoubleQ, 1e-12);
    }

    @Test
    public void shouldHaveHigherCenterlineConcentrationThanCrosswind() {
        double centerline = GaussianPlumeModel.concentration(
            1200d,
            5d,
            1000d,
            0d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );
        double offCenter = GaussianPlumeModel.concentration(
            1200d,
            5d,
            1000d,
            200d,
            1.5d,
            5d,
            DispersionStabilityClass.D
        );

        assertTrue(centerline > offCenter);
    }
}
