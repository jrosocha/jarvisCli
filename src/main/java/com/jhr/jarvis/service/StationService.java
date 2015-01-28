package com.jhr.jarvis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.parboiled.common.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.table.TableRenderer;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

@Service
public class StationService {

    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private CommodityService commodityService;
    
    @Autowired
    private StarSystemService starSystemService;
    
    @Autowired
    private OrientDbService orientDbService;
       
    private Station userLastStoredStation = null;
    
    
    public Map<String, Commodity> getStationBuyCommodities(Vertex stationVertex, Ship ship) {
        Map<String, Commodity> stationBuyCommodities = new HashMap<>();
        for (Edge exchange: stationVertex.getEdges(Direction.OUT, "Exchange")) {            

            int sellPrice = exchange.getProperty("sellPrice");
            int buyPrice = exchange.getProperty("buyPrice");
            int supply = exchange.getProperty("supply");
            int demand = exchange.getProperty("demand");
                       
            if (buyPrice > 0 && supply >= ship.getCargoSpace() && (buyPrice * ship.getCargoSpace()) <= ship.getCash()) {
                Vertex commodityVertex = exchange.getVertex(Direction.IN);
                Commodity commodity = new Commodity(commodityVertex.getProperty("name"), buyPrice, supply, sellPrice, demand);
                stationBuyCommodities.put(commodity.getName(), commodity);
            }
        }
        return stationBuyCommodities;
    }
    
    public Map<String, Commodity> getReleventStationSellCommodities(Vertex stationVertex, Map<String, Commodity> buyCommodities, Ship ship) {
        Map<String, Commodity> stationSellReleventCommodities = new HashMap<>();
        for (Edge exchange: stationVertex.getEdges(Direction.OUT, "Exchange")) {            

            int sellPrice = exchange.getProperty("sellPrice");
            int buyPrice = exchange.getProperty("buyPrice");
            int supply = exchange.getProperty("supply");
            int demand = exchange.getProperty("demand");
            
            if (demand > ship.getCargoSpace() && sellPrice > 0) {
                Vertex commodityVertex = exchange.getVertex(Direction.IN);
                Commodity sellCommodity = new Commodity(commodityVertex.getProperty("name"), buyPrice, supply, sellPrice, demand);
                Commodity boughtCommodity = buyCommodities.get(sellCommodity.getName());
                
                if (boughtCommodity != null && boughtCommodity.getBuyPrice() < sellCommodity.getSellPrice()) {
                    stationSellReleventCommodities.put(sellCommodity.getName(), sellCommodity);
                }
            }
        }
        
        return stationSellReleventCommodities;
    }
    
    /**
     * Gives an exact match on the station passed in, the unique station found matching what was passed in, the in memory store of a station of nothing was passed in, or an exception.
     * 
     * @param station
     * @param usage
     * @return
     * @throws StationNotFoundException
     */
    public Station getBestMatchingStationOrStoredStation(String station) throws StationNotFoundException {
        
        if (station == null && getUserLastStoredStation() != null) {
            return getUserLastStoredStation();
        } else if (!StringUtils.isEmpty(station)) {
            return findUniqueStation(station, true);            
        }
        
        throw new StationNotFoundException("No unique station could be found.");
    }
    
    /**
     * Runs an exact match and a partial match looking to identify a single station.
     * If a single station is found, it is loaded into memory
     * 
     * @param partial
     * @return
     * @throws Exception
     */
    public Station findUniqueStation(String partial, boolean loadIntoMemory) throws StationNotFoundException {
        
        Station foundStation = null;
        boolean found = false;
        
        try {
            foundStation = findExactStation(partial);
            found = true;
        } catch (Exception e) {
            // not an exact patch. proceed
        }
     
        if (!found) {
            String query = "MATCH (system:System)-[:HAS]->(station:Station)"
                            + " WHERE station.name=~{stationName}"
                            + " RETURN system.name, station.name";                
    
            Map<String, Object> cypherParams = ImmutableMap.of("stationName", partial.toUpperCase() + ".*");
            List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
            
            if (results.size() == 0 || results.size() > 1 ) {
                throw new StationNotFoundException("Unique station could not be identified for '" + partial + "'.");
            }
            foundStation = new Station((String) results.get(0).get("station.name"), (String) results.get(0).get("system.name"));
        }
        
        if (loadIntoMemory) {
            setUserLastStoredStation(foundStation);
        }
        return foundStation;
    }
 
    /**
     * Matches if the station exists as typed
     * If a single station is found, it is loaded into memory
     * 
     * @param station
     * @return
     * @throws Exception 
     */
    public Station findExactStation(String station) throws StationNotFoundException {
        String query = "MATCH (system:System)-[:HAS]->(station:Station)"
                + " WHERE station.name={stationName}"
                + " RETURN system.name, station.name";                

        Station foundStation = null;
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", station.toUpperCase());
        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        if (results.size() == 0 || results.size() > 1 ) {
            throw new StationNotFoundException("Exact station '" + station + "' could not be identified");
        }
        foundStation = new Station((String) results.get(0).get("station.name"), (String) results.get(0).get("system.name"));
        
        setUserLastStoredStation(foundStation);
        return foundStation;
    }


    public List<Station> findStations(String partial) {
        
        String query = "MATCH (system:System)-[:HAS]->(station:Station)"
                        + " WHERE station.name=~{stationName}"
                        + " RETURN station.name, system.name";                

        List<Station> out = new ArrayList<>();
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", partial.toUpperCase() + ".*");        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        for (Map<String, Object> res: results) {
            out.add(new Station((String) res.get("station.name"), (String) res.get("system.name")));                
        }
        
        if (out.size() == 1) {
            setUserLastStoredStation(out.get(0));
        }
        
        return out;
    }
    
    
    
    /**
     * Creates a station and its HAS with its system is the station is not yet present in the graph.
     * 
     * @param system
     * @param station
     * @return
     */
    public void createStationOrientDb(StarSystem system, Station station) {
        
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            OrientVertex vertexStation = (OrientVertex) graph.getVertexByKey("Station.name", station.getName());
            if (vertexStation == null) {
                vertexStation = graph.addVertex("class:Station");
                vertexStation.setProperty("name", station.getName());
                
                OrientVertex vertexSystem = (OrientVertex) graph.getVertexByKey("System.name", system.getName());
                graph.addEdge(vertexSystem.getProperty("name") + "-" + station.getName(), vertexSystem, vertexStation, "Has");
                
            }
            graph.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (graph != null) {
                graph.rollback();
            }
        }
    }
    
    public void createCommodityExchangeRelationshipOrientDb(Station station, Commodity commodity, int sellPrice, int buyPrice, int supply, int demand, long date) {
        
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            OrientVertex vertexStation = (OrientVertex) graph.getVertexByKey("Station.name", station.getName());
            OrientVertex vertexCommodity = (OrientVertex) graph.getVertexByKey("Commodity.name", commodity.getName());
            OrientEdge edgeExchange = graph.addEdge(station.getName() + "-" + commodity.getName(), vertexStation, vertexCommodity, "Exchange");
            edgeExchange.setProperty("sellPrice", sellPrice);
            edgeExchange.setProperty("buyPrice", buyPrice);
            edgeExchange.setProperty("supply", supply);
            edgeExchange.setProperty("demand", demand);
            edgeExchange.setProperty("date", date);
            graph.commit();
        } catch (Exception e) {
            graph.rollback();
        }
    }

    public void createCommodityOrientDb(Commodity commodity) {
        
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            OrientVertex vertexCommodity = (OrientVertex) graph.getVertexByKey("Commodity.name", commodity.getName());
            if (vertexCommodity == null) {
                vertexCommodity = graph.addVertex("class:Commodity");
                vertexCommodity.setProperty("name", commodity.getName());
            }
            graph.commit();
        } catch (Exception e) {
            graph.rollback();
        }
    }
    
    public int clearStationOfExchangesOrientDb(Station station) {
        int edgesRemoved = 0;
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            OrientVertex vertexStation = (OrientVertex) graph.getVertexByKey("Station.name", station.getName());
            for (Edge exchange: vertexStation.getEdges(Direction.OUT, "Exchange")) {
                exchange.remove();
                edgesRemoved++;
            };
            
        } catch (Exception e) {
            graph.rollback();
        }
        return edgesRemoved;
    }
    
    
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
    
    /**
     * Deletes all [:EXCHANGE] edges from a station
     * @param s
     * @return
     */
    public String clearStationOfExchanges(Station s) {
        
        String query = "MATCH (station:Station)-[e:EXCHANGE]->(:Commodity) WHERE station.name = {stationName} DELETE e;";
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", s.getName());
        return graphDbService.runCypherWithTransaction(query, cypherParams);
    }
    
    public String createCommodityExchangeRelationship(Station s, Commodity c, int sellPrice, int buyPrice, int supply, int demand, long date) {
        
        String out = "";
        // add new exchange data
        Map<String, Object> cypherParams2 = new HashMap<>();
        cypherParams2.put("stationName", s.getName());
        cypherParams2.put("commodityName", c.getName());
        cypherParams2.put("sellPrice", sellPrice);
        cypherParams2.put("buyPrice", buyPrice);
        cypherParams2.put("supply", supply);
        cypherParams2.put("demand", demand);      
        cypherParams2.put("date", date);
        
        //, sellPrice, "buyPrice", buyPrice, "supply", supply, "createdOn", createdOn.getTime());
        String createExchange = "MATCH (station:Station), (commodity:Commodity) " +
                                "WHERE station.name = {stationName} " + 
                                "AND commodity.name = {commodityName} " + 
                                "MERGE (station)-[exchange:EXCHANGE{buyPrice:{buyPrice}, sellPrice:{sellPrice}, supply:{supply}, demand:{demand}, date:{date}}]->(commodity);";
        out += graphDbService.runCypherWithTransaction(createExchange, cypherParams2);
        
        return out;
    }
    
    public String stationDetails(Station station) {
        
        String query = "MATCH (system:System)-[:HAS]->(station:Station)-[ex:EXCHANGE]->(commodity:Commodity)"
                     + "WHERE station.name = {stationName} "
                     + "RETURN system.name AS `SYSTEM`, "
                     + "station.name AS `STATION`, "
                     + "commodity.name AS `COMMODITY`, "
                     + "ex.buyPrice AS `BUY @`, "
                     + "ex.supply AS `SUPPLY`, "
                     + "ex.sellPrice AS `SELL @`, "
                     + "ex.demand AS `DEMAND`, "
                     + "ROUND((timestamp() - ex.date)/1000/60/60/24) AS `DAYS OLD`";
        
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", station.getName());
        
        List<Map<String, Object>> result = graphDbService.runCypherNative(query, cypherParams);
        
        List<Map<String, Object>> sortedResult = result.parallelStream().map(inRecord->{
            Map<String,Object> row = new HashMap<>(inRecord);
            Commodity c = null;
            try {
                c = commodityService.getCommodityByName((String) row.get("COMMODITY"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            String group = c != null ? c.getGroup() : "_UNKNOWN"; 
            row.put("GROUP", group);
            return row;
        }).filter(map->{
            if ((int) map.get("SUPPLY") == 0 && (int) map.get("DEMAND") == 0) {
                return false;
            }
            return true;
        }).sorted((row1,row2)->{
            String group1 = (String) row1.get("GROUP");
            String group2 = (String) row2.get("GROUP");
            String comm1 = (String) row1.get("COMMODITY");
            String comm2 = (String) row2.get("COMMODITY");
            if (group1.equals(group2)) {
                return comm1.compareTo(comm2);
            }
            return group1.compareTo(group2);
        }).collect(Collectors.toList());
        
        String out = OsUtils.LINE_SEPARATOR;
        out += "System: " + sortedResult.get(0).get("SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "Station: " + sortedResult.get(0).get("STATION") + OsUtils.LINE_SEPARATOR;
        out += "Data Age in Days: " + sortedResult.get(0).get("DAYS OLD") + OsUtils.LINE_SEPARATOR + OsUtils.LINE_SEPARATOR;
        out += TableRenderer.renderMapDataAsTable(sortedResult, ImmutableList.of("GROUP", "COMMODITY", "BUY @", "SUPPLY", "SELL @", "DEMAND"));
        return out;

    }
    
    public String joinStationsAsString(List<Station> stations) {
        return stations.stream().map(s->{ return s.getName() + " @ " + s.getSystem();}).collect(Collectors.joining(OsUtils.LINE_SEPARATOR));
    }
    
    public List<Station> getStationsForSystem(String system) {
    
        String query = "MATCH (system:System)-[:HAS]->(station:Station)-[ex:EXCHANGE]-(:Commodity)"
                        + " WHERE system.name={systemName}"
                        + " RETURN DISTINCT station.name, system.name, MAX(ex.date) AS LAST_UPDATED";                

        List<Station> out = new ArrayList<>();
        Map<String, Object> cypherParams = ImmutableMap.of("systemName", system);        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        for (Map<String, Object> res: results) {
            out.add(new Station((String) res.get("station.name"), (String) res.get("system.name"), (long) res.get("LAST_UPDATED")));                
        }
        
        return out;
    }

    public long stationCountOrientDb() {
        
        long stationCount = 0;
        try {
            OrientGraph graph = orientDbService.getFactory().getTx();
            stationCount = graph.countVertices("Station");
            graph.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stationCount;        
    }
    
    public long stationCount() {
        
        long stationCount = 0;
        String query = "MATCH (station:Station)"
                + " RETURN COUNT(station) AS `STATION COUNT`";
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, new HashMap<>());
        stationCount = (long) results.get(0).get("STATION COUNT");
        return stationCount;
    }
    
    /**
     * @return the userLastStoredStation
     */
    public Station getUserLastStoredStation() {
        return userLastStoredStation;
    }

    /**
     * @param userLastStoredStation the userLastStoredStation to set
     */
    public void setUserLastStoredStation(Station station) {
        this.userLastStoredStation = station;
        starSystemService.setUserLastStoredSystem(station.getSystem());
    }
    
}
