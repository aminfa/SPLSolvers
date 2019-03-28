package de.upb.spl.hasco;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.FeatureSet;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import hasco.model.Component;
import hasco.model.ComponentInstance;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static de.upb.spl.FMUtil.*;

public class MultiDummyReduction extends FM2CM {

    private final AtomicInteger dummyCounter = new AtomicInteger(0);
    public MultiDummyReduction(FeatureModel fm) {
        super(fm);
        createComponents();
        createInterfaces();
    }

    @Override
    public FeatureSelection transform(ComponentInstance object) {
        FeatureSelection selection = new FeatureSet();
        traverseComponentInstance(selection, object);
        return selection;
    }

    /**
     * Walks through the component instance and adds the matching features to the given selection.
     *
     * @param selection This feature selection is filled with components contained by the rootComponent instance.
     * @param rootInstance the root component instance which matches the root feature.
     */
    private void traverseComponentInstance(final FeatureSelection selection, final ComponentInstance rootInstance) {
        /*
         * Deque of unvisited component instances, i.e. the ones that weren't added to the selection.
         */
        final Deque<ComponentInstance> unvisited = new ArrayDeque<>(selection.size());
        final FeatureModel fm = getFM();

        unvisited.add(rootInstance);
        selection.add(fm.getRoot());

        while(!unvisited.isEmpty()) {
            ComponentInstance instance = unvisited.removeFirst();
            for(ComponentInstance child : instance.getSatisfactionOfRequiredInterfaces().values()) {
                String childComponentName = child.getComponent().getName();
                if(childComponentName.startsWith(HASCOSPLReasoner.DUMMY_COMPONENT)) {
                    continue;
                } else {
                    FeatureTreeNode feature = find(fm, childComponentName);
                    if(feature == null) {
                        throw new IllegalArgumentException("Component name was not recognized: " + childComponentName);
                    }
                    if(selection.add(feature)) {
                        traverseComponentInstance(selection, child);
                    }
                }
            }
        }

    }

    /**
     * Creates a component for each feature.
     */
    private void createComponents() {
        for(FeatureTreeNode featureTreeNode : listFeatures(getFM())) {
            Component c = new Component(id(featureTreeNode));
            components.put(featureTreeNode, c);
        }
    }

    /**
     * Adds required and provided interface information that enforce the parent-child relationship of the feature model.
     */
    private void createInterfaces() {
        /*
         * The root feature offers the ROOT interface:
         */
        get(getFM().getRoot()).addProvidedInterface(rootInterface());

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
                    createDummy().addProvidedInterface(optionalInterface);
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
                        createDummy().addProvidedInterface(optionalInterface);
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

//        for(CrossTreeConstraint ctc : FMUtil.crossTreeConstraints(getFM())) {
//            if(ctc.implication) {
//                String mandatoryInterface = createParentChildInterfaceName(ctc.feature1, ctc.feature2);
//                if(FMUtil.crosstreePremises(getFM(), ctc.feature1).contains(ctc.feature2)) {
//                    continue; // no recursive interfaces
//                }
//                get(ctc.feature1).addRequiredInterface(mandatoryInterface, mandatoryInterface);
//                get(ctc.feature2).addProvidedInterface(mandatoryInterface);
//            }
//        }
    }

    private Component createDummy() {
        int dummyId = dummyCounter.getAndIncrement();
        Component c = new Component(HASCOSPLReasoner.DUMMY_COMPONENT + "_" + dummyId);
        addDummy(c);
        return c;
    }

}
