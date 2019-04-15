package de.upb.spl.hasco;

import com.google.common.eventbus.Subscribe;
import de.upb.spl.FeatureSelection;
import de.upb.spl.ailibsintegration.FeatureSelectionOrdering;
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

import java.util.Collections;
import java.util.concurrent.TimeoutException;

public class HASCOSPLReasoner implements SPLReasoner {
    public static final String DUMMY_COMPONENT = "DUMMY";
    public static final String NAME = "HASCO";

    private final String name;

    public HASCOSPLReasoner() {
        this.name = NAME;
    }


    public HASCOSPLReasoner(String name, boolean isSuffix) {
        this.name = ((isSuffix ? NAME : "") + name);
    }

    @Override
    public SPLReasonerAlgorithm algorithm(BenchmarkEnvironment env) {
        return new HASCOWrap(env);
    }

    @Override
    public String name() {
        return name;
    }

    class FeatureSelectionProblem extends SoftwareConfigurationProblem<FeatureSelectionOrdering> {
        private final BenchmarkEnvironment env;
        public FeatureSelectionProblem(BenchmarkEnvironment env) {
            super(env.componentModel().getComponents(), env.componentModel().rootInterface(), new FeatureComponentEvaluator(env));
            this.env = env;
        }
    }

    class HASCOWrap extends SPLReasonerAlgorithm {
        private final HASCOViaFDAndBestFirst<FeatureSelectionOrdering> hasco;
        public HASCOWrap(BenchmarkEnvironment env) {
            super(env, name());
            /*
             * Create HASCO factory.
             */
            HASCOViaFDAndBestFirstFactory<FeatureSelectionOrdering> hascoFactory =
                    new HASCOViaFDAndBestFirstFactory<>();
            if(env.configuration().getHascoRandomSearch()) {
                /*
                 * Create random node evaluator:
                 */
                long seed = env.seed();
                long seedUpper = seed >> 32;
                long seedLower = seed & 0xffffffffL;
                long compressedSeed = seedLower ^ seedUpper;
                GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformerViaRDFS <TFDNode, String, FeatureSelectionOrdering>
                        randomNodes =
                        new GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformerViaRDFS<>(
                                null, // null safe
                                node -> {
                                    if(node == null) {
                                        return false;
                                    }
                                     if(    node.getAppliedMethodInstance() == null ||
                                            node.getAppliedMethodInstance().getMethod() == null ||
                                            node.getAppliedMethodInstance().getMethod().getName() == null) {
                                        if(node.getAppliedAction() == null) {
                                            return false;
                                        } else {
                                            return (node.getAppliedAction().getOperation().getName().contains(DUMMY_COMPONENT));
                                        }
                                    }
                                    return node.getAppliedMethodInstance().getMethod().getName().contains(DUMMY_COMPONENT);
                                }, // null safe
                                (int) compressedSeed,
                                env.configuration().getHascoRandomSearchSamples(),
                                env.configuration().getHascoTimeoutForEval(),
                                env.configuration().getHascoTimeoutForNodeEval());
                hascoFactory.setSearchProblemTransformer(randomNodes);
            } else {
                /*
                 * DFS with 0 heuristik
                 */
                GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformer <TFDNode, String, FeatureSelectionOrdering>
                        dfs =
                        new GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformer<>(
                                n -> CMUtil.getBestPerformance(env));
                hascoFactory.setSearchProblemTransformer(dfs);
            }

            /*
             * Provide the component model and an empty map for feature parameters; there are no parameters to be refined.
             */
            RefinementConfiguredSoftwareConfigurationProblem<FeatureSelectionOrdering> problem
                    = new RefinementConfiguredSoftwareConfigurationProblem<FeatureSelectionOrdering>(
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
