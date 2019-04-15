package de.upb.spl.attributes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMUtil;
import de.upb.spl.util.DefaultMap;
import de.upb.spl.util.FileUtil;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.FeatureTreeNode;
import fm.XMLFeatureModel;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BayesNet3AttributeGenerator {


    private static class Evidence {

        double E1;
        double E2;
        double a1;
        double a2;

        final static int evidenceCount = 2;

        public Evidence(Random random, Evidence parent) {
            this.E1 = nextGaussian(random, parent.E1 + a1, 0.5);
            this.E2 = nextGaussian(random, parent.E2 + a2, 0.5);
            a1 = parent.a1;
            a2 = parent.a2;
            if(parent.E1 - parent.E2 < 2) {
                a1 ++;
                a2 --;
            } else {
                a1 --;
                a2 ++;
            }
            if(E1 < 0) {
                a1 ++;
            }
            if(E2 > 0) {
                a2 --;
            }
        }

        // root evidence
        public Evidence() {
            this.E1 = 1;
            this.E2 = -1.;
        }
        double getE(int index) {
            while(index >= evidenceCount || index < 0) {
                index = index %evidenceCount ;
                if (index<0) index += evidenceCount;
            }
            if(index == 0) {
                return E1;
            }
            if(index == 1) {
                return E2;
            }
            throw new RuntimeException("BUG: This cannot be reached: " + index);
        }

        @Override
        public String toString() {
            return "Evidence{" +
                    "E1= " + String.format("%.2f", E1) +
                    ", E2= " + String.format("%.2f", E2) +
                    ", a1= " + String.format("%.2f", a1) +
                    ", a2= " + String.format("%.2f", a2) +
                    '}';

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
            double e = evidence.getE(variableId);
            if(variableId % 2 == 1) {
                e = -e;
            }
            return e;
//            if(e >= 0.) {
//                return 1.;
//            } else {
//                return -1.;
//            }
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
                (cache, feature) -> {
            if(FMUtil.isRoot(feature)) {
                return new Evidence();
            } else {
                Evidence parentEvidence = cache.get((FeatureTreeNode) feature.getParent());
                return new Evidence(
                        random,
                        parentEvidence);
            }
        });

        for(FeatureTreeNode feature : FMUtil.listFeatures(featureModel)) {
            for (int i = 0; i < FMUtil.depth(featureModel, feature); i++) {
                System.out.print("  ");
            }
            System.out.println(feature.getName() + ": " + evidenceCache.get(feature).toString());
        }

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
