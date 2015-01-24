package com.jhr.jarvis.commands;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.service.StationService;

@Component
public class StationCommands implements CommandMarker {

    @Autowired
    private StationService stationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @CliCommand(value = { "find" }, help = "usage: find Goo \n Find the station starting with the crap you typed.")
    public String find(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String usage = "usage: find <partal station name>" 
                + OsUtils.LINE_SEPARATOR
                + "Identifies stations starting with user input. If a single system is returned, it is stored for future trade commands.";
        
        if (StringUtils.isEmpty(buffer)) {
            return usage;
        }
        
        String out = "";
        
        List<Station> stations = stationService.findStations(buffer);
        if (stations.size() > 0) {
            out += stationService.joinStationsAsString(stations);
        } else {
            out += "Not stations found starting with " + buffer;
        }
        return out;
    }
    
    @CliCommand(value = { "station" }, help = "usage: station Goo \n Find the station starting with the crap you typed.")
    public String stationDetails(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String usage = "usage: station <partal station name>" 
                + OsUtils.LINE_SEPARATOR
                + "Identifies stations starting with user input. If a single system is returned, it is stored for future trade commands.";
        
        if (StringUtils.isEmpty(buffer)) {
            return usage;
        }
        
        Station station = null;
        try {
            station = stationService.getBestMatchingStationOrStoredStation(buffer);
        } catch (StationNotFoundException e) {
            String out = null;
            out = stationService.joinStationsAsString(stationService.findStations(buffer));
            if (StringUtils.isEmpty(out)) {
                out = "No stations found starting with: " + buffer + OsUtils.LINE_SEPARATOR;
            }
            return out;
        }
        
        return stationService.stationDetails(station);

    }
}
