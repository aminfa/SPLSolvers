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
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class BestCandidates   implements IGUIPlugin,  IGUIPluginController, IGUIPluginView, IGUIPluginModel {


    private final static Logger logger = LoggerFactory.getLogger(BestCandidates.class);

    private final BenchmarkEnvironment env;

    private final Parent rootNode;

    @FXML
    private ScatterChart<Number, Number> chart;

    @FXML
    private ComboBox<String> XAxisSelection;

    @FXML
    private ComboBox<String> YAxisSelection;

    private ObjectiveTuple currentObjectives = new ObjectiveTuple("", "");

    private final static String NAME = "SPL Reasoner Performance Timeline";

    private final static String FXML_RESOURCE = BestCandidates.class.getSimpleName() + ".fxml";

    private final
    Map<ObjectiveTuple, Map<String, XYChart.Series<Number, Number>>> chartData = new LinkedHashMap<>();

    private Insertion currentInsertion = new Insertion();

    public BestCandidates(BenchmarkEnvironment env) {
        this.env = env;
        /*
         * Load node from fxml resource:
         */
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(FileUtil.getResourceFile(FXML_RESOURCE).toURI().toURL());
            loader.setController(this);
            rootNode = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error while setting up gui: " + NAME, e);
        }

        fillData();
        fillComboBoxes();

        XAxisSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue != null && oldValue.equals(newValue)) {
                return;
            }
            currentObjectives.setX(newValue);
            reAddSeries();
        });
        YAxisSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue != null && oldValue.equals(newValue)) {
                return;
            }
            currentObjectives.setY(newValue);
            reAddSeries();
        });
    }

    private void reAddSeries() {
        Platform.runLater(() -> {
            chart.getData().clear();
            chartData.get(currentObjectives).forEach((reasoner, series) -> {
                chart.getData().add(series);
            });
        });
    }


    private void fillData() {
        runAndWait(() -> {
            for (int i = 0, size = env.objectives().size(); i < size; i++) {
                for (int j = 0; j < size; j++) {
                    ObjectiveTuple tuple = new ObjectiveTuple(env.objectives().get(i), env.objectives().get(j));
                    Map<String,XYChart.Series<Number, Number>> reasonerData = new LinkedHashMap<>();
                    chartData.put(tuple, reasonerData);
                }
            }
        });
    }

    private void fillComboBoxes() {
        runAndWait(() ->
        {
            List<String> selections = XAxisSelection.getItems();
            selections.clear();
            selections.addAll(env.objectives());
            if(selections.size() > 0) {
                String currentXAxis = selections.get(0);
                XAxisSelection.setValue(currentXAxis);
                currentObjectives.setX(currentXAxis);
            }
            selections = YAxisSelection.getItems();
            selections.clear();
            selections.addAll(env.objectives());
            String currentYAxis = "";
            if(selections.size() > 1) {
                currentYAxis = selections.get(1);
            } else {
                currentYAxis = selections.get(0);
            }
            currentObjectives.setY(currentYAxis);
            YAxisSelection.setValue(currentYAxis);
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

    private void insert(String category, ObjectiveTuple tuple, XYChart.Data<Number, Number> data) {
        XYChart.Series<Number, Number> series = chartData.get(tuple).get(category);
        if(series == null) {
            series = newSeries(tuple, category);
        }
        insertIntoView(series, data);
    }

    private XYChart.Series<Number, Number> newSeries(ObjectiveTuple tuple, String reasoner) {
        // defining a series
        XYChart.Series<Number, Number> performanceSeries = new XYChart.Series<>();
        performanceSeries.setName(reasoner);
        chartData.get(tuple).put(reasoner, performanceSeries);
        if(currentObjectives.equals(tuple)) {
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
        return NAME;
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
            String objectivex = env.objectives().get(i);
            for (int j = 0; j < size; j++) {
                String objectivey = env.objectives().get(j);
                ObjectiveTuple tuple = new ObjectiveTuple(env.objectives().get(i), env.objectives().get(j));
                Optional<Double> performancex = env.interpreter(event.getReport()).rawResult(objectivex);
                Optional<Double> performancey = env.interpreter(event.getReport()).rawResult(objectivey);
                if(!performancex.isPresent() || !performancey.isPresent()) {
                    logger.warn("Couldn't add data point for {} evaluation of {}. Performance is empty.", Iterators.ordinal(event.getEvaluationIndex()), event.getAlgorithmId());
                    return;
                } else {
                    XYChart.Data<Number, Number> data = new XYChart.Data<>(performancex.get(), performancey.get());
                    insert(reasoner, tuple,  data);
                }
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

    private class ObjectiveTuple {

        String objective1, objective2;

        private ObjectiveTuple(String objective1, String objective2) {
            this.objective1 = Objects.requireNonNull(objective1);
            this.objective2 = Objects.requireNonNull(objective2);

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ObjectiveTuple)) return false;

            ObjectiveTuple that = (ObjectiveTuple) o;

            if (!objective1.equals(that.objective1)) return false;
            return objective2.equals(that.objective2);
        }
        @Override
        public int hashCode() {
            int result = objective1.hashCode();
            result = 31 * result + objective2.hashCode();
            return result;
        }

        public String getX() {

            return objective1;
        }

        public void setX(String objective1) {
            this.objective1 = objective1;
        }

        public String getY() {
            return objective2;
        }

        public void setY(String objective2) {
            this.objective2 = objective2;
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
