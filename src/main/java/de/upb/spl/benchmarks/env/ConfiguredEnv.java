package de.upb.spl.benchmarks.env;

import de.upb.spl.reasoner.SPLReasonerConfiguration;
import org.aeonbits.owner.ConfigFactory;

public class ConfiguredEnv extends BenchmarkEnvironmentDecoration {

    private final SPLReasonerConfiguration configuration;

    public ConfiguredEnv(BenchmarkEnvironment env) {
        super(env);
        configuration = ConfigFactory.create(SPLReasonerConfiguration.class);
    }

    public SPLReasonerConfiguration configuration(){
        return configuration;
    }

}
