package de.upb.spl.attributes;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMSAT;
import de.upb.spl.FMUtil;
import de.upb.spl.hierons.Hierons;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BayesNet2AttributeGenerator {


    private static class Evidence {

        double E1, E2, V1, V2;
        int depth;

        final static int evidenceCount = 2;

        public Evidence(Random random, int depth) {
            draw(random);
            this.depth = depth;
        }

        public Evidence(String featureName, long seed, int depth) {
            String hashInput = featureName;
            long innerSeed = Hashing.sha256().hashString(hashInput, Charset.defaultCharset()).asLong() ^ seed;
            Random random = new Random(innerSeed);
            this.depth = depth;
            draw(random);

        }

        void draw(Random random) {
//            E1 = nextGaussian(random, 10., 2.5);
//            E2 = nextGaussian(random, 10., 2.5);

            E1 = (random.nextDouble() * 5.) + 7.5;
            V1 = random.nextDouble() * 3.;
            V2 = random.nextDouble() * 3.;
        }

        double getEValue(int evidenceIndex) {
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
            throw new RuntimeException("BUG: This cannot be reached: " + evidenceIndex);
        }

        double getVValue(int evidenceIndex) {
            while(evidenceIndex >= evidenceCount || evidenceIndex < 0) {
                evidenceIndex = evidenceIndex %evidenceCount ;
                if (evidenceIndex<0) evidenceIndex += evidenceCount;
            }
            if(evidenceIndex == 0) {
                return V1;
            }
            if(evidenceIndex == 1) {
                return V2;
            }
            throw new RuntimeException("BUG: This cannot be reached: " + evidenceIndex);
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
                return evidence.getEValue(0) + evidence.getVValue(0);
            }
            else {
                return (10. / evidence.getEValue(0)) + evidence.getVValue(1);
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
        DefaultMap<FeatureTreeNode, Evidence> evidenceCache = new DefaultMap<>(
                feature -> new Evidence(
                        FMUtil.id(feature),
                        finalSeed,
                        FMUtil.depth(featureModel, feature)));
        NovelRepresentation representation = new NovelRepresentation(null, "", featureModel, FMSAT.transform(featureModel));

        for(String attribute : attributes) {
            Random random = new Random(finalSeed);
            Map<String, Double> attributeValues = FMUtil
                    .featureStream(featureModel)
                    .collect(Collectors.toMap(
                            feature -> feature.getName(),
                            feature -> {
                                if(representation.isRepresented(feature))
                                    return new Question(attribute, evidenceCache.get(feature), random).infer();
                                else
                                    return 0.;
                            }));
            Map attributeObj = new HashMap();
            attributeValueCollection.put(attribute, attributeObj);
            attributeObj.put("values", attributeValues);
            attributeObj.put("aggregation", "sum");
            attributeObj.put("minimized", true);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String attributeValuesJSON = gson.toJson(attributeValueCollection);
        FileUtil.writeStringToFile(outputFilePath, attributeValuesJSON);
    }
}
