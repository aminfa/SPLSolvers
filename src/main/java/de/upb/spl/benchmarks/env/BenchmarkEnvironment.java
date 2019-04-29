package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.hasco.FM2CM;
import de.upb.spl.reasoner.SPLReasonerConfiguration;
import fm.FeatureModel;
import org.sat4j.core.VecInt;

import java.util.List;
import java.util.concurrent.Future;

public interface BenchmarkEnvironment {

	FeatureModel model();

	FMSAT sat();

	FM2CM componentModel();

	List<String> objectives();

	List<VecInt> richSeeds();

    Future<JobReport> run(FeatureSelection selection);

    Long seed();

    ReportInterpreter interpreter(JobReport jobReport);

	SPLReasonerConfiguration configuration();

    BenchmarkBill currentTab();

    default boolean isRaw() {
        return false;
    }

    default boolean violatesDemand() {
        return false;
    }

    <I extends BenchmarkEnvironment> I getDecoration(Class<I> clazz);
}
