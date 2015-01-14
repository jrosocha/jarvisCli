package com.jhr.jarvis.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
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
    private Settings settings;
	
    @CliCommand(value = { "find", "f" }, help = "Find the station starting with the crap you typed.")
    public String find(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String out = "";
        out += stationService.find(buffer);
        return out;
    }
    
    @CliCommand(value = { "gon", "gox" }, help = "Best rescouce exchange with n hops of your jump distance. Takes 20 seconds or so. ")
    public String gon(
            @CliOption(key = { "start" }, mandatory = true, help = "Starting Station in CAPS") final String station,
            @CliOption(key = { "hops" }, mandatory = false, help = "Number of hops") Integer hops
        ) throws IOException {
        
        if (hops == null) {
            hops = 2;
        }
        
        String out = "";
        
        Ship s = shipService.loadShip();
        out += tradeService.gon(station, s, hops);
        
        return out;
    }
    
    @CliCommand(value = { "go", "go1" }, help = "Single hop trading, not jumping more than one node.")
    public String go(
            @CliOption(key = { "start" }, mandatory = true, help = "Starting Station in CAPS") final String station
        ) throws IOException {
        
        String out = "";
        
        Ship s = shipService.loadShip();
        out += tradeService.go(station, s);
        
        return out;
    }
    
    @CliCommand(value = "go2", help = "2 hop trading, not jumping more than one node.")
    public String go2(
            @CliOption(key = { "start" }, mandatory = true, help = "Starting Station in CAPS") final String station
        ) throws IOException {
        
        String out = "";
        
        Ship s = shipService.loadShip();
        out += tradeService.go2(station, s);
        
        return out;
    }
    
    @CliCommand(value = "go4", help = "2 hop trading, not jumping more than one node.")
    public String go4(
            @CliOption(key = { "start" }, mandatory = true, help = "Starting Station in CAPS") final String station
        ) throws IOException {
        
        String out = "";
        
        Ship s = shipService.loadShip();
        out += tradeService.go4(station, s);
        
        return out;
    }
	
}
