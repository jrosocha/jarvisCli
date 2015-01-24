package com.jhr.jarvis.commands;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.service.StarSystemService;
import com.jhr.jarvis.service.StationService;

@Component
public class SystemCommands implements CommandMarker {
	
    @Autowired
    private ShipService shipService;

    @Autowired
    private StarSystemService starSystemService;
    
    @Autowired
    private StationService stationService;
    
    @CliCommand(value = { "system" }, help = "usage: system <exact or regex>")
    public String path(
            @CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "usage: system <regex>") String buffer) {
        
        StarSystem starSystem;
        try {
            starSystem = starSystemService.findExactSystem(buffer);           
        } catch (SystemNotFoundException e) {
            List<StarSystem> systems = starSystemService.findSystem(buffer);
            if (systems.size() == 0) {
                return "No systems found matching " + buffer;
            } else if (systems.size() == 1) {
                starSystem = systems.get(0);
            } else {
                return systems.stream().map(StarSystem::getName).collect(Collectors.joining(OsUtils.LINE_SEPARATOR));
            }
        }
        
        List<Station> stations = stationService.getStationsForSystem(starSystem.getName());
        
        String out = "";
        out += "SYSTEM: " +  starSystem.getName() + OsUtils.LINE_SEPARATOR;
        out += "-------------------------------------------------" + OsUtils.LINE_SEPARATOR;
        for (Station sta: stations) {
            float lastUpdated =  (new Date().getTime() - sta.getDate()) / 1000 / 60 / 60 / 24;
            out += sta.getName() + " last updated " + lastUpdated + " days ago" + OsUtils.LINE_SEPARATOR;
        }
        return out;
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
        
        StarSystem foundFrom;
        try {
            foundFrom = starSystemService.findUniqueSystem(from);
        } catch (SystemNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usgae;
            return out;
        }
        
        StarSystem foundTo;
        try {
            foundTo = starSystemService.findUniqueSystem(to);
        } catch (SystemNotFoundException e) {
            out += e.getMessage() + OsUtils.LINE_SEPARATOR + usgae;
            return out;
        }
        
        return starSystemService.calculateShortestPathBetweenSystems(foundFrom.getName(), foundTo.getName(), ship.jumpDistance);
    }
	
}
