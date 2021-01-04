/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.graph.gui.layouts;

import java.awt.Dimension;
import java.util.Collection;

import org.eclipse.titanium.graph.gui.layouts.algorithms.DAGLayoutAlgorithm;

import edu.uci.ics.jung.graph.Graph;

/**
 * This class implements the normal DAG layout.
 * It maybe used even on cyclic graphs.
 * @author Gabor Jenei
 *
 * @param <V> Node type
 * @param <E> Edge type
 */
public class TitaniumDAGLayout<V,E> extends BaseHierarchicalLayout<V, E> {

	/**
	 * Constructor
	 * @param g : The graph to show
	 * @param size : The size of the canvas to draw on
	 */
	public TitaniumDAGLayout(final Graph<V, E> g, final Dimension size) {
		super(g, size);
	}

	@Override
	protected Collection<V> getNeighbours(final V v) {
		return graph.getPredecessors(v);
	}

	@Override
	protected void initAlg() {
		alg = new DAGLayoutAlgorithm<V, E>(graph);
	}



}