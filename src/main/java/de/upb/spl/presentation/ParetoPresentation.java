package de.upb.spl.presentation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import org.json.simple.JSONObject;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.Population;
import util.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class ParetoPresentation {

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String presentationFileNameForCurrentTime(String name) {
        return String.format("%1$tY.%1$tm.%1$td-%1$tH.%1$tM--%2$s", new Date(), name);
    }
    public static void savePlotAsSvg(BenchmarkEnvironment environment,
                                     SPLReasoner reasoner, Population population) {
        try {
            String plotFile = "presentations/" + presentationFileNameForCurrentTime(reasoner.name()) + ".svg";
            plot(environment, reasoner, population)
                    .save(new File(plotFile), "SVG", 800, 600);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void saveSolutionAsJson(BenchmarkEnvironment environment,
                                            SPLReasoner reasoner, Population population) {
        String solutionFile = "results/" + presentationFileNameForCurrentTime(reasoner.name()) + ".solution.json";
        List<Map> solutions = new ArrayList<>();
        population.forEach(solution -> {
            JSONObject solutionObj = new JSONObject();
            solutionObj.put("variable", solution.getVariable(0).toString());
            JSONObject objectives = new JSONObject();
            solutionObj.put("objectives", objectives);
            int objIndex = 0;
            for(String objectiveName : environment.objectives()) {
                objectives.put(objectiveName, solution.getObjective(objIndex++));
            }
            solutions.add(solutionObj);
        });
        String solutionJsonString = gson.toJson(solutions);
        FileUtil.writeStringToFile(solutionFile, solutionJsonString);
    }

    public static Plot plot(BenchmarkEnvironment environment,
                            SPLReasoner reasoner, Population population) {
        Plot plot = new Plot().add(reasoner.name(), population)
                .setXLabel(environment.objectives().get(0));
        if(environment.objectives().size() > 1)
            plot.setYLabel(environment.objectives().get(1));
        return plot;
    }

    public static JFrame showGUI(BenchmarkEnvironment environment,
                                 SPLReasoner reasoner, Population population) {
        JFrame frame = plot(environment, reasoner, population)
                .show();
        return frame;
    }

    public static JDialog showDialog(BenchmarkEnvironment environment,
                                  SPLReasoner reasoner, Population population) {
        JPanel chartPanel =  plot(environment, reasoner, population).getChartPanel();
        JDialog frame = new JDialog();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(chartPanel, "Center");
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo((Component)null);
        frame.setDefaultCloseOperation(2);
        frame.setTitle("Solutions from " + reasoner.name());
        frame.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        return frame;
    }

}
