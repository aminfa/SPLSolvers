package de.upb.spl.attributes;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMUtil;
import de.upb.spl.util.DefaultMap;
import de.upb.spl.util.FileUtil;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.aeonbits.owner.ConfigFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BayesNetAttributeGenerator {


    private static class Evidence {
        final double E1, E2, E3;
        final static int evidenceCount = 3;
        public Evidence(Random random) {
            E1 = random.nextDouble() > 0.5 ? 2. : -2;
            E2 = random.nextDouble() > 0.5 ? 2. : -2;
            E3 = random.nextDouble() > 0.5 ? 2. : -2;
        }

        public Evidence(String featureName, long seed) {
            String hashInput = featureName;
            long innerSeed = Hashing.sha256().hashString(hashInput, Charset.defaultCharset()).asLong() ^ seed;
            Random random = new Random(innerSeed);
            E1 = random.nextDouble() > 0.5 ? 1. : -1;
            E2 = random.nextDouble() > 0.5 ? 1. : -1;
            E3 = random.nextDouble() > 0.5 ? 1. : -1;
        }

        double getEvidenceValue(int evidenceIndex) {
            while(evidenceIndex >= evidenceCount || evidenceIndex < 0) {
                evidenceIndex = evidenceIndex %evidenceCount ;
                if (evidenceIndex<0) evidenceIndex += evidenceCount;
            }
            if(evidenceIndex == 0) {
                return E1;
            }
            if(evidenceIndex == 1) {
                return E2;
            }
            if(evidenceIndex == 2) {
                return E3;
            }
            throw new RuntimeException("BUG: This cannot be reached: " + evidenceIndex);
        }
    }

    private static class Question {

        final Evidence evidence;

        final String variable;

        final Random random;

        private Pattern postfixNumber = Pattern.compile("^[A-Za-z]*?(\\d+)$");

        Question(String variable, Random random) {
            this.variable = Objects.requireNonNull(variable);
            this.evidence = new Evidence(random);
            if(this.variable.isEmpty()) {
                throw new IllegalArgumentException("empty variable");
            }
            this.random = random;
        }

        Question(String variable, Evidence evidence, Random random) {
            this.variable = Objects.requireNonNull(variable);
            this.evidence = evidence;
            if(this.variable.isEmpty()) {
                throw new IllegalArgumentException("empty variable");
            }
            this.random = random;
        }

        double infer() {
            int variableId;
            try {
                Matcher matcher = postfixNumber.matcher(variable);
                matcher.find();
                variableId = Integer.parseInt(matcher.group(1));
            } catch(Exception ex) {
                throw new IllegalStateException("Variable " + variable + " doesn't contain a number at the end: " + ex.getMessage());
            }

            double mean = evidence.getEvidenceValue(variableId) - evidence.getEvidenceValue(variableId + 1);
            double variance = 0.75;
            return random.nextGaussian() * variance + mean;
        }
    }


    public static double generate(String featureID, String attributeName, long seed) {
        String hashInput = featureID;
        long innerSeed = Hashing.sha256().hashString(hashInput, Charset.defaultCharset()).asLong() ^ seed;
        Random generator = new Random(innerSeed);
        return new Question(attributeName, generator).infer();
    }

    public static void main(String[]  args) throws FeatureModelException, IOException {
        if(args.length < 3) {
            System.err.println("Provide at least two arguments: \n" +
                    "1: path to the Features model xml file.\n" +
                    "2: path to the output file. (WILL BE OVERRIDDEN)\n" +
                    "3: Seed as an integer. 0 will be replaced by a random seed.");
            System.exit(1);
        }
        String fmFilePath = args[0];
        String outputFilePath = args[1];
        long seed;
        seed = Long.parseLong(args[2]);
        if(seed == 0) {
            seed = new Random().nextLong();
        }
        final long finalSeed = seed;
        List<String> attributes = Arrays.asList("B1", "B2", "B3");
        Map attributeValueCollection = new HashMap<>();
        DefaultMap<String, Evidence> evidenceCache = new DefaultMap<String, Evidence>(featureName -> new Evidence(featureName, finalSeed));
        for(String attribute : attributes) {
            FeatureModel featureModel = new XMLFeatureModel(fmFilePath, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
            featureModel.loadModel();
            Random random = new Random(finalSeed);
            Map<String, Double> attributeValues = FMUtil
                    .featureStream(featureModel)
                    .collect(Collectors.toMap(
                            feature -> feature.getName(),
                            feature -> new Question(attribute, evidenceCache.get(FMUtil.id(feature)), random).infer()));
            Map attributeObj = new HashMap();
            attributeValueCollection.put(attribute, attributeObj);
            attributeObj.put("values", attributeValues);
            attributeObj.put("aggregation", "mean");
            attributeObj.put("minimized", true);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String attributeValuesJSON = gson.toJson(attributeValueCollection);
        FileUtil.writeStringToFile(outputFilePath, attributeValuesJSON);
    }
}
