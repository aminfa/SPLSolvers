package de.upb.spl.hasco;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import hasco.model.Component;
import hasco.model.ComponentInstance;

import static de.upb.spl.FMUtil.*;
import static de.upb.spl.FMUtil.id;

public class SimpleTransformation extends FM2CM {

    public SimpleTransformation(FeatureModel fm) {
        super(fm);
        createComponents();
        createInterfaces();
    }

    @Override
    public FeatureSelection transform(ComponentInstance object) {
        return null;
    }

    private void createComponents() {
        for(FeatureTreeNode featureTreeNode : listFeatures(getFM())) {
            Component c = new Component(id(featureTreeNode));
            components.put(featureTreeNode, c);
        }
    }

    private void createInterfaces() {
        for(FeatureTreeNode feature : FMUtil.listFeatures(getFM())) {
            Component component = get(feature);

            if(FMUtil.isAlternativeGroup(feature)) {
                /*
                 * The parent only requires the interface once.
                 * Each child provides it once.
                 */
                String alternativeChildInterface = createAlternativeChildrenInterfaceName(feature);
                component.addRequiredInterface(alternativeChildInterface, alternativeChildInterface);
                for(FeatureTreeNode child : children(feature)) {
                    Component childComponent = get(child);
                    childComponent.addProvidedInterface(alternativeChildInterface);
                }
            } else if(FMUtil.isOrGroup(feature)) {
                /*
                 * The parent requires the interface once and for each of its children it requires an interface
                 * that is also provided by the dummy component.
                 */
                String alternativeChildInterface = createAlternativeChildrenInterfaceName(feature);
                component.addRequiredInterface(alternativeChildInterface, alternativeChildInterface);
                for(FeatureTreeNode child : children(feature)) {
                    Component childComponent = get(child);
                    childComponent.addProvidedInterface(alternativeChildInterface);
                    /*
                     * A optional interface that only provided by this child interface
                     * and the dummy object:
                     */
                    String optionalInterface = createParentChildInterfaceName(feature, child);
                    component.addRequiredInterface(optionalInterface, optionalInterface);
                    childComponent.addProvidedInterface(optionalInterface);
                    getDummy().addProvidedInterface(optionalInterface);
                }
            } else {
                /*
                 * Parent-Child-Relations ships can only be optional or mandatory:
                 */
                for(FeatureTreeNode child : children(feature)) {
                    if(isOptionalFeature(child)) {
                        /*
                         * Optional Child
                         * Optional interface that is provided by the child or the dummy component.
                         */
                        String optionalInterface = createParentChildInterfaceName(feature, child);
                        component.addRequiredInterface(optionalInterface, optionalInterface);
                        Component childComponent = get(child);
                        childComponent.addProvidedInterface(optionalInterface);
                        getDummy().addProvidedInterface(optionalInterface);
                    } else {
                        /*
                         * Mandatory child
                         * Interface that is solely provided by the child component.
                         */
                        String mandatoryInterface = createParentChildInterfaceName(feature, child);
                        component.addRequiredInterface(mandatoryInterface, mandatoryInterface);
                        Component childComponent = get(child);
                        childComponent.addProvidedInterface(mandatoryInterface);
                    }
                }
            }
        }
    }

}
