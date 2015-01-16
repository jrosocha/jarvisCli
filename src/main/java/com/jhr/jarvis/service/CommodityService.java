package com.jhr.jarvis.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.exceptions.CommodityNotFoundException;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Station;

@Service
public class CommodityService {

    @Autowired
    private GraphDbService graphDbService;

    @Autowired
    private Settings settings;

    @Autowired
    private ObjectMapper objectMapper;

    String userLastStoredCommodity = null;

    /**
     * Matches a commodity with @param partial If more than one commodity is
     * found, returns a \n separated string. If a single commodity is found, it
     * is loaded into memory If none is found, returns a message to that effect.
     * 
     * @param partial
     * @return
     */
    public String findCommodity(String partial) {

        String query = "MATCH (commodity:Commodity)" + " WHERE commodity.name=~{commodityName}" + " RETURN commodity.name";

        String out = "";
        Map<String, Object> cypherParams = ImmutableMap.of("commodityName", partial.toUpperCase() + ".*");

        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);

        if (results.size() == 0) {
            out += String.format("No commodity found with name starting with '%s'", partial);
        } else {
            for (Map<String, Object> res : results) {
                out += res.get("commodity.name") + OsUtils.LINE_SEPARATOR;
            }
        }

        if (results.size() == 1) {
            userLastStoredCommodity = (String) results.get(0).get("commodity.name");
        }

        return out;
    }
    
    /**
     * Matches if the commodity exists as typed
     * If a single commodity is found, it is loaded into memory
     * 
     * @param station
     * @return
     * @throws Exception 
     */
    public String findExactCommodity(String commodity) throws CommodityNotFoundException {
        
        String query = "MATCH (commodity:Commodity)" + " WHERE commodity.name={commodityName}" + " RETURN commodity.name";             

        String foundCommodity;
        Map<String, Object> cypherParams = ImmutableMap.of("commodityName", commodity.toUpperCase());
        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        if (results.size() == 0 || results.size() > 1 ) {
            throw new CommodityNotFoundException("Exact commodity '" + commodity + "' could not be identified");
        }
        foundCommodity = (String) results.get(0).get("commodity.name");
        userLastStoredCommodity = foundCommodity;
        return foundCommodity;
    }

    public String sellCommodity(Station s, Commodity c) {
        return "not foundCommodity";
    }
    
    /**
     * Runs an exact match and a partial match looking to identify a single commodity.
     * If a single commodity is found, it is loaded into memory
     * 
     * @param partial
     * @return
     * @throws Exception
     */
    public String findUniqueCommodity(String partial) throws CommodityNotFoundException {
        
        String foundCommodity = null;
        boolean found = false;
        
        try {
            foundCommodity = findExactCommodity(partial);
            found = true;
        } catch (Exception e) {
            // not an exact patch. proceed
        }
     
        if (!found) {
            String query = "MATCH (commodity:Commodity)" + " WHERE commodity.name=~{commodityName}" + " RETURN commodity.name";           
    
            Map<String, Object> cypherParams = ImmutableMap.of("commodityName", partial.toUpperCase() + ".*");
            List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
            
            if (results.size() == 0 || results.size() > 1 ) {
                throw new CommodityNotFoundException("Unique commodity could not be identified for '" + partial + "'.");
            }
            
            foundCommodity = (String) results.get(0).get("commodity.name");
        }
        userLastStoredCommodity = foundCommodity; 
        return foundCommodity;
    }
    
    /**
     * @return the userLastStoredCommodity
     */
    public String getUserLastStoredCommodity() {
        return userLastStoredCommodity;
    }

    /**
     * @param userLastStoredCommodity the userLastStoredCommodity to set
     */
    public void setUserLastStoredCommodity(String userLastStoredCommodity) {
        this.userLastStoredCommodity = userLastStoredCommodity;
    }

}
