package de.upb.spl.benchmarks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

public class VMMaintainer {

	private final static Logger logger = LoggerFactory.getLogger(VMMaintainer.class);

	private static final int MaximumConcurrentVms = 4;
	private static final String MEDIUM_CACHE_DIR = "/Volumes/HDD4/medium_cache/";

	private final Semaphore freeVMSlots = new Semaphore(MaximumConcurrentVms);



	public String deployNew() {
		try {
			freeVMSlots.acquire();
			String vmName = VMCtrl.deployPreKernelCompile();
			return vmName;
		} catch(InterruptedException ex) {
			logger.error("Interrupted at deployment", ex);
			throw new RuntimeException(ex);
		}
	}

//	private String

	private void cacheVm(String vmName, String cacheFile){

	}

}
