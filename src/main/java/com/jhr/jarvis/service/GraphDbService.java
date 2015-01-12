package com.jhr.jarvis.service;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    
    
    public String runCypher(String query) {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute(query);
            return result.dumpToString();
        }        
    }
    
    public String runCypherWithTransaction(String query, Map<String, Object> params) {
        try (Transaction tx = graphDb.beginTx(); )
        {
            ExecutionResult result = engine.execute(query, params);
            tx.success();
            return result.dumpToString();
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
    
}
