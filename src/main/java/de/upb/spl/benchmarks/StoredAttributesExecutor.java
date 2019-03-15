package de.upb.spl.benchmarks;


import com.tdunning.math.stats.AVLTreeDigest;
import com.tdunning.math.stats.TDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StoredAttributesExecutor implements  Runnable {
	public static final String GROUP = "fixed-attributes";
	private final static Logger logger = LoggerFactory.getLogger(StoredAttributesExecutor.class);

	private final static AtomicInteger GLOBAL_ID_COUNT = new AtomicInteger(0);
	private final Thread executor;
	private final String id;
	private final BenchmarkAgent agent;
	private final String group;
	private final Function<Map, List<String>> featureExtractor;
	private final Map attributeValues;

	public StoredAttributesExecutor(BenchmarkAgent agent, Map attributeValues) {
		this(agent, GROUP, config -> (List<String>) config.get("features"), attributeValues);
	}

	public StoredAttributesExecutor(BenchmarkAgent agent, String group, Function<Map, List<String>> featureExtractor, Map attributeValues) 	{
		id = String.format("fixed-attributes-%d", GLOBAL_ID_COUNT.getAndIncrement());
		this.agent = agent;
		this.group = group;
		this.featureExtractor = featureExtractor;
		this.attributeValues = attributeValues;
		executor = new Thread(this);
		executor.setDaemon(true);
		executor.setName(id);
		executor.setPriority(Thread.MIN_PRIORITY);
		executor.start();
	}

	private JobApplication getJobApplication () {
		JobApplication application = new JobApplication(id, group, Collections.emptyList());
		return application;
	}
	private void executeJob() throws InterruptedException {
		logger.info("Asking agent for job");
		JobReport report = agent.jobs().waitForJob(getJobApplication());
		try {
			executeJob(report);
		} catch (Exception ex) {
			logger.error("{} error while executing job with id {}: ", id, report.getJobId(), ex);
			report.setResultsIfNull();
		} finally {
			agent.jobs().update(report);
		}
	}

	public void executeJob(JobReport report) throws Exception {
		Map configuration = report.getConfiguration();
		List<String> features = featureExtractor.apply(configuration);
		List<String> attributes = report.getObjectives();
		report.setResults(getJobResult(features, attributes));
	}

	private Collector<Double, ?, Double> aggregator(String aggregationMethod) {
	    if(aggregationMethod.endsWith("percentile")) {
	        final double percentage;
            if(aggregationMethod.equals("5-percentile")) {
                percentage = 0.05;
            }
            else if(aggregationMethod.equals("10-percentile")) {
                percentage = 0.1;
            }
             else if(aggregationMethod.equals("15-percentile")) {
                percentage = .15;
            }
            else {
                percentage = 0.2;
            }

            return new Collector<Double, TDigest, Double>() {
                @Override
                public Supplier<TDigest> supplier() {
                    return () -> new AVLTreeDigest(100);
                }

                @Override
                public BiConsumer<TDigest, Double> accumulator() {
                    return (tDigest, value) -> {
                        tDigest.add(value);
                    };
                }

                @Override
                public BinaryOperator<TDigest> combiner() {
                    return (tDigest1, tDigest2) ->  {
                        tDigest1.add(tDigest2);
                        return tDigest1;
                    };
                }

                @Override
                public Function<TDigest, Double> finisher() {
                    return tDigest -> tDigest.quantile(percentage);
                }

                @Override
                public Set<Characteristics> characteristics() {
                    return Collections.singleton(Characteristics.UNORDERED);
                }
            };
        }
		if(aggregationMethod.equals("max")) {
			return Collectors.collectingAndThen(
					Collectors.maxBy(Double::compare),
					o -> o.orElse(0d));
		} if(aggregationMethod.equals("min")) {

			return Collectors.collectingAndThen(
					Collectors.minBy(Double::compare),
					o -> o.orElse(0d));

		} if(aggregationMethod.equals("mean")) {
			return Collectors.averagingDouble(v -> v);
		} else {
			/*
			 * Default to sum:
			 */
			return Collectors.<Double>summingDouble(v -> v);
		}
	}


	private Map getJobResult(List<String> features, List<String> attributes) {
		Map<String, Double> results = new HashMap<>();

		for(int attrIndex = 0, attrCount = attributes.size();
				attrIndex < attrCount;
				attrIndex++) {
			String attributeName = attributes.get(attrIndex);
			String aggregationMethod = getAggregationMethod(attributeName);
			Collector<Double, ?, Double> aggregator = aggregator(aggregationMethod);
			Map<String, Double> values = values(attributeName);
			if(values == null) {
			    continue;
            }
			double result =
					features.stream()
							.map(values::get)
							.collect(aggregator);
			results.put(attributeName, result);
		}
		return results;
	}

    private Map attributeMap(String attributeName) {
	    return (Map) attributeValues.get(attributeName);
    }

	private Map<String, Double> values(String attributeName) {
        Map attributeMap = attributeMap(attributeName);
        if(attributeMap == null) {
            return null;
        } else {
            return (Map<String, Double>) attributeMap.get("values");
        }
    }

    private String getAggregationMethod(String attributeName) {
        Map attributeMap = attributeMap(attributeName);
        if(attributeMap == null) {
            return "sum";
        } else {
            String aggregateMethod = (String) attributeMap.getOrDefault("aggregation", "sum");
            return aggregateMethod;
        }
    }

    public void run() {
		logger.info("`{}` started.", id);
		if(agent == null) {
            logger.warn("Cannot execute because agent is null. id={}", id);
		    return;
        }
		while(true) {
			try{
				executeJob();
			}catch(Exception ex) {
				logger.error("`{}` unexpected error while executing job:\n ", id, ex);
			}
		}
	}

}
