package de.upb.spl.attributes;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Mutable;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({ "file:attribute.properties",
        "classpath:de/upb/spl/attribute.properties"})
public interface AttributeConfiguration extends Mutable {

    @Key("de.upb.spl.AttributeConfiguration.attributeName")
    String getAttributeName();

    @Key("de.upb.spl.AttributeConfiguration.distribution")
    @DefaultValue("Uniform")
    String getDistributionType();

    @Key("de.upb.spl.AttributeConfiguration.Gaussian.mean")
    @DefaultValue("0.")
    Double getMean();

    @Key("de.upb.spl.AttributeConfiguration.Gaussian.deviation")
    @DefaultValue("1.")
    Double getDeviation();

    @Key("de.upb.spl.AttributeConfiguration.Uniform.rangeStart")
    @DefaultValue("0.")
    Double getRangeStart();

    @Key("de.upb.spl.AttributeConfiguration.Uniform.rangeEnd")
    @DefaultValue("1.")
    Double getRangeEnd();

    @Key("de.upb.spl.AttributeConfiguration.Bernoulli.p")
    @DefaultValue("0.5")
    Double getBernoulliP();

    @Key("de.upb.spl.AttributeConfiguration.aggregationMethod")
    @DefaultValue("sum")
    String getAggregationMethod();

    @Key("de.upb.spl.AttributeConfiguration.minimized")
    @DefaultValue("true")
    Boolean toBeMinimized();

}
