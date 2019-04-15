package de.upb.spl.jumpstarter;

import de.upb.spl.jumpstarter.panels.DeviationPlotter;
import de.upb.spl.util.FileUtil;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class HVTimelinePlotter {
    private final String dataFilePath;
    private final String outputFilePathFormat;
    public HVTimelinePlotter(String dataFilePath, String outputFileFormat) {
        this.dataFilePath = dataFilePath;
        this.outputFilePathFormat = outputFileFormat;
    }

    public void drawLatexFigures() {
        JSONParser parser = new JSONParser();
        Map<String, List<Map<String, Number>>> hvData;
        try {
            hvData = (Map<String, List<Map<String, Number>>>) parser.parse(FileUtil.readFileAsString(dataFilePath));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        for(String reasoner : hvData.keySet()) {
            List<Map<String, Number>> data = hvData.get(reasoner);
            DeviationPlotter plotter = new DeviationPlotter();
            plotter.setyMin(0.);
            plotter.setyMax(1.);
            plotter.setxMin(1.);
            OptionalInt maxX = data.stream()
                    .mapToInt(series ->
                        series.keySet().stream()
                            .mapToInt(Integer::parseInt)
                            .max().orElse(0))
                    .max();
            Optional<Double> xMax = Optional.empty();
            if(maxX.isPresent()) {
                xMax = Optional.of(((double) maxX.getAsInt()));
            }
            plotter.setxMax(xMax);

            plotter.setxLabel("Evaluation Sample Index");
            plotter.setyLabel("Hypervolume of Paretofront");
            // add samples:
            data.forEach(sample -> sample.forEach(plotter::addSample));
            String latexFigure = plotter.latexFigure();
            FileUtil.writeStringToFile(String.format(outputFilePathFormat, reasoner), latexFigure);
        }
    }
}
