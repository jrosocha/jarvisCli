package com.jhr.jarvis.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.service.StationService;

@Component
public class StationCommands implements CommandMarker {

    @Autowired
    private StationService stationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
    
    @CliCommand(value = { "station", "st" }, help = "usage: station Goo \n Find the station starting with the crap you typed.")
    public String stationDetails(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String usage = "usage: station <partal station name>" 
                + OsUtils.LINE_SEPARATOR
                + "Identifies stations starting with user input. If a single system is returned, it is stored for future trade commands.";
        
        if (StringUtils.isEmpty(buffer)) {
            return usage;
        }
        
        String station = null;
        try {
            station = stationService.getBestMatchingStationOrStoredStation(buffer);
        } catch (StationNotFoundException e) {
            return stationService.findStation(buffer);
        }
        
        return stationService.stationDetails(station);

    }
}
