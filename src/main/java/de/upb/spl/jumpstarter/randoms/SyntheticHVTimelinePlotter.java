package de.upb.spl.jumpstarter.randoms;

import de.upb.spl.jumpstarter.EvalTimelinePlotter;

import java.util.Optional;
import java.util.OptionalInt;

public class SyntheticHVTimelinePlotter {
    public static void main(String... args) {
        EvalTimelinePlotter plotter =
                new EvalTimelinePlotter(
                        args[0],
                        "plots/%s-index-hv.tex",
                        EvalTimelinePlotter::indexExtractor,
                        EvalTimelinePlotter::hvExtractor,
                        (plot, data) -> {
                            plot.setxLogAxis(true);
                            plot.setyMin(0.);
                            plot.setyMax(1.);
                            plot.setxMin(1.);
                            plot.setxLabel(EvalTimelinePlotter.INDEX_AXIS);
                            plot.setyLabel(EvalTimelinePlotter.HV_AXIS);
                            OptionalInt maxX = data.stream()
                                    .mapToInt(series ->
                                            series.keySet().stream()
                                                    .max(Double::compare).orElse(0))
                                    .max();
                            Optional<Double> xMax = Optional.empty();
                            if(maxX.isPresent()) {
                                xMax = Optional.of(((double) maxX.getAsInt()));
                            }
                            plot.setxMax(xMax);
                        }
                );
        plotter.drawLatexFigures();
        plotter =
                new EvalTimelinePlotter(
                        args[0],
                        "plots/%s-index-rank.tex",
                        EvalTimelinePlotter::indexExtractor,
                        EvalTimelinePlotter::rankExtractor,
                        (plot, data) -> {
                            plot.setxLogAxis(true);
                            plot.setyMin(0.);
                            plot.setyMax(1.);
                            plot.setxMin(1.);
                            plot.setxLabel(EvalTimelinePlotter.INDEX_AXIS);
                            plot.setyLabel(EvalTimelinePlotter.RANK_AXIS);
                            OptionalInt maxX = data.stream()
                                    .mapToInt(series ->
                                            series.keySet().stream()
                                                    .max(Double::compare).orElse(0))
                                    .max();
                            Optional<Double> xMax = Optional.empty();
                            if(maxX.isPresent()) {
                                xMax = Optional.of(((double) maxX.getAsInt()));
                            }
                            plot.setxMax(xMax);
                        }
                );
        plotter.drawLatexFigures();
    }
}
