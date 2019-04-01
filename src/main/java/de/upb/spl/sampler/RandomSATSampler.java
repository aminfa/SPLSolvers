package de.upb.spl.sampler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMSAT;
import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.util.FileUtil;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.RandomLiteralSelectionStrategy;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RandomSATSampler {

    private static final long iteratorTimeout = 150000;

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
        List<List<String>> generatedSeeds = new ArrayList<>();

        FeatureModel featureModel = new XMLFeatureModel(fmFilePath, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
        // Load the XML file and creates the listFeatures model
        featureModel.loadModel();

        FMSAT fmsat = FMSAT.transform(featureModel);
        Solver solver = (Solver) SolverFactory.newDefault();
        solver.setOrder( new RandomWalkDecorator(new VarOrderHeap(new RandomLiteralSelectionStrategy()), 1));
        fmsat.insertCNF(solver);
        ISolver solverIterator = new ModelIterator(solver);
        solverIterator.setTimeoutMs(iteratorTimeout);



        while(seeds > 0 && solverIterator.isSatisfiable()) {
            seeds--;
            int[] i = solverIterator.model();
            FeatureSelection selection = fmsat.toSelection(featureModel, i);
            List<String> featureList = selection.stream().map(FMUtil::id).collect(Collectors.toList());
            generatedSeeds.add(featureList);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(generatedSeeds);
        FileUtil.writeStringToFile(seedFilePath, jsonString);
    }
}
