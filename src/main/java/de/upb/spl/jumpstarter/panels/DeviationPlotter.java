package de.upb.spl.jumpstarter.panels;

import org.tools4j.meanvar.MeanVarianceSampler;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DeviationPlotter {

    private Map<Double, MeanVarianceSampler> samplers = new LinkedHashMap<>();

    private Optional<Double> yMin = Optional.empty(),
            yMax= Optional.empty(),
            xMin= Optional.empty(),
            xMax= Optional.empty();

    private Optional<String> xLabel = Optional.empty(), yLabel = Optional.empty();

    public DeviationPlotter() {

    }

    public void addSample(int x, double y) {
        samplers.computeIfAbsent((double)x, i -> new MeanVarianceSampler()).add(y);
    }

    public String pgfPlot(){
        StringBuilder plot = new StringBuilder();
        plot    .append("\\begin{semilogxaxis}[\n")
                .append("  ymajorgrids=true,\n")
                .append("  grid style=dashed,\n")
                .append(yMax.map(y -> String.format("  ymax=%.2f,\n", y)).orElse(""))
                .append(yMin.map(y -> String.format("  ymin=%.2f,\n", y)).orElse(""))
                .append(xMax.map(x -> String.format("  xmax=%.2f,\n", x)).orElse(""))
                .append(xMin.map(x -> String.format("  xmin=%.2f,\n", x)).orElse(""))
                .append(xLabel.map(label -> String.format("  xlabel=%s,\n", label)).orElse(""))
                .append(yLabel.map(label -> String.format("  ylabel=%s,\n", label)).orElse(""));
        plot.delete(plot.length() - 2, plot.length()); // delete comma
        plot
                .append("]\n")
                .append("\n");
        StringBuilder meanCoordinates = new StringBuilder();
        StringBuilder varianceTopCoordinates = new StringBuilder();
        StringBuilder varianceDownCoordinates = new StringBuilder();

        samplers.keySet().stream().sorted().forEach(i -> {
            MeanVarianceSampler sampler = samplers.get(i);
            if(sampler == null) {
                return;
            }
            double mean = sampler.getMean();
            double variance = sampler.getVariance();
            double varianceTop = mean + variance;
            double varianceDown = mean - variance;
            meanCoordinates        .append("(").append(i).append(",").append(mean)         .append(")");
            varianceTopCoordinates .append("(").append(i).append(",").append(varianceTop)  .append(")");
            varianceDownCoordinates.append("(").append(i).append(",").append(varianceDown) .append(")");
        });
        plot.append("    \\addplot[mark=none] coordinates {").append(meanCoordinates).append("};\n");
        plot.append("    \\addplot[mark=none, name path=top, opacity=0.] coordinates {").append(varianceTopCoordinates).append("};\n");
        plot.append("    \\addplot[mark=none, name path=down, opacity=0.] coordinates {").append(varianceDownCoordinates).append("};\n");
        plot.append("    \\addplot[fill opacity=0.35] fill between[of=top and down];\n");
        plot.append("\\end{semilogxaxis}");
        return plot.toString();
    }

    public String latexFigure() {
        StringBuilder builder = new StringBuilder();
        builder
                .append("\\documentclass{standalone}\n")
                .append("\\usepackage{tikz}\n")
                .append("\\usepackage{pgfplots}\n")
                .append("\\usepgfplotslibrary{fillbetween}\n")
                .append("\n")
                .append("\\begin{document}")
                .append("\\begin{tikzpicture}")
                .append("\n")
                .append(pgfPlot())
                .append("\n")
                .append("\\end{tikzpicture}\n")
                .append("\\end{document}")
                ;

        return builder.toString();
    }

    public Optional<Double> getyMin() {
        return yMin;
    }

    public void setyMin(double yMin) {
        this.yMin = Optional.of(yMin);
    }

    public void setyMin(Optional<Double> yMin) {
        this.yMin = yMin;
    }

    public Optional<Double> getyMax() {
        return yMax;
    }

    public void setyMax(double yMax) {
        this.yMax = Optional.of(yMax);
    }

    public void setyMax(Optional<Double> yMax) {
        this.yMax = yMax;
    }

    public Optional<Double> getxMin() {
        return xMin;
    }

    public void setxMin(double xMin) {
        this.xMin = Optional.of(xMin);
    }

    public void setxMin(Optional<Double> xMin) {
        this.xMin = xMin;
    }

    public Optional<Double> getxMax() {
        return xMax;
    }

    public void setxMax(double xMax) {
        this.xMax = Optional.of(xMax);
    }
    public void setxMax(Optional<Double> xMax) {
        this.xMax = xMax;
    }

    public Optional<String> getxLabel() {
        return xLabel;
    }

    public void setxLabel(Optional<String> xLabel) {
        this.xLabel = xLabel;
    }

    public Optional<String> getyLabel() {
        return yLabel;
    }

    public void setyLabel(Optional<String> yLabel) {
        this.yLabel = yLabel;
    }


    public void setxLabel(String label) {
        this.setxLabel(Optional.of(label));
    }
    public void setyLabel(String label) {
        this.setyLabel(Optional.of(label));
    }

    public void addSample(String x, Number y) {
        this.addSample(Integer.parseInt(x), y.doubleValue());
    }
}
