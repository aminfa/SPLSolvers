package de.upb.spl.benchmarks.env;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.BenchmarkReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Bookkeeper extends BenchmarkEnvironmentDecoration {

    private final static Logger logger = LoggerFactory.getLogger(Bookkeeper.class);
    private Map<String, BenchmarkBill> books = new HashMap<>();

    private final BenchmarkBill offBooksTab = new BenchmarkBill(null) {
        public void logEvaluation(FeatureSelection selection, BenchmarkReport report) {
            // log nothing.
        }
    };

    public Bookkeeper(BenchmarkEnvironment env) {
        super(env);
    }

    @Override
    public BenchmarkBill currentTab() {
        return offBooksTab;
    }

    public synchronized BenchmarkBill bill(String clientName) {
        if(clientName == null) {
            return offBooksTab;
        }
        return books.computeIfAbsent(clientName, newClient-> {
            logger.info("Creating a new bill for {}.", newClient);
            return new BenchmarkBill(newClient);
        });
    }

    public BenchmarkEnvironment openTab(String clientName) {
        logger.info("Opening a tab for {}.", clientName);
        BenchmarkBill newBill = bill(clientName);
        if(currentTab() == newBill) {
            return this;
        } else {
            return new Billed(this, newBill);
        }
    }

    private static class Billed extends BenchmarkEnvironmentDecoration {

        private final BenchmarkBill bill;

        public Billed(BenchmarkEnvironment env, BenchmarkBill bill) {
            super(env);
            this.bill = bill;
        }


        public BenchmarkBill currentTab() {
            return bill;
        }
    }

}
