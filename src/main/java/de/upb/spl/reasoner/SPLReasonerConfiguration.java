package de.upb.spl.reasoner;

import jmetal.encodings.variable.Int;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Mutable;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({ "file:splreasoners.properties",
        "classpath:de/upb/spl/splreasoners.properties"})
public interface SPLReasonerConfiguration extends Mutable {

    /*
     * Universal
     */

    @Key("de.upb.spl.SPLReasoner.evaluations")
    @DefaultValue("100")
    Integer getEvaluationPermits();

    @Key("de.upb.spl.videoEncoding.RAWSourceFile")
    @DefaultValue("stockholm")
    String getVideoSourceFile();


    /*
     * Guo
     */
    @Key("de.upb.spl.SPLReasoner.Guo11.populationSize")
    @DefaultValue("30")
    Integer getGUOPopulationSize();

    @Key("de.upb.spl.SPLReasoner.Guo11.d")
    @DefaultValue("0.6")
    Double getGUOD();


    /*
     * Basic Ibea
     */
    @Key("de.upb.spl.SPLReasoner.ibea.populationSize")
    @DefaultValue("30")
    Integer getBasicIbeaPopulationSize();
    @Key("de.upb.spl.SPLReasoner.ibea.indicator")
    @DefaultValue("epsilon")
    String getBasicIbeaIndicator();

    @Key("de.upb.spl.SPLReasoner.ibea.p.bitflip")
    @DefaultValue("0.001")
    Double getBasicIbeaBitFlipProbability();

    @Key("de.upb.spl.SPLReasoner.ibea.p.singlePointCO")
    @DefaultValue("0.01")
    Double getBasicIbeaSinglePointCrossoverProbability();

    /*
     * Sayyad
     */
    @Key("de.upb.spl.SPLReasoner.sayyad.populationSize")
    @DefaultValue("50")
    Integer getSayyadPopulationSize();


    @Key("de.upb.spl.SPLReasoner.sayyad.seedCount")
    @DefaultValue("1") // seed
    Integer getSayyadSeedCount();

    /*
     * Hernard
     */
    @Key("de.upb.spl.SPLReasoner.Hernard.populationSize")
    @DefaultValue("30")
    Integer getHenardPopulationSize();

    @Key("de.upb.spl.SPLReasoner.Henard.p.smartMutation")
    @DefaultValue("0.01")
    Double getHenardSmartMutationProbability();

    @Key("de.upb.spl.SPLReasoner.Henard.p.smartReplacement")
    @DefaultValue("0.01")
    Double getHenardSmartReplacementProbability();

    @Key("de.upb.spl.SPLReasoner.Hernard.seedCount")
    @DefaultValue("1") // seed
    Integer getHernardSeedCount();

    /*
     * Hierons
     */
    @Key("de.upb.spl.SPLReasoner.Hierons.populationSize")
    @DefaultValue("30")
    Integer getHieronsPopulationSize();

    @Key("de.upb.spl.SPLReasoner.Hierons.p.smartMutation")
    @DefaultValue("0.01")
    Double getHieronsSmartMutationProbability();

    @Key("de.upb.spl.SPLReasoner.Hierons.p.smartReplacement")
    @DefaultValue("0.01")
    Double getHieronsSmartReplacementProbability();

    @Key("de.upb.spl.SPLReasoner.Hierons.seedCount")
    @DefaultValue("1") // seed
    Integer getHieronsSeedCount();

    /*
     * HASCO
     */
    @Key("de.upb.spl.SPLReasoner.Hasco.randomSearch")
    @DefaultValue("true")
    Boolean getHascoRandomSearch();

    @Key("de.upb.spl.SPLReasoner.Hasco.randomSearchSamples")
    @DefaultValue("3")
    Integer getHascoRandomSearchSamples();

    @Key("de.upb.spl.SPLReasoner.Hasco.evalTimeout")
    @DefaultValue("600000") // 600 seconds ~ 10 minutes
    Integer getHascoTimeoutForEval();

    @Key("de.upb.spl.SPLReasoner.Hasco.nodeEvalTimeout")
    @DefaultValue("600000") // 600 seconds
    Integer getHascoTimeoutForNodeEval();

    /*
     * Replay
     */
    @Key("de.upb.spl.SPLReasoner.Replay.rerunSelection")
    @DefaultValue("false")
    Boolean getReplayRerunSelection();
}
