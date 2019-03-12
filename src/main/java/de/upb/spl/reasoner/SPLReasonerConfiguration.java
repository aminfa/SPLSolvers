package de.upb.spl.reasoner;

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
    @DefaultValue("50")
    Integer getEvaluationPermits();

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
    @DefaultValue("0.005")
    Double getBasicIbeaBitFlipProbability();

    @Key("de.upb.spl.SPLReasoner.ibea.p.singlePointCO")
    @DefaultValue("0.1")
    Double getBasicIbeaSinglePointCrossoverProbability();

    /*
     * Sayyad
     */
    @Key("de.upb.spl.SPLReasoner.sayyad.populationSize")
    @DefaultValue("30")
    Integer getSayyadPopulationSize();


    @Key("de.upb.spl.SPLReasoner.sayyad.seedCount")
    @DefaultValue("10")
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
    @DefaultValue("0.001")
    Double getHenardSmartReplacementProbability();

    @Key("de.upb.spl.SPLReasoner.Hernard.seedCount")
    @DefaultValue("0")
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
    @DefaultValue("0.001")
    Double getHieronsSmartReplacementProbability();

    @Key("de.upb.spl.SPLReasoner.Hierons.seedCount")
    @DefaultValue("0")
    Integer getHieronsSeedCount();
}
