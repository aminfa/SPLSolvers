package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.hasco.FM2CM;
import fm.FeatureModel;
import org.sat4j.core.VecInt;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

public class BaseEnv implements BenchmarkEnvironment {
    @Override
    public FeatureModel model() {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public FMSAT sat() {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public FM2CM componentModel() {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public List<String> objectives() {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public List<VecInt> richSeeds() {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public Future<JobReport> run(FeatureSelection selection, BenchmarkBill bill) {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public Random generator() {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public ReportInterpreter interpreter(JobReport jobReport) {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public BenchmarkBill currentTab() {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }

    @Override
    public BenchmarkBill bill(String reasonerName) {
        throw new UnsupportedOperationException("Base Environment needs to be decorated to support this operation.");
    }
}
