/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.LinkedList;

/**
 * MHRIFocusTraversalPolicy
 *
 * @date 02.12.2014
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class MHRIFocusTraversalPolicy extends FocusTraversalPolicy {

    LinkedList<Component> order;

    public MHRIFocusTraversalPolicy(LinkedList<Component> order) {
        this.order = order;
        this.order.addAll(order);
    }

    @Override
    public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
        int idx = (order.indexOf(aComponent) + 1) % order.size();
        return order.get(idx);
    }

    @Override
    public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
        int idx = order.indexOf(aComponent) - 1;
        if (idx < 0) {
            idx = order.size() - 1;
        }
        return order.get(idx);
    }

    @Override
    public Component getDefaultComponent(Container focusCycleRoot) {
        return order.get(0);
    }

    @Override
    public Component getLastComponent(Container focusCycleRoot) {
        return order.getLast();
    }

    @Override
    public Component getFirstComponent(Container focusCycleRoot) {
        return order.getFirst();
    }
}
