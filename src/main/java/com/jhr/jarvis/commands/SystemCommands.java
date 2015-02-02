package com.jhr.jarvis.commands;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ImmutableList;
import com.jhr.jarvis.exceptions.SystemNotFoundException;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.service.StarSystemService;
import com.jhr.jarvis.service.StationService;
import com.jhr.jarvis.table.TableRenderer;
import com.jhr.jarvis.util.DrawUtils;

@Component
public class SystemCommands implements CommandMarker {
	
    @Autowired
    private ShipService shipService;

    @Autowired
    private StarSystemService starSystemService;
    
    @Autowired
    private StationService stationService;
    
    @Autowired
    private DrawUtils drawUtils;

    @CliCommand(value = { "system" }, help = "usage: system <exact or starts with>")
    public String osystem(
            @CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "usage: system <regex>") String buffer) {
        
        String out = "";
        StarSystem starSystem;
        Date start = new Date();
        
        try {
            starSystem = starSystemService.findExactSystemOrientDb(buffer);           
        } catch (SystemNotFoundException e) {
            List<StarSystem> systems = starSystemService.findSystemsOrientDb(buffer);
            if (systems.size() == 0) {
                out += drawUtils.messageBox(3, "Error: No systems matching '" + buffer + "' could be found.",
                                               "Jarvis won't load a system until you've been close to it.");
                return out;
            } else if (systems.size() == 1) {
                starSystem = systems.get(0);
            } else {
                
                List<Map<String, Object>> tableData = systems.stream().map(sys->{
                    Map<String, Object> tableRow = new HashMap<>();
                    tableRow.put("SYSTEM", sys.getName());
                    tableRow.put("X", sys.getX());
                    tableRow.put("Y", sys.getY());
                    tableRow.put("Z", sys.getZ());
                    return tableRow;
                }).collect(Collectors.toList());
                
                out += "Systems matching '" + buffer + "'" + OsUtils.LINE_SEPARATOR;
                out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(tableData, ImmutableList.of("SYSTEM", "X", "Y", "Z"));
                out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
                return out;
            }
        }
        
        List<Station> stations = stationService.getStationsForSystemOrientDb(starSystem.getName());
        List<Map<String, Object>> tableData = stations.stream().map(station->{
            Map<String, Object> tableRow = new HashMap<>();
            tableRow.put("STATION", station.getName());
            tableRow.put("DAYS OLD", (new Date().getTime() - station.getDate())/1000/60/60/24 );
            return tableRow;
        }).collect(Collectors.toList());

        out += OsUtils.LINE_SEPARATOR;
        out += "SYSTEM: " + starSystem.getName() + " @ " + starSystem.getX() + ", " + starSystem.getY() + ", " + starSystem.getZ() + OsUtils.LINE_SEPARATOR ; 
        out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(tableData, ImmutableList.of("STATION", "DAYS OLD"));
        out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
        return out;
    }
    
    @CliCommand(value = { "path" }, help = "usage: path --from 'System Name' --to 'System Name'")
    public String path(
            @CliOption(key = { "from" }, mandatory = false, help = "Starting System") final String from,
            @CliOption(key = { "to" }, mandatory = false, help = "End System") final String to
        ) throws JsonParseException, JsonMappingException, IOException {
        
        String out = "";
        String usage = "Usage:    path --from 'System Name' --to 'System Name'";
        
        Ship ship;
        try {
            ship = shipService.loadShip();
        } catch (IOException e) {
            out += drawUtils.messageBox(3, "Error: There was an error loading your ship.",
                                           "Check your write permissions in the ../data dir.");
            return out;
        }
        
        StarSystem foundFrom = null;
        if (StringUtils.isEmpty(from)) {
            String storedSystem = starSystemService.getUserLastStoredSystem();
            if (StringUtils.isEmpty(storedSystem)) {
                out += drawUtils.messageBox(3, "Error:    Could not find 'from' system matching '" + from + "'",
                                                usage,
                                               "Example:  path --from igala --to pemede",
                                               "If you have stored a station with a 'find' or 'station' command, it will default to 'from'.");
                return out;
            }
            try {
                foundFrom = starSystemService.findExactSystemOrientDb(storedSystem);                
            } catch (SystemNotFoundException e) {
                out += drawUtils.messageBox(3, "Error:    Could not find 'from' system matching '" + from + "'",
                                                usage,
                                               "Example:  path --from igala --to pemede",
                                               "If you have stored a station with a 'find' or 'station' command, it will default to 'from'.");
                return out;
            }
        } else {
            try {
                foundFrom = starSystemService.findUniqueSystemOrientDb(from);
            } catch (SystemNotFoundException e) {
                out += drawUtils.messageBox(3, "Error:    Could not find 'from' system matching '" + from + "'",
                                                usage,
                                               "Example:  path --from igala --to pemede");
                return out;
            }
        }
        
        StarSystem foundTo = null;
        if (StringUtils.isEmpty(to)) {
            out += drawUtils.messageBox(3, "Error:    Could not find 'to' system matching '" + to + "'",
                    usage,
                   "Example:  path --from igala --to pemede");
            return out;
        }
        
        try {
            foundTo = starSystemService.findUniqueSystemOrientDb(to);
        } catch (SystemNotFoundException e) {
            out += drawUtils.messageBox(3, "Error:    Could not find 'to' system matching '" + to + "'",
                                            usage,
                                           "Example:  path --from igala --to pemede");
            return out;
        }
        
       return starSystemService.shortestPath(ship, foundFrom.getName(), foundTo.getName());

    }
	
}
