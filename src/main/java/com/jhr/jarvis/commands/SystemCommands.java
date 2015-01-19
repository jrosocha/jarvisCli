package com.jhr.jarvis.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.exceptions.SystemNotFoundException;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.service.StarSystemService;
import com.jhr.jarvis.service.StationService;

@Component
public class SystemCommands implements CommandMarker {
	
    @Autowired
    private ShipService shipService;

    @Autowired
    private StarSystemService starSystemService;
    
    @CliCommand(value = { "system" }, help = "usage: system <regex>")
    public String path(
            @CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "usage: system <regex>") String regex) {
        
        return starSystemService.findSystem(regex);
    }
    
    @CliCommand(value = { "path" }, help = "usage: path --from 'System Name' --to 'System Name'")
    public String path(
            @CliOption(key = { "from" }, mandatory = true, help = "Starting System") final String from,
            @CliOption(key = { "to" }, mandatory = true, help = "End System") final String to
        ) throws JsonParseException, JsonMappingException, IOException {
        
        String out = "";
        String usgae = "usage: path --from 'System Name' --to 'System Name'";
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usgae;
            return out;
        }
        
        String foundFrom;
        try {
            foundFrom = starSystemService.findUniqueSystem(from);
        } catch (SystemNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usgae;
            return out;
        }
        
        String foundTo;
        try {
            foundTo = starSystemService.findUniqueSystem(to);
        } catch (SystemNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usgae;
            return out;
        }
        
        return starSystemService.calculateShortestPathBetweenStations(foundFrom, foundTo, ship.jumpDistance);
    }
	
}
