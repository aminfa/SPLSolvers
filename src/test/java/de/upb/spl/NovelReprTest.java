package de.upb.spl;

import de.upb.spl.benchmarks.BenchmarkReport;
import de.upb.spl.benchmarks.env.AbstractBenchmarkEnv;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.hierons.NovelRepresentation;
import de.upb.spl.sayyad.Sayyad;
import fm.FeatureModel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.moeaframework.core.variable.BinaryVariable;
import org.sat4j.core.VecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class NovelReprTest {

    static BenchmarkEnvironment env;

    @BeforeClass
    public static void setEnv() {
        env = new AbstractBenchmarkEnv(
                "src/main/resources",
                "Eshop") {
            @Override
            public Future<BenchmarkReport> run(FeatureSelection selection, String clientName) {
                return null;
            }
        };

    }

    @Test
    public void testFixedLiterals() {
        FeatureModel fm = env.model();
        FMSAT fmsat = env.sat();
        VecInt vecInt = FMSatUtil.unitLiterals(fmsat);
        System.out.println("Unit propagation fixed: " + Arrays.toString(vecInt.toArray()));
        System.out.println("Size: " + vecInt.size());
        NovelRepresentation representation = new NovelRepresentation(env, "");
        IntStream intStream = IntStream.rangeClosed(1, fmsat.highestLiteral())
                .filter(i -> !representation.literalOrder().contains(i));
        int[]  novel = intStream.toArray();
        System.out.println("Novel: " +Arrays.toString(novel));
        System.out.println("Size: " + novel.length);

        BinaryVariable variable = Sayyad.binarizeSeed(representation.literalOrder(), env.richSeeds().get(0));
        FeatureSelection selection = FMUtil.selectFromPredicate(representation.featureOrder(), variable::get);
        Assert.assertFalse(FMUtil.isValidSelection(fm, selection));

//        FMUtil.addImpliedFeatures(fm, selection);

        representation.augment(selection);

        Assert.assertTrue(FMUtil.isValidSelection(fm, selection));




    }

    @Test
    public void testRandoms() throws TimeoutException {
        FeatureModel fm = env.model();
        FMSAT fmsat = env.sat();
        NovelRepresentation representation = new NovelRepresentation(env, "");
        ModelIterator solver = new ModelIterator(fmsat.getDiverseSolver(env.generator()));
        int modelCount = 0, successfullAugmentedCount = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000 && solver.isSatisfiable(); i++) {
            VecInt model = new VecInt(solver.model());
            BinaryVariable variable = Sayyad.binarizeSeed(representation.literalOrder(), model);
            FeatureSelection selection = FMUtil.selectFromPredicate(representation.featureOrder(), variable::get);
            representation.augment(selection);
            modelCount++;
            if(FMUtil.isValidSelection(env.model(), selection)){
                System.out.println("Selection " + variable + " is valid.");
                successfullAugmentedCount++;
                if(fmsat.violatedConstraints(model.toArray())> 0) {
                    Assert.fail();
                }
            } else {
                System.out.println("Selection " + variable + " is NOT valid.");
            }
        }
        System.out.println("Stopwatch: " + (System.currentTimeMillis() - startTime));
        System.out.println(successfullAugmentedCount + "/" + modelCount + " valid augments.");
    }

    @Test
    public void testToModel() {
        int[] partialModel = {-12, 15, 120, 21, 0, 0};
        int[] model = env.sat().toModel(partialModel, false);
        System.out.println(Arrays.toString(model));
    }
}
