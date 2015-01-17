package com.jhr.jarvis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.model.Settings;


@Service
public class GraphDbService {
    
    @Autowired 
    private Settings settings;

    private GraphDatabaseService graphDb = null;
    private ExecutionEngine engine = null;
    
    @PostConstruct
    private void startDb() {
        
        graphDb = new GraphDatabaseFactory()
        .newEmbeddedDatabaseBuilder(settings.getGraphDb())
        .setConfig( GraphDatabaseSettings.nodestore_mapped_memory_size, "10M" )
        .setConfig( GraphDatabaseSettings.string_block_size, "60" )
        .setConfig( GraphDatabaseSettings.array_block_size, "300" )
        .setConfig( GraphDatabaseSettings.node_keys_indexable, "name" )
        .setConfig( GraphDatabaseSettings.relationship_keys_indexable, "ly,buyPrice,sellPrice,supply" )
        .setConfig( GraphDatabaseSettings.node_auto_indexing, "true" )
        .setConfig( GraphDatabaseSettings.relationship_auto_indexing, "true" )
        .newGraphDatabase();
        
        registerShutdownHook(graphDb);
        engine = new ExecutionEngine(graphDb);

    }
    
    private void registerShutdownHook(final GraphDatabaseService graphDb)
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }
    
    public List<Map<String, Object>> runCypherNative(String query, Map<String, Object> params) {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute(query, params);
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> row: result) {
                out.add(row);
            }
            return out;
        }        
    }
    
    public String runCypher(String query) {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute(query);
            return result.dumpToString();
        }        
    }
    
    public String runCypher(String query,  Map<String, Object> params) {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute(query, params);
            return result.dumpToString();
        }        
    }
    
    public String runCypherWithTransaction(String query) {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute(query);
            tx.success();
            return result.dumpToString() + OsUtils.LINE_SEPARATOR + result.getQueryStatistics().toString();
        }        
    }
    
    public String runCypherWithTransaction(String query, Map<String, Object> params) {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute(query, params);
            tx.success();
            return result.dumpToString() + OsUtils.LINE_SEPARATOR + result.getQueryStatistics().toString();
        }        
    }
    
    public String wipeDb() {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r;");
            tx.success();
            return result.dumpToString();
        } 
    }
    
    
    //pathing work
    public void x(String fromSystem, String toSystem) {
        
        try (Transaction tx = graphDb.beginTx();) {
        
            Map<String, Object> cypherParams = ImmutableMap.of("fromSystem", fromSystem, "toSystem", toSystem);
            ExecutionResult result = engine.execute("MATCH (fromSystem:System),(toSystem:System) WHERE fromSystem.name = {fromSystem} AND toSystem.name = {toSystem} RETURN fromSystem, toSystem", cypherParams);
            
            //result.
            
        
        }
        
        
//        Label label = DynamicLabel.label( "System" );
//        
//        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(
//                PathExpanders.forTypeAndDirection( RelationshipType., Direction.BOTH ), "ly" );
//
//            WeightedPath path = finder.findSinglePath( nodeA, nodeB );
//
//            // Get the weight for the found path
//            path.weight();     

    }
    
}
