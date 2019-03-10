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
    @DefaultValue("300")
    Integer getEvaluationPermits();

    /*
     * Basic Ibea
     */
    @Key("de.upb.spl.SPLReasoner.ibea.populationSize")
    @DefaultValue("30")
    Integer getBasicIbeaPopulationSize();

    @Key("de.upb.spl.SPLReasoner.ibea.indicator")
    @DefaultValue("hypervolume")
    String getBasicIbeaIndicator();

    @Key("de.upb.spl.SPLReasoner.sayyad.populationSize")
    @DefaultValue("30")
    Integer getSayyadPopulationSize();
}
