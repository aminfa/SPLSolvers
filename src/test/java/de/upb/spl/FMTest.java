package de.upb.spl;

import constraints.PropositionalFormula;
import fm.*;
import org.junit.BeforeClass;
import org.junit.Test;


public class FMTest {


	static FeatureModel featureModel;

	@BeforeClass
	public static void load() throws FeatureModelException {
		String featureModelFile = FMTest.class.getClassLoader().getResource("video_encoder_simple.xml").getPath();
		featureModel = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		// Load the XML file and creates the listFeatures model
		featureModel.loadModel();
	}

	@Test
	public void print() {
		

		// A listFeatures model object contains a listFeatures tree and a set of contraints
		// Let's traverse the listFeatures tree first. We start at the root listFeatures in depth first search.
		System.out.println("FEATURE TREE --------------------------------");
		traverseDFS(featureModel.getRoot(), 0);

		// Now, let's traverse the extra constraints as a CNF formula
		System.out.println("EXTRA CONSTRAINTS ---------------------------");
		traverseConstraints(featureModel);

		// Now, let's print some statistics about the listFeatures model
		FeatureModelStatistics stats = new FeatureModelStatistics(featureModel);
		stats.update();

		stats.dump();
			
			
	}

	@Test
	public void testIterator() {
		System.out.println("Features: ");
		FMUtil.featureStream(featureModel).forEach(feature -> System.out.println( "\t" + feature.getName() ));

	}
		
	public void traverseDFS(FeatureTreeNode node, int tab) {
		for( int j = 0 ; j < tab ; j++ ) {
			System.out.print("\t");
		}
		// Root Feature
		if ( node instanceof RootNode ) {
			System.out.print("Root");
		}
		// Solitaire Feature
		else if ( node instanceof SolitaireFeature ) {
			// Optional Feature
			if ( ((SolitaireFeature)node).isOptional())
				System.out.print("Optional");
			// Mandatory Feature
			else
				System.out.print("Mandatory");
		}
		// Feature Group
		else if ( node instanceof FeatureGroup ) {
			int minCardinality = ((FeatureGroup)node).getMin();
			int maxCardinality = ((FeatureGroup)node).getMax();
			System.out.print("Feature Group[" + minCardinality + "," + maxCardinality + "]"); 
		}
		// Grouped listFeatures
		else {
			System.out.print("Grouped");
		}
		System.out.print( "(ID=" + node.getID() + ", NAME=" + node.getName() + ")\r\n");
		for( int i = 0 ; i < node.getChildCount() ; i++ ) {
			traverseDFS((FeatureTreeNode )node.getChildAt(i), tab+1);
		}
	}
	
	public void traverseConstraints(FeatureModel featureModel) {
		for( PropositionalFormula formula : featureModel.getConstraints() ) {
			System.out.println(formula);			
		}
	}
}
