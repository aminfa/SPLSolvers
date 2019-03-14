package de.upb.spl;

import com.google.gson.Gson;
import de.upb.spl.benchmarks.*;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.VideoEncoderEnv;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.presentation.ParetoPresentation;
import de.upb.spl.reasoner.SPLEvaluator;
import de.upb.spl.sayyad.Sayyad;
import fm.FeatureModelException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class ReasonerTest {

	static BenchmarkEnvironment video_encoding_env;
	static BenchmarkAgent agent;

	final static String spl = "video_encoder";

	private static Map<String, Population> results = new HashMap<>();

//	@BeforeClass
	public static void setEnvironment() throws FeatureModelException, IOException {
		agent = new BenchmarkAgent(10000);
		VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent, "/Users/aminfaez/Documents/BA/x264_1");
//		VideoEncoderExecutor.fixedAttributesExecutor(agent);
		// Load the XML file and creates the listFeatures model
		video_encoding_env = new VideoEncoderEnv(agent);
	}

    @BeforeClass
	public static void setupAttributeEnvironment() {
        video_encoding_env = new AttributedFeatureModelEnv("src/main/resources", spl);
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
        plot.setXLabel(video_encoding_env.objectives().get(0));
        if(video_encoding_env.objectives().size() > 1)
            plot.setYLabel(video_encoding_env.objectives().get(1));

        String plotFile = "presentations/" + ParetoPresentation.presentationFileNameForCurrentTime(video_encoding_env.toString()) + ".svg";
        plot.save(new File(plotFile), "SVG", 800, 600);
	}

	@Test
	public void testRunGUO() {
		Guo11 guo11 = new Guo11();
		Population population = guo11.run(video_encoding_env);
		dumpBinaryString(population);
        Population moPopulation = new NondominatedPopulation();
		population.forEach(solution ->
        {
            FeatureSelection selection = guo11.assemble(video_encoding_env, solution);
            double[] evaluation = SPLEvaluator.evaluateFeatureSelection(video_encoding_env, selection, null, false);
            moPopulation.add(SPLEvaluator.toSolution((BinaryVariable) solution.getVariable(0), evaluation));
        });
        results.put(guo11.name(), moPopulation);
//        ParetoPresentation.savePlotAsSvg(video_encoding_env, guo11, moPopulation);
        ParetoPresentation.saveSolutionAsJson(video_encoding_env, guo11, moPopulation);
//        ParetoPresentation.showDialog(video_encoding_env, guo11, moPopulation).setVisible(true);
	}

//	@Test
    public void testBasicIbea() throws ExecutionException, InterruptedException {
        BasicIbea basicIbea = new BasicIbea();
        Population population = basicIbea.run(video_encoding_env);
        dumpBinaryString(population);
        Population moPopulation = new NondominatedPopulation();
        population.forEach(solution ->
        {
            FeatureSelection selection = basicIbea.assemble(video_encoding_env, solution);
            double[] evaluation = SPLEvaluator.evaluateFeatureSelection(video_encoding_env, selection, null, false);
            moPopulation.add(SPLEvaluator.toSolution((BinaryVariable) solution.getVariable(0), evaluation));
        });
        results.put(basicIbea.name(), moPopulation);
//        ParetoPresentation.savePlotAsSvg(video_encoding_env, basicIbea, moPopulation);
        ParetoPresentation.saveSolutionAsJson(video_encoding_env, basicIbea, moPopulation);
    }

    @Test
    public void testSayyad() {
        Sayyad sayyad = new Sayyad();
        Population population = sayyad.run(video_encoding_env);
        dumpBinaryString(population);
        Population moPopulation = new NondominatedPopulation();
        population.forEach(solution ->
        {
            FeatureSelection selection = sayyad.assemble(video_encoding_env, solution);
            double[] evaluation = SPLEvaluator.evaluateFeatureSelection(video_encoding_env, selection, null, false);
            moPopulation.add(SPLEvaluator.toSolution((BinaryVariable) solution.getVariable(0), evaluation));
        });
        results.put(sayyad.name(), moPopulation);
//        ParetoPresentation.savePlotAsSvg(video_encoding_env, sayyad, moPopulation);
        ParetoPresentation.saveSolutionAsJson(video_encoding_env, sayyad, moPopulation);
    }

	@Test
	public void testRunHenard()  {
        Henard henard = new Henard();
        Population population = henard.run(video_encoding_env);
        Population moPopulation = new NondominatedPopulation();
        population.forEach(solution ->
        {
            FeatureSelection selection = henard.assemble(video_encoding_env, solution);
            double[] evaluation = SPLEvaluator.evaluateFeatureSelection(video_encoding_env, selection, null, false);
            moPopulation.add(SPLEvaluator.toSolution((BinaryVariable) solution.getVariable(0), evaluation));
        });
        dumpBinaryString(moPopulation);
        results.put(henard.name(), moPopulation);
//        ParetoPresentation.savePlotAsSvg(video_encoding_env, sayyad, moPopulation);
        ParetoPresentation.saveSolutionAsJson(video_encoding_env, henard, moPopulation);
	}

	@Test
    public void testRunHierons() {
        Hierons hierons = new Hierons();
        Population population = hierons.run(video_encoding_env);
        Population moPopulation = new NondominatedPopulation();
        population.forEach(solution ->
        {
            FeatureSelection selection = hierons.assemble(video_encoding_env, solution);
            double[] evaluation = SPLEvaluator.evaluateFeatureSelection(video_encoding_env, selection, null, false);
            moPopulation.add(SPLEvaluator.toSolution((BinaryVariable) solution.getVariable(0), evaluation));
        });
        dumpBinaryString(moPopulation);
        results.put(hierons.name(), moPopulation);
//        ParetoPresentation.savePlotAsSvg(video_encoding_env, sayyad, moPopulation);
        ParetoPresentation.saveSolutionAsJson(video_encoding_env, hierons, moPopulation);
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

    private void dumpPopulation(Population population) throws ExecutionException,
            InterruptedException {
        for(Solution solution : population) {
            BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
            FeatureSelection selection = FMUtil.selectFromPredicate(FMUtil.featureStream(video_encoding_env.model()).collect(Collectors.toList()),
                    variable::get);
            System.out.println("Configuration: "      + selection);
            System.out.println("File size: "          + solution.getObjective(0));
            System.out.println("Subjective quality: " + solution.getObjective(1));
        }
    }
}
