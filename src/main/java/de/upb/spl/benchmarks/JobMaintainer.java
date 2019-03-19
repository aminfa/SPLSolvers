package de.upb.spl.benchmarks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JobMaintainer {

	private final static Logger logger = LoggerFactory.getLogger(JobMaintainer.class);


	private final List<JobReport> unassignedJobs = new ArrayList<>(2 << 7);
	private final List<JobReport> runningJobs = new ArrayList<>(2 << 7);
	private final List<JobReport> finishedJobs = new ArrayList<>(2 << 7);


	public void offerJob(JobReport newJob) {
		synchronized (unassignedJobs) {
			unassignedJobs.add(newJob);
			unassignedJobs.notifyAll();
		}
		logger.debug("New job added: {}", newJob);
	}

	public JobReport waitForJob(JobApplication application) throws InterruptedException {
		synchronized (unassignedJobs) {
			while(unassignedJobs.isEmpty() || unassignedJobs.stream().noneMatch(j -> j.getGroup().equals(application.getGroup()))) {
				unassignedJobs.wait();
			}
			Optional<JobReport> job = unassignedJobs.stream()
					.filter(j -> j.getGroup().equals(application.getGroup()))
					.max(Comparator.comparingInt((JobReport j) -> application.cacheRating(j.getConfigHashes())));
			if(!job.isPresent()) {
				logger.error("No job found for {} in unassignedJobs:\n{}", application, unassignedJobs);
				return waitForJob(application);
			} else {
				logger.info("Gave job `{}` to {} with cache hits {}.", job.get(), application, application.cacheRating(job.get().getConfigHashes()));
				job.get().setWorkerId(application.getWorkerId());
				unassignedJobs.remove(job.get());
				synchronized (runningJobs) {
					runningJobs.add(job.get());
				}
				return job.get();
			}
		}
	}


	public void update(JobReport job) {
		if(!job.getResults().isPresent()) {
			logger.warn("updating job although no results are provided. Job: `{}`", job);
		}
		synchronized (runningJobs) {
			for(JobReport otherJob : runningJobs) {
				if(otherJob.getJobId().equals(job.getJobId())) {
					otherJob.update(job);
					logger.debug("Job updated: {}", otherJob.getJobId(), otherJob);
					synchronized (otherJob) {
						otherJob.notifyAll();
					}
					return;
				}
			}
		}
		logger.warn("Job with id `" + job.getJobId() + "` wasn't found in runnigJobs list. Updating finished Jobs instead..");

		synchronized (finishedJobs) {
			for(JobReport otherJob : finishedJobs) {
				if(otherJob.getJobId().equals(job.getJobId())) {
					otherJob.update(job);
					logger.info("Finished Job updated: {}", otherJob.getJobId(), otherJob);
					synchronized (otherJob) {
						otherJob.notifyAll();
					}
					return;
				}
			}
		}

		throw new IllegalArgumentException("Job with id `" + job.getJobId() + "` wasn't found. Job: " + job.toString());
	}


	public void waitForResults(JobReport newJob) throws InterruptedException {
		synchronized (newJob) {
			logger.debug("Waiting for results of job `{}`. ", newJob);
			while(!newJob.getResults().isPresent()) {
				newJob.wait();
			}
		}
		logger.debug("Job results are in: {} ", newJob);
		synchronized (runningJobs) {
			runningJobs.remove(newJob);
		}
		synchronized (finishedJobs) {
			logger.debug("Putting jobs in finished jobs list: {} ", newJob);
			finishedJobs.remove(newJob);
		}
		logger.debug("Returning results from job `{}`. ", newJob);

	}

}
