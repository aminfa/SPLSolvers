package de.upb.spl.guo11;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.FeatureSet;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FesTransformTest {

	static FeatureModel featureModel;
	static Random random = new Random(-2);

	@BeforeClass
	public static void load() throws FeatureModelException {
		String featureModelFile = FesTransformTest.class.getClassLoader().getResource("video_encoder_simple.xml").getPath();
		featureModel = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		// Load the XML file and creates the listFeatures model
		featureModel.loadModel();
	}

	@Test
	public void testFesTransform() {
		FeatureSelection selection = new FeatureSet(featureModel, "small", "fast");
		FesTransform transformation = new FesTransform(featureModel, selection, random);
		System.out.println(transformation);

		Assert.assertTrue(FMUtil.isValidSelection(featureModel, transformation.getValidSelection()));
	}

	@Test
	public void testAll() {
		int optionalFeatures = FMUtil.countFeatures(featureModel);
		final Function<Integer, String> binarize = i -> String.format("%" + optionalFeatures +  "s", Integer.toBinaryString(i)).replace(' ', '0');
		final Function<String, FeatureSelection> selectioner = binaryString -> FMUtil.selectFromPredicate(FMUtil.featureStream(featureModel).collect(Collectors.toList()),
				binaryString.chars().mapToObj(ch -> (char)ch).map(ch -> ch.equals('1')).collect(Collectors.toList())::get);

		int variants = (int) Math.pow(2, optionalFeatures);
		int countValids = 0;
		List<FeatureSelection> validSelections = new ArrayList<>();
		for(int i = 0; i < variants; i++) {
			String binaryString = binarize.apply(i);
			System.out.println("Binary Selection String: " + binaryString);
			FeatureSelection selection = selectioner.apply(binaryString);
//			System.out.println("Selection: " + assemble);
			FesTransform transformation = new FesTransform(featureModel, selection, random);
			boolean validSelection = FMUtil.isValidSelection(featureModel, transformation.getValidSelection());
			System.out.println(transformation + " is " + (validSelection? "a" : "NOT a") + " valid assemble!\n\n");
			if(validSelection) {
				validSelections.add(transformation.getValidSelection());
				countValids++;
			}

		}
		System.out.println(countValids + "/" + variants + " valid variants.");

		/*
		 * Count unique variants:
		 */
		List<FeatureSelection> uniqueVariants = new ArrayList<>();
		for(FeatureSelection selection : validSelections) {
			boolean unique = true;
			for(FeatureSelection otherSelection : uniqueVariants) {
				if(selection.isSelectionEqual(featureModel.getNodes().iterator(), otherSelection)) {
					unique = false;
					break;
				}
			}
			if(unique) {
				uniqueVariants.add(selection);
			}
		}
		System.out.println(uniqueVariants.size() + " unique variants.");
	}
}
