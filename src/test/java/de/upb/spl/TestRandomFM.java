package de.upb.spl;

import constraints.PropositionalFormula;
import fm.*;
import org.junit.Test;

public class TestRandomFM {
	@Test
	public void random1() throws FeatureModelException {
		RandomFeatureModel featureModel = new
				RandomFeatureModel("Random1", 10000, 5,5,1,1,
				0, 5, 10, 0 );
		featureModel.loadModel();

		// A listFeatures model object contains a listFeatures tree and a set of contraints
		// Let's traverse the listFeatures tree first. We start at the root listFeatures in depth first search.
//		System.out.println("FEATURE TREE --------------------------------");
		traverseDFS(featureModel.getRoot(), 0);

		// Now, let's traverse the extra constraints as a CNF formula
//		System.out.println("EXTRA CONSTRAINTS ---------------------------");
//		traverseConstraints(featureModel);

		// Now, let's print some statistics about the listFeatures model
//		FeatureModelStatistics stats = new FeatureModelStatistics(featureModel);
//		stats.update();

//		stats.dump();


	}

	public void traverseDFS(FeatureTreeNode node, int tab) {
		for( int j = 0 ; j < tab ; j++ ) {
			System.out.print("\t");
		}
		// Root Feature
		if ( node instanceof RootNode) {
			System.out.print(":r");
		}
		// Solitaire Feature
		else if ( node instanceof SolitaireFeature) {
			// Optional Feature
			if ( ((SolitaireFeature)node).isOptional())
				System.out.print(":o");
				// Mandatory Feature
			else
				System.out.print(":m");
		}
		// Feature Group
		else if ( node instanceof FeatureGroup ) {
			int minCardinality = ((FeatureGroup)node).getMin();
			int maxCardinality = ((FeatureGroup)node).getMax();
			System.out.print(":g [" + minCardinality + "," + maxCardinality + "]");
		}
		// Grouped listFeatures
		else {
			System.out.print(":");
		}
		System.out.print( " " + node.getName() +"(" + node.getID() + ")\n");
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
