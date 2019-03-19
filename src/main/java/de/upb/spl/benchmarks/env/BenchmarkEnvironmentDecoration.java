package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.BenchmarkReport;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.hasco.FM2CM;
import de.upb.spl.reasoner.SPLReasonerConfiguration;
import fm.FeatureModel;
import org.sat4j.core.VecInt;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

public class BenchmarkEnvironmentDecoration implements BenchmarkEnvironment {
    private final BenchmarkEnvironment env;


    public BenchmarkEnvironmentDecoration(BenchmarkEnvironment env) {
        this.env = env;
    }

    public FeatureModel model() {
        return env.model();
    }

    public FMSAT sat() {
        return env.sat();
    }

    public FM2CM componentModel() {
        return env.componentModel();
    }

    public List<String> objectives() {
        return env.objectives();
    }

    public List<VecInt> richSeeds() {
        return env.richSeeds();
    }

    public Future<JobReport> run(FeatureSelection selection, BenchmarkBill bill) {
        return env.run(selection, bill);
    }

    public Random generator() {
        return env.generator();
    }

    public boolean violatesConstraints(JobReport report) {
        return env.violatesConstraints(report);
    }

    @Override
    public BenchmarkReport reader(JobReport jobReport) {
        return env.reader(jobReport);
    }

    public SPLReasonerConfiguration configuration() {
        return env.configuration();
    }

    public BenchmarkBill currentTab() {
        return env.currentTab();
    }

    public boolean isRaw() {
        return env.isRaw();
    }

    @Override
    public BenchmarkEnvironment openTab(String reasoner) {
        return env.openTab(reasoner);
    }

    @Override
    public BenchmarkBill bill(String reasonerName) {
        return env.bill(reasonerName);
    }

    protected BenchmarkEnvironment getBaseEnv() {
        return env;
    }



}
