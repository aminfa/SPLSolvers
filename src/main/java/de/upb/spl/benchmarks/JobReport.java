package de.upb.spl.benchmarks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;

public class JobReport {

	private final Map report = new HashMap<>();

	private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public JobReport() {
		report.put("jobId", DigestUtils.sha256Hex(UUID.randomUUID().toString()).substring(0, 5));
	}


	public JobReport(Map report) {
		update(report);
		if(getJobId() == null) {
			this.report.put("jobId", DigestUtils.sha256Hex(UUID.randomUUID().toString()));
		}
	}

	public String getJobId() {
		return (String) report.get("jobId");
	}

	public void update(Map jobReport) {
		this.report.putAll(jobReport);
	}


	public String getGroup() {
		return (String) report.get("group");
	}

	public void setGroup(String group) {
		report.put("group", group);
	}


	public Optional<String> getWorkerId() {
		return Optional.ofNullable((String) report.get("workerId"));
	}


	public void setWorkerId(String workerId) {
		report.put("workerId", workerId);
	}

	public void setConfiguration(Object configuration, String... hashEntries) {
		report.put("configuration", configuration);
		setConfigHashRoots(hashEntries);
	}

	public Map<String, ?> getConfiguration() {
		return (Map) report.get("configuration");
	}

	public List<String> getErrors() {
		List<String> errorList = (List<String>) report.get("errors");
		if(errorList == null) {
			errorList = new ArrayList<>();
			report.put("errors", errorList);
		}
		return errorList;
	}

	public void addError(String s) {
		getErrors().add(s);
	}

	public Map getJsonObj() {
	    return report;
    }

	public String jsonSerialization() {
		return gson.toJson(report);
	}

	public Optional<Map> getResults() {
		return Optional.ofNullable((Map)report.get("results"));
	}

	public void setResultsIfNull() {
		if( !getResults().isPresent()) {
			report.put("results", Collections.EMPTY_MAP);
		}
	}

	public void setResults(Map results) {
		report.put("results", results);
	}

	public String toString() {
		return getGroup() + ":" + getJobId() + ":" + getWorkerId().orElse("InQueue");
	}

	public void update(JobReport job) {
		this.update(job.report);
	}

	private void setConfigHashRoots(String... configroots) {
		List<String> hashes = new ArrayList<>();
		for(String configHashEntry:configroots) {
			hashes.add((String) getConfiguration().get(configHashEntry));
		}
		report.put("hash", hashes);
	}

	public List<String> getConfigHashes() {
		return (List<String>) report.get("hash");
	}

	public List<String> getObjectives() {
		return (List<String>) report.get("objectives");
	}

	public void setObjectives(List<String> objectives) {
		report.put("objectives", objectives);
	}

    public void setClient(String clientName) {
	    report.put("client", clientName);
    }

    public String getClientName() {
	    return (String) report.get("client");
    }
}
