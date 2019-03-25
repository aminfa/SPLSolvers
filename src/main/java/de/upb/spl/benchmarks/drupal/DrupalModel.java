package de.upb.spl.benchmarks.drupal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.FMAttributes;
import de.upb.spl.benchmarks.env.FMXML;
import de.upb.spl.util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for Drupal Black box
 */
public class DrupalModel extends BenchmarkEnvironmentDecoration {

    private static final String SPL_NAME = "drupal";

    private final static Logger logger = LoggerFactory.getLogger(DrupalModel.class);

    public enum Objective {
        ModuleCount,
        Size ,
        CC ,
        TestCases ,
        TestAssertions ,
        Installations ,
        Developers ,
        Changes ,
        MinorFaults ,
        NormalFaults ,
        MajorFaults ,
        CriticalFaults ,
        IntegrationFaults
    }

    private static final List<String> OBJECTIVES_LIST = Arrays.stream(Objective.class.getEnumConstants())
                                                    .map(Enum::name)
                                                    .collect(Collectors.toList());

    protected final int moduleSize = 47;

    protected final List<String> modulesList;

    protected final Map<Objective, List<Number>> attributes;

    protected final int[][] integrationFaults;


    public DrupalModel() {
        this(
                new FMAttributes(
                        new FMXML(FileUtil.getPathOfResource("drupal/feature-model.xml")),
                        new File(FileUtil.getPathOfResource("drupal/feature-model.xml")).getParent(),
                        SPL_NAME)
        );
    }


    public DrupalModel(BenchmarkEnvironment env) {
        super(env);
        try {
            modulesList = loadModuleList();
            attributes = loadAttributes();
            integrationFaults = loadIntegrationFaults();
        } catch(Exception ex) {
            logger.error("Couldn't load drupal files: ", ex);
            throw new IllegalStateException("Drupal files couldn't be found or are corrupted.");
        }

    }

    private List<String> loadModuleList() throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject featureIndicesMap = (JSONObject) parser.parse(FileUtil.readResourceAsString("drupal/feature-indices.json"));
        return (List<String>) featureIndicesMap.get("Modules");
    }

    private Map<Objective, List<Number>> loadAttributes() throws ParseException {
        JSONParser parser = new JSONParser();
        Map<Objective, List<Number>> attributes = new HashMap<>();
        Map<String, List> featureAttributes = (JSONObject) parser.parse(
                FileUtil.readResourceAsString("drupal/feature-attributes.json"));
        Map<String, List> featureFaults = (JSONObject) parser.parse(
                FileUtil.readResourceAsString("drupal/feature-faults.json"));
        attributes.putAll(featureAttributes.keySet().stream().map(Objective::valueOf).collect(Collectors.toMap(objective -> objective, objective -> featureAttributes.get(objective.name()))));
        attributes.putAll(featureFaults.keySet().stream().map(Objective::valueOf).collect(Collectors.toMap(objective -> objective, objective -> featureFaults.get(objective.name()))));
        return attributes;
    }

    /**
     * Is instead handled in loadAttributes.
     */
    @Deprecated
    private Multimap<Objective, Integer> loadFaults() throws ParseException {
        Multimap<Objective, Integer> faultValues = ArrayListMultimap.create();
        JSONObject featureFaults;
        JSONParser parser = new JSONParser();
        featureFaults = (JSONObject) parser.parse(FileUtil.readResourceAsString("drupal/feature-faults.json"));
        faultValues.putAll(Objective.MinorFaults,     ((List<Number>) featureFaults.get("Minor"))
                                            .stream().map(Number::intValue).collect(Collectors.toList()));
        faultValues.putAll(Objective.NormalFaults,    ((List<Number>) featureFaults.get("Normal"))
                                            .stream().map(Number::intValue).collect(Collectors.toList()));
        faultValues.putAll(Objective.MajorFaults,     ((List<Number>) featureFaults.get("Major"))
                                            .stream().map(Number::intValue).collect(Collectors.toList()));
        faultValues.putAll(Objective.CriticalFaults,  ((List<Number>) featureFaults.get("Critical"))
                                            .stream().map(Number::intValue).collect(Collectors.toList()));
        return faultValues;
    }

    private int[][] loadIntegrationFaults() throws ParseException {
        List<JSONArray> jsonArr;
        JSONParser parser = new JSONParser();
        jsonArr = (List<JSONArray>) parser.parse(FileUtil.readResourceAsString("drupal/integration-faults.json"));
        int[][] integrationFaultArr = new int[jsonArr.size()][];
        int index = 0;
        for(JSONArray integrationFault : jsonArr) {
            integrationFaultArr[index] = new int[integrationFault.size()];
            for (int i = 0; i < integrationFault.size(); i++) {
                integrationFaultArr[index][i] =  ((Number)integrationFault.get(i)).intValue();
            }
            index++;
        }
        return integrationFaultArr;
    }

    public List<String> objectives() {
        return OBJECTIVES_LIST;
    }


    public Optional<Integer> getModuleIndex(String featureId) {
        int index = modulesList.indexOf(featureId);
        if(index == -1) {
            return Optional.empty();
        } else {
            return Optional.of(index);
        }
    }

    public String getFeatureId(int moduleIndex) {
        if(moduleIndex < 0 || moduleIndex >= moduleSize) {
            throw new IllegalArgumentException("Internal feature index is out of bounds: " + moduleIndex + ". Size of feature id list: " + modulesList.size());
        }
        return modulesList.get(moduleIndex);
    }

}
