package com.jhr.jarvis.service;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
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
        Date start = new Date();
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
            + " ORDER BY `CARGO PROFIT` DESC"
            + " LIMIT 5 ";   
        
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";        
    }
    
    /**
     * provides a 2 stop trade solution for systems that are one jump apart
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public String go2(String fromStation, Ship s) throws JsonParseException, JsonMappingException, IOException {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)-[sell:EXCHANGE]-(fSta:Station)-[:HAS]-(fSys:System)-[shift:FRAMESHIFT]-(tSys:System)-[:HAS]-(tSta:Station)-[buy:EXCHANGE]-(commodity)"
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
            
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    

    public String gon(String fromStation, Ship s, int hops) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)<-[sell:EXCHANGE]-(fromStation:Station)<-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT*1.."+ hops +"]-(toSystem:System)-[:HAS]->(toStation:Station)-[buy:EXCHANGE]->(commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND sell.sellPrice > 0"
            + " AND sell.supply >= {cargo}"
            + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
            + " AND buy.buyPrice > 0"
            + " AND buy.buyPrice > sell.sellPrice"
            + " AND ALL(x IN shift WHERE x.ly <= {distance})"
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
        
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    
    /**
     * provides a 2 stop trade solution for systems that are 1..n jumps apart
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public String gon2(String fromStation, Ship s, int jumps) throws JsonParseException, JsonMappingException, IOException {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)-[sell:EXCHANGE]-(fSta:Station)-[:HAS]-(fSys:System)-[shift:FRAMESHIFT*1.."+ jumps +"]-(tSys:System)-[:HAS]-(tSta:Station)-[buy:EXCHANGE]-(commodity)"
            + " WHERE fSta.name={fromStation}"
            + " AND sell.sellPrice > 0"
            + " AND sell.supply > {cargo}"
            + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
            + " AND buy.buyPrice > 0"
            + " AND buy.buyPrice > sell.sellPrice"
            + " AND ALL(x IN shift WHERE x.ly <= {distance})"
            + " WITH fSys as fromSystem,"
            + " fSta as fromStation," 
            + " tSta as toStation," 
            + " tSys as toSystem," 
            + " commodity as hop1Commodity," 
            + " (buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as profit1" 
            + " MATCH (commodity2)-[sell2:EXCHANGE]-(toStation)-[:HAS]-(toSystem)-[shift2:FRAMESHIFT*1.."+ jumps +"]-(tSys2:System)-[:HAS]-(tSta2:Station)-[buy2:EXCHANGE]-(commodity2)"
            + " WHERE sell2.sellPrice > 0"
            + " AND {cash} - (sell2.sellPrice * {cargo}) > 0"
            + " and sell2.supply > {cargo}"
            + " and buy2.buyPrice > 0"
            + " and buy2.buyPrice > sell2.sellPrice"
            + " AND ALL(x IN shift2 WHERE x.ly <= {distance})"
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
        
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }

    
    /**
     * Buy a commodity with n jumps of this station, including this station
     * 
     * @param fromStation
     * @param ship
     * @param jumps
     * @param commodity
     * @return
     */
    public String buy(String fromStation, Ship ship, int jumps, String commodity) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", ship.getJumpDistance(), "commodity", commodity);
        String query = "MATCH (hereSys:System)-[:HAS]-(here:Station)-[sell:EXCHANGE]-(commodity:Commodity)"
            + " WHERE here.name={fromStation}"                 
            + " AND sell.sellPrice > 0"
            + " AND sell.supply > 0"
            + " AND commodity.name = {commodity}"
            + " RETURN" 
            + " here.name as `TO STATION`," 
            + " hereSys.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " sell.sellPrice as `UNIT PRICE`"
            + " UNION"
            + " MATCH (fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT*1.."+ jumps +"]-(toSystem:System)-[:HAS]-(toStation:Station)-[sell:EXCHANGE]-(commodity:Commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND ALL(x IN shift WHERE x.ly <= {distance})"                     
            + " AND sell.sellPrice > 0"
            + " AND sell.supply > 0"
            + " AND commodity.name = {commodity}" 
            + " RETURN DISTINCT" 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " sell.sellPrice as `UNIT PRICE`"
            + " ORDER BY `UNIT PRICE` ASC"
            + " LIMIT 5 ";   
        
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    
    /**
     * Sell a commodity with n jumps of this station, including this station
     * 
     * @param fromStation
     * @param ship
     * @param jumps
     * @param commodity
     * @return
     */
    public String sell(String fromStation, Ship ship, int jumps, String commodity) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", ship.getJumpDistance(), "commodity", commodity);
        String query = "MATCH (hereSys:System)-[:HAS]-(here:Station)-[buy:EXCHANGE]-(commodity:Commodity)"
            + " WHERE here.name={fromStation}"                 
            + " AND buy.buyPrice > 0"
            + " AND commodity.name = {commodity}"
            + " RETURN" 
            + " here.name as `TO STATION`," 
            + " hereSys.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " buy.buyPrice as `UNIT PRICE`"
            + " UNION"                
            + " MATCH (fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT*1.."+ jumps +"]-(toSystem:System)-[:HAS]-(toStation:Station)-[buy:EXCHANGE]-(commodity:Commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND ALL(x IN shift WHERE x.ly <= {distance})"                     
            + " AND buy.buyPrice > 0"
            + " AND commodity.name = {commodity}" 
            + " RETURN DISTINCT" 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " buy.buyPrice as `UNIT PRICE`"
            + " ORDER BY `UNIT PRICE` DESC"
            + " LIMIT 5 ";   

        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    
    public String stationToStation(String fromStation, Ship s, String toStation) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash(), "toStation", toStation);
        String query = "MATCH (commodity)<-[sell:EXCHANGE]-(fromStation:Station)<-[:HAS]-(fromSystem:System),(toSystem:System)-[:HAS]->(toStation:Station)-[buy:EXCHANGE]->(commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND toStation.name={toStation}"
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
        
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    
}
