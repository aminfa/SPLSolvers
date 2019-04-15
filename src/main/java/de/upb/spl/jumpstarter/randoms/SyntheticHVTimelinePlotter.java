package de.upb.spl.jumpstarter.randoms;

import de.upb.spl.jumpstarter.HVTimelinePlotter;

public class SyntheticHVTimelinePlotter {
    public static void main(String... args) {
        HVTimelinePlotter plotter =
                new HVTimelinePlotter("synthetic-hv-steps.json",
                "plots/%s-hv-timeline.pre.tex"
                );
        plotter.drawLatexFigures();
    }
}
