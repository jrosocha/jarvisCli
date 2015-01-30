package com.jhr.jarvis.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.parboiled.common.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.jhr.jarvis.exceptions.CommodityNotFoundException;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.BestExchange;
import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.SavedExchange;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.service.CommodityService;
import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.service.StarSystemService;
import com.jhr.jarvis.service.StationService;
import com.jhr.jarvis.service.TradeService;
import com.jhr.jarvis.table.TableRenderer;
import com.jhr.jarvis.util.DrawUtils;

@Component
public class TradeCommands implements CommandMarker {
    
    @Autowired
    private TradeService tradeService;
    
    @Autowired
    private ShipService shipService;

    @Autowired
    private StationService stationService;
    
    @Autowired
    private StarSystemService starSystemService;

    @Autowired
    private CommodityService commodityService;
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private DrawUtils drawUtils;
    
    @CliCommand(value = "select", help = "usage: select <1-5> Selects a trade route, determines a path, and sets your active station/system to the destination.")
    public String select(
            @CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "1-5 option from your previous go(n) search.") String buffer) {
        
        String out = "";
        Ship ship = null;
        
        if (StringUtils.isEmpty(buffer)) {
            out += drawUtils.messageBox(3, "Error: No select <number> passed in",
                    "Usage: select <number>",
                    "Selects a trade route, determines a path, and sets your active station/system to the destination.",
                    "Try running a 'go' command and then 'select' a result!");
             return out;
        }
        
        int selection = Integer.parseInt(buffer);
        SavedExchange savedExchange = tradeService.getLastSearchedExchanges().get(selection);
        if (savedExchange == null) {
            out += drawUtils.messageBox(3, "Error: No exchange found",
                   "Try running a 'go' command and then 'select' a result!");
            return out;
        }
        
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += drawUtils.messageBox(3, "Error: There was an error loading your ship.",
                                           "Check your write permissions in the ../data dir.");
            return out;
        }
        
        if (shipService.isShipEmpty(ship)) {           
            out += drawUtils.messageBox(3, "Error: Your ship may ot be configured",
                                           "First set your ship with: ship cargo;distance;cash ");
            return out;
        }
        
        stationService.setUserLastStoredStation(savedExchange.getTo());
        out += starSystemService.calculateShortestPathBetweenSystems(savedExchange.getFrom().getSystem(), savedExchange.getTo().getSystem(), ship.getJumpDistance());        
        return out;
    }
    
    @CliCommand(value = { "go", "trade", "gon" }, help = "usage: gon --start 'Station Name' --jumps 2 \n Best resource exchange with n jumps of your jump distance. Takes 20 seconds or so for --jumps 3. ")
    public String gon(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps
        ) {
        
        String usage1 = "Usage:   go --start 'Station Name' --jumps 2";
        String usage2 = "Example: go (Will use last stored station)";
        String usage3 = "Example: go --jumps 2 (best exchange within 2 jumps of your ship's jump distance)";
        String usage4 = "or try a find '<partial station match>'.";
        String usage5 = "or try a ship cargo;distance;cash to set up your ship.";
        
        String out = "";
        Station foundStation;
        Ship ship;
        
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += drawUtils.messageBox(3, "Error: Station matching expression '" + station + "' not found",
                    usage1, usage2, usage3, usage4, usage5);
            return out;
        }
        
        int jumpDistance = 1;
        if (jumps != null) {
            jumpDistance = jumps;
        }
        
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += drawUtils.messageBox(3, "Error: There was an error loading your ship.",
                                           "Check your write permissions in the ../data dir.");
            return out;
        }
        
        if (shipService.isShipEmpty(ship)) {           
            out += drawUtils.messageBox(3, "Error: Your ship may ot be configured",
                                           "First set your ship with: ship cargo;distance;cash ");
            return out;
        }
        
        out += tradeService.trade(foundStation.getName(), ship, jumpDistance);
        out += OsUtils.LINE_SEPARATOR + "Try 'select <1-5>' to select a trade route, calculate a path, and set your active station to the trade destination.";
        
        return out;
    }
    
    @CliCommand(value = { "go2", "trade2" }, help = "usage: go2 --start 'Station Name' --jumps 2 \n Best resource exchange with 1..n jumps of your jump distance. Takes 30 seconds or so for --jumps 3. ")
    public String go2n(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps
        ) {
        
        String usage1 = "Usage:   go2 --start 'Station Name' --jumps 2";
        String usage2 = "Example: go2 (Will use last stored station)";
        String usage3 = "Example: go2 --jumps 2 (best 2 stop exchange within 2 jumps of your ship's jump distance)";
        String usage4 = "or try a find '<partial station match>'.";
        String usage5 = "or try a ship cargo;distance;cash to set up your ship.";
        
        String out = "";
        Station foundStation;
        Ship ship;
        
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += drawUtils.messageBox(3, "Error: Station matching expression '" + station + "' not found",
                    usage1, usage2, usage3, usage4, usage5);
            return out;
        }
        
        int jumpDistance = 1;
        if (jumps != null) {
            jumpDistance = jumps;
        }
        
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += drawUtils.messageBox(3, "Error: There was an error loading your ship.",
                                           "Check your write permissions in the ../data dir.");
            return out;
        }
        
        if (shipService.isShipEmpty(ship)) {           
            out += drawUtils.messageBox(3, "Error: Your ship may ot be configured",
                                           "First set your ship with: ship cargo;distance;cash ");
            return out;
        }
        
        out += tradeService.trade2(foundStation.getName(), ship, jumpDistance);
        return out;
    }
    
    /**
     * When you are trying to sell a commodity. 
     * 
     * @param station
     * @param jumps
     * @param commodity
     * @return
     */
    @CliCommand(value = { "sell" }, help = "usage: sell --start 'Station Name' --jumps 2 --commodity 'GOLD' \n Best resource sell price with n jumps of your jump distance.")
    public String sell(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps,
            @CliOption(key = { "commodity" }, mandatory = false, help = "Commodity name") final String commodity
        ) {
        
        String usage1 = "Usage: sell --start 'Station Name' --jumps 2 --commodity 'GOLD'";
        String usage2=  "Omit --jumps and you search every station in the graph.";
        String usage3=  "With --jumps Best resource sell price with n jumps of your jump distance.";
        String usage4=  "With --jumps Depends on --start station or saved station, and your ship.";
        
        String out = "";
        Station foundStation;
        String foundCommodity;
        
        try {
            foundCommodity = getBestMatchingCommodityOrStoredCommodity(commodity);
        } catch (CommodityNotFoundException e) {          
            out += drawUtils.messageBox(3, "Error: Commodity matching expression '" + commodity + "' not found",
                    usage1, usage2, usage3, usage4);
            return out;
        }
        
        if (jumps == null) {
            out += tradeService.bestSell(foundCommodity);
            return out;
        }
        
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += drawUtils.messageBox(3, "Error: Station matching expression '" + station + "' not found",
                    usage1, usage2, usage3, usage4);
            return out;
        }
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += drawUtils.messageBox(3, "Error: There was an error loading your ship.",
                    "Check your write permissions in the ../data dir.");
            return out;
        }
        
        if (shipService.isShipEmpty(ship)) {           
            out += drawUtils.messageBox(3, "Error: Your ship may ot be configured",
                    "First set your ship with: ship cargo;distance;cash ");
            return out;
        }
        
        out += tradeService.sell(foundStation.getName(), ship, jumps, foundCommodity);
        return out;
    }
    
    @CliCommand(value = { "buy" }, help = "usage: buy --start 'Station Name' --jumps 2 --commodity 'GOLD' \n Best resource buy price with n jumps of your jump distance.")
    public String buy(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps,
            @CliOption(key = { "commodity" }, mandatory = false, help = "Commodity name") final String commodity
        ) {
        
        
        String usage1 = "Usage: buy --start 'Station Name' --jumps 2 --commodity 'GOLD'";
        String usage2=  "Omit --jumps and you search every station in the graph.";
        String usage3=  "With --jumps Best resource buy price with n jumps of your jump distance.";
        String usage4=  "With --jumps Depends on --start station or saved station, and your ship.";
        String out = "";
        Station foundStation;
        String foundCommodity;

        try {
            foundCommodity = getBestMatchingCommodityOrStoredCommodity(commodity);
        } catch (CommodityNotFoundException e) {          
            out += drawUtils.messageBox(3, "Error: Commodity matching expression '" + commodity + "' not found",
                    usage1, usage2, usage3, usage4);
            return out;
        }
        
        if (jumps == null) {
            out += tradeService.bestBuy(foundCommodity);
            return out;
        }
        
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += drawUtils.messageBox(3, "Error: Station matching expression '" + station + "' not found",
                    usage1, usage2, usage3, usage4);
            return out;
        }
      
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += drawUtils.messageBox(3, "Error: There was an error loading your ship.",
                    "Check your write permissions in the ../data dir.");
            return out;
        }
        
        if (shipService.isShipEmpty(ship)) {           
            out += drawUtils.messageBox(3, "Error: Your ship may ot be configured",
                    "First set your ship with: ship cargo;distance;cash ");
            return out;
        }
        
        out += tradeService.buy(foundStation.getName(), ship, jumps, foundCommodity);
        return out;
    }
 
    @CliCommand(value = { "gos" }, help = "usage: go --start 'Station Name' --to 'Station Name'")
    public String goStation(
            @CliOption(key = { "start" }, mandatory = false, help = "--start 'Starting Station'") final String station,
            @CliOption(key = { "to" }, mandatory = true, help = "--to 'Target Station'") final String toStation
        ) {
        
        String usage = "usage: go --start 'Station Name' --to 'Station Name'" 
                + OsUtils.LINE_SEPARATOR
                + "Best commodity exchange between 2 stations non stop." 
                + OsUtils.LINE_SEPARATOR
                + "or try a find '<partial station match>'"
                + OsUtils.LINE_SEPARATOR
                + "or try a ship cargo;distance;cash to set up your ship.";;
        
        String out = "";
        Station foundStation;
        Station foundStation2;
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
            foundStation2 = stationService.findUniqueStation(toStation, false);
        } catch (StationNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }

        if (shipService.isShipEmpty(ship)) {
            out += "First set your ship with: ship cargo;distance;cash " + OsUtils.LINE_SEPARATOR;
            return out;
        }
        
        out += tradeService.stationToStation(foundStation.getName(), ship, foundStation2.getName());
        return out;
    }

    


    protected String getBestMatchingCommodityOrStoredCommodity(String commodity) throws CommodityNotFoundException {
        
        if (commodity == null && commodityService.getUserLastStoredCommodity() != null) {
            String c =  commodityService.getUserLastStoredCommodity();
            if (StringUtils.isEmpty(c)) {
                throw new CommodityNotFoundException("No unique commodity could be found.");
            }
            return c;
        } else if (!StringUtils.isEmpty(commodity)) {
            return commodityService.findUniqueCommodity(commodity);            
        }
        
        throw new CommodityNotFoundException("No unique commodity could be found.");
    }
    
    @CliCommand(value = { "otrade" }, help = "...")
    public String otrade(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jump per trade (defaults to 1)") final Integer jumps,
            @CliOption(key = { "trades" }, mandatory = false, help = "Number of trades (defaults to 1)") final Integer trades
        ) {
        
        Date start = new Date();
        
        String usage1 = "Usage:   trade --start 'Station Name' --jumps 2";
        String usage2 = "Example: trade (Will use last stored station)";
        String usage3 = "Example: trade --jumps 2 (best 1 stop exchange within 2 jumps of your ship's jump distance)";
        String usage4 = "or try a find '<partial station match>'.";
        String usage5 = "or try a ship cargo;distance;cash to set up your ship.";
        
        String out = "";
        Station foundStation;
        Ship ship;
        
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += drawUtils.messageBox(3, "Error: Station matching expression '" + station + "' not found",
                    usage1, usage2, usage3, usage4, usage5);
            return out;
        }
        
        int jumpDistance = 1;
        if (jumps != null) {
            jumpDistance = jumps;
        }
        
        int tradeStops = 1;
        if (trades != null) {
            tradeStops = trades;
        }
        
        
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += drawUtils.messageBox(3, "Error: There was an error loading your ship.",
                                           "Check your write permissions in the ../data dir.");
            return out;
        }
        
        if (shipService.isShipEmpty(ship)) {           
            out += drawUtils.messageBox(3, "Error: Your ship may ot be configured",
                                           "First set your ship with: ship cargo;distance;cash ");
            return out;
        }
        
        List<BestExchange> endOfRunTrades = new ArrayList<>();
        List<BestExchange> sortedBestExchangeList = tradeService.tradeNOrientDb(foundStation.getName(), ship, jumpDistance, tradeStops, endOfRunTrades);
        
        /* format for N stations */
        if (tradeStops > 1) {
            
            out += "Best " + tradeStops + " trade stops exchange within " + ship.jumpDistance + " lys of each visited system." + OsUtils.LINE_SEPARATOR;
            List<BestExchange> endOfRunTradesSorted =  endOfRunTrades.parallelStream().sorted((a,b)->{ return Integer.compare(a.getRoutePerProfitUnit(), b.getRoutePerProfitUnit()); }).collect(Collectors.toList());
            endOfRunTradesSorted = Lists.reverse(endOfRunTradesSorted);
            
            int stopIdx = endOfRunTradesSorted.size() > 5 ? 5 : endOfRunTradesSorted.size();
            
            for (int i = 0; i < stopIdx; i++) {
                List<BestExchange> path = tradeService.pathToExchange(endOfRunTradesSorted, i);
                
                out += "Path #" + i + (i == 0 ? " (Best) " : "") + OsUtils.LINE_SEPARATOR;
                int tradeStop = 0;
                List<Map<String, Object>> tableData = new ArrayList<>();
                
                for (BestExchange stop: path) {
                    tradeStop ++;
                    Map<String, Object> mappedExchange = stop.toMap(tradeStop);
                    tableData.add(mappedExchange);
                }
                
                List<String> columns = ImmutableList.of("#", "FROM SYSTEM", "FROM STATION", "TO SYSTEM", "TO STATION", "COMMODITY", "UNIT PROFIT", "ROUTE PROFIT");
                out += OsUtils.LINE_SEPARATOR;
                out += TableRenderer.renderMapDataAsTable(tableData, columns);
                out += OsUtils.LINE_SEPARATOR;
            }
            out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
            return out;
        }
        
        /*
         * Format for a single station
         */
        List<Map<String, Object>> tableData = new ArrayList<>();
        int limit = sortedBestExchangeList.size() >= 10 ? 10 : sortedBestExchangeList.size();
        int index = 0;
        tradeService.getLastSearchedExchanges().clear();
        for (BestExchange exchange : sortedBestExchangeList.subList(0, limit)) {
            index++;
            Map<String, Object> mappedExchange = exchange.toMap(index);
            tableData.add(mappedExchange);
            SavedExchange e = new SavedExchange(new Station(exchange.getBuyStationName(), exchange.getBuySystemName()), 
                    new Station(exchange.getSellStationName(), exchange.getSellSystemName()));
            tradeService.getLastSearchedExchanges().put(index, e);
        }
        
        if (tableData.size() == 0) {
            return "No exchange available in provided range";
        }
        
        List<String> columns = ImmutableList.of("#", "TO SYSTEM", "TO STATION", "COMMODITY", "BUY @", "CARGO COST", "SELL @", "UNIT PROFIT", "PROFIT");
        out += OsUtils.LINE_SEPARATOR;
        out += "From System: " + tableData.get(0).get("FROM SYSTEM") + OsUtils.LINE_SEPARATOR;
        out += "From Station: " + tableData.get(0).get("FROM STATION") + OsUtils.LINE_SEPARATOR;
        out += "Cargo Capacity: " + ship.getCargoSpace() + OsUtils.LINE_SEPARATOR;
        out += tableData.size() + " Best trading solution within " + jumpDistance + " jump(s) @ " + ship.getJumpDistance() + " ly or less." + OsUtils.LINE_SEPARATOR;
        out += OsUtils.LINE_SEPARATOR;
        out += TableRenderer.renderMapDataAsTable(tableData, columns);
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        
        return out;
    }
    
}
