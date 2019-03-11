package de.upb.spl.reasoner;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Mutable;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({ "file:splreasoners.properties",
        "classpath:de/upb/spl/splreasoners.properties"})
public interface SPLReasonerConfiguration extends Mutable {
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
     * Hernard
     */
    @Key("de.upb.spl.SPLReasoner.Hernard.populationSize")
    @DefaultValue("30")
    Integer getHenardPopulationSize();

    /*
     * Universal
     */
    @Key("de.upb.spl.SPLReasoner.evaluations")
    @DefaultValue("50")
    Integer getEvaluationPermits();

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
    @DefaultValue("30")
    Integer getSayyadPopulationSize();

    /*
     * Henard
     */
    @Key("de.upb.spl.SPLReasoner.henard.p.smartMutation")
    @DefaultValue("0.01")
    Double getHenardSmartMutationProbability();

    @Key("de.upb.spl.SPLReasoner.henard.p.smartReplacement")
    @DefaultValue("0.003")
    Double getHenardSmartReplacementProbability();
}
