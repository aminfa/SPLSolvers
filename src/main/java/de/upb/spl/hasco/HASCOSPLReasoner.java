package de.upb.spl.hasco;

import com.google.common.eventbus.Subscribe;
import de.upb.spl.FeatureSelection;
import de.upb.spl.ailibsintegration.FeatureSelectionPerformance;
import de.upb.spl.ailibsintegration.FeatureComponentEvaluator;
import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.reasoner.SPLReasoner;
import hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import hasco.core.SoftwareConfigurationProblem;
import hasco.model.ComponentInstance;
import hasco.variants.forwarddecomposition.HASCOViaFDAndBestFirst;
import hasco.variants.forwarddecomposition.HASCOViaFDAndBestFirstFactory;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.AlgorithmState;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.events.AlgorithmFinishedEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import jaicore.search.problemtransformers.GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformer;
import jaicore.search.problemtransformers.GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformerViaRDFS;
import org.moeaframework.core.Solution;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

public class HASCOSPLReasoner implements SPLReasoner {
    public static final String DUMMY_COMPONENT = "DUMMY";
    public static final String NAME = "HASCO";

    @Override
    public SPLReasonerAlgorithm algorithm(BenchmarkEnvironment env) {
        return new HASCOWrap(env);
    }

    @Override
    public Collection<FeatureSelection> search(BenchmarkEnvironment env) {
        return null;
    }

    @Override
    public FeatureSelection assemble(BenchmarkEnvironment env, Solution solution) {
        return null;
    }

    @Override
    public String name() {
        return NAME;
    }

    class FeatureSelectionProblem extends SoftwareConfigurationProblem<FeatureSelectionPerformance> {
        private final BenchmarkEnvironment env;
        public FeatureSelectionProblem(BenchmarkEnvironment env) {
            super(env.componentModel().getComponents(), env.componentModel().rootInterface(), new FeatureComponentEvaluator(env));
            this.env = env;
        }
    }

    class HASCOWrap extends SPLReasonerAlgorithm {
        private final HASCOViaFDAndBestFirst<FeatureSelectionPerformance> hasco;
        public HASCOWrap(BenchmarkEnvironment env) {
            super(env, name());
            /*
             * Create HASCO factory.
             */
            HASCOViaFDAndBestFirstFactory<FeatureSelectionPerformance> hascoFactory =
                    new HASCOViaFDAndBestFirstFactory<>();
            if(env.configuration().getHascoRandomSearch()) {
                /*
                 * Create random node evaluator:
                 */
                GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformerViaRDFS <TFDNode, String, FeatureSelectionPerformance>
                        randomNodes =
                        new GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformerViaRDFS<>(
                                null, // null safe
                                null, // null safe
                                env.generator().nextInt(),
                                env.configuration().getHascoRandomSearchSamples(),
                                env.configuration().getHascoTimeoutForEval(),
                                env.configuration().getHascoTimeoutForNodeEval());
                hascoFactory.setSearchProblemTransformer(randomNodes);
            } else {
                /*
                 * DFS with 0 heuristik
                 */
                GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformer <TFDNode, String, FeatureSelectionPerformance>
                        dfs =
                        new GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformer<>(
                                n -> CMUtil.getBestPerformance(env));
                hascoFactory.setSearchProblemTransformer(dfs);
            }

            /*
             * Provide the component model and an empty map for feature parameters; there are no parameters to be refined.
             */
            RefinementConfiguredSoftwareConfigurationProblem<FeatureSelectionPerformance> problem
                    = new RefinementConfiguredSoftwareConfigurationProblem<FeatureSelectionPerformance>(
                    new FeatureSelectionProblem(env), Collections.EMPTY_MAP);
            hascoFactory.setProblemInput(problem);
            /*
             * Create hasco algorithm:
             */
             hasco = hascoFactory.getAlgorithm();
             hasco.registerListener(new Object() {
                 @Subscribe
                 public void repost(AlgorithmEvent event) {
                     post(event);
                 }
             });
        }

        @Override
        protected void init() throws InterruptedException, TimeoutException, AlgorithmExecutionCanceledException, AlgorithmException {
            hasco.nextWithException();
            if(hasco.getState() == AlgorithmState.active) {
                activate();
            }
        }

        @Override
        protected void proceed() throws InterruptedException, TimeoutException, AlgorithmExecutionCanceledException, AlgorithmException {
            hasco.nextWithException();
//            if(hasco.getState() == AlgorithmState.inactive) {
//                terminate();
//            }
        }

        @Override
        protected FeatureSelection best() {
            ComponentInstance instance = hasco.getBestSeenSolution().getComponentInstance();
            return getInput().componentModel().transform(instance);
        }

        public AlgorithmFinishedEvent terminate() {
            hasco.cancel();
            return super.terminate();
        }
    }


}
