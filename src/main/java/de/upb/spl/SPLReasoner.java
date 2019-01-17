package de.upb.spl;

import fm.FeatureModel;

import java.util.List;

public interface SPLReasoner {
	List<FeatureSelection> search(BenchmarkEnvironment env);
}
