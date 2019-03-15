package de.upb.spl.attributes;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMUtil;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.FeatureTreeNode;
import fm.XMLFeatureModel;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.aeonbits.owner.ConfigFactory;
import de.upb.spl.util.FileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

public class FeatureAttributeGenerator {
	private final long seed;

	public FeatureAttributeGenerator(long seed) {
		this.seed = seed;
	}

	public FeatureAttributeGenerator() {
		this.seed = new Random().nextLong();
	}

	public double generate(FeatureAttribute featureAttribute) {
		String hashInput = featureAttribute.featureName() + featureAttribute.attributeName();
		long innerSeed = Hashing.sha256().hashString(hashInput, Charset.defaultCharset()).asLong() ^ seed;
		Random generator = new Random(innerSeed);
		return featureAttribute.getAttribute().value(generator.nextDouble());
	}

    public Map<FeatureTreeNode, Double> generate(FeatureModel fm, AbstractAttribute attribute) {
        Object2DoubleMap<FeatureTreeNode> attributeValues = new Object2DoubleOpenHashMap<>();
        FMUtil.featureStream(fm).forEachOrdered(feature -> {
            FeatureAttribute featureAttribute = new FeatureAttribute(feature.getName(), attribute);
            attributeValues.put(feature, generate(featureAttribute));
        });
        return attributeValues;
    }


    public Map<String, Double> generateWithNames(FeatureModel fm, AbstractAttribute attribute) {
        Map<FeatureTreeNode, Double> attributeValues = generate(fm, attribute);
        Object2DoubleMap<String> attributeNameValues = new Object2DoubleOpenHashMap<>();
        attributeValues.forEach((FeatureTreeNode feature, Double value) -> attributeNameValues.put(feature.getName(), value.doubleValue()));
        return attributeNameValues;
    }

	public static void main(String[]  args) throws FeatureModelException, IOException {
		if(args.length < 4) {
			System.err.println("Provide at least two arguments: \n" +
					"1: path to the listFeatures model xml file.\n" +
					"2: path to the output file. (WILL BE OVERRIDDEN)\n" +
					"3: Seed as an integer. 0 will be replaced by a random seed.\n" +
                    "4...: The remaining arguments are interpreted to attribute property files.");
			System.exit(1);
        }
        String fmFilePath = args[0];
        String outputFilePath = args[1];
        AttributeConfiguration configuration = ConfigFactory.create(AttributeConfiguration.class);
        long seed;
        seed = Long.parseLong(args[2]);
        if(seed == 0) {
            seed = new Random().nextLong();
        }
        List<AbstractAttribute> attributes = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            String propertiesFilePath = args[i];
            try (InputStream input  = new FileInputStream(propertiesFilePath)) {
                Properties prop = new Properties();
                prop.load(input);
                AttributeConfiguration attributeConfiguration = ConfigFactory
                        .create(AttributeConfiguration.class, prop);
                attributes.add(AbstractAttribute.createFromConfig(attributeConfiguration));
            }
        }
        Map attributeValueCollection = new HashMap<>();
        for(AbstractAttribute attribute : attributes) {
            FeatureAttributeGenerator generator = new FeatureAttributeGenerator(seed);
            FeatureModel featureModel = new XMLFeatureModel(fmFilePath, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
            featureModel.loadModel();
            Map<String, Double> attributeValues = generator.generateWithNames(featureModel, attribute);
            Map attributeObj = new HashMap();
            attributeValueCollection.put(attribute.name(), attributeObj);
            attributeObj.put("values", attributeValues);
            attributeObj.put("aggregation", attribute.getAggregationMethod());
            attributeObj.put("minimized", attribute.isToBeMinimized());
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String attributeValuesJSON = gson.toJson(attributeValueCollection);
        FileUtil.writeStringToFile(outputFilePath, attributeValuesJSON);
	}
}
