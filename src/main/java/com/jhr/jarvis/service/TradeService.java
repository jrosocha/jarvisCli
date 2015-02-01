package com.jhr.jarvis.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.parboiled.common.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jhr.jarvis.model.BestExchange;
import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.SavedExchange;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.table.TableRenderer;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

@Service
public class TradeService {

    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private StationService stationService;
    
    @Autowired
    private StarSystemService starSystemService;
    
    @Autowired
    private OrientDbService orientDbService;
    
    private Map<Integer, SavedExchange> lastSearchedExchanges = new HashMap<>(); 

    
    public List<BestExchange> pathToExchange(List<BestExchange> data, int index) {

        List<BestExchange> pathToExchange = new ArrayList<>();
        BestExchange currentExchange = data.get(index);
        
        while (currentExchange != null) {
            pathToExchange.add(currentExchange);
            currentExchange = currentExchange.getParent();
        }
        
        Collections.reverse(pathToExchange);
        return pathToExchange;
          
    }
    
    public List<BestExchange> tradeNOrientDb(String fromStationName, Ship ship, int maxJumps, int tradeStops, List<BestExchange> endList) {
        
        tradeStops--;
        
        List<BestExchange> trades = tradeOrientDb(fromStationName, ship, maxJumps);
        
        if (tradeStops > 0) {        
            for (BestExchange trade: trades) {
                List<BestExchange> thisTrip = tradeNOrientDb(trade.getSellStationName(), ship, maxJumps, tradeStops, endList);
                thisTrip.parallelStream().forEach(exchange->{ exchange.setParent(trade); exchange.setRoutePerProfitUnit( exchange.getPerUnitProfit() + trade.getPerUnitProfit()); });
                trade.setNextTrip(thisTrip);
            }
        } else {
            endList.addAll(trades);
        }
        
        return trades;
    }

    
    public List<BestExchange> tradeOrientDb(String fromStationName, Ship ship, int maxJumps) {
        Station fromStation = new Station();
        OrientGraph graph = null;
        List<BestExchange> bestExchangeList = new ArrayList<>();
        
        try {
            graph = orientDbService.getFactory().getTx();
            
            //starting station
            OrientVertex vertexStation = (OrientVertex) graph.getVertexByKey("Station.name", fromStationName);
            fromStation.setName(vertexStation.getProperty("name"));
            
            // populate worthwile buy commodities
            Map<String, Commodity> buyCommodities = stationService.getStationBuyCommodities(vertexStation, ship);
            
            // get a system for a station.
            Vertex originSystem = stationService.getSystemVertexForStationVertex(vertexStation);
            fromStation.setSystem(originSystem.getProperty("name"));
    
            Set<Vertex> systemsWithinNShipJumps = starSystemService.findSystemsWithinNFrameshiftJumpsOfDistance(graph, originSystem, ship.getJumpDistance(), maxJumps);
            
            for (Vertex destinationSystem: systemsWithinNShipJumps) {
                
                String destinationSystemName = destinationSystem.getProperty("name");
                Set<Vertex> systemStations = starSystemService.findStationsInSystem(destinationSystem);
                
                for (Vertex station: systemStations) {
                    Station toStation = new Station(station.getProperty("name"), destinationSystemName);
                    Map<String, Commodity> sellCommodities = stationService.getReleventStationSellCommodities(station, buyCommodities, ship);
                    
                    for (String commodity: sellCommodities.keySet()) {
                        
                        Commodity buyCommodity = buyCommodities.get(commodity);
                        Commodity sellCommodity = sellCommodities.get(commodity);
                        BestExchange bestExchange = new BestExchange(fromStation, toStation, buyCommodity, sellCommodity, ship.getCargoSpace());
                        bestExchange.setParent(null);
                        bestExchange.setRoutePerProfitUnit(bestExchange.getPerUnitProfit());
                        // add the exchange to the master list.
                        bestExchangeList.add(bestExchange);
                    }
                }
            }
            
            graph.commit();
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        List<BestExchange> sortedBestExchangeList =  bestExchangeList.parallelStream().sorted((a,b)->{ return Integer.compare(a.getPerUnitProfit(), b.getPerUnitProfit()); }).collect(Collectors.toList());
        sortedBestExchangeList = Lists.reverse(sortedBestExchangeList);
        return sortedBestExchangeList;
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
    
    
    
    public String sellOrientDb(String fromStationName, Ship ship, int maxJumps, String commodity) {
        Date start = new Date();
        List<Map<String, Object>> tableData = new ArrayList<>();
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            
            Vertex stationVertex = graph.getVertexByKey("Station.name", fromStationName);
            Vertex systemVertex = stationService.getSystemVertexForStationVertex(stationVertex);
            
            Set<Vertex> systemsWithinNShipJumps = starSystemService.findSystemsWithinNFrameshiftJumpsOfDistance(graph, systemVertex, ship.getJumpDistance(), maxJumps);
            for (Vertex destinationSystem: systemsWithinNShipJumps) {
                String destinationSystemName = destinationSystem.getProperty("name");
                Set<Vertex> systemStations = starSystemService.findStationsInSystem(destinationSystem);
                for (Vertex station: systemStations) {
                    Station toStation = new Station(station.getProperty("name"), destinationSystemName);
                    
                    for (Edge exchange: station.getEdges(Direction.OUT, "Exchange")) {            

                        int sellPrice = exchange.getProperty("sellPrice");
                        int buyPrice = exchange.getProperty("buyPrice");
                        int supply = exchange.getProperty("supply");
                        int demand = exchange.getProperty("demand");
                        long date = exchange.getProperty("date");
                        
                        if (demand > ship.getCargoSpace() && sellPrice > 0) {
                            Vertex commodityVertex = exchange.getVertex(Direction.IN);
                            if (commodityVertex.getProperty("name").equals(commodity)) {
                                Commodity sellCommodity = new Commodity(commodityVertex.getProperty("name"), buyPrice, supply, sellPrice, demand);
                                Map<String, Object> row = new HashMap<>();
                                row.put("TO SYSTEM", destinationSystem.getProperty("name"));
                                row.put("TO STATION", station.getProperty("name"));
                                row.put("UNIT PRICE", sellPrice);
                                row.put("DEMAND", demand);
                                row.put("DAYS OLD", (((new Date().getTime() - date)/1000/60/60/24) * 100)/100);
                            }
                        }
                    }
                    
                }
            }           
            graph.commit();
        } catch(Exception e) {
            if (graph != null) {
                graph.rollback();
            }
        }
            
        if (tableData.size() == 0) {
            return "No sale available in provided range";
        }
        
        tableData = tableData.parallelStream().sorted((row1,row2)->{
            int p1 = (int) row1.get("UNIT PRICE");
            int p2 = (int) row2.get("UNIT PRICE");
            return Integer.compare(p1, p2);
        }).collect(Collectors.toList());
        Collections.reverse(tableData);
        
        String out = OsUtils.LINE_SEPARATOR;
        out += "From System: " + tableData.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + tableData.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += "Cargo Capacity: " + ship.getCargoSpace() + OsUtils.LINE_SEPARATOR;
        out += tableData.size() + " Best stations to sell " + tableData.get(0).get("COMMODITY") + " within " + maxJumps + " jump(s) @ " + ship.getJumpDistance() + " ly or less." + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(tableData, 
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
    
    public String bestSellPriceOrientDb(String commodityName) {
        Date start = new Date();
        
        List<Map<String, Object>> tableData = new ArrayList<>();
        
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            //starting commodity
            OrientVertex commodityVertex = (OrientVertex) graph.getVertexByKey("Commodity.name", commodityName);
            
            for (Edge hasExchange: commodityVertex.getEdges(Direction.IN, "Exchange")) {
                
                int demand = hasExchange.getProperty("demand");
                int sellPrice = hasExchange.getProperty("sellPrice");
                long date = hasExchange.getProperty("date");
                if (demand > 0 && sellPrice > 0) {
                    Vertex stationVertex = hasExchange.getVertex(Direction.OUT);
                    Vertex systemVertex = stationService.getSystemVertexForStationVertex(stationVertex);
                    Map<String, Object> row = new HashMap<>();
                    row.put("TO SYSTEM", systemVertex.getProperty("name"));
                    row.put("TO STATION", stationVertex.getProperty("name"));
                    row.put("UNIT PRICE", sellPrice);
                    row.put("DEMAND", demand);
                    row.put("DAYS OLD", (((new Date().getTime() - date)/1000/60/60/24) * 100)/100);
                    tableData.add(row);
                }
            }
            graph.commit();
        } catch (Exception e) {
            if (graph != null) {
                graph.rollback();
            }
        }
        
        if (tableData.size() == 0) {
            return "No sale available in data";
        }
        
        tableData = tableData.parallelStream().sorted((row1,row2)->{
            int p1 = (int) row1.get("UNIT PRICE");
            int p2 = (int) row2.get("UNIT PRICE");
            return Integer.compare(p1, p2);
        }).collect(Collectors.toList());
        
        String out = OsUtils.LINE_SEPARATOR;
        out += tableData.size() + " Best stations to sell " + commodityName + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(tableData, 
                ImmutableList.of("TO SYSTEM", "TO STATION", "UNIT PRICE", "DEMAND", "DAYS OLD"));
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
            SavedExchange e = new SavedExchange(new Station((String)result.get("FROM STATION"), (String) result.get("FROM SYSTEM")), 
                    new Station((String) result.get("TO STATION"), (String) result.get("TO SYSTEM")));
            lastSearchedExchanges.put(i, e);
            modifiedResultList.add(modifiedResult);
        }
        return modifiedResultList;
    }

    public long exchangeCountOrientDb() {
        
        long exchangeCount = 0;
        try {
            OrientGraph graph = orientDbService.getFactory().getTx();
            exchangeCount = graph.countEdges("Exchange");
            graph.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exchangeCount;
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
    public Map<Integer, SavedExchange> getLastSearchedExchanges() {
        return lastSearchedExchanges;
    }

    /**
     * @param lastSearchedExchanges the lastSearchedExchanges to set
     */
    public void setLastSearchedExchanges(Map<Integer, SavedExchange> lastSearchedExchanges) {
        this.lastSearchedExchanges = lastSearchedExchanges;
    }
    
}
