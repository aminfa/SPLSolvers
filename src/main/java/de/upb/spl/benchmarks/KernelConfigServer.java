package de.upb.spl.benchmarks;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class KernelConfigServer {

    private static final Logger logger = Logger.getLogger(KernelConfigServer.class.getName());

    private static final int timout_MS = 120000; // 120 seconds timeout for kernel boot
    private static final boolean kill_timeout_VM = true; // kills the vm if after timeout time the vm doesn't boot and make a config request
    private static final int MaximumConcurrentVms = 4;

    private final List<KernelCompileReport> jobList = new ArrayList<>();
    private final Map<String, KernelCompileReport> vmAssignments = new ConcurrentHashMap<>();
    private final List<KernelCompileReport> resultList = new ArrayList<>();
    private final Semaphore concurrentVmTicket = new Semaphore(MaximumConcurrentVms);


    public KernelCompileReport offerJob(String kernelConfig) {
        KernelCompileReport newJob = new KernelCompileReport(kernelConfig);
        synchronized (jobList) {
            jobList.add(newJob);
            jobList.notifyAll();
        }
        try {
            concurrentVmTicket.acquire();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return newJob;
    }

    public KernelCompileReport takeJob(String vmId) {
        synchronized (jobList) {
            while(jobList.isEmpty()) {
                try {
                    jobList.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            KernelCompileReport newJob = jobList.remove(0);
            KernelCompileReport assignedJob = newJob.setVMID(vmId);
            updateJob(assignedJob);
            return assignedJob;
        }
    }

    public Optional<KernelCompileReport> getAssignedJob(String vmId) {
        return Optional.ofNullable(vmAssignments.get(vmId));
    }

    public void updateJob(KernelCompileReport jobWithUpdates) {
        logger.info("Job update: " + jobWithUpdates.toString());
        vmAssignments.put(jobWithUpdates.getVmId(), jobWithUpdates);
    }



    public void postResult(KernelCompileReport result) {
        synchronized (resultList) {
            if(getAssignedJob(result.getVmId()).isPresent()) {
                vmAssignments.remove(result.getVmId());
                resultList.add(result);
                resultList.notifyAll();
            }
        }
    }

    public KernelCompileReport retrieveResult(KernelCompileReport job) {
        synchronized (resultList) {
            while(!resultList.contains(job)) {
                try {
                    resultList.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int index = resultList.indexOf(job);
            return resultList.remove(index);
        }
    }


    /**
     * This server serves kernel configurations to virtual machines on this host.
     */
    private final HttpServer kernelServer;

    public KernelConfigServer() throws IOException {
        kernelServer = HttpServer.create(new InetSocketAddress(30000), 0);
        kernelServer.createContext("/config", new ConfigHttpHandler());
        kernelServer.createContext("/compiled", new CompileKernelHttpHandler());
        kernelServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        kernelServer.start();
    }

    public static void main(String... args)  {
        logger.info("Starting Kernel benchmark server");
        try {
            new KernelConfigServer();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    void bootTimeoutEventHandle(String vmId) {
        logger.info("BOOT TIMEOUT... Killing VM: " + vmId );
        Optional<KernelCompileReport> sample_ = getAssignedJob(vmId);
        if(sample_.isPresent()) {
            KernelCompileReport sample =  sample_.get().setBootSuccess(false);
            postResult(sample);
            if (kill_timeout_VM) {
                VMCtrl.kill(sample.getVmId());
            }
        }
    }

    KernelCompileReport benchmark(String kernelConfig) {
        KernelCompileReport job = offerJob(kernelConfig);
        logger.info("Benchmarking a new sample. Hash: " + job.getHash()  + ", Config string Length: " + job.getConfig().length());
        KernelCompileReport result = retrieveResult(job); // blocks until a vm has compiled and booted it.. or failed.
        logger.info("Benchmarking finished for config: "+ result.toString());
        return result;
    }

    void compileFinished(final String vmId, String configHash) {
        Optional<KernelCompileReport> sample_ = getAssignedJob(vmId);
        if(sample_.isPresent()) {
            KernelCompileReport sample = sample_.get().setCompileSuccess(true).setBootStartTime(System.currentTimeMillis());
            updateJob(sample);
            new Thread(() -> {
                try {
                    Thread.sleep(timout_MS);
                    bootTimeoutEventHandle(vmId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            if(!configHash.equals(sample.getHash())) {
                logger.warning("Kernel Config Hash values do not match: " +
                        "requested config hash: " + sample.getHash() + ", vm reported config hash: " + configHash);
            }
        } else {
            logger.warning("VM with id " + vmId + " compiled but no request is mapped to its id....");
        }
    }

    void bootFinished(String vmId, String configHash) {
        Optional<KernelCompileReport> sample_ = getAssignedJob(vmId);
        if(sample_.isPresent()) {
            KernelCompileReport sample = sample_.get().setBootFinishTime(System.currentTimeMillis()).setBootSuccess(true);
            postResult(sample);
            if(!configHash.equals(sample.getHash())) {
                logger.warning("Kernel Config Hash values do not match: " +
                        "requested config hash: " + sample.getHash() + ", vm reported config hash: " + configHash);
            }
        } else {
            logger.info("VM with id " + vmId + " booted but noone is interested in his boot time .. :/");
        }
    }

    String takeConfig(String vmId) {
            KernelCompileReport newJob = takeJob(vmId).setCompileStartTime(System.currentTimeMillis());
            updateJob(newJob);
            return newJob.getConfig();
    }


    class CompileKernelHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                logger.info("Received compile notification.. ");
                String vmId = httpExchange.getRequestHeaders().getFirst("VM-Id");
                String kernelConfigHash = httpExchange.getRequestHeaders().getFirst("Kernel-Hash");

                compileFinished(vmId, kernelConfigHash);

                httpExchange.getResponseHeaders().add("Content-Type", "text/plain");
                httpExchange.getResponseHeaders().add("Content-Type", "charset=UTF-8");
                httpExchange.sendResponseHeaders(200, 0);
                httpExchange.getResponseBody().close();
            } catch (RuntimeException ex) {
                String requester = httpExchange.getRemoteAddress().getHostName();
                String port = "" + httpExchange.getRemoteAddress().getPort();
                String url = httpExchange.getRequestURI().getPath();
                logger.warning("Error handle of request " + url + " from entity " + requester + ":" + port + "\n" + ex.getMessage());
                ex.printStackTrace();
                httpExchange.sendResponseHeaders(500, 0);
                httpExchange.getResponseBody().close();
            }
            httpExchange.close();
        }
    }

    class ConfigHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                logger.info("Received kernel config request... ");
                String vmId = httpExchange.getRequestHeaders().getFirst("VM-Id");
                String kernelConfigHash = httpExchange.getRequestHeaders().getFirst("Kernel-Hash");

                bootFinished(vmId, kernelConfigHash);
                String newkernelConfig = takeConfig(vmId);

                httpExchange.getResponseHeaders().add("Content-Type", "text/plain");
                httpExchange.getResponseHeaders().add("Content-Type", "charset=UTF-8");
                byte[] returnBody = newkernelConfig.getBytes(Charset.defaultCharset());
                httpExchange.sendResponseHeaders(200, returnBody.length);
                httpExchange.getResponseBody().write(returnBody);
                httpExchange.getResponseBody().close();
            } catch(RuntimeException ex) {
                String requester = httpExchange.getRemoteAddress().getHostName();
                String port = "" + httpExchange.getRemoteAddress().getPort();
                String url = httpExchange.getRequestURI().getPath();
                logger.warning("Error handle of request " + url + " from entity "+ requester  + ":" +  port + "\n" + ex.getMessage());
                ex.printStackTrace();
                httpExchange.sendResponseHeaders(500, 0);
                httpExchange.getResponseBody().close();
            }
            httpExchange.close();
        }

    }

}
