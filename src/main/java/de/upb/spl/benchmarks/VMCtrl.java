package de.upb.spl.benchmarks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ShellUtil;

import java.io.File;

import static de.upb.spl.benchmarks.VMState.*;

public class VMCtrl {
	private final static Logger logger = LoggerFactory.getLogger("VM_CTRL");
	private final static String SCRIPT_HOME;
	private final static String HOME_BASE_MEDIUM = "/Volumes/HDD4/Home_Base.vdi",
								HOST_ADAPTER = "vboxnet3";
	private final static Integer RAM = 2048, VRAM = 16;

	static {
		if(System.getenv().containsKey("VM_SCRIPT_HOME")) {
			String home = System.getenv().get("VM_SCRIPT_HOME");
			if(!home.endsWith(File.separator)) {
				home += File.separator;
			}
			SCRIPT_HOME = home;
		} else {
			SCRIPT_HOME = "";
		}
	}


	private static String getCommandScript(String scriptname, Object... args) {
		StringBuilder command = new StringBuilder("python " + SCRIPT_HOME + scriptname + ".py");
		for(Object arg : args) {
			command.append(" ").append(arg.toString());
		}
		return command.toString();
	}


	private static String run(String command) {
		String out = ShellUtil.sh(command);
		logger.debug("RUNNING \n\t {}\nOUT \n\t{}", command, out);
		return out;
	}

	private static String getUUID(String vmName){
		String command = String.format("VBoxManage showvminfo %s --machinereadable | grep \"^UUID\" |  sed -E 's|.*=\"(.*)\"|\\1|'", vmName);
		return run(command);
	}

	public static String getName(String nameOrId) {
		String command = "VBoxManage showvminfo " + nameOrId + " --machinereadable | grep \"^name\" |  sed -E 's|.*=\"(.*)\"|\\1|'";
		return run(command);
	}

	public static String vmName(int vmIndex, VMState vmState) {
		switch(vmState) {
			case PRE_KERNEL:
				return String.format("Linux_KernelCompile_%2d", vmIndex);
			case PRE_DB:
				return String.format("Linux_DBCompile_%2d", vmIndex);
			case DB_RUN:
				return String.format("Linux_DBRun_%2d", vmIndex);
			default:
				throw new RuntimeException("State unrecognized: " + vmState);
		}
	}

	public synchronized static String deployPreKernelCompile() {
		int vmIndex = 0;
		String vmName;
		while (exists((vmName  = vmName(vmIndex, PRE_KERNEL)))) {
			vmIndex ++;
		}
		create(HOME_BASE_MEDIUM, vmName);
		return vmName;
	}

	public synchronized static String deployPreDbCompile(String medium) {
		int vmIndex = 0;
		String vmName;
		while (exists((vmName  = vmName(vmIndex, PRE_DB)))) {
			vmIndex ++;
		}
		create(medium, vmName);
		return vmName;
	}

	public synchronized static String deployDbRun(String medium) {
		int vmIndex = 0;
		String vmName;
		while (exists((vmName  = vmName(vmIndex, DB_RUN)))) {
			vmIndex ++;
		}
		create(medium, vmName);
		return vmName;
	}

	public synchronized static void kill(String vmName) {
		String cmd = getCommandScript("killvm", vmName);
		run(cmd);
	}

	public synchronized static boolean exists(String vmName) {
		String uuid = getUUID(vmName);
		return uuid != null && !uuid.isEmpty();
	}

	public synchronized static void clonesnap(String vmName, String outputFile) {
		String cmd = getCommandScript("clonesnap", vmName, outputFile);
		run(cmd);
	}

	public synchronized static void create(String medium, String vmName) {
		String cmd = getCommandScript("createvm", medium, vmName);
		run(cmd);
	}

}
