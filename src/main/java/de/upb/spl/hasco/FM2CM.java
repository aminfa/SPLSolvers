package de.upb.spl.hasco;

import de.upb.spl.FeatureSelection;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import hasco.model.Component;
import hasco.model.ComponentInstance;

import java.util.HashMap;
import java.util.Map;
import static de.upb.spl.FMUtil.*;

/**
 * This class provides a component model for a feature model.
 * It also offers transformation for a grounded component to its feature selection.
 */
public abstract class FM2CM {

    protected Map<FeatureTreeNode, Component> components = new HashMap<>();

    private final FeatureModel fm;

    private Component dumdum = new Component(HascoSPLReasoner.DUMMY_COMPONENT);

    public FM2CM(FeatureModel fm) {
        this.fm = fm;
    }

    public Component get(FeatureTreeNode feature) {
        return components.get(feature);
    }

    public String createAlternativeChildrenInterfaceName(FeatureTreeNode feature) {
        return "i" + id(feature);
    }

    public String createParentChildInterfaceName(FeatureTreeNode feature, FeatureTreeNode child) {
        return "i" + id(feature) + "_" + id(child);
    }

    public Component getDummy() {
        return dumdum;
    }

    public FeatureModel getFM() {
        return fm;
    }

    public abstract FeatureSelection transform(ComponentInstance object);
}
