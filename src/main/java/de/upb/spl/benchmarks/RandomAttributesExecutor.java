package de.upb.spl.benchmarks;


import de.upb.spl.attributes.AbstractAttribute;
import de.upb.spl.attributes.AttributeConfiguration;
import de.upb.spl.attributes.FeatureAttribute;
import de.upb.spl.attributes.FeatureAttributeGenerator;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.upb.spl.util.DefaultMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RandomAttributesExecutor implements Function<FeatureAttribute, Double>, Runnable {
	public static final String GROUP = "random";
	private final static Logger logger = LoggerFactory.getLogger(RandomAttributesExecutor.class);
	private DefaultMap<FeatureAttribute, Double> cache = new DefaultMap<>(this);
	private final long seed = 0;
    private final FeatureAttributeGenerator generator = new FeatureAttributeGenerator(seed);

	private final static AtomicInteger GLOBAL_ID_COUNT = new AtomicInteger(0);
	private final Thread executor;
	private final String id;
	private final BenchmarkAgent agent;
	private final String group;
	private final Function<Map, List<String>> featureExtractor;
    private final AttributeConfiguration attributeConfiguration = ConfigFactory.create(AttributeConfiguration.class);
    public RandomAttributesExecutor(BenchmarkAgent agent) {
		this(agent, GROUP, config -> (List<String>) config.get("features"));
	}

	public RandomAttributesExecutor(BenchmarkAgent agent, String group, Function<Map, List<String>> featureExtractor) 	{
		id = String.format("random-attributes-%d", GLOBAL_ID_COUNT.getAndIncrement());
		this.agent = agent;
		this.group = group;
		this.featureExtractor = featureExtractor;
		executor = new Thread(this);
		executor.setDaemon(true);
		executor.setName(id);
		executor.setPriority(Thread.MIN_PRIORITY);
		executor.start();
	}

    @Override
    public Double apply(FeatureAttribute featureAttribute) {
        return generator.generate(featureAttribute);
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

	private void executeJob(JobReport report) throws Exception {
		Map configuration = report.getConfiguration();
		List<String> features = featureExtractor.apply(configuration);
		List<String> attributes = report.getObjectives();

		report.setResults(getJobResult(features, attributes));
	}

	private Collector<Double, ?, Double> aggregator(String aggregationMethod) {
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

			AbstractAttribute attribute = AbstractAttribute.createFromConfig(attributeConfiguration, attributeName);
			Collector<Double, ?, Double> aggregator = aggregator(attribute.getAggregationMethod());

			double result =
					features.stream()
							.map(featureName -> cache.get(new FeatureAttribute(featureName, attribute)))
                            .collect(aggregator);
			results.put(attributeName, result);
		}
		return results;
	}



	public void run() {
		logger.info("`{}` started.", id);
		while(true) {
			try{
				executeJob();
			}catch(Exception ex) {
				logger.error("`{}` unexpected error while executing job:\n ", id, ex);
			}
		}
	}

}
