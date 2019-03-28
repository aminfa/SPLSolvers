package de.upb.spl.hasco;

import de.upb.spl.FeatureSelection;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import hasco.model.Component;
import hasco.model.ComponentInstance;

import java.util.*;

import static de.upb.spl.FMUtil.*;

/**
 * This class provides a component model for a specific feature model.
 * It also offers transformation for a grounded component instance to its feature selection.
 */
public abstract class FM2CM {

    protected Map<FeatureTreeNode, Component> components = new HashMap<>();

    private final FeatureModel fm;

    private Component dumdum = new Component(HASCOSPLReasoner.DUMMY_COMPONENT);
    private List<Component> dumdums = new ArrayList<>();

    public FM2CM(FeatureModel fm) {
        this.fm = fm;
        addDummy(dumdum);
    }

    public Component get(FeatureTreeNode feature) {
        return components.get(feature);
    }

    public String createAlternativeChildrenInterfaceName(FeatureTreeNode feature) {
        return "i" + id(feature);
    }

    public Collection<Component> getComponents() {
        List<Component> components = new ArrayList<>(this.components.values());
        components.addAll(dumdums);
        return components;
    }

    public String createParentChildInterfaceName(FeatureTreeNode feature, FeatureTreeNode child) {
        return "i" + id(feature) + "_" + id(child);
    }

    public void addDummy(Component dummy) {
        dumdums.add(dummy);
    }

    public Component getDummy() {
        return dumdum;
    }

    public FeatureModel getFM() {
        return fm;
    }

    public abstract FeatureSelection transform(ComponentInstance object);

    public String rootInterface() {
        return "ROOT";
    }
}
