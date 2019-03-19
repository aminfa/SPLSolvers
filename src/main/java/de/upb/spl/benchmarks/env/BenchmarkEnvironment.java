package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.hasco.FM2CM;
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

	FM2CM componentModel();

	List<String> objectives();

	List<VecInt> richSeeds();

    default Future<JobReport> run(FeatureSelection selection) {
        return run(selection, currentTab());
    }

    Future<JobReport> run(FeatureSelection selection, BenchmarkBill bill);


	Random generator();

	default boolean violatesConstraints(JobReport report){
	    return false;
    }

    BenchmarkReport reader(JobReport jobReport);


	default SPLReasonerConfiguration configuration(){
	    return ConfigFactory.create(SPLReasonerConfiguration.class);
    }

    BenchmarkBill currentTab();

    default boolean isRaw() {
        return false;
    }

    BenchmarkEnvironment openTab(String reasoner);

    BenchmarkBill bill(String reasonerName);
}
