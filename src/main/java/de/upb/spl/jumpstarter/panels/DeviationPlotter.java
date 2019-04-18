package de.upb.spl.jumpstarter.panels;

import org.tools4j.meanvar.MeanVarianceSampler;

import java.util.*;

public class DeviationPlotter {

    private Map<Integer, Sample> samplers = new LinkedHashMap<>();

    private Optional<Double> yMin = Optional.empty(),
            yMax= Optional.empty(),
            xMin= Optional.empty(),
            xMax= Optional.empty();

    private Optional<String> xLabel = Optional.empty(), yLabel = Optional.empty();

    private boolean xLogAxis, yLogAxis;
    private boolean shade = true;

    public DeviationPlotter() {

    }

    public void addSample(int evalIndex, double x, double y) {
        samplers.computeIfAbsent(evalIndex, i -> new Sample()).add(x, y);
    }


    public String pgfPlot(){
        StringBuilder plot = new StringBuilder();

        String axisType = "axis";
        if(xLogAxis && yLogAxis) {
            axisType = "loglogaxis";
        } else if(xLogAxis) {
            axisType = "semilogxaxis";
        } else if(yLogAxis) {
            axisType = "semilogyaxis";
        }

        plot    .append("\\begin{").append(axisType).append("}[\n")
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
            Sample sample = samplers.get(i);
            if(sample == null) {
                return;
            }
            double mean = sample.getMeanY();
            double variance = sample.getVarianceY();
//            double varianceTop = mean + variance;
//            double varianceDown = mean - variance;
            double x = sample.getMeanX();

            meanCoordinates        .append("(").append(x).append(",").append(mean)         .append(")");
            if(shade) {
                double varianceTop = sample.getMaxY();
                double varianceDown = sample.getMinY();
                varianceTopCoordinates .append("(").append(x).append(",").append(varianceTop)  .append(")");
                varianceDownCoordinates.append("(").append(x).append(",").append(varianceDown) .append(")");
            }
        });
        plot.append("    \\addplot[mark=none] coordinates {").append(meanCoordinates).append("};\n");
        if(shade) {
            plot.append("    \\addplot[mark=none, name path=top, opacity=0.] coordinates {").append(varianceTopCoordinates).append("};\n");
            plot.append("    \\addplot[mark=none, name path=down, opacity=0.] coordinates {").append(varianceDownCoordinates).append("};\n");
            plot.append("    \\addplot[fill opacity=0.35] fill between[of=top and down];\n");
        }
        plot.append("\\end{").append(axisType).append("}");
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


    public void setxLogAxis(boolean xLogAxis) {
        this.xLogAxis = xLogAxis;
    }

    public void setyLogAxis(boolean yLogAxis) {
        this.yLogAxis = yLogAxis;
    }

    public void shade(boolean b) {
        this.shade = b;
    }

    static class Sample {
        MeanVarianceSampler samplerX = new MeanVarianceSampler();
        MeanVarianceSampler samplerY = new MeanVarianceSampler();
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;

        void add(double x, double y) {
            samplerX.add(x);
            if(x < minX) {
                minX = x;
            }
            if(x > maxX) {
                maxX = x;
            }
            samplerY.add(y);
            if(y < minY) {
                minY = y;
            }
            if(y > maxY) {
                maxY = y;
            }
        }

        double getMeanY() {
            return samplerY.getMean();
        }

        double getMeanX() {
            return samplerX.getMean();
        }

        double getVariancX() {
            return samplerX.getVariance();
        }

        double getVarianceY() {
            return samplerY.getVariance();
        }

        public double getMinY() {
            return minY;
        }

        public double getMaxY() {
            return maxY;
        }

        public double getMinX() {
            return minX;
        }

        public double getMaxX() {
            return maxX;
        }
    }
}
