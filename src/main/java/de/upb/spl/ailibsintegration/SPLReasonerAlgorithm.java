package de.upb.spl.ailibsintegration;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkBill;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.reasoner.EAReasoner;
import de.upb.spl.reasoner.SPLEvaluator;
import jaicore.basic.algorithm.AAlgorithm;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.events.ASolutionCandidateFoundEvent;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.events.AlgorithmFinishedEvent;
import jaicore.basic.algorithm.events.AlgorithmInitializedEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

public abstract class SPLReasonerAlgorithm extends AAlgorithm<BenchmarkEnvironment, FeatureSelection> {

    private final static Logger logger = LoggerFactory.getLogger(EAReasoner.class);
    private final String name;


    private int lastEvalCountLog = -1;
    private int nextEvalIndex = 0;

    public SPLReasonerAlgorithm(BenchmarkEnvironment env,  String name) {
        super(env);
        this.name = name;

    }



    @Override
    public final String getId() {
        return name;
    }

    @Override
    public final AlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, TimeoutException, AlgorithmException {
        switch(getState()) {
            case created:
                try {
                    init();
                    return new AlgorithmInitializedEvent(getId());
                } catch(Exception ex) {
                    throw new AlgorithmException(ex, "Error while initializing in: " + getId());
                }
            case active:
                AlgorithmEvent event = step();
                post(event);
                return event;
            case inactive:
                throw new AlgorithmException("Algorithm is inactive.");
            default:
                return new AlgorithmFinishedEvent(getId());
        }
    }

    @Override
    public final FeatureSelection call() throws InterruptedException, AlgorithmExecutionCanceledException, TimeoutException, AlgorithmException {
        while (hasNext()) {
            next();
        }
        try{
            return best();
        } catch (IllegalArgumentException ex) {
            throw new AlgorithmException(ex, "Error selecting best performer: " + getId());
        }
    }

    /**
     * Advances the Reasoner until a never-seen-before candidate has been evaluated.
     * When multiple new candidates are evaluated in one proceed step, they are returned separately in each step invocation.
     * Returns candidates in the order they were found.
     * Ends the algorithm if the evaluation permits are reached.
     *
     * @return Candidate solution found by ea.
     * @throws AlgorithmException error when running ea algorithm
     */
    private final ASolutionCandidateFoundEvent step() throws AlgorithmException {
        int evals = getInput().configuration().getEvaluationPermits();
        int currentEvals = getInput().bill(name).getEvaluationsCount();
        if(nextEvalIndex > evals) {
            throw new AlgorithmException("Algorithm already finished.");
        }
        if(currentEvals > lastEvalCountLog) {
            lastEvalCountLog = currentEvals;
            logger.debug("{} performing evaluation {}/{}.", name, currentEvals, evals);
        }
        while(nextEvalIndex == currentEvals) {
            try {
                proceed();
            } catch(Exception ex) {
                throw new AlgorithmException(ex, "Error while proceeding in: " + getId());
            }
            currentEvals = getInput().bill(name).getEvaluationsCount();
        }
        BenchmarkBill.Log log = getInput().bill(name).checkLog(nextEvalIndex);

        double[] evaluation = SPLEvaluator.evaluateFeatureSelection(getInput(), log.selection(), null, true);
        FeatureSelectionPerformance reevaluatedPerformance = new FeatureSelectionPerformance(0, evaluation);
        nextEvalIndex++;
        if(nextEvalIndex > evals) {
            terminate();
        }

        return new FeatureSelectionEvaluatedEvent(getId(), log.selection(), nextEvalIndex-1, reevaluatedPerformance);
    }

    /**
     * Initialize Algorithm.
     * This method will be called until it executes `activate()`.
     * @throws Exception initialization error.
     */
    protected abstract void init() throws Exception;

    /**
     * Proceed in the algorithm.
     * This method will be called until it executes `terminate()`.
     * @throws Exception error while running the algorithm.
     */
    protected abstract void proceed() throws Exception;

    /**
     * Calculates the best candidate solution.
     *
     * @return
     */
    protected abstract FeatureSelection best();

}