package com.jhr.jarvis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.table.TableRenderer;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

@Service
public class StationService {
    
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
                commodity.setGroup(commodityService.getCommodityGroup(commodity.getName()));
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
                sellCommodity.setGroup(commodityService.getCommodityGroup(sellCommodity.getName()));
                Commodity boughtCommodity = buyCommodities.get(sellCommodity.getName());
                
                if (boughtCommodity != null && boughtCommodity.getBuyPrice() < sellCommodity.getSellPrice()) {
                    stationSellReleventCommodities.put(sellCommodity.getName(), sellCommodity);
                }
            }
        }
        
        return stationSellReleventCommodities;
    }
    
    public List<Commodity> getStationCommodities(Vertex stationVertex) {
        
        List<Commodity> stationCommodities = new ArrayList<>();
        
        for (Edge exchange: stationVertex.getEdges(Direction.OUT, "Exchange")) {            

            int sellPrice = exchange.getProperty("sellPrice");
            int buyPrice = exchange.getProperty("buyPrice");
            int supply = exchange.getProperty("supply");
            int demand = exchange.getProperty("demand");
            long date = exchange.getProperty("date");
            
            if ( (demand > 0 && sellPrice > 0) || (supply > 0 && buyPrice > 0)) {
                Vertex commodityVertex = exchange.getVertex(Direction.IN);
                Commodity commodity = new Commodity(commodityVertex.getProperty("name"), buyPrice, supply, sellPrice, demand, date);
                commodity.setGroup(commodityService.getCommodityGroup(commodity.getName()));
                stationCommodities.add(commodity);
            }
        }
        
        return stationCommodities;
    }
    
    public Station findExactStationOrientDb(String stationName) throws StationNotFoundException {
        OrientGraph graph = null;
        graph = orientDbService.getFactory().getTx();
        Station out = new Station();
        try {    
            OrientVertex stationVertex = (OrientVertex) graph.getVertexByKey("Station.name", stationName);
            if (stationVertex == null) {
                throw new StationNotFoundException("No station matching '" + stationName + "' in graph.");
            }
            out = vertexToStation(stationVertex);
            graph.commit();
        } catch (Exception e) {
            if (graph != null) {
                graph.rollback();
            }
            throw new StationNotFoundException("No station matching '" + stationName + "' in graph.");
        }
        return out;
   }
    
    /**
     * Runs an exact match and a partial match looking to identify a single station.
     * If a single station is found, it is loaded into memory
     * 
     * @param partial
     * @return
     * @throws Exception
     */
    public Station findUniqueStationOrientDb(String partial, boolean loadIntoMemory) throws StationNotFoundException {
        
        Station foundStation = null;
        boolean found = false;
        
        try {
            foundStation = findExactStationOrientDb(partial);
            found = true;
        } catch (Exception e) {
            // not an exact match. proceed
        }
     
        if (!found) {
            List<Station> stations = findStationsOrientDb(partial, loadIntoMemory);
            
            if (stations.size() == 0 || stations.size() > 1 ) {
                throw new StationNotFoundException("Unique station could not be identified for '" + partial + "'.");
            }
            foundStation = stations.get(0);
        }
        
        if (loadIntoMemory) {
            setUserLastStoredStation(foundStation);
        }
        return foundStation;
    }
    
    public String stationDetailsOrientDb(Station station) {
        
        OrientGraph graph = null;
        List<Commodity> stationCommodities = new ArrayList<>();
        try {   
            graph = orientDbService.getFactory().getTx();
            
            OrientVertex stationVertex = (OrientVertex) graph.getVertexByKey("Station.name", station.getName());
            stationCommodities = getStationCommodities(stationVertex);
            
            graph.commit();
        } catch (Exception e) {
            if (graph != null) {
                graph.rollback();
            }
        }

        // build the map so that you can create a table
        List<Map<String, Object>> tableData = stationCommodities.stream().map(Commodity::toMap).sorted((row1,row2)->{
            String group1 = (String) row1.get("GROUP");
            String group2 = (String) row2.get("GROUP");
            String comm1 = (String) row1.get("COMMODITY");
            String comm2 = (String) row2.get("COMMODITY");
            if (group1.equals(group2)) {
                return comm1.compareTo(comm2);
            }
            return group1.compareTo(group2);
        }).collect(Collectors.toList());

        String out = "";
        out += "System: " +station.getSystem() + OsUtils.LINE_SEPARATOR;
        out += "Station: " +station.getName() + OsUtils.LINE_SEPARATOR;
        out += "Black Market: " +station.getBlackMarket() + OsUtils.LINE_SEPARATOR;
        out += "Data Age in Days: " + tableData.get(0).get("DAYS OLD") + OsUtils.LINE_SEPARATOR + OsUtils.LINE_SEPARATOR;
        out += TableRenderer.renderMapDataAsTable(tableData, ImmutableList.of("GROUP", "COMMODITY", "BUY @", "SUPPLY", "SELL @", "DEMAND"));
        return out;

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
            return findUniqueStationOrientDb(station, true);            
        }
        
        throw new StationNotFoundException("No unique station could be found.");
    }

    public Vertex getSystemVertexForStationVertex(Vertex stationVertex) {
        Vertex originSystem= null;
        for (Edge hasEdge: stationVertex.getEdges(Direction.IN, "Has")) {
            originSystem = hasEdge.getVertex(Direction.OUT);
            return originSystem;
        }
        return null;
    }

    public List<Station> findStationsOrientDb(String partial, boolean loadToMemory) {
        
        List<Station> out = new ArrayList<>();
        
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            
            for (Vertex stationVertex : (Iterable<Vertex>) graph.command(
                    new OCommandSQL("select from Station where name like '" + partial.toUpperCase() + "%'")).execute()) {
                
                Station station = vertexToStation(stationVertex);
                out.add(station);
            }
            graph.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (graph != null) {
                graph.rollback();
            }
        }
        
        if (out.size() == 1 && loadToMemory) {
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
        
    public String joinStationsAsString(List<Station> stations) {
        return stations.stream().map(s->{ return s.getName() + " @ " + s.getSystem();}).collect(Collectors.joining(OsUtils.LINE_SEPARATOR));
    }
    
    public List<Station> getStationsForSystemOrientDb(String system) {
        
        List<Station> stations = new ArrayList<>();
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            Vertex systemVertex = graph.getVertexByKey("System.name", system);
            Set<Vertex> systemStations = starSystemService.findStationsInSystemOrientDb(systemVertex, null);
            
            for (Vertex stationVertex: systemStations) {
            	
                Station station = vertexToStation(stationVertex);
                stations.add(station);
            }
            graph.commit();
        } catch (Exception e) {
            graph.rollback();
        }
    
        return stations;
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
    
    public void addPropertyToStationOrientDb(Station station, String property, Object value) {
        
        OrientGraph graph = null;
        try {
            graph = orientDbService.getFactory().getTx();
            OrientVertex vertexStation = (OrientVertex) graph.getVertexByKey("Station.name", station.getName());
            if (vertexStation == null) {            	
            	throw new StationNotFoundException(station.getName() + " not found.");
            }
            vertexStation.setProperty(property, value);
            graph.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (graph != null) {
                graph.rollback();
            }
        }
    }
    
    public Station vertexToStation(Vertex stationVertex) {
    	Station out = new Station();
        out.setName(stationVertex.getProperty("name"));
        
        Boolean blackMarket = stationVertex.getProperty("blackMarket") != null ? stationVertex.getProperty("blackMarket") : Boolean.FALSE;
        out.setBlackMarket(blackMarket);
        
        Vertex systemVertex = getSystemVertexForStationVertex(stationVertex);
        out.setSystem(systemVertex.getProperty("name"));
        
        for (Edge exchange: stationVertex.getEdges(Direction.OUT, "Exchange")) {
            out.setDate(exchange.getProperty("date"));
            break;
        }
        
        return out;
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
