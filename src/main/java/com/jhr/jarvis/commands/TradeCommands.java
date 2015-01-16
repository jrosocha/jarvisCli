package com.jhr.jarvis.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.jhr.jarvis.exceptions.CommodityNotFoundException;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.service.CommodityService;
import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.service.StationService;
import com.jhr.jarvis.service.TradeService;

@Component
public class TradeCommands implements CommandMarker {
    
    @Autowired
    private TradeService tradeService;
    
    @Autowired
    private ShipService shipService;

    @Autowired
    private StationService stationService;

    @Autowired
    private CommodityService commodityService;
    
    @Autowired
    private Settings settings;
    
    @CliCommand(value = { "find", "f" }, help = "usage: find Goo \n Find the station starting with the crap you typed.")
    public String find(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String usage = "usage: find <partal station name>" 
                + OsUtils.LINE_SEPARATOR
                + "Identifies stations starting with user input. If a single system is returned, it is stored for future trade commands.";
        
        if (StringUtils.isEmpty(buffer)) {
            return usage;
        }
        
        String out = "";
        out += stationService.findStation(buffer);
        return out;
    }
    
    @CliCommand(value = { "gon", "gox" }, help = "usage: gon --start 'Station Name' --jumps 2 \n Best resource exchange with n jumps of your jump distance. Takes 20 seconds or so for --jumps 3. ")
    public String gon(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station in CAPS") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps
        ) {
        
        String usage = "usage: gon --start 'Station Name' --jumps 2" 
                        + OsUtils.LINE_SEPARATOR
                        + "Best resource exchange within n jumps of your jump distance. Takes 20+ seconds or so for --jumps 3." 
                        + OsUtils.LINE_SEPARATOR
                        + "or try a find '<partial station match>'."
                        + OsUtils.LINE_SEPARATOR
                        + "or try a ship cargo;distance;cash to set up your ship.";
        String out = "";
        String foundStation;
        try {
            foundStation = getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        int jumpDistance = 2;
        if (jumps != null) {
            jumpDistance = jumps;
        }
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        out += tradeService.gon(foundStation, ship, jumpDistance);
        
        return out;
    }
    
    @CliCommand(value = { "go2n", "go2x" }, help = "usage: gon --start 'Station Name' --jumps 2 \n Best resource exchange with 1..n jumps of your jump distance. Takes 30 seconds or so for --jumps 3. ")
    public String go2n(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station in CAPS") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps
        ) {
        
        String usage = "usage: gon --start 'Station Name' --jumps 2" 
                        + OsUtils.LINE_SEPARATOR
                        + "Best resource exchange within 1..n jumps of your jump distance. Takes 20+ seconds or so for --jumps 3." 
                        + OsUtils.LINE_SEPARATOR
                        + "or try a find '<partial station match>'."
                        + OsUtils.LINE_SEPARATOR
                        + "or try a ship cargo;distance;cash to set up your ship.";
        
        String out = "";
        String foundStation;
        try {
            foundStation = getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        int jumpDistance = 2;
        if (jumps != null) {
            jumpDistance = jumps;
        }
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        try {
            out += tradeService.gon2(foundStation, ship, jumpDistance);
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
        }
        
        return out;
    }
    
    @CliCommand(value = { "go", "go1" }, help = "usage: go --start 'Station Name' \n Single jump trading, not jumping more than one node.")
    public String go(
            @CliOption(key = { "start" }, mandatory = false, help = "--start Starting Station in CAPS") final String station
        ) {
        
        String usage = "usage: go --start 'Station Name'" 
                + OsUtils.LINE_SEPARATOR
                + "Single jump trading." 
                + OsUtils.LINE_SEPARATOR
                + "or try a find '<partial station match>'"
                + OsUtils.LINE_SEPARATOR
                + "or try a ship cargo;distance;cash to set up your ship.";;
        
        String out = "";
        String foundStation;
        try {
            foundStation = getBestMatchingStationOrStoredStation(station);
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
        
        try {
            out += tradeService.go(foundStation, ship);
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
        }
        
        return out;
    }
    
    @CliCommand(value = "go2", help = "usage: go --start 'Station Name' \n 2 stop trading, not jumping more than one system.")
    public String go2(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station in CAPS") final String station
        ) {
        
        String usage = "usage: go --start 'Station Name'" 
                + OsUtils.LINE_SEPARATOR
                + "Single jump trading." 
                + OsUtils.LINE_SEPARATOR
                + "or try a find '<partial station match>'"
                + OsUtils.LINE_SEPARATOR
                + "or try a ship cargo;distance;cash to set up your ship.";;

        String out = "";
        String foundStation;
        try {
            foundStation = getBestMatchingStationOrStoredStation(station);
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
                
        try {
            out += tradeService.go2(foundStation, ship);
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
        }
        
        return out;
    }
    
    @CliCommand(value = { "sell" }, help = "usage: sell --start 'Station Name' --jumps 2 --commodity 'GOLD' \n Best resource sell price with n jumps of your jump distance.")
    public String sell(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station in CAPS") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps,
            @CliOption(key = { "commodity" }, mandatory = false, help = "Commodity name") final String commodity
        ) {
        
        String usage = "usage: sell --start 'Station Name' --jumps 2 --commodity 'GOLD'" 
                        + OsUtils.LINE_SEPARATOR
                        + "Best resource sell price with n jumps of your jump distance.";
        String out = "";
        String foundStation;
        try {
            foundStation = getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        String foundCommodity;
        try {
            foundCommodity = getBestMatchingCommodityOrStoredCommodity(commodity);
        } catch (CommodityNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        int jumpDistance = 2;
        if (jumps != null) {
            jumpDistance = jumps;
        }
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        out += tradeService.sell(foundStation, ship, jumpDistance, foundCommodity);
        return out;
    }
    
    @CliCommand(value = { "buy" }, help = "usage: buy --start 'Station Name' --jumps 2 --commodity 'GOLD' \n Best resource buy price with n jumps of your jump distance.")
    public String buy(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station in CAPS") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps,
            @CliOption(key = { "commodity" }, mandatory = false, help = "Commodity name") final String commodity
        ) {
        
        String usage = "usage: buy --start 'Station Name' --jumps 2 --commodity 'GOLD'" 
                        + OsUtils.LINE_SEPARATOR
                        + "Best resource buy price with n jumps of your jump distance.";
        String out = "";
        String foundStation;
        try {
            foundStation = getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        String foundCommodity;
        try {
            foundCommodity = getBestMatchingCommodityOrStoredCommodity(commodity);
        } catch (CommodityNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        int jumpDistance = 2;
        if (jumps != null) {
            jumpDistance = jumps;
        }
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
            return out;
        }
        
        out += tradeService.buy(foundStation, ship, jumpDistance, foundCommodity);
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
    protected String getBestMatchingStationOrStoredStation(String station) throws StationNotFoundException {
        
        if (station == null && stationService.getUserLastStoredStation() != null) {
            return stationService.getUserLastStoredStation();
        } else if (!StringUtils.isEmpty(station)) {
            return stationService.findUniqueStation(station);            
        }
        
        throw new StationNotFoundException("No unique station could be found.");
    }

    protected String getBestMatchingCommodityOrStoredCommodity(String commodity) throws CommodityNotFoundException {
        
        if (commodity == null && commodityService.getUserLastStoredCommodity() != null) {
            return commodityService.getUserLastStoredCommodity();
        } else if (!StringUtils.isEmpty(commodity)) {
            return commodityService.findUniqueCommodity(commodity);            
        }
        
        throw new CommodityNotFoundException("No unique commodity could be found.");
    }
}
