package com.jhr.jarvis.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;

@Service
public class TradeService {

    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * runs the 1 hop from where you are search, providing 5 best options
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public String go(String fromStation, Ship s) throws JsonParseException, JsonMappingException, IOException {

        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)-[sell:EXCHANGE]-(fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT]-(toSystem:System)-[:HAS]-(toStation:Station)-[buy:EXCHANGE]-(commodity)" 
                        + " WHERE fromStation.name={fromStation}"
                        + " AND shift.ly <= {distance}"
                        + " AND sell.sellPrice > 0"
                        + " AND sell.supply >= {cargo}"
                        + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
                        + " AND buy.buyPrice > 0"
                        + " AND buy.buyPrice > sell.sellPrice"
                        + " RETURN DISTINCT" 
                        + " fromStation.name as `FROM STATION`," 
                        + " toStation.name as `TO STATION`," 
                        + " toSystem.name as `TO SYSTEM`," 
                        + " commodity.name as `COMMODITY`," 
                        + " (buy.buyPrice - sell.sellPrice) as `PER UNIT PROFIT`,"
                        + " (sell.sellPrice * {cargo}) as `CARGO COST`,"
                        + " (buy.buyPrice * {cargo}) as `CARGO SOLD FOR`,"
                        + " (buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as `CARGO PROFIT`"
                        //+ " shift.ly as `DISTANCE`"
                        + " ORDER BY `CARGO PROFIT` DESC"
                        + " LIMIT 5 ";   
        
        return graphDbService.runCypher(query, cypherParams);        
    }
    
    /**
     * provides a 2 stop trade solution for systems that are one jump apart
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public String go2(String fromStation, Ship s) throws JsonParseException, JsonMappingException, IOException {

        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = 
        "MATCH (commodity)-[sell:EXCHANGE]-(fSta:Station)-[:HAS]-(fSys:System)-[shift:FRAMESHIFT]-(tSys:System)-[:HAS]-(tSta:Station)-[buy:EXCHANGE]-(commodity)"
        + " WHERE fSta.name={fromStation}"
        + " AND shift.ly <= {distance}"
        + " AND sell.sellPrice > 0"
        + " AND sell.supply > {cargo}"
        + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
        + " AND buy.buyPrice > 0"
        + " AND buy.buyPrice > sell.sellPrice"
        + " WITH fSys as fromSystem,"
        + " fSta as fromStation," 
        + " tSta as toStation," 
        + " tSys as toSystem," 
        + " commodity as hop1Commodity," 
        + " (buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as profit1," 
        + " shift.ly as ly1"
        + " MATCH (commodity2)-[sell2:EXCHANGE]-(toStation)-[:HAS]-(toSystem)-[shift2:FRAMESHIFT]-(tSys2:System)-[:HAS]-(tSta2:Station)-[buy2:EXCHANGE]-(commodity2)"
        + " WHERE shift2.ly <= {distance}"
        + " and sell2.sellPrice > 0"
        + " AND {cash} - (sell2.sellPrice * {cargo}) > 0"
        + " and sell2.supply > {cargo}"
        + " and buy2.buyPrice > 0"
        + " and buy2.buyPrice > sell2.sellPrice"
        + " RETURN DISTINCT"
        //+ " fromSystem.name as startSystem," 
        //+ " fromStation.name as startStation," 
        + " hop1Commodity.name as `COMMODITY 1`,"
        + " profit1 as `PROFIT 1`,"
        //+ " ly1,"
        + " toSystem.name as `SYSTEM 1`," 
        + " toStation.name as `STATION 1`,"
        + " commodity2.name as `COMMODITY 2`," 
        + " (buy2.buyPrice * {cargo}) - (sell2.sellPrice * {cargo}) as `PROFIT 2`," 
        //+ " shift2.ly as ly2,"
        + " tSys2.name as `SYSTEM 2`," 
        + " tSta2.name as `STATION 2`," 
        + " profit1 + ((buy2.buyPrice * {cargo}) - (sell2.sellPrice * {cargo})) as `TRIP PROFIT`"
        + " ORDER BY `TRIP PROFIT` DESC"
        + " LIMIT 5";
        
        return graphDbService.runCypher(query, cypherParams);
    }
    
    
    public String go4(String fromStation, Ship s) throws JsonParseException, JsonMappingException, IOException {

        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)-[sell:EXCHANGE]-(fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT]-(toSystem:System)-[:HAS]-(toStation:Station)-[buy:EXCHANGE]-(commodity)" 
                        + " WHERE fromStation.name={fromStation}"
                        + " AND shift.ly <= {distance}"
                        + " AND sell.sellPrice > 0"
                        + " AND sell.supply >= {cargo}"
                        + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
                        + " AND buy.buyPrice > 0"
                        + " AND buy.buyPrice > sell.sellPrice"
                        + " RETURN DISTINCT" 
                        + " fromStation.name as `FROM STATION`," 
                        + " toStation.name as `TO STATION`," 
                        + " toSystem.name as `TO SYSTEM`," 
                        + " commodity.name as `COMMODITY`," 
                        + " (buy.buyPrice - sell.sellPrice) as `PER UNIT PROFIT`,"
                        + " (sell.sellPrice * {cargo}) as `CARGO COST`,"
                        + " (buy.buyPrice * {cargo}) as `CARGO SOLD FOR`,"
                        + " (buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as `CARGO PROFIT`"
                        //+ " shift.ly as `DISTANCE`"
                        + " ORDER BY `CARGO PROFIT` DESC"
                        + " LIMIT 5 ";   
        
        return objectMapper.writeValueAsString(graphDbService.runCypherNative(query, cypherParams));        
    }

    public String gon(String fromStation, Ship s, int hops) {
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)-[sell:EXCHANGE]-(fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT*1.."+ hops +"]-(toSystem:System)-[:HAS]-(toStation:Station)-[buy:EXCHANGE]-(commodity)" 
                        + " WHERE fromStation.name={fromStation}"
                        + " AND ALL(x IN shift WHERE x.ly <= {distance})"                     
                        + " AND sell.sellPrice > 0"
                        + " AND sell.supply >= {cargo}"
                        + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
                        + " AND buy.buyPrice > 0"
                        + " AND buy.buyPrice > sell.sellPrice"
                        + " RETURN DISTINCT" 
                        + " fromStation.name as `FROM STATION`," 
                        + " toStation.name as `TO STATION`," 
                        + " toSystem.name as `TO SYSTEM`," 
                        + " commodity.name as `COMMODITY`," 
                        + " (buy.buyPrice - sell.sellPrice) as `PER UNIT PROFIT`,"
                        + " (sell.sellPrice * {cargo}) as `CARGO COST`,"
                        + " (buy.buyPrice * {cargo}) as `CARGO SOLD FOR`,"
                        + " (buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as `CARGO PROFIT`"
                        + " ORDER BY `CARGO PROFIT` DESC"
                        + " LIMIT 5 ";   
        
        return graphDbService.runCypher(query, cypherParams); 
    }
    
    /**
     * provides a 2 stop trade solution for systems that are 1..n jumps apart
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public String gon2(String fromStation, Ship s, int jumps) throws JsonParseException, JsonMappingException, IOException {

        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = 
        "MATCH (commodity)-[sell:EXCHANGE]-(fSta:Station)-[:HAS]-(fSys:System)-[shift:FRAMESHIFT*1.."+ jumps +"]-(tSys:System)-[:HAS]-(tSta:Station)-[buy:EXCHANGE]-(commodity)"
        + " WHERE fSta.name={fromStation}"
        + " AND ALL(x IN shift WHERE x.ly <= {distance})"
        + " AND sell.sellPrice > 0"
        + " AND sell.supply > {cargo}"
        + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
        + " AND buy.buyPrice > 0"
        + " AND buy.buyPrice > sell.sellPrice"
        + " WITH fSys as fromSystem,"
        + " fSta as fromStation," 
        + " tSta as toStation," 
        + " tSys as toSystem," 
        + " commodity as hop1Commodity," 
        + " (buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as profit1" 
        + " MATCH (commodity2)-[sell2:EXCHANGE]-(toStation)-[:HAS]-(toSystem)-[shift2:FRAMESHIFT*1.."+ jumps +"]-(tSys2:System)-[:HAS]-(tSta2:Station)-[buy2:EXCHANGE]-(commodity2)"
        + " WHERE sell2.sellPrice > 0"
        + " AND ALL(x IN shift2 WHERE x.ly <= {distance})"
        + " AND {cash} - (sell2.sellPrice * {cargo}) > 0"
        + " and sell2.supply > {cargo}"
        + " and buy2.buyPrice > 0"
        + " and buy2.buyPrice > sell2.sellPrice"
        + " RETURN DISTINCT"
        + " hop1Commodity.name as `COMMODITY 1`,"
        + " profit1 as `PROFIT 1`,"
        + " toSystem.name as `SYSTEM 1`," 
        + " toStation.name as `STATION 1`,"
        + " commodity2.name as `COMMODITY 2`," 
        + " (buy2.buyPrice * {cargo}) - (sell2.sellPrice * {cargo}) as `PROFIT 2`," 
        + " tSys2.name as `SYSTEM 2`," 
        + " tSta2.name as `STATION 2`," 
        + " profit1 + ((buy2.buyPrice * {cargo}) - (sell2.sellPrice * {cargo})) as `TRIP PROFIT`"
        + " ORDER BY `TRIP PROFIT` DESC"
        + " LIMIT 5";
        
        return graphDbService.runCypher(query, cypherParams);
    }

    
}
