package de.upb.spl.benchmarks.drupal;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.upb.spl.benchmarks.drupal.DrupalModel.Objective;

public class DrupalFilteredObjectives extends BenchmarkEnvironmentDecoration {

    private final List<String> objectives;


    public DrupalFilteredObjectives(BenchmarkEnvironment env) {
        this(env,
                Objective.ModuleCount,
                Objective.Size ,
                Objective.CC ,
                Objective.TestCases ,
                Objective.TestAssertions ,
                Objective.Installations ,
                Objective.Developers ,
                Objective.Changes);
    }

    public DrupalFilteredObjectives(BenchmarkEnvironment env, Objective... objectives) {
        this(env, Arrays.stream(objectives));
    }

    public DrupalFilteredObjectives(BenchmarkEnvironment env, Stream<Objective> objectives) {
        super(env);
        this.objectives = objectives.map(Objective::name)
                .collect(Collectors.toList());
    }


    public DrupalFilteredObjectives(BenchmarkEnvironment env, List<Objective> objectives) {
        this(env, objectives.stream());
    }

    @Override
    public List<String> objectives() {
        return objectives;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + objectives.toString() + ") - " + getBaseEnv().toString();
    }
}
