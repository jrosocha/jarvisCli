package com.jhr.jarvis.orientDb.annotations;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class OrientDb {

	private OrientGraph graph;
	
	public OrientDb(OrientGraph graph) {
		super();
		this.graph = graph;
	}

	public OrientDb() {
	}

	public OrientGraph getGraph() {
		return graph;
	}

	public void setGraph(OrientGraph graph) {
		this.graph = graph;
	}
}
