package de.upb.spl.jumpstarter;

import com.google.gson.Gson;
import de.upb.spl.jumpstarter.panels.DeviationPlotter;
import de.upb.spl.util.FileUtil;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class EvalTimelinePlotter {

    public static final String TIME_AXIS = "Time since start (Seconds)";
    public static final String HV_AXIS = "Hypervolume of Paretofront";
    public static final String INDEX_AXIS = "Sample Index";
    public static final String RANK_AXIS = "Sample Average Rank Performance";
    public static final String MEMORY_AXIS = "Memory consumption (Mbyte)";


    private final String dataFilePath;
    private final String outputFilePathFormat;
    private final BiFunction<Integer, EvaluationAnalysis, Double> xExtractor, yExtractor;
    private final BiConsumer<DeviationPlotter, List<Map<Integer, EvaluationAnalysis>>> plotterConfigurator;

    public EvalTimelinePlotter(
            String dataFilePath,
            String outputFilePathFormat,
            BiFunction<Integer, EvaluationAnalysis, Double> xExtractor,
            BiFunction<Integer, EvaluationAnalysis, Double> yExtractor,
           BiConsumer<DeviationPlotter, List<Map<Integer, EvaluationAnalysis>>> plotterConfigurator) {
        this.dataFilePath = dataFilePath;
        this.outputFilePathFormat = outputFilePathFormat;
        this.xExtractor = xExtractor;
        this.yExtractor = yExtractor;
        this.plotterConfigurator = plotterConfigurator;
    }

    public void drawLatexFigures() {
        Map<String, List<Map<Integer, EvaluationAnalysis>>> hvData;
        hvData = new Gson().fromJson(FileUtil.readFileAsString(dataFilePath), EvaluationAnalysis.dataFileToken());
        for(String reasoner : hvData.keySet()) {
            List<Map<Integer, EvaluationAnalysis>> data = hvData.get(reasoner);
            DeviationPlotter plotter = new DeviationPlotter();
            plotterConfigurator.accept(plotter, data);


            // add samples:
            for (Map<Integer, EvaluationAnalysis> samples : data) {
                samples.forEach((x, sample) -> {
                    plotter.addSample(
                            x,
                            xExtractor.apply(x, sample),
                            yExtractor.apply(x, sample));
                });
            }
            String latexFigure = plotter.latexFigure();
            FileUtil.writeStringToFile(String.format(outputFilePathFormat, reasoner), latexFigure);
        }
    }


    public static double timeExtractor(Integer index, EvaluationAnalysis e) {
        return (double) e.getTime() /(1000.);
    }

    public static double hvExtractor(Integer index, EvaluationAnalysis e) {
        return e.getHv();
    }

    public static double indexExtractor(Integer index, EvaluationAnalysis e) {
        return index;
    }

    public static double memoryExtractor(Integer index, EvaluationAnalysis e) {
        return e.getMemory();
    }

    public static double rankExtractor(Integer index, EvaluationAnalysis e) {
        double rank = e.getRank();
        if(rank == -1) {
            return 0.;
        } else {
            return rank;
        }
    }
}
