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
    

    
    @CliCommand(value = { "gon", "gox" }, help = "usage: gon --start 'Station Name' --jumps 2 \n Best resource exchange with n jumps of your jump distance. Takes 20 seconds or so for --jumps 3. ")
    public String gon(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
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
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
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
        
        if (shipService.isShipEmpty(ship)) {
            out += "First set your ship with: ship cargo;distance;cash " + OsUtils.LINE_SEPARATOR;
            return out;
        }
        
        out += tradeService.gon(foundStation, ship, jumpDistance);
        
        return out;
    }
    
    @CliCommand(value = { "go2n", "go2x" }, help = "usage: gon --start 'Station Name' --jumps 2 \n Best resource exchange with 1..n jumps of your jump distance. Takes 30 seconds or so for --jumps 3. ")
    public String go2n(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
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
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
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
        
        if (shipService.isShipEmpty(ship)) {
            out += "First set your ship with: ship cargo;distance;cash " + OsUtils.LINE_SEPARATOR;
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
            @CliOption(key = { "start" }, mandatory = false, help = "--start Starting Station") final String station
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
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
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
        
        try {
            out += tradeService.go(foundStation, ship);
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
        }
        
        return out;
    }
    
    @CliCommand(value = "go2", help = "usage: go --start 'Station Name' \n 2 stop trading, not jumping more than one system.")
    public String go2(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station
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
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
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
                
        try {
            out += tradeService.go2(foundStation, ship);
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usage;
        }
        
        return out;
    }
    
    @CliCommand(value = { "sell" }, help = "usage: sell --start 'Station Name' --jumps 2 --commodity 'GOLD' \n Best resource sell price with n jumps of your jump distance.")
    public String sell(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps,
            @CliOption(key = { "commodity" }, mandatory = false, help = "Commodity name") final String commodity
        ) {
        
        String usage = "usage: sell --start 'Station Name' --jumps 2 --commodity 'GOLD'" 
                        + OsUtils.LINE_SEPARATOR
                        + "Best resource sell price with n jumps of your jump distance.";
        String out = "";
        String foundStation;
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
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
        
        if (shipService.isShipEmpty(ship)) {
            out += "First set your ship with: ship cargo;distance;cash " + OsUtils.LINE_SEPARATOR;
            return out;
        }
        
        out += tradeService.sell(foundStation, ship, jumpDistance, foundCommodity);
        return out;
    }
    
    @CliCommand(value = { "buy" }, help = "usage: buy --start 'Station Name' --jumps 2 --commodity 'GOLD' \n Best resource buy price with n jumps of your jump distance.")
    public String buy(
            @CliOption(key = { "start" }, mandatory = false, help = "Starting Station") final String station,
            @CliOption(key = { "jumps" }, mandatory = false, help = "Number of jumps") final Integer jumps,
            @CliOption(key = { "commodity" }, mandatory = false, help = "Commodity name") final String commodity
        ) {
        
        String usage = "usage: buy --start 'Station Name' --jumps 2 --commodity 'GOLD'" 
                        + OsUtils.LINE_SEPARATOR
                        + "Best resource buy price with n jumps of your jump distance.";
        String out = "";
        String foundStation;
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
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
        
        if (shipService.isShipEmpty(ship)) {
            out += "First set your ship with: ship cargo;distance;cash " + OsUtils.LINE_SEPARATOR;
            return out;
        }
        
        out += tradeService.buy(foundStation, ship, jumpDistance, foundCommodity);
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
        String foundStation;
        String foundStation2;
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
        
        out += tradeService.stationToStation(foundStation, ship, foundStation2);
        return out;
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
