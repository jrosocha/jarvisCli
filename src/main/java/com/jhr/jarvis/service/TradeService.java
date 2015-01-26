package com.jhr.jarvis.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.parboiled.common.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.model.Exchange;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.table.TableRenderer;

@Service
public class TradeService {

    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Map<Integer, Exchange> lastSearchedExchanges = new HashMap<>(); 

    public String trade(String fromStation, Ship s, int hops) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity:Commodity)<-[buy:EXCHANGE]-(fromStation:Station)<-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT*1.."+ hops +"]-(toSystem:System)-[:HAS]->(toStation:Station)-[sell:EXCHANGE]->(commodity:Commodity)" 
            + " WHERE fromStation.name={fromStation}"
            + " AND buy.buyPrice > 0"
            + " AND buy.supply >= {cargo}"
            + " AND {cash} - (buy.buyPrice * {cargo}) > 0"
            + " AND sell.sellPrice > 0"
            + " AND sell.sellPrice > buy.buyPrice"
            + " AND sell.demand >= {cargo}"
            + " AND ALL(x IN shift WHERE x.ly <= {distance})"
            + " RETURN DISTINCT" 
            + " fromSystem.name as `FROM SYSTEM`," 
            + " fromStation.name as `FROM STATION`," 
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`," 
            + " buy.buyPrice AS `BUY @`,"
            + " sell.sellPrice AS `SELL @`,"
            + " (buy.buyPrice * {cargo}) as `CARGO COST`,"
            + " (sell.sellPrice * {cargo}) - (buy.buyPrice * {cargo}) as `PROFIT`"
            + " ORDER BY `PROFIT` DESC"
            + " LIMIT 5 ";   
        
        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);             
        
        if (results.size() == 0) {
            return "No exchange available in provided range";
        }
        
        List<Map<String, Object>> modifiedResultList = includeSavedExchangeIndexWithQueryResults(results); 
        
        List<String> columns = ImmutableList.of("#", "TO SYSTEM", "TO STATION", "COMMODITY", "BUY @", "CARGO COST", "SELL @", "PROFIT");
               
        String out = OsUtils.LINE_SEPARATOR;
        out += "From System: " + results.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + results.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += "Cargo Capacity: " + s.getCargoSpace() + OsUtils.LINE_SEPARATOR;
        out += results.size() + " Best trading solution within " + hops + " jump(s) @ " + s.getJumpDistance() + " ly or less." + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR;
        out += TableRenderer.renderMapDataAsTable(modifiedResultList, columns);
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
    }
    
    
    /**
     * provides a 2 stop trade solution for systems that are 1..n jumps apart
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public String trade2(String fromStation, Ship s, int jumps) {
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
            + " ORDER BY profit1 DESC"
            + " LIMIT 10"
            + " MATCH (commodity2)-[buy2:EXCHANGE]-(toStation)-[:HAS]-(toSystem)-[shift2:FRAMESHIFT*1.."+ jumps +"]-(tSys2:System)-[:HAS]-(tSta2:Station)-[sell2:EXCHANGE]-(commodity2)"
            + " WHERE buy2.buyPrice > 0"
            + " AND {cash} - (buy2.buyPrice * {cargo}) > 0"
            + " and buy2.supply > {cargo}"
            + " and sell2.sellPrice > 0"
            + " and sell2.sellPrice > buy2.buyPrice"
            + " AND sell2.demand >= {cargo}"
            + " AND ALL(x IN shift2 WHERE x.ly <= {distance})"
            + " RETURN DISTINCT"
            + " fromSystem.name AS `FROM SYSTEM`,"
            + " fromStation.name AS `FROM STATION`,"
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
        
        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);     
        
        if (results.size() == 0) {
            return "No exchange available in provided range";
        }
        
        String out = OsUtils.LINE_SEPARATOR;
        out += "From System: " + results.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + results.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += "Cargo Capacity: " + s.getCargoSpace() + OsUtils.LINE_SEPARATOR;
        out += results.size() + " Best 2 station trading solutions within " + jumps + " jump of each other @ " + s.getJumpDistance() + " ly or less." + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(results, 
                ImmutableList.of("COMMODITY 1", "PROFIT 1", "SYSTEM 1", "STATION 1", "COMMODITY 2", "PROFIT 2", "SYSTEM 2", "STATION 2", "TRIP PROFIT"));
        
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
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
            + " hereSys.name as `FROM SYSTEM`,"
            + " here.name as `FROM STATION`,"
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
            + " fromSystem as `FROM SYSTEM`,"            
            + " fromStation as `FROM STATION`,"
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " buy.buyPrice as `UNIT PRICE`"
            + " ORDER BY `UNIT PRICE` ASC"
            + " LIMIT 5 ";   
        
        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);     
        
        if (results.size() == 0) {
            return "No purchase available in provided range";
        }
        
        String out = OsUtils.LINE_SEPARATOR;
        out += "From System: " + results.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + results.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += "Cargo Capacity: " + ship.getCargoSpace() + OsUtils.LINE_SEPARATOR;
        out += results.size() + " Best stations to buy " + results.get(0).get("COMMODITY") + " within " + jumps + " jump(s) @ " + ship.getJumpDistance() + " ly or less." + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(results, 
                ImmutableList.of("TO SYSTEM", "TO STATION", "UNIT PRICE"));
        
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
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
            + " hereSys.name as `FROM SYSTEM`,"
            + " here.name as `FROM STATION`,"
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
            + " fromSystem as `FROM SYSTEM`,"            
            + " fromStation as `FROM STATION`,"
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " sell.sellPrice as `UNIT PRICE`"
            + " ORDER BY `UNIT PRICE` DESC"
            + " LIMIT 5 ";   

        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);     
        
        if (results.size() == 0) {
            return "No sale available in provided range";
        }
        
        String out = OsUtils.LINE_SEPARATOR;
        out += "From System: " + results.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + results.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += "Cargo Capacity: " + ship.getCargoSpace() + OsUtils.LINE_SEPARATOR;
        out += results.size() + " Best stations to sell " + results.get(0).get("COMMODITY") + " within " + jumps + " jump(s) @ " + ship.getJumpDistance() + " ly or less." + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(results, 
                ImmutableList.of("TO SYSTEM", "TO STATION", "UNIT PRICE"));
        
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
    }
    
    /**
     * Sell a commodity, checks against all stations
     * 
     * @param fromStation
     * @param ship
     * @param jumps
     * @param commodity
     * @return
     */
    public String bestSell(String commodity) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("commodity", commodity);
        String query = "MATCH (toSystem:System)-[:HAS]-(toStation:Station)-[sell:EXCHANGE]-(commodity:Commodity)" 
            + " WHERE sell.sellPrice > 0"
            + " AND sell.demand > 0"
            + " AND commodity.name = {commodity}" 
            + " RETURN DISTINCT"
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " sell.sellPrice as `UNIT PRICE`,"
            + " sell.demand as `DEMAND`"
            + " ORDER BY `UNIT PRICE` DESC"
            + " LIMIT 10 ";   

        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);     
        
        if (results.size() == 0) {
            return "No sale available in data";
        }
        
        String out = OsUtils.LINE_SEPARATOR;
        out += results.size() + " Best stations to sell " + results.get(0).get("COMMODITY") + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(results, 
                ImmutableList.of("TO SYSTEM", "TO STATION", "UNIT PRICE", "DEMAND"));
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
    }
    
    /**
     * Sell a commodity, checks against all stations
     * 
     * @param fromStation
     * @param ship
     * @param jumps
     * @param commodity
     * @return
     */
    public String bestBuy(String commodity) {
        Date start = new Date();
        Map<String, Object> cypherParams = ImmutableMap.of("commodity", commodity);
        String query = "MATCH (toSystem:System)-[:HAS]-(toStation:Station)-[buy:EXCHANGE]-(commodity:Commodity)" 
            + " WHERE buy.buyPrice > 0"
            + " AND buy.supply > 0"
            + " AND commodity.name = {commodity}" 
            + " RETURN DISTINCT"
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`,"
            + " buy.buyPrice as `UNIT PRICE`,"
            + " buy.supply as `SUPPLY`"            
            + " ORDER BY `UNIT PRICE` ASC"
            + " LIMIT 10";   

        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);     
        
        if (results.size() == 0) {
            return "No buy available in data";
        }
        
        String out = OsUtils.LINE_SEPARATOR;
        out += results.size() + " Best stations to buy " + results.get(0).get("COMMODITY") + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(results, 
                ImmutableList.of("TO SYSTEM", "TO STATION", "UNIT PRICE", "SUPPLY"));     
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
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
            + " AND sell.demand >= {cargo}"
            + " AND sell.sellPrice > 0"
            + " AND sell.sellPrice > buy.buyPrice"
            + " RETURN DISTINCT" 
            + " fromStation.name as `FROM STATION`," 
            + " fromSystem.name as `FROM SYSTEM`,"
            + " toStation.name as `TO STATION`," 
            + " toSystem.name as `TO SYSTEM`," 
            + " commodity.name as `COMMODITY`," 
            + " buy.buyPrice as `BUY @`,"
            + " sell.sellPrice as `SELL @`,"
            + " (sell.sellPrice - buy.buyPrice) as `UNIT PROFIT`,"
            + " (buy.buyPrice * {cargo}) as `CARGO COST`,"
            + " (sell.sellPrice * {cargo}) as `CARGO SOLD FOR`,"
            + " (sell.sellPrice * {cargo}) - (buy.buyPrice * {cargo}) as `CARGO PROFIT`"
            + " ORDER BY `CARGO PROFIT` DESC"
            + " LIMIT 5 ";   
        
        List<Map<String, Object>> results =  graphDbService.runCypherNative(query, cypherParams);     
        
        if (results.size() == 0) {
            return "No exchange available between provided stations";
        }
        
        String out = OsUtils.LINE_SEPARATOR;
        out += "From System: " + results.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + results.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += "To System: " + results.get(0).get("TO SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "To Station: " + results.get(0).get("TO STATION") + OsUtils.LINE_SEPARATOR;
        out += "Cargo Capacity: " + s.getCargoSpace() + OsUtils.LINE_SEPARATOR;
        out += results.size() + " Commodities to exchange between stations." + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(results, 
                ImmutableList.of("COMMODITY", "BUY @", "SELL @", "UNIT PROFIT", "CARGO PROFIT"));
        
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
    }
    
    /**
     * resets lastSearchedExchanges
     * with the results of the passed in query so long as the query has 
     * FROM STATION, FROM SYSTEM, TO STATION, TO SYSTEM
     * as returned values.
     * 
     * @param queryResults
     */
    protected List<Map<String,Object>> includeSavedExchangeIndexWithQueryResults(List<Map<String,Object>> queryResults) {
        int i = 0;
        lastSearchedExchanges.clear();
        List<Map<String, Object>> modifiedResultList = new ArrayList<>();
        for (Map<String, Object> result: queryResults) {
            i++;
            Map<String, Object> modifiedResult = new HashMap<>();
            modifiedResult.putAll(result);
            modifiedResult.put("#", i);
            Exchange e = new Exchange(new Station((String)result.get("FROM STATION"), (String) result.get("FROM SYSTEM")), 
                    new Station((String) result.get("TO STATION"), (String) result.get("TO SYSTEM")));
            lastSearchedExchanges.put(i, e);
            modifiedResultList.add(modifiedResult);
        }
        return modifiedResultList;
    }

    public long exchangeCount() {
        
        long exchangeCount = 0;
        String query = "MATCH ()-[e:EXCHANGE]->()"
                + " RETURN COUNT(e) AS `EXCHANGE COUNT`";
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, new HashMap<>());
        exchangeCount = (long) results.get(0).get("EXCHANGE COUNT");
        return exchangeCount;
    }
    
    /**
     * @return the lastSearchedExchanges
     */
    public Map<Integer, Exchange> getLastSearchedExchanges() {
        return lastSearchedExchanges;
    }

    /**
     * @param lastSearchedExchanges the lastSearchedExchanges to set
     */
    public void setLastSearchedExchanges(Map<Integer, Exchange> lastSearchedExchanges) {
        this.lastSearchedExchanges = lastSearchedExchanges;
    }
    
}
