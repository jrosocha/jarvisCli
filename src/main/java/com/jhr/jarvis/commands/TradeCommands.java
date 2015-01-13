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
import com.jhr.jarvis.service.TradeService;

@Component
public class TradeCommands implements CommandMarker {
	
    @Autowired
    private TradeService tradeService;
    
    @Autowired
    private ShipService shipService;
    
    @Autowired
    private Settings settings;
	
    @CliCommand(value = "go", help = "Functions related to trading.")
    public String ocrCommands(
            @CliOption(key = { "start" }, mandatory = true, help = "Starting Station in CAPS") final String station
        ) throws IOException {
        
        String out = "";
        
        Ship s = shipService.loadShip();
        out += tradeService.go(station, s);
        
        return out;
    }
	
}
