package de.upb.spl;

import de.upb.spl.benchmarks.*;
import de.upb.spl.benchmarks.env.*;
import de.upb.spl.benchmarks.x264.VideoEncoderBlackBox;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.jumpstarter.ParetoPresentation;
import de.upb.spl.reasoner.EAReasoner;
import de.upb.spl.sayyad.Sayyad;
import fm.FeatureModelException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.*;
import org.moeaframework.core.variable.BinaryVariable;

import java.io.File;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

public class EAReasonerTest {

	static BenchmarkEnvironment env;
	static BenchmarkEnvironment rawEnv;
	static BenchmarkAgent agent;

	final static String spl = "Eshop";

	private static Map<String, Population> results = new HashMap<>();

	private static final BiFunction<Solution, EAReasoner, Solution> reevaluator = (solution, reasoner) ->
    {
        FeatureSelection selection = reasoner.assemble(env, solution);
        double[] evaluation = BenchmarkHelper.evaluateFeatureSelection(rawEnv, selection);
        return BenchmarkHelper.toSolution((BinaryVariable) solution.getVariable(0), evaluation);
    };

//	@BeforeClass
	public static void setEnvironment() throws FeatureModelException, IOException {
		agent = new BenchmarkAgent(10000);
		VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent, "/Users/aminfaez/Documents/BA/x264_1");
//		VideoEncoderExecutor.fixedAttributesExecutor(agent);
		// Load the XML file and creates the listFeatures model
		env = new Bookkeeper(new VideoEncoderBlackBox(agent));
        rawEnv = new RawResults(env);
	}

    @BeforeClass
	public static void setupAttributeEnvironment() {
        env = new Bookkeeper(new AttributedFeatureModelEnv("src/main/resources", spl));
        rawEnv = new RawResults(env);
    }

    @AfterClass
    public static void saveResults() throws IOException {
        Plot plot = new Plot();
        for(String splreasonerName : results.keySet()) {
            Population p = results.get(splreasonerName);
            if(p.isEmpty()) {
                System.err.println("SPL reasoner " + splreasonerName + " found 0 solutions...");
                continue;
            }
            plot.add(splreasonerName, results.get(splreasonerName));
        }
        plot.setXLabel(env.objectives().get(0));
        if(env.objectives().size() > 1)
            plot.setYLabel(env.objectives().get(1));

        String plotFile = "presentations/" + ParetoPresentation.presentationFileNameForCurrentTime(env.toString()) + ".svg";
        plot.save(new File(plotFile), "SVG", 800, 600);
	}

	public void testReasoner(EAReasoner reasoner) {
	    BenchmarkEnvironment billedEnv = new Bookkeeper.Bill(env, env.bill(reasoner.name()));
        AbstractEvolutionaryAlgorithm alg = reasoner.runAlgorithm(billedEnv);
        Population population = alg.getPopulation();
        Population moPopulation = new NondominatedPopulation();

        StreamSupport.stream(population.spliterator(), false)
                .map(s -> reevaluator.apply(s, reasoner))
                .forEach(moPopulation::add);

        Solution bestPerformer = EAReasoner.bestPerformer(alg.getProblem(), population);
        Solution reevaluatedBestPerformer = reevaluator.apply(bestPerformer, reasoner);
        dumpSolution(reevaluatedBestPerformer);

        results.put(reasoner.name(), moPopulation);
        ParetoPresentation.saveSolutionAsJson(env, reasoner, moPopulation);
    }

	@Test
	public void testRunGUO() {
		Guo11 guo11 = new Guo11();
		testReasoner(guo11);
	}

//	@Test
    public void testBasicIbea() throws ExecutionException, InterruptedException {
        BasicIbea basicIbea = new BasicIbea();
        testReasoner(basicIbea);
    }

//    @Test
    public void testSayyad() {
        Sayyad sayyad = new Sayyad();
        testReasoner(sayyad);
    }

	@Test
	public void testRunHenard()  {
        Henard henard = new Henard();
        testReasoner(henard);
	}

	@Test
    public void testRunHierons() {
        Hierons hierons = new Hierons();
        testReasoner(hierons);
    }


    private void dumpBinaryString(Population population)  {
        for(Solution solution : population) {
            BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
            System.out.println("BinaryVariable: "      + variable.toString());
            DoubleSummaryStatistics stats = DoubleStream.of(solution.getObjectives()).collect(DoubleSummaryStatistics::new,
                    DoubleSummaryStatistics::accept,
                    DoubleSummaryStatistics::combine);
            System.out.println("Performance average: "          + stats.getAverage() );
            System.out.println("Performance sum: "          + stats.getSum() );
        }
    }

    private void dumpSolution(Solution solution) {
        BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
        System.out.println("BinaryVariable: "      + variable.toString());
        DoubleSummaryStatistics stats = DoubleStream.of(solution.getObjectives()).collect(DoubleSummaryStatistics::new,
                DoubleSummaryStatistics::accept,
                DoubleSummaryStatistics::combine);
        System.out.println("Performance average: "      + stats.getAverage() );

    }
}
