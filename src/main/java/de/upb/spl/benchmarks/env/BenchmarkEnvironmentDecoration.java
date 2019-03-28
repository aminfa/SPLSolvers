package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.ReportInterpreter;
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

    public <I> I getDecoration(Class<? extends BenchmarkEnvironment> clazz) {
        if(clazz.isInstance(this)) {
            return (I) this;
        }
        if(clazz.isInstance(env)) {
            return (I) env;
        } else if(env instanceof BenchmarkEnvironmentDecoration) {
            return ((BenchmarkEnvironmentDecoration) env).getDecoration(clazz);
        } else {
            return null;
        }
    }

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

    public Future<JobReport> run(FeatureSelection selection) {
        return env.run(selection);
    }

    public Random generator() {
        return env.generator();
    }

    public boolean violatesConstraints(JobReport report) {
        return env.violatesConstraints(report);
    }

    @Override
    public ReportInterpreter interpreter(JobReport jobReport) {
        return env.interpreter(jobReport);
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

    protected BenchmarkEnvironment getBaseEnv() {
        return env;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " - " +getBaseEnv().toString();
    }
}
