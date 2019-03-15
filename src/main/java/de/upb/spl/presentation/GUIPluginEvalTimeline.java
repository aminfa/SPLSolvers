package de.upb.spl.presentation;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.hasco.FeatureSelectionEvaluatedEvent;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.events.ScoredSolutionCandidateFoundEvent;
import jaicore.graphvisualizer.events.graph.bus.AlgorithmEventSource;
import jaicore.graphvisualizer.events.gui.GUIEvent;
import jaicore.graphvisualizer.events.gui.GUIEventSource;
import jaicore.graphvisualizer.plugin.*;
import jaicore.graphvisualizer.plugin.controlbar.ResetEvent;
import jaicore.graphvisualizer.plugin.solutionperformanceplotter.SolutionPerformanceTimelinePlugin;
import jaicore.graphvisualizer.plugin.solutionperformanceplotter.SolutionPerformanceTimelinePluginView;
import jaicore.graphvisualizer.plugin.timeslider.GoToTimeStepEvent;
import javafx.application.Platform;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GUIPluginEvalTimeline implements IGUIPlugin {

    private Logger logger = LoggerFactory.getLogger(GUIPluginEvalTimeline.class);

    private final Model model;
    private final View view;
    private final Controller controller;

    private final int objectiveIndex;
    private final BenchmarkEnvironment env;
    private final Map<String, XYChart.Series<Number, Number>> performanceSeries = new HashMap<>();

    public GUIPluginEvalTimeline(BenchmarkEnvironment env, int objectiveIndex) {
        this.env = env;
        this.objectiveIndex = objectiveIndex;
        model = new Model();
        view = new View(model);
        controller = new Controller(model, view);

        model.setController(controller);
        model.setView(view);

        view.setController(controller);

    }

    @Override
    public IGUIPluginController getController() {
        return controller;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public IGUIPluginView getView() {
        return view;
    }

    @Override
    public void setAlgorithmEventSource(AlgorithmEventSource graphEventSource) {
        graphEventSource.registerListener(controller);
    }

    @Override
    public void setGUIEventSource(GUIEventSource guiEventSource) {
        guiEventSource.registerListener(controller);
    }

    public class Model extends ASimpleMVCPluginModel<View, Controller> {

        public Model() {
        }

        public final void addEntry(FeatureSelectionEvaluatedEvent event) {
            if(!performanceSeries.containsKey(event.getAlgorithmId())) {
                addReasoner(event.getAlgorithmId());
            }
            XYChart.Series<Number, Number> series = performanceSeries.get(event.getAlgorithmId());
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(event.getEvaluationIndex(), event.getScore().objectives()[objectiveIndex]);

            Platform.runLater(() -> {
                series.getData().add(dataPoint);
            });
        }

        private void addReasoner(String reasonerName) {
            // defining a series
            logger.info("Adding evaluation performance series for {}.", reasonerName);
            XYChart.Series<Number, Number> performanceSeries = new XYChart.Series<>();
            performanceSeries.setName(reasonerName);
            GUIPluginEvalTimeline.this.performanceSeries.put(reasonerName, performanceSeries);
            Platform.runLater(() -> {
                List<XYChart.Series<Number, Number>> seriesList =  getView().getNode().getData();
                if(seriesList!=null) {
                    seriesList.add(performanceSeries);
                }
            });
        }

        public void clear() {

        }
    }

    public class View extends ASimpleMVCPluginView<Model, Controller, AreaChart<Number, Number>> {

        public View(Model model) {
            super(model, new AreaChart<Number, Number>(new NumberAxis(), new NumberAxis()));

            // defining the axes
            getNode().getXAxis().setLabel("elapsed time (s)");
            Axis<Number> axis = getNode().getYAxis();
            if(axis!= null) {
                axis.setLabel(env.objectives().get(objectiveIndex));
            }

            // creating the chart
            getNode().setTitle(getTitle());
        }

        @Override
        public void update() {
        }

        @Override
        public String getTitle() {
            return env.objectives().get(objectiveIndex) +  " Evaluation Performance Timeline";
        }

        @Override
        public void clear() {
            Platform.runLater(()->{
                for(XYChart.Series<Number, Number> series : performanceSeries.values()) {
                    series.getData().clear();
                }
                performanceSeries.clear();
            });
        }
    }

    public class Controller extends ASimpleMVCPluginController<Model, View> {

        private Logger logger = LoggerFactory.getLogger(SolutionPerformanceTimelinePlugin.class);

        public Controller(Model model, View view) {
            super(model, view);
        }

        @Override
        public void handleGUIEvent(GUIEvent guiEvent) {
            if (guiEvent instanceof ResetEvent || guiEvent instanceof GoToTimeStepEvent) {
                getModel().clear();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleAlgorithmEventInternally(AlgorithmEvent algorithmEvent) {
            if (algorithmEvent instanceof FeatureSelectionEvaluatedEvent) {
                getModel().addEntry((FeatureSelectionEvaluatedEvent) algorithmEvent);
            }
        }
    }
}
