package de.upb.spl.jumpstarter.randoms;

import de.upb.spl.jumpstarter.EvalTimelinePlotter;
import de.upb.spl.jumpstarter.EvaluationAnalysis;

import java.util.Optional;
import java.util.OptionalInt;
import static de.upb.spl.jumpstarter.EvalTimelinePlotter.*;

public class HASCOAnalysisTimelinePlotter {


    public static void main(String... args) {
        EvalTimelinePlotter plotter =
                new EvalTimelinePlotter(
                        args[0],
                        "plots/%s-time-hv.tex",
                        EvalTimelinePlotter::timeExtractor,
                        EvalTimelinePlotter::hvExtractor,
                        (plot, data) -> {
                            plot.setxLogAxis(true);
                            plot.setyMin(0.);
                            plot.setyMax(1.);
                            plot.shade(false);
                            plot.setxLabel(TIME_AXIS);
                            plot.setyLabel(HV_AXIS);
                        }

                );
        plotter.drawLatexFigures();
        plotter =
                new EvalTimelinePlotter(
                        args[0],
                        "plots/%s-time-rank.tex",
                        EvalTimelinePlotter::timeExtractor,
                        EvalTimelinePlotter::rankExtractor,
                        (plot, data) -> {
                            plot.setxLogAxis(true);
                            plot.setyMin(0.);
                            plot.setyMax(1.);
                            plot.shade(false);
                            plot.setxLabel(TIME_AXIS);
                            plot.setyLabel(RANK_AXIS);
                        }

                );
        plotter.drawLatexFigures();
        plotter =
                new EvalTimelinePlotter(
                        args[0],
                        "plots/%s-time-index.tex",
                        EvalTimelinePlotter::timeExtractor,
                        EvalTimelinePlotter::indexExtractor,
                        (plot, data) -> {
//                            plot.setxLogAxis(true);
                            plot.shade(false);
                            plot.setxLabel(TIME_AXIS);
                            plot.setyLabel(INDEX_AXIS);
                        }

                );
        plotter.drawLatexFigures();
        plotter =
                new EvalTimelinePlotter(
                        args[0],
                        "plots/%s-time-memory.tex",
                        EvalTimelinePlotter::timeExtractor,
                        EvalTimelinePlotter::memoryExtractor,
                        (plot, data) -> {
//                            plot.setxLogAxis(true);
                            plot.shade(false);
                            plot.setxLabel(TIME_AXIS);
                            plot.setyLabel(MEMORY_AXIS);
                        }

                );
        plotter.drawLatexFigures();


    }
}
