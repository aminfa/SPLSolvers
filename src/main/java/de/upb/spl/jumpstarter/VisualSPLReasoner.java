package de.upb.spl.jumpstarter;

import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.BenchmarkAgent;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.benchmarks.env.ConfiguredEnv;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.util.Cache;
import jaicore.graphvisualizer.events.recorder.AlgorithmEventHistoryRecorder;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VisualSPLReasoner {

    private final static Logger logger = LoggerFactory.getLogger(VisualSPLReasoner.class);

    private Cache<BenchmarkAgent> agent;

    private BenchmarkEnvironment env;

    private AlgorithmEventHistoryRecorder eventRecorder;

    private IGUIPlugin main;
    private List<IGUIPlugin> tabs = new ArrayList<>();


    private List<SPLReasoner> reasoners = new ArrayList<>();

    private List<Runnable> finishers = new ArrayList<Runnable>();
    private List<Runnable> exitFinishers = new ArrayList<Runnable>();

    private boolean parallelExecution = false;
    private boolean guiEnabled = true;
    private AlgorithmVisualizationWindow gui;

    public final void setup(Class<? extends VisualSPLReasoner> runnerClass) {
        Optional<Method> agentCreator = Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Agent.class))
                .findFirst();

        if(agentCreator.isPresent()) {
            try {
                BenchmarkAgent agent = (BenchmarkAgent) agentCreator.get().invoke(this);
                this.agent = Cache.of(agent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            agent = new Cache<>(() -> new BenchmarkAgent(10000));
        }

        Optional<Method> envCreator = Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Env.class))
                .filter(m->m.getAnnotation(Env.class).enabled())
                .findFirst();
        if(envCreator.isPresent()) {
            parallelExecution = envCreator.get().getAnnotation(Env.class).parallel();
            try {
                BenchmarkEnvironment env = (BenchmarkEnvironment) envCreator.get().invoke(this);
                setEnvironment(env);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if(!this.hasEnv()) {
            logger.error("Benchmark Environment wasn't initialized!");
            System.exit(1);
        }

        logger.info("Benchmark environment: {}", env);

        this.dumpSetting();

        if(runnerClass.isAnnotationPresent(GUI.class)) {
            guiEnabled = runnerClass.getAnnotation(GUI.class).enabled();
        }
        if(guiEnabled) {
            new JFXPanel();
            Arrays.stream(runnerClass.getMethods())
                    .filter(m -> m.isAnnotationPresent(GUI.class))
                    .filter(m->m.getAnnotation(GUI.class).enabled())
                    .sorted(Comparator.comparingInt(m->m.getAnnotation(GUI.class).order()))
                    .forEach(m-> {
                        GUI gui = m.getAnnotation(GUI.class);
                        IGUIPlugin plugin;
                        try {
                            plugin = (IGUIPlugin) m.invoke(this);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return;
                        }
                        if(gui.main()){
                            setMain(plugin);
                        } else {
                            addTab(plugin);
                        }
                    });
            this.createUI();
        }

        Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Reasoner.class))
                .filter(m->m.getAnnotation(Reasoner.class).enabled())
                .sorted(Comparator.comparingInt(m->m.getAnnotation(Reasoner.class).order()))
                .forEach(m-> {
                    try {
                        Object candidates =  m.invoke(this);
                        List<SPLReasoner> reasonerList;
                        if(candidates instanceof SPLReasoner[]) {
                            reasonerList = Arrays.stream((SPLReasoner[])candidates).collect(Collectors.toList());
                        } else if(candidates instanceof List) {
                            reasonerList = new ArrayList<>();
                            for(Object obj :  (List) candidates) {
                                if(obj instanceof SPLReasoner) {
                                    reasonerList.add((SPLReasoner) obj);
                                } else {
                                    logger.warn("Type of reasoner was not recognized: {}", obj.getClass().getName());
                                }
                            }
                        } else if(candidates instanceof SPLReasoner) {
                            reasonerList = Collections.singletonList((SPLReasoner)candidates);
                        } else {
                            logger.warn("Type of reasoner was not recognized: {}", candidates.getClass().getName());
                            return;
                        }
                        reasonerList.forEach(this::addReasoner);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                });

        Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Finish.class))
                .filter(m->m.getAnnotation(Finish.class).enabled())
                .sorted(Comparator.comparingInt(m->m.getAnnotation(Finish.class).order()))
                .forEach(m-> {
                    boolean runOnExit = m.getAnnotation(Finish.class).runOnExit();
                    try {
                        Object candidates = m.invoke(this);
                        List<Runnable> finishers;
                        if(candidates instanceof Runnable[]) {
                            finishers = Arrays.stream((Runnable[])candidates).collect(Collectors.toList());
                        } else if(candidates instanceof  List) {
                            finishers = new ArrayList<>();
                            for(Object obj :  (List) candidates) {
                                if(obj instanceof Runnable) {
                                    finishers.add((Runnable) obj);
                                } else {
                                    logger.warn("Type of finisher was not recognized: {}", obj.getClass().getName());
                                }
                            }
                        }
                        else if(candidates instanceof Runnable) {
                            Runnable finisher = (Runnable) candidates;
                            finishers = Collections.singletonList(finisher);
                        } else {
                            logger.error("Collector not recognized: " + candidates.getClass().getName());
                            return;
                        }
                        finishers.forEach(finisher -> addFinisher(finisher, runOnExit));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                });

        this.addFinishersToShutdownHook();

        this.start();
        this.finish(false);
//        this.gui.getStage().showAndWait();
    }

    private void setEnvironment(BenchmarkEnvironment env) {
        if(env.getDecoration(BookkeeperEnv.class) == null) {
            logger.warn("Benchmark environment doesn't have a book keeper. Decorating it with a new BookkeeperEnv");
            env = new BookkeeperEnv(env);
        }
        if(env.getDecoration(ConfiguredEnv.class) == null) {
            logger.warn("Benchmark environment doesn't have configuration decoration. Decorating it with a new ConfiguredEnv");
            env = new ConfiguredEnv(env);
        }
        this.env = env;
    }

    private void dumpSetting() {
        ByteArrayOutputStream config = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(config);
        env.configuration().list(out);
        out.close();
        logger.info("Run properties: \n {}", config.toString());
        logger.info("Runner = {}", this.getClass().getName());

    }

    private void addFinishersToShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                logger.info("Received shutdown signal.");
                if(!finished) {
                    finish(true);
                }
            }
        });
    }


    protected final boolean hasEnv() {
        return env != null;
    }

    public void createUI() {
        eventRecorder = new AlgorithmEventHistoryRecorder();
        if(main == null && tabs.size() > 0) {
            main = tabs.remove(0);
        }
        if(main == null) {
            guiEnabled = false;
            return;
        }

        final IGUIPlugin[] tabs = this.tabs.toArray(new IGUIPlugin[0]);
        logger.info("Creating GUI with {} many tabs.", tabs.length);
        gui = new AlgorithmVisualizationWindow(eventRecorder.getHistory(), main, tabs);
        Platform.runLater(gui);
    }

	private void addReasoner(SPLReasoner reasoner) {
        reasoners.add(reasoner);
    }


    protected void addFinisher(Runnable finisher, boolean runOnExit) {
        finishers.add(finisher);
        if(runOnExit) {
            exitFinishers.add(finisher);
        }
    }

    public void start() {
        logger.info("Starting spl benchmark.");
        Stream<SPLReasoner> splReasonerStream = parallelExecution ? reasoners.parallelStream() : reasoners.stream();
        BookkeeperEnv bookkeeper = bookkeeper();
        splReasonerStream.forEach(reasoner -> {
            BenchmarkEnvironment billedEnv =  bookkeeper.billedEnvironment(env(), reasoner.name());
            SPLReasonerAlgorithm alg = reasoner.algorithm(billedEnv);
            if(guiEnabled) {
                alg.registerListener(eventRecorder);
            }
            try {
                logger.info("Starting reasoner {}.", reasoner.name());
                alg.call();
                logger.info("Reasoner {} finished.", reasoner.name());
            } catch (Exception ex) {
                logger.error("Execution Error in algorithm {}: ", reasoner.name(), ex);
            }
        });
//        ExecutorService evaluatorService
        logger.info("Benchmark finished. Starting evaluations.");
    }

    boolean finished = false;

    public void finish(boolean exit) {
        logger.info("Performing finishers.");
        finished = true;
        for(Runnable finisher : (exit? exitFinishers : finishers)) {
            try {
                logger.info("Running finisher: " + finisher.toString());
                CompletableFuture<Void> finishFuture = CompletableFuture.runAsync(finisher);
                finishFuture.get(10, TimeUnit.MINUTES);
            } catch(Exception ex) {
                logger.error("Error while running finisher: " , ex);
            }
        }
    }

    private void addTab(IGUIPlugin plugin) {
        if (plugin == null) {
            return;
        }
        tabs.add(plugin);
    }

    private void setMain(IGUIPlugin plugin) {
        if(plugin == null) {
            return;
        }
        if(main != null) {
            addTab(main);
        }
        main = plugin;
    }

    public BenchmarkAgent agent() {
        return agent.get();
    }

    public BenchmarkEnvironment env() {
        return env;
    }

    public BookkeeperEnv bookkeeper() {
        return (env()).getDecoration(BookkeeperEnv.class);
    }

}
