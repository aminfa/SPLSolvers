package de.upb.spl.benchmarks;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JobApplication implements Predicate<JobReport> {
	private final String workerId;
	private final String group;
	private final List<List<String>> cache;

	public static final Type type = new TypeToken<JobApplication>(){}.getType();

	public JobApplication(String workerId, String group, List<List<String>> cache) {
		this.workerId = workerId;
		this.group = group;
		this.cache = cache;
	}

	public String getWorkerId() {
		if(workerId != null)
			return workerId;
		throw new IllegalStateException("Worker Id is not set.");
	}

	public String getGroup() {
		return group;
	}

	public List<List<String>> getCache() {
		return cache;
	}



	public int cacheRating(List<String> configHash) {
		int min = Math.min(configHash.size(), cache.size());
		int cacheHits = 0;
		for (int i = 0; i < min; i++) {
			if(cache.get(i).contains(configHash.get(i))) {
				cacheHits ++;
			}
		}
		return cacheHits;
	}


	@Override
	public String toString() {
		return "JobApplication{" +
				"workerId='" + workerId + '\'' +
				", group='" + group + '\'' +
				", cache-count=" + cache.stream().map(List::size).mapToInt(i -> i).sum() +
				'}';
	}

	@Override
	public boolean test(JobReport jobReport) {
		return !jobReport.getWorkerId().isPresent() && jobReport.getGroup().equals(group);
	}


}
