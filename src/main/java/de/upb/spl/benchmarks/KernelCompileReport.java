package de.upb.spl.benchmarks;

import org.json.simple.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

public class KernelCompileReport {
	private final String config;
	private final String sampleId;
	private final String hash;
	private final String vmId;
	private final long compileStartTime, bootStartTime, bootFinishTime;
	private final boolean compileSuccess, bootSuccess;

	KernelCompileReport(String config){
		this(UUID.randomUUID().toString(), config, null, -1, -1, -1, false, false);
	}

	private KernelCompileReport(String sampleId, String config, String vmId, long compileStartTime, long bootStartTime, long bootFinishTime, boolean compileSuccess, boolean bootSuccess) {
		this.config = Objects.requireNonNull(config);
		this.sampleId = sampleId;
		this.hash = md5sum(config);
		this.vmId = vmId;
		this.compileStartTime = compileStartTime;
		this.bootStartTime = bootStartTime;
		this.bootFinishTime = bootFinishTime;
		this.bootSuccess = bootSuccess;
		this.compileSuccess = compileSuccess;
	}

	public KernelCompileReport setBootSuccess(boolean success) {
		return new KernelCompileReport(sampleId, this.getConfig(), this.getVmId(), getCompileStartTime(), this.getBootStartTime(), this.getBootFinishTime(), getCompileSuccess(), success);
	}

	public KernelCompileReport setCompileSuccess(boolean success) {
		return new KernelCompileReport(sampleId, this.getConfig(), this.getVmId(), getCompileStartTime(), this.getBootStartTime(), this.getBootFinishTime(), success, getBootSuccess());
	}

	public KernelCompileReport setBootStartTime(long time) {
		return new KernelCompileReport(sampleId, this.getConfig(),this.getVmId(), getCompileStartTime(), time, this.getBootFinishTime(), this.getCompileSuccess(), this.getBootSuccess());
	}

	public KernelCompileReport setBootFinishTime(long time) {
		return new KernelCompileReport(sampleId, this.getConfig(), this.getVmId(), getCompileStartTime() , this.getBootStartTime(), time, this.getCompileSuccess(), this.getBootSuccess());
	}

	public KernelCompileReport setVMID(String vmId) {
		return new KernelCompileReport(sampleId, this.getConfig(), vmId, getCompileStartTime(), this.getBootStartTime(), this.getBootFinishTime(), this.getCompileSuccess(), this.getBootSuccess());
	}

	public KernelCompileReport setCompileStartTime(long compileStartTime) {
		return new KernelCompileReport(sampleId, this.getConfig(), getVmId(), compileStartTime, this.getBootStartTime(), this.getBootFinishTime(), this.getCompileSuccess(), this.getBootSuccess());
	}


	public String getHash() {
		return hash;
	}

	public String getVmId() {
		return vmId;
	}

	public long getBootStartTime() {
		return bootStartTime;
	}

	public long getBootFinishTime() {
		return bootFinishTime;
	}

	public String getConfig() {
		return config;
	}


	private boolean getCompileSuccess() {
		return compileSuccess;
	}

	public boolean getBootSuccess() {
		return bootSuccess;
	}

	public long getCompileStartTime() {
		return compileStartTime;
	}

	public String toJsonString() {
		JSONObject serialized = new JSONObject();
		serialized.put("compile_start", getCompileStartTime());
		if(getCompileSuccess()) {
			serialized.put("compile_time", getBootStartTime() - getCompileStartTime());
		}
		serialized.put("boot_start", getBootStartTime());
		serialized.put("boot_finish", getBootFinishTime());
		if(getBootSuccess()) {
			serialized.put("boot_time", getBootFinishTime() - getBootStartTime());
		}
		return serialized.toJSONString();
	}

	@Override
	public String toString() {
		return "SamplePoint{" +
				"hash='" + hash + '\'' +
				", vmId='" + vmId + '\'' +
				", bootStartTime=" + bootStartTime +
				", bootFinishTime=" + bootFinishTime +
				", compileSuccess=" + compileSuccess +
				", bootSuccess=" + bootSuccess +
				'}';
	}

	static String md5sum(String input) {
		try {
			MessageDigest md = null;
			md = MessageDigest.getInstance("MD5");
			md.update(input.getBytes());
			byte[] digest = md.digest();
			String outputHash = DatatypeConverter
					.printHexBinary(digest).toUpperCase();
			return outputHash;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "";
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		KernelCompileReport that = (KernelCompileReport) o;

		return sampleId != null ? sampleId.equals(that.sampleId) : that.sampleId == null;
	}

	@Override
	public int hashCode() {
		return sampleId != null ? sampleId.hashCode() : 0;
	}
}
