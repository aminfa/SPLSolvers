package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import de.upb.spl.reasoner.SPLReasonerConfiguration;
import de.upb.spl.benchmarks.BenchmarkReport;
import fm.FeatureModel;
import org.aeonbits.owner.ConfigFactory;
import org.sat4j.core.VecInt;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

public interface BenchmarkEnvironment {

	FeatureModel model();

	FMSAT sat();

	List<String> objectives();

	List<VecInt> richSeeds();

    Future<BenchmarkReport> run(FeatureSelection selection, String clientName);

    default Future<BenchmarkReport> runAnonymously(FeatureSelection selection){
        return run(selection, null);
    }

	Random generator();

	default boolean violatesConstraints(BenchmarkReport report){
	    return false;
    }

	default SPLReasonerConfiguration configuration(){
	    return ConfigFactory.create(SPLReasonerConfiguration.class);
    }

    BenchmarkBill bill(String clientName);
}
