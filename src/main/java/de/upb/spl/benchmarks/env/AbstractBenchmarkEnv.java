package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import fm.FeatureModel;
import fm.XMLFeatureModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileUtil;

import java.io.File;
import java.util.*;

public abstract class AbstractBenchmarkEnv implements BenchmarkEnvironment {

    private final static Logger logger = LoggerFactory.getLogger(AbstractBenchmarkEnv.class);
    private final static String SUB_FOLDER_NAME = "attributes";
    private String splName;
    private Map<String, BenchmarkBill> books = new HashMap<>();
    private Optional<List<String>> objectives = Optional.empty();
    private Optional<List<VecInt>> richSeeds = Optional.empty();
    private Optional<Map> attributeValues = Optional.empty();
    private Random generator;
    private Optional<FeatureModel> fm;
    private Optional<FMSAT> fmsat;

    public AbstractBenchmarkEnv(String resourceFolder, String splName) {
        this.splName = splName;
        logger.info("Creating environemt for spl-name={} in resource-folder={}.", splName, resourceFolder);
        String modelFile = resourceFolder + File.separator + splName + ".xml";
        logger.info("Loading feature model from: {}.", modelFile);
        String dimacsFile = resourceFolder + File.separator + splName + ".dimacs";
        logger.info("Loading sat formula from from: {}.", dimacsFile);
        String attributeValuesFile = resourceFolder + File.separator + SUB_FOLDER_NAME + File.separator + splName + ".attributes.json";
        logger.info("Loading attribute values from: {}.", attributeValuesFile);
        String objectivesFile = resourceFolder + File.separator + SUB_FOLDER_NAME + File.separator + splName + ".objectives.json";
        logger.info("Loading objectives from: {}.", objectivesFile);
        String richSeedFile = resourceFolder + File.separator + SUB_FOLDER_NAME + File.separator + splName + ".richseed.json";
        logger.info("Loading rich seed from: {}.", richSeedFile);

        try {
            // Load the XML file and creates the listFeatures model
            FeatureModel fm = new XMLFeatureModel(modelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
            fm.loadModel();
            this.fm = Optional.of(fm);
        } catch (Exception e) {
            logger.warn("Error loading feature model at {}: {}", modelFile, e);
            this.fm = Optional.empty();
        }

        if(fm.isPresent()) {
            this.fmsat = Optional.of(FMSAT.transform(fm.get()));
        } else {
            try {
                // TODO load dimacs
            } catch (Exception e) {
            }
        }

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
        this.generator = new Random(seed);
    }

    public Map attributes() {
        return attributeValues.get();
    }

    @Override
    public FeatureModel model() {
        return fm.get();
    }

    @Override
    public FMSAT sat() {
        return fmsat.get();
    }

    @Override
    public List<String> objectives() {
        return objectives.get();
    }

    @Override
    public List<VecInt> richSeeds() {
        return richSeeds.get();
    }

    @Override
    public Random generator() {
        return generator;
    }

    public synchronized BenchmarkBill bill(String clientName) {
        if(clientName == null) {
            return new BenchmarkBill("anonymous");
        }
        BenchmarkBill bill = books.get(clientName);
        if(bill == null) {
            bill = new BenchmarkBill(clientName);
            books.put(clientName, bill);
        }
        return bill;
    }

    public String toString() {
        return splName;
    }
}
