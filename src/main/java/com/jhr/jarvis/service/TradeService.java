package com.jhr.jarvis.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;

@Service
public class TradeService {

    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
    
    /**
     * runs the 1 hop from where you are search, providing 5 best options
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public String go(String fromStation, Ship s) throws JsonParseException, JsonMappingException, IOException {
        
/*
MATCH (commodity)-[sell:EXCHANGE]-(fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT]-(toSystem:System)-[:HAS]-(toStation:Station)-[buy:EXCHANGE]-(commodity)
WHERE fromStation.name={fromStation}
AND shift.ly <= {distance}
AND sell.sellPrice > 0
AND sell.supply >= {cargo}
AND {cash} - (sell.sellPrice * {cargo}) > 0
AND buy.buyPrice > 0
AND buy.buyPrice > sell.sellPrice
RETURN 
fromStation.name as fromStation, 
toStation.name as toStation, 
toSystem.name as toSystem, 
commodity.name as commodity, 
(buy.buyPrice - sell.sellPrice) as unitProfit,
(sell.sellPrice * {cargo}) as cargoBoughtFor,
(buy.buyPrice * {cargo}) as cargoSoldFor,
(buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as profit,
shift.ly as distance
ORDER BY unitProfit DESC
LIMIT 5        
 */
        Map<String, Object> cypherParams = ImmutableMap.of("fromStation", fromStation, "distance", s.getJumpDistance(), "cargo", s.getCargoSpace(), "cash",s.getCash());
        String query = "MATCH (commodity)-[sell:EXCHANGE]-(fromStation:Station)-[:HAS]-(fromSystem:System)-[shift:FRAMESHIFT]-(toSystem:System)-[:HAS]-(toStation:Station)-[buy:EXCHANGE]-(commodity)" 
                        + " WHERE fromStation.name={fromStation}"
                        + " AND shift.ly <= {distance}"
                        + " AND sell.sellPrice > 0"
                        + " AND sell.supply >= {cargo}"
                        + " AND {cash} - (sell.sellPrice * {cargo}) > 0"
                        + " AND buy.buyPrice > 0"
                        + " AND buy.buyPrice > sell.sellPrice"
                        + " RETURN" 
                        + " fromStation.name as `FROM STATION`," 
                        + " toStation.name as `TO STATION`," 
                        + " toSystem.name as `TO SYSTEM`," 
                        + " commodity.name as `COMMODITY`," 
                        + " (buy.buyPrice - sell.sellPrice) as `PER UNIT PROFIT`,"
                        + " (sell.sellPrice * {cargo}) as `CARGO COST`,"
                        + " (buy.buyPrice * {cargo}) as `CARGO SOLD FOR`,"
                        + " (buy.buyPrice * {cargo}) - (sell.sellPrice * {cargo}) as `CARGO PROFIT`,"
                        + " shift.ly as `DISTANCE`"
                        + " ORDER BY `CARGO PROFIT` DESC"
                        + " LIMIT 5 ";   
        
        return graphDbService.runCypher(query, cypherParams);
        
    }
    
}
