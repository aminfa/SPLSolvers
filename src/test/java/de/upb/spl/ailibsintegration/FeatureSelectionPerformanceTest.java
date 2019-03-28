package de.upb.spl.ailibsintegration;

import org.junit.Test;

import static org.junit.Assert.*;

public class FeatureSelectionPerformanceTest {

    @Test
    public void compareTo() {
        double[] objectives1 = {1., 1.};
        double[] objectives2 = {2., 1.};
        double[] objectives3 = {1., 2.};
        double[] objectives4 = {2., 2.};

        FeatureSelectionPerformance performance_0_1 = new FeatureSelectionPerformance(0, objectives1);
        FeatureSelectionPerformance performance_1_1 = new FeatureSelectionPerformance(1, objectives1);
        FeatureSelectionPerformance performance_2_1 = new FeatureSelectionPerformance(2, objectives1);

        FeatureSelectionPerformance performance_0_2 = new FeatureSelectionPerformance(0, objectives2);
        FeatureSelectionPerformance performance_1_2 = new FeatureSelectionPerformance(1, objectives2);
        FeatureSelectionPerformance performance_2_2 = new FeatureSelectionPerformance(2, objectives2);

        FeatureSelectionPerformance performance_0_3 = new FeatureSelectionPerformance(0, objectives3);
        FeatureSelectionPerformance performance_1_3 = new FeatureSelectionPerformance(1, objectives3);
        FeatureSelectionPerformance performance_2_3 = new FeatureSelectionPerformance(2, objectives3);


        FeatureSelectionPerformance performance_0_4 = new FeatureSelectionPerformance(0, objectives4);
        FeatureSelectionPerformance performance_1_4 = new FeatureSelectionPerformance(1, objectives4);
        FeatureSelectionPerformance performance_2_4 = new FeatureSelectionPerformance(2, objectives4);

        assertSuperior(performance_0_1, performance_1_2);
        assertSuperior(performance_1_4, performance_2_1);
        assertSuperior(performance_0_2, performance_1_3);

        assertEquals(performance_1_2, performance_1_3);
        assertSuperior(performance_1_1, performance_1_3);
        assertSuperior(performance_1_3, performance_1_4);
    }

    void assertSuperior(FeatureSelectionPerformance superiorPerformance, FeatureSelectionPerformance inferiorPerformance) {
        assertTrue(
                "Performance " + superiorPerformance +
                        " was expected to be better superior than " + inferiorPerformance + ".",
                superiorPerformance.compareTo(inferiorPerformance) < 0);
    }

    void assertEquals(FeatureSelectionPerformance superiorPerformance, FeatureSelectionPerformance inferiorPerformance) {
        assertTrue(
                "Performance " + superiorPerformance +
                        " was expected to be equal to " + inferiorPerformance + ".",
                superiorPerformance.compareTo(inferiorPerformance) == 0);
    }
}