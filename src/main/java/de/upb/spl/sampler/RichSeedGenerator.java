package de.upb.spl.sampler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMSAT;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.PositiveLiteralSelectionStrategy;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;
import de.upb.spl.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class RichSeedGenerator {

    private static final long iteratorTimeout = 1500000;

    public static void main(String[] args) throws FeatureModelException, TimeoutException {

        if(args.length < 2) {
            System.err.println("Provide input file and output file.");
            System.exit(1);
        }

        String fmFilePath = args[0];
        String seedFilePath = args[1];
        int seeds = 10;
        if(args.length > 2) {
            seeds = Integer.parseInt(args[2]);
        }
        List<List<Integer>> generatedSeeds = new ArrayList<>();

        FeatureModel featureModel = new XMLFeatureModel(fmFilePath, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
        // Load the XML file and creates the listFeatures model
        featureModel.loadModel();

        FMSAT fmsat = FMSAT.transform(featureModel);
        Solver solver = (Solver) SolverFactory.newDefault();
        solver.setOrder( new RandomWalkDecorator(new VarOrderHeap(new PositiveLiteralSelectionStrategy()), 1));
        fmsat.insertCNF(solver);
        ISolver solverIterator = new ModelIterator(solver);
        solverIterator.setTimeoutMs(iteratorTimeout);



        while(seeds > 0 && solverIterator.isSatisfiable()) {
            seeds--;
            int[] i = solverIterator.model();
            List<Integer> model = new ArrayList<>();
            for (int j = 0; j < i.length; j++) {
                model.add(i[j]);
            }
            generatedSeeds.add(model);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(generatedSeeds);
        FileUtil.writeStringToFile(seedFilePath, jsonString);
    }
}
