package de.upb.spl.benchmarks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.upb.spl.util.SimpleHttpHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;


@SuppressWarnings("ALL")
/**
 * Matches jobb offers with job applications.
 *
 * API:
 * 		POST; /job/offer ; 	The body of this request is a json object which contains 'group' and 'config' and 'hash' entries.
 * 							Puts the new job into `unassignedJobs` and blocks until the results are in.
 *
 * 		POST; /job/application ; Signs up with a json object as its body containing 3 entries:
 * 									-	'workerId': string, unique worker id
 * 								    -   'group': string, this application will be matched with jobs of the same group
 * 								    -   'cache': optional, list of strings, contains hash of configs that are (partially) cached.
 * 								    Blocks until a job is available. Prefers jobs whose config hashes are cached. (See JobApplication::cacheRating)
 *
 *      POST; /job/update ; The body contains the josn representation of JobReport with its "result" set.
 *      					Updates the appropriate job in unassignedJobs.
 *
 */
public class BenchmarkAgent {

	private final static Logger logger = LoggerFactory.getLogger(BenchmarkAgent.class);

	private final JobMaintainer jobs = new JobMaintainer();

	private final HttpServer jobService;
	private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public BenchmarkAgent(int jobServerPort) {
        try {
            jobService = HttpServer.create(new InetSocketAddress(jobServerPort), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        jobService.createContext("/job/offer", new HandleJobOffer());

		jobService.createContext("/job/application", new HandleJobApplication());
		jobService.createContext("/job/update", new HandleJobUpdate());

		jobService.setExecutor(Executors.newCachedThreadPool());
		jobService.start();

		logger.info("Benchmark agent started listening to port {}.", jobServerPort);
	}


	private class HandleJobOffer extends SimpleHttpHandler {

		HandleJobOffer() {
			super("/job/offer");
		}

		@Override
		protected void writeHeaders(Headers responseHeaders) {
			responseHeaders.add("Content-Type", "application/json");
		}

		@Override
		protected HttpResponse handle(String[] urlfields, String body) {
			HashMap jobDetails = gson.fromJson(body, HashMap.class);
			JobReport jobReport = new JobReport(jobDetails);
			if(jobReport.getGroup() == null) {
				jobReport.addError("Group missing.");
				return response(400, jobReport.jsonSerialization());
			}
			jobs.offerJob(jobReport);
			try {
				jobs.waitForResults(jobReport);
				return response(200, jobReport.jsonSerialization());
			} catch (InterruptedException e) {
				jobReport.addError("Interrupted while waiting for results: " + e.getMessage());
				logger.info("Interrupted while waiting for results for jobId `{}` with group `{}`.", jobReport.getJobId(), jobReport.getGroup());
				return response(500, jobReport.jsonSerialization());
			}
		}
	}


	private class HandleJobApplication extends SimpleHttpHandler {
		HandleJobApplication() {
			super("/job/application");
		}

		@Override
		protected void writeHeaders(Headers responseHeaders) {
			responseHeaders.add("Content-Type", "application/json");
		}

		@Override
		protected HttpResponse handle(String[] urlfields, String body) {
			JobApplication application = gson.fromJson(body, JobApplication.type);
			try {
				JobReport job = jobs.waitForJob(application);
				return response(200, job.jsonSerialization());
			} catch (InterruptedException e) {
				logger.info("Interrupted while waiting for job with `{}`.", application);
				return response(500, "Interrupted");
			}
		}
	}

	private class HandleJobUpdate extends SimpleHttpHandler {

		HandleJobUpdate() {
			super("/job/update");
		}

		@Override
		protected void writeHeaders(Headers responseHeaders) {
			responseHeaders.add("Content-Type", "application/json");
		}

		@Override
		protected HttpResponse handle(String[] urlfields, String body) {
			JobReport job = new JobReport(gson.fromJson(body, Map.class));
			jobs.update(job);
			return response(200, "");
		}
	}


	public static void main(String[] args) throws IOException {
		int port = 6000;
		if(args.length > 0) {
			try{
				port = Integer.parseInt(args[0]);
			} catch (Exception ex) {
				logger.error("Error trying to cast first argument `{}` to port.", args[0]);
			}
		}
		new BenchmarkAgent(port);
	}

	public JobMaintainer jobs() {
		return jobs;
	}
}
