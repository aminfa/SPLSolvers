package de.upb.spl.benchmarks.env;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.sat4j.core.VecInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.upb.spl.util.FileUtil;

import java.io.File;
import java.util.*;

public final class FMAttributes extends BenchmarkEnvironmentDecoration {

    private final static Logger logger = LoggerFactory.getLogger(FMAttributes.class);
    private final static String SUB_FOLDER_NAME = "attributes";
    private String splName;

    private Optional<List<String>> objectives = Optional.empty();
    private Optional<List<VecInt>> richSeeds = Optional.empty();
    private Optional<Map> attributeValues = Optional.empty();

    private Long seed;



    public FMAttributes(String resourceFolder, String splName) {
        this(new FMXML(resourceFolder + File.separator + splName + ".xml"), resourceFolder, splName);
    }

    public FMAttributes(BenchmarkEnvironment env, String resourceFolder, String splName) {
        super(env);
        this.splName = splName;
        logger.info("Creating environemt for spl-name={} in resource-folder={}.", splName, resourceFolder);
//
//        String componentModelFile = resourceFolder + File.separator + splName + ".json";
//        logger.info("Loading component model from: {}.", componentModelFile);
//        String dimacsFile = resourceFolder + File.separator + splName + ".dimacs";
//        logger.info("Loading sat formula from from: {}.", dimacsFile);

        String attributeValuesFile = resourceFolder + File.separator + SUB_FOLDER_NAME + File.separator + splName + ".attributes.json";
        logger.info("Loading attribute values from: {}.", attributeValuesFile);
        String objectivesFile = resourceFolder + File.separator + SUB_FOLDER_NAME + File.separator + splName + ".objectives.json";
        logger.info("Loading objectives from: {}.", objectivesFile);
        String richSeedFile = resourceFolder + File.separator + SUB_FOLDER_NAME + File.separator + splName + ".richseed.json";
        logger.info("Loading rich seed from: {}.", richSeedFile);



        JSONParser parser = new JSONParser();
        JSONObject attributeValues;
        try {
            attributeValues = (JSONObject) parser.parse(
            FileUtil.readFileAsString(attributeValuesFile));
            this.attributeValues = Optional.of(attributeValues);
        } catch (Exception e) {
            logger.warn("Couldn't load attributes from {}: {}", attributeValuesFile, e);
        }

        List<String> objectives;

        try {
            objectives = (List<String>) parser.parse(FileUtil.readFileAsString(objectivesFile));
            this.objectives = Optional.of(objectives);
        } catch (Exception e) {
            logger.warn("Couldn't load objectives from {}: {}", objectivesFile, e);
        }

        List<List<Long>> richSeeds;

        try {
            richSeeds = (List<List<Long>>) parser.parse(FileUtil.readFileAsString(richSeedFile));
            this.richSeeds = Optional.of(new ArrayList<>());
            for(List<Long> seed : richSeeds) {
                VecInt seed1 = new VecInt();
                for(Long i : seed) {
                    seed1.push(i.intValue());
                }
                this.richSeeds.get().add(seed1);
            }
        } catch (Exception e) {
            logger.warn("Couldn't load richseed from {}: {}", richSeedFile, e);
        }



        long seed = 0;
        try{
            String seedFile = resourceFolder + File.separator + "attributes" + File.separator + splName + ".seed";
            seed = Long.parseLong(FileUtil.readFileAsString(seedFile));
            logger.info("Loading seed from: {}.", seedFile);
        } catch(Exception ex) {
        }
        if(seed == 0) {
            seed = new Random().nextLong();
            logger.warn("Generated random seed = {}.", seed);
        }
        this.seed = seed;
    }

    public Map attributes() {
        return attributeValues.get();
    }


    @Override
    public List<String> objectives() {
        return objectives.get();
    }

    @Override
    public List<VecInt> richSeeds() {
        return richSeeds.get();
    }


    public String toString() {
        return splName;
    }

    @Override
    public Long seed() {
        return seed;
    }
}
