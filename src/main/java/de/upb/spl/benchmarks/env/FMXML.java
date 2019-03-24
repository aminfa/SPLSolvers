package de.upb.spl.benchmarks.env;

import de.upb.spl.FMSAT;
import de.upb.spl.hasco.FM2CM;
import de.upb.spl.hasco.SimpleReduction;
import de.upb.spl.util.Cache;
import fm.FeatureModel;
import fm.XMLFeatureModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class FMXML extends BenchmarkEnvironmentDecoration {

    private final static Logger logger = LoggerFactory.getLogger(FMXML.class);

    private Optional<FeatureModel> fm;
    private Cache<FMSAT> fmsat;
    private Cache<FM2CM> cm;


    public FMXML(String fmXMLFile) {
        this(new BaseEnv(), fmXMLFile);
    }

    public FMXML(BenchmarkEnvironment env, String fmXMLFile) {
        super(env);

        logger.info("Loading feature model from: {}.", fmXMLFile);

        try {
            // Load the XML file and creates the listFeatures model
            FeatureModel fm = new XMLFeatureModel(fmXMLFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
            fm.loadModel();
            this.fm = Optional.of(fm);
        } catch (Exception e) {
            logger.warn("Error loading feature model at {}: {}", fmXMLFile, e);
            this.fm = Optional.empty();
        }
        this.fmsat = new Cache<>(() -> FMSAT.transform(fm.get()));
        this.cm = new Cache<>(() ->  new SimpleReduction(fm.get()));
    }


    @Override
    public FeatureModel model() {
        return fm.get();
    }

    public FM2CM componentModel() {
        return cm.get();
    }

    @Override
    public FMSAT sat() {
        return fmsat.get();
    }

}
