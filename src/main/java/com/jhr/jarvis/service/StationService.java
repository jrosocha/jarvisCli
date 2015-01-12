package com.jhr.jarvis.service;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;

@Service
public class StationService {

    @Autowired
    private GraphDbService graphDbService;
    
    /**
     * Creates a station and its HAS with its system is the station is not yet present in the graph.
     * 
     * @param system
     * @param station
     * @return
     */
    public String createStationIfNotExists(StarSystem system, Station station) {
        String query = "MATCH (system:System)"
                        + " WHERE system.name={systemName}"
                        + " OPTIONAL MATCH (system)-[:HAS]->(station:Station)"
                        + " WHERE station.name={stationName}"
                        + " WITH system, station "
                        + " WHERE station IS NULL "
                        + " MERGE (system)-[has:HAS]->(station2:Station {name:{stationName}})";
        
        Map<String, Object> cypherParams = ImmutableMap.of("systemName", system.getName(), "stationName", station.getName());
        
        String out = graphDbService.runCypherWithTransaction(query, cypherParams);
        return out;
    }
    
    public String createCommodityIfNotExists(Commodity commodity) {
        
        String query = "MERGE (commodity:Commodity{name:{commodityName}}) RETURN commodity";
        
        Map<String, Object> cypherParams = ImmutableMap.of("commodityName", commodity.getName());
 
        String out = graphDbService.runCypherWithTransaction(query, cypherParams);
        return out; 
    }
    
    public String createCommodityExchangeRelationship(Station s, Commodity c, int sellPrice, int buyPrice, int supply) {
        
        String out = "";
        
        // delete old exchange data
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", s.getName(), "commodityName", c.getName());
        String deleteExchangeQuery = "MATCH (station:Station)-[e:EXCHANGE]->(commodity:Commodity) " +
                        "WHERE station.name = {stationName} " + 
                        "AND commodity.name = {commodityName} " + 
                        "DELETE e";
        
        out += graphDbService.runCypherWithTransaction(deleteExchangeQuery, cypherParams);
        
        // add new exchange data
        cypherParams = ImmutableMap.of("stationName", s.getName(), "commodityName", c.getName(), "sellPrice", sellPrice, "buyPrice", buyPrice, "supply", supply);
        //, sellPrice, "buyPrice", buyPrice, "supply", supply, "createdOn", createdOn.getTime());
        String createExchange = "MATCH (station:Station), (commodity:Commodity) " +
                                "WHERE station.name = {stationName} " + 
                                "AND commodity.name = {commodityName} " + 
                                "MERGE (station)-[exchange:EXCHANGE{buyPrice:{buyPrice}, sellPrice:{sellPrice}, supply:{supply}}]->(commodity);";
        out += graphDbService.runCypherWithTransaction(createExchange, cypherParams);
        
        return out;
    }
    
}
