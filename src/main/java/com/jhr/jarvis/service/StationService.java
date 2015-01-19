package com.jhr.jarvis.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.parboiled.common.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.table.TableRenderer;

@Service
public class StationService {

    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private CommodityService commodityService;
       
    private String userLastStoredStation = null;
    
    /**
     * Gives an exact match on the station passed in, the unique station found matching what was passed in, the in memory store of a station of nothing was passed in, or an exception.
     * 
     * @param station
     * @param usage
     * @return
     * @throws StationNotFoundException
     */
    public String getBestMatchingStationOrStoredStation(String station) throws StationNotFoundException {
        
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
    public String findUniqueStation(String partial, boolean loadIntoMemory) throws StationNotFoundException {
        
        String foundStation = null;
        boolean found = false;
        
        try {
            foundStation = findExactStation(partial);
            found = true;
        } catch (Exception e) {
            // not an exact patch. proceed
        }
     
        if (!found) {
            String query = "MATCH (station:Station)"
                            + " WHERE station.name=~{stationName}"
                            + " RETURN station.name";                
    
            Map<String, Object> cypherParams = ImmutableMap.of("stationName", partial.toUpperCase() + ".*");
            List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
            
            if (results.size() == 0 || results.size() > 1 ) {
                throw new StationNotFoundException("Unique station could not be identified for '" + partial + "'.");
            }
            
            foundStation = (String) results.get(0).get("station.name");
        }
        if (loadIntoMemory) {
            userLastStoredStation = foundStation; 
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
    public String findExactStation(String station) throws StationNotFoundException {
        String query = "MATCH (station:Station)"
                + " WHERE station.name={stationName}"
                + " RETURN station.name";                

        String foundStation = "";
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", station.toUpperCase());
        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        if (results.size() == 0 || results.size() > 1 ) {
            throw new StationNotFoundException("Exact station '" + station + "' could not be identified");
        }
        foundStation = (String) results.get(0).get("station.name");
        userLastStoredStation = foundStation;
        return foundStation;
    }

     /**
     * Matches a starting starting with @param partial 
     * If more than one station is found, returns \n separated string.
     * If a single station is found, it is loaded into memory
     * If none is found, returns a message to that effect.
     * 
     * @param partial
     * @return
     */
    public String findStation(String partial) {
        
        String query = "MATCH (station:Station)"
                        + " WHERE station.name=~{stationName}"
                        + " RETURN station.name";                

        String out = "";
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", partial.toUpperCase() + ".*");
        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        if (results.size() == 0) {
            out += String.format("No station found with name starting with '%s'", partial);
        } else {
            for (Map<String, Object> res: results) {
                out += res.get("station.name") + OsUtils.LINE_SEPARATOR;
            }
        }
        
        if (results.size() == 1) {
            userLastStoredStation = (String) results.get(0).get("station.name");
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
    
    public String stationDetails(String stationName) {
        
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
        
        Map<String, Object> cypherParams = ImmutableMap.of("stationName", stationName);
        
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
    
    /**
     * @return the userLastStoredStation
     */
    public String getUserLastStoredStation() {
        return userLastStoredStation;
    }

    /**
     * @param userLastStoredStation the userLastStoredStation to set
     */
    public void setUserLastStoredStation(String findStationUniqueResult) {
        this.userLastStoredStation = findStationUniqueResult;
    }
    
}
