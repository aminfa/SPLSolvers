package de.upb.spl.jumpstarter.panels;

import de.upb.spl.ailibsintegration.FeatureSelectionEvaluatedEvent;
import de.upb.spl.ailibsintegration.FeatureSelectionOrdering;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.util.FileUtil;
import de.upb.spl.util.Iterators;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.graphvisualizer.events.graph.bus.AlgorithmEventSource;
import jaicore.graphvisualizer.events.graph.bus.HandleAlgorithmEventException;
import jaicore.graphvisualizer.events.gui.GUIEvent;
import jaicore.graphvisualizer.events.gui.GUIEventSource;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.plugin.IGUIPluginController;
import jaicore.graphvisualizer.plugin.IGUIPluginModel;
import jaicore.graphvisualizer.plugin.IGUIPluginView;
import jaicore.graphvisualizer.plugin.controlbar.ResetEvent;
import jaicore.graphvisualizer.plugin.timeslider.GoToTimeStepEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ReasonerPerformanceTimeline implements IGUIPlugin,  IGUIPluginController, IGUIPluginView, IGUIPluginModel {

    private final static Logger logger = LoggerFactory.getLogger(ReasonerPerformanceTimeline.class);

    private final BenchmarkEnvironment env;

    private final Parent rootNode;

    @FXML
    private AreaChart<Number, Number> chart;

    @FXML
    private ComboBox<String> compareSelection;

    private String selectedComparisson = "";

    private final String name;

    private final static String FXML_RESOURCE = ReasonerPerformanceTimeline.class.getSimpleName() + ".fxml";

//    private final Map<String, List<MarkedDataPoint>> data = new ConcurrentHashMap<>();

    private final Map<ReasonerObjectiveTuple, XYChart.Series<Number, Number>> chartData = new LinkedHashMap<>();

    private Insertion currentInsertion = new Insertion();

    public ReasonerPerformanceTimeline(BenchmarkEnvironment env) {
        this.env = env;
        this.name =  "SPL Reasoner Performance Timeline";
        /*
         * Load node from fxml resource:
         */
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(FileUtil.getResourceFile(FXML_RESOURCE).toURI().toURL());
            loader.setController(this);
            rootNode = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error while setting up gui: " + name, e);
        }

        compareSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue != null && oldValue.equals(newValue)) {
                return;
            }
            selectedComparisson = newValue;
            chart.getData().clear();
            chartData.forEach((tuple, series) -> {
                if(tuple.objective.equals(newValue)) {
                    chart.getData().add(series);
                }
            });
        });
        compareAlgorithms();

    }

    void compareAlgorithms() {
        runAndWait(() ->
        {
            List<String> selections = compareSelection.getItems();
            selections.clear();
            selections.addAll(env.objectives());
            if(selections.size() > 0) {
                compareSelection.setValue(selections.get(0));
            }
        });
    }


    @Override
    public Node getNode() {
        return rootNode;
    }

    private void insertIntoView(XYChart.Series<Number, Number> series, XYChart.Data<Number, Number> data) {
        while(!currentInsertion.insert(series, data)) {
            currentInsertion = new Insertion();
        }
    }

    private void insert(String category, String objective, XYChart.Data<Number, Number> data) {
        ReasonerObjectiveTuple tuple = new ReasonerObjectiveTuple(category, objective);
        XYChart.Series<Number, Number> series = chartData.get(tuple);
        if(series == null) {
            series = newSeries(tuple);
        }
        insertIntoView(series, data);
    }

    private XYChart.Series<Number, Number> newSeries(ReasonerObjectiveTuple tuple) {
        // defining a series
        XYChart.Series<Number, Number> performanceSeries = new XYChart.Series<>();
        performanceSeries.setName(tuple.reasoner);
        chartData.put(tuple, performanceSeries);
        if(selectedComparisson.equals(tuple.objective)) {
            Platform.runLater(() -> {
                List<XYChart.Series<Number, Number>> seriesList = chart.getData();
                if (seriesList != null) {
                    seriesList.add(performanceSeries);
                }
            });
        }
        return performanceSeries;
    }

//    private void addAgain() {
//        for (Map.Entry<String, List<MarkedDataPoint>> entry : data.entrySet()) {
//            String reasoner = entry.getKey();
//            List<MarkedDataPoint> points = entry.getValue();
//            for (int i = 0, size = points.size(); i < size; i++) {
//                MarkedDataPoint point = points.get(i);
//                if (point.isNotMarked()) {
//                    for (int j = 0, objSize = env.objectives().size(); j < objSize; j++) {
//                        String objective = env.objectives().get(j);
//                        XYChart.Data<Number, Number> data = new XYChart.Data<>(i, point.dataPoint.objectives()[j]);
//                        insert(reasoner, objective, data);
//                    }
//                    point.mark();
//                }
//            }
//        }
//    }

    @Override
    public void update() {

    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public void handleAlgorithmEvent(AlgorithmEvent algorithmEvent) throws HandleAlgorithmEventException {
        if (algorithmEvent instanceof FeatureSelectionEvaluatedEvent) {
            this.addEntry((FeatureSelectionEvaluatedEvent) algorithmEvent);
        }
    }

    private void addEntry(FeatureSelectionEvaluatedEvent event) {
        String reasoner = event.getAlgorithmId();
        int index = event.getEvaluationIndex();
        for (int i = 0, size = env.objectives().size(); i < size; i++) {
            String objective = env.objectives().get(i);
            Optional<Double> performance = env.interpreter(event.getReport()).rawResult(objective);
            if(!performance.isPresent()) {
                logger.warn("Couldn't add data point for {} evaluation of {}. Performance is empty.", Iterators.ordinal(event.getEvaluationIndex()), event.getAlgorithmId());
                return;
            } else {
                XYChart.Data<Number, Number> data = new XYChart.Data<>(index, performance.get());
                insert(reasoner, objective, data);
            }
        }
    }

    private class Insertion implements Runnable {

        boolean inQueue = true;
        boolean started = false;

        Map<XYChart.Series<Number, Number>, List<XYChart.Data<Number, Number>>>
                tobeInserted = new HashMap<>();

        @Override
        public synchronized void run() {
            inQueue = false;
            tobeInserted.forEach((series, points) -> points.forEach(point -> series.getData().add(point)));
        }
        synchronized boolean insert(XYChart.Series<Number, Number> series, XYChart.Data<Number, Number> data) {
            if(!inQueue) {
                return false;
            }
            tobeInserted.computeIfAbsent(series, s -> new ArrayList<>()).add(data);
            if(!started) {
                Platform.runLater(this);
                started = true;
            }
            return true;
        }
    }
    private class ReasonerObjectiveTuple {

        final String reasoner, objective;

        private ReasonerObjectiveTuple(String reasoner, String objective) {
            this.reasoner = Objects.requireNonNull(reasoner);
            this.objective = Objects.requireNonNull(objective);

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReasonerObjectiveTuple)) return false;

            ReasonerObjectiveTuple that = (ReasonerObjectiveTuple) o;

            if (!reasoner.equals(that.reasoner)) return false;
            return objective.equals(that.objective);
        }
        @Override
        public int hashCode() {
            int result = reasoner.hashCode();
            result = 31 * result + objective.hashCode();
            return result;
        }

    }

    private class MarkedDataPoint {
        FeatureSelectionOrdering dataPoint;
        boolean marked = false;
        void clearMark() {
            marked = false;
        }
        void setData(FeatureSelectionOrdering data) {
            this.dataPoint = data;
        }
        void mark() {
            marked = true;
        }
        boolean isMarked() {
            return marked;
        }
        boolean isNotMarked() {
            return !marked;
        }
    }

    /*
     * BOILER PLATE CODE BELOW
     */

    @Override
    public void handleGUIEvent(GUIEvent guiEvent) {
        if (guiEvent instanceof ResetEvent || guiEvent instanceof GoToTimeStepEvent) {
            clearView();
        }
    }


    void clearView() {
        chartData.clear();
        Platform.runLater(() -> chart.getData().clear());
    }


    @Override
    public IGUIPluginController getController() {
        return this;
    }

    @Override
    public IGUIPluginModel getModel() {
        return this;
    }

    @Override
    public IGUIPluginView getView() {
        return this;
    }

    @Override
    public void setAlgorithmEventSource(AlgorithmEventSource source) {
        try{
            /*
             * Try to deregister first:
             */
            source.unregisterListener(this);
        } catch(Exception ex) {}
        source.registerListener(this);

    }

    @Override
    public void setGUIEventSource(GUIEventSource source) {
        try{
            /*
             * Try to deregister first:
             */
            source.unregisterListener(this);
        } catch(Exception ex) {}
        source.registerListener(this);
    }

    public static void runAndWait(Runnable action) {
        if (action == null)
            throw new NullPointerException("action");

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        // queue on JavaFX thread and wait for completion
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            // ignore exception
        }
    }
}
