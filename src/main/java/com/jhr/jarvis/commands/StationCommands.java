package com.jhr.jarvis.commands;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.service.StationService;
import com.jhr.jarvis.util.DrawUtils;

@Component
public class StationCommands implements CommandMarker {

    @Autowired
    private StationService stationService;
    
    @Autowired
    private DrawUtils drawUtils;
    
    @Autowired
    private ObjectMapper objectMapper;

    @CliCommand(value = { "find" }, help = "usage: find Goo \n Find the station starting with the crap you typed.")
    public String findOrientDb(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String usage1 = "Usage:       find <parial station name>";
        String usage2 = "Description: Finds a station or stations starting with the input expression.";
        String usage3 = "If a single system is returned, it is stored for future trade commands.";
        String usage4 = "Example:     find goo";
        
        String out = "";
        
        if (StringUtils.isEmpty(buffer)) {
            out += drawUtils.messageBox(3, 
            "Error: Invalid Use of 'find' Command",
            usage1,
            usage2,
            usage3,
            usage4);
            return out;
        }
        
        List<Station> stations = stationService.findStationsOrientDb(buffer);
        if (stations.size() > 0) {
            out += stationService.joinStationsAsString(stations);
        } else {
            out += drawUtils.messageBox(3, 
            "No Stations Found Starting With '" + buffer + "'",
            usage1,
            usage2,
            usage3,
            usage4);
        }
        return out;
    }
    
    @CliCommand(value = { "station" }, help = "usage: station Goo \n Find the station starting with the crap you typed.")
    public String stationDetailsOrientDb(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String usage1 = "Usage:       station <parial station name>";
        String usage2 = "Description: Finds a station starting with the input expression.";
        String usage3 = "If a single system is returned, it is stored for future trade commands.";
        String usage4 = "Example:     sta goo";
        
        if (StringUtils.isEmpty(buffer)) {
            return drawUtils.messageBox(3, 
                    "Error: Invalid Use of 'station' Command",
                    usage1,
                    usage2,
                    usage3,
                    usage4);
        }
        
        Station station = null;
        try {
            station = stationService.getBestMatchingStationOrStoredStation(buffer);
        } catch (StationNotFoundException e) {
            String out = null;
            out = stationService.joinStationsAsString(stationService.findStationsOrientDb(buffer));
            if (StringUtils.isEmpty(out)) {
                out += drawUtils.messageBox(3, 
                    "No Stations Found Starting With '" + buffer + "'",
                    usage1,
                    usage2,
                    usage3,
                    usage4);
            }
            return out;
        }
        
        return stationService.stationDetailsOrientDb(station);

    }
}
