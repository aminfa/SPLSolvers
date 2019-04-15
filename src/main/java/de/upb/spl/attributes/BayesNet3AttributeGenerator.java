package de.upb.spl.attributes;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMSAT;
import de.upb.spl.FMUtil;
import de.upb.spl.hierons.NovelRepresentation;
import de.upb.spl.util.DefaultMap;
import de.upb.spl.util.FileUtil;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.FeatureTreeNode;
import fm.XMLFeatureModel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BayesNet3AttributeGenerator {


    private static class Evidence {

        double E;
        double V;

        final static int evidenceCount = 2;

        public Evidence(Random random, int height, double parentE) {
            this.E = nextGaussian(random, parentE, 1/((double) height));
            this.V = nextGaussian(random,0, 1);
        }

        // root evidence
        public Evidence() {
            this.E = 0.;
            this.V = 0.;
        }

    }

    private static class Question {

        final Evidence evidence;

        final String variable;

        final Random random;

        private Pattern postfixNumber = Pattern.compile("^[A-Za-z]*?(\\d+)$");


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

//            double mean = evidence.getEValue(variableId) / (evidence.getEValue(variableId + 1) + evidence.depth);
//            double variance = evidence.getVValue(variableId);
//            return nextGaussian(random, mean, variance);

//            return (evidence.getEValue(variableId)) /
//                    (evidence.getEValue(variableId + 1))
//                    + evidence.getVValue(variableId);
            if(variableId == 1) {
                return evidence.E + evidence.V;
            }
            else {
                return -evidence.E;
            }
        }
    }

    static double nextGaussian(Random random, double mean, double variance) {
        return random.nextGaussian() * variance + mean;
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
        List<String> attributes = Arrays.asList("B1", "B2");
        Map attributeValueCollection = new HashMap<>();
        FeatureModel featureModel = new XMLFeatureModel(fmFilePath, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
        featureModel.loadModel();
        Random random = new Random(seed);
        DefaultMap<FeatureTreeNode, Evidence> evidenceCache = new DefaultMap<>(
                (Function) null);
        evidenceCache.setDefaultFunc(feature -> {
            if(FMUtil.isRoot(feature)) {
                return new Evidence();
            } else {
                return new Evidence(
                        random,
                        FMUtil.calcHeight(feature),
                        evidenceCache.get((FeatureTreeNode) feature.getParent()).E);
            }
        });

        for(String attribute : attributes) {
            Map<String, Double> attributeValues = FMUtil
                    .featureStream(featureModel)
                    .collect(Collectors.toMap(
                            feature -> feature.getName(),
                            feature -> new Question(attribute, evidenceCache.get(feature), random).infer()));
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
