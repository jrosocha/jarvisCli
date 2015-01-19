package com.jhr.jarvis.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.parboiled.common.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.table.TableRenderer;
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
        String query = "MATCH (commodity)-[buy:EXCHANGE]-(fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT]-(toSystem:System)-[:HAS]-(toStation:Station)-[sell:EXCHANGE]-(commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND shift.ly <= {distance}"
            + " AND buy.buyPrice > 0"
            + " AND buy.supply >= {cargo}"
            + " AND {cash} - (buy.buyPrice * {cargo}) > 0"
            + " AND sell.sellPrice > 0"
            + " AND sell.demand >= {cargo}"            
            + " AND sell.sellPrice > buy.buyPrice"
            + " RETURN DISTINCT" 
            + " fromSystem.name as `FROM SYSTEM`," 
            + " fromStation.name as `FROM STATION`," 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`," 
            + " buy.buyPrice AS `BUY @`,"
            + " sell.sellPrice AS `SELL @`,"
            + " (sell.sellPrice - buy.buyPrice) as `UNIT PROFIT`,"
            + " (buy.buyPrice * {cargo}) as `CARGO COST`,"
            + " (sell.sellPrice * {cargo}) as `CARGO SOLD FOR`,"
            + " (sell.sellPrice * {cargo}) - (buy.buyPrice * {cargo}) as `CARGO PROFIT`"
            + " ORDER BY `CARGO PROFIT` DESC"
            + " LIMIT 5 ";   
        
        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);     
        
        String out = OsUtils.LINE_SEPARATOR;
        out += "From System: " + results.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + results.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(results, ImmutableList.of("TO SYSTEM", "TO STATION", "COMMODITY", "BUY @", "SELL @", "UNIT PROFIT", "CARGO PROFIT"));
        
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
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
        String query = "MATCH (commodity)-[buy:EXCHANGE]-(fSta:Station)-[:HAS]-(fSys:System)-[shift:FRAMESHIFT]-(tSys:System)-[:HAS]-(tSta:Station)-[sell:EXCHANGE]-(commodity)"
            + " WHERE fSta.name={fromStation}"
            + " AND shift.ly <= {distance}"
            + " AND buy.buyPrice > 0"
            + " AND buy.supply > {cargo}"
            + " AND {cash} - (buy.buyPrice * {cargo}) > 0"
            + " AND sell.sellPrice > 0"
            + " AND sell.demand >= {cargo}"
            + " AND sell.sellPrice > buy.buyPrice"
            + " WITH fSys as fromSystem,"
            + " fSta as fromStation," 
            + " tSta as toStation," 
            + " tSys as toSystem," 
            + " commodity as hop1Commodity," 
            + " (sell.sellPrice * {cargo}) - (buy.buyPrice * {cargo}) as profit1," 
            + " shift.ly as ly1"
            + " MATCH (commodity2)-[buy2:EXCHANGE]-(toStation)-[:HAS]-(toSystem)-[shift2:FRAMESHIFT]-(tSys2:System)-[:HAS]-(tSta2:Station)-[sell2:EXCHANGE]-(commodity2)"
            + " WHERE shift2.ly <= {distance}"
            + " AND buy2.buyPrice > 0"
            + " AND {cash} - (buy2.buyPrice * {cargo}) > 0"
            + " AND buy2.supply > {cargo}"
            + " AND sell2.sellPrice > 0"
            + " AND sell2.sellPrice > buy2.buyPrice"
            + " AND sell2.demand >= {cargo}"
            + " RETURN DISTINCT"
            + " hop1Commodity.name as `COMMODITY 1`,"
            + " profit1 as `PROFIT 1`,"
            + " toSystem.name as `SYSTEM 1`," 
            + " toStation.name as `STATION 1`,"
            + " commodity2.name as `COMMODITY 2`," 
            + " (sell2.sellPrice * {cargo}) - (buy2.buyPrice * {cargo}) as `PROFIT 2`," 
            + " tSys2.name as `SYSTEM 2`," 
            + " tSta2.name as `STATION 2`," 
            + " profit1 + ((sell2.sellPrice * {cargo}) - (buy2.buyPrice * {cargo})) as `TRIP PROFIT`"
            + " ORDER BY `TRIP PROFIT` DESC"
            + " LIMIT 5";
            
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    

    public String gon(String fromStation, Ship s, int hops) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)<-[buy:EXCHANGE]-(fromStation:Station)<-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT*1.."+ hops +"]-(toSystem:System)-[:HAS]->(toStation:Station)-[sell:EXCHANGE]->(commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND buy.buyPrice > 0"
            + " AND buy.supply >= {cargo}"
            + " AND {cash} - (buy.buyPrice * {cargo}) > 0"
            + " AND sell.sellPrice > 0"
            + " AND sell.sellPrice > buy.buyPrice"
            + " AND sell.demand >= {cargo}"
            + " AND ALL(x IN shift WHERE x.ly <= {distance})"
            + " RETURN DISTINCT" 
            + " fromStation.name as `FROM STATION`," 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`," 
            + " (sell.sellPrice - buy.buyPrice) as `PER UNIT PROFIT`,"
            + " (buy.buyPrice * {cargo}) as `CARGO COST`,"
            + " (sell.sellPrice * {cargo}) as `CARGO SOLD FOR`,"
            + " (sell.sellPrice * {cargo}) - (buy.buyPrice * {cargo}) as `CARGO PROFIT`"
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
        String query = "MATCH (commodity)-[buy:EXCHANGE]-(fSta:Station)-[:HAS]-(fSys:System)-[shift:FRAMESHIFT*1.."+ jumps +"]-(tSys:System)-[:HAS]-(tSta:Station)-[sell:EXCHANGE]-(commodity)"
            + " WHERE fSta.name={fromStation}"
            + " AND buy.buyPrice > 0"
            + " AND buy.supply > {cargo}"
            + " AND {cash} - (buy.buyPrice * {cargo}) > 0"
            + " AND sell.sellPrice > 0"
            + " AND sell.sellPrice > buy.buyPrice"
            + " AND sell.demand >= {cargo}"
            + " AND ALL(x IN shift WHERE x.ly <= {distance})"
            + " WITH fSys as fromSystem,"
            + " fSta as fromStation," 
            + " tSta as toStation," 
            + " tSys as toSystem," 
            + " commodity as hop1Commodity," 
            + " (sell.sellPrice * {cargo}) - (buy.buyPrice * {cargo}) as profit1" 
            + " MATCH (commodity2)-[buy2:EXCHANGE]-(toStation)-[:HAS]-(toSystem)-[shift2:FRAMESHIFT*1.."+ jumps +"]-(tSys2:System)-[:HAS]-(tSta2:Station)-[sell2:EXCHANGE]-(commodity2)"
            + " WHERE buy2.buyPrice > 0"
            + " AND {cash} - (buy2.buyPrice * {cargo}) > 0"
            + " and buy2.supply > {cargo}"
            + " and sell2.sellPrice > 0"
            + " and sell2.sellPrice > buy2.buyPrice"
            + " AND sell2.demand >= {cargo}"
            + " AND ALL(x IN shift2 WHERE x.ly <= {distance})"
            + " RETURN DISTINCT"
            + " hop1Commodity.name as `COMMODITY 1`,"
            + " profit1 as `PROFIT 1`,"
            + " toSystem.name as `SYSTEM 1`," 
            + " toStation.name as `STATION 1`,"
            + " commodity2.name as `COMMODITY 2`," 
            + " (sell2.sellPrice * {cargo}) - (buy2.buyPrice * {cargo}) as `PROFIT 2`," 
            + " tSys2.name as `SYSTEM 2`," 
            + " tSta2.name as `STATION 2`," 
            + " profit1 + ((sell2.sellPrice * {cargo}) - (buy2.buyPrice * {cargo})) as `TRIP PROFIT`"
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
        String query = "MATCH (hereSys:System)-[:HAS]-(here:Station)-[buy:EXCHANGE]-(commodity:Commodity)"
            + " WHERE here.name={fromStation}"                 
            + " AND buy.buyPrice > 0"
            + " AND buy.supply > 0"
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
            + " AND buy.supply > 0"
            + " AND commodity.name = {commodity}" 
            + " RETURN DISTINCT" 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " buy.buyPrice as `UNIT PRICE`"
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
        String query = "MATCH (hereSys:System)-[:HAS]-(here:Station)-[sell:EXCHANGE]-(commodity:Commodity)"
            + " WHERE here.name={fromStation}"                 
            + " AND sell.sellPrice > 0"
            + " AND sell.demand > 0"
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
            + " AND sell.demand > 0"
            + " AND commodity.name = {commodity}" 
            + " RETURN DISTINCT" 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " sell.sellPrice as `UNIT PRICE`"
            + " ORDER BY `UNIT PRICE` DESC"
            + " LIMIT 5 ";   

        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    
    public String stationToStation(String fromStation, Ship s, String toStation) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash(), "toStation", toStation);
        String query = "MATCH (commodity)<-[buy:EXCHANGE]-(fromStation:Station)<-[:HAS]-(fromSystem:System),(toSystem:System)-[:HAS]->(toStation:Station)-[sell:EXCHANGE]->(commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND toStation.name={toStation}"
            + " AND buy.buyPrice > 0"
            + " AND buy.supply >= {cargo}"
            + " AND {cash} - (buy.buyPrice * {cargo}) > 0"
            + " AND sell.sellPrice > 0"
            + " AND sell.sellPrice > buy.buyPrice"
            + " RETURN DISTINCT" 
            + " fromStation.name as `FROM STATION`," 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`," 
            + " (sell.sellPrice - buy.buyPrice) as `PER UNIT PROFIT`,"
            + " (buy.buyPrice * {cargo}) as `CARGO COST`,"
            + " (sell.sellPrice * {cargo}) as `CARGO SOLD FOR`,"
            + " (sell.sellPrice * {cargo}) - (buy.buyPrice * {cargo}) as `CARGO PROFIT`"
            + " ORDER BY `CARGO PROFIT` DESC"
            + " LIMIT 5 ";   
        
        return graphDbService.runCypher(query, cypherParams) + OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
    }
    
}
