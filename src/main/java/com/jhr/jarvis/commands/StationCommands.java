package com.jhr.jarvis.commands;

import java.io.IOException;
import java.util.List;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhr.jarvis.exceptions.StationNotFoundException;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.service.AvoidStationService;
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
    
    @Autowired
    private AvoidStationService avoidStationService;

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
        
        List<Station> stations = stationService.findStationsOrientDb(buffer, true);
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
            out = stationService.joinStationsAsString(stationService.findStationsOrientDb(buffer, true));
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
    
    @CliCommand(value = { "avoid" }, help = "...")
    public String avoid(@CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "...") String buffer) {
        
        String usage1 = "Usage:       avoid <parial station name>";
        String usage2 = "Description: Finds a station starting with the input expression and adds it to your avoid list";
        String usage3 = "If more than one station matches, it just otputs the matching list.";
        String usage4 = "Example:     av goo";
        String out = "";
        
        if (StringUtils.isEmpty(buffer)) {
            out += "Avoid Stations:" + OsUtils.LINE_SEPARATOR + OsUtils.LINE_SEPARATOR;
            for (String stationName : avoidStationService.getAvoidStations()) {
                out += stationName + OsUtils.LINE_SEPARATOR;
            }
            return out;
        }
        
        List<Station> stations = stationService.findStationsOrientDb(buffer, false);
        if (stations.size() > 1) {
            String stationsString = stations.stream().map(Station::getName).collect(Collectors.joining(OsUtils.LINE_SEPARATOR));
            out += drawUtils.messageBox(3, 
                    "Found more than one matching station for  '" + buffer + "'",
                    stationsString);
            return out;
        }
        if (stations.size() == 0) {
            String stationsString = stations.stream().map(Station::getName).collect(Collectors.joining(OsUtils.LINE_SEPARATOR));
            out += drawUtils.messageBox(3, null,
                    "No matching station found for  '" + buffer + "'");
            return out;
        }
        
        avoidStationService.getAvoidStations().add(stations.get(0).getName());
        avoidStationService.saveAvoidStations();
        out += "Avoid Stations:" + OsUtils.LINE_SEPARATOR + OsUtils.LINE_SEPARATOR;
        for (String stationName : avoidStationService.getAvoidStations()) {
            out += stationName + OsUtils.LINE_SEPARATOR;
        }
        
        return out;
    }
    
    @CliCommand(value = { "set" }, help = "usage: set --station goo --blackmarket")
    public String set(
            @CliOption(key = { "station" }, mandatory = false, help = "Station") final String station,
            @CliOption(key = { "blackmarket" }, mandatory = false, help = "Toggle Station Black Market Flag", specifiedDefaultValue="true") final String blackMarket
        ) throws JsonParseException, JsonMappingException, IOException {
        
        String usage1 = "Usage:       set --station <parial station name> --blackmarket";
        String usage2 = "Description: Sets propeties on a station";
        String usage3 = "Example:     set --station goo --blackmarket";
        
        String out = "";
        Station foundStation;
        
        try {
            foundStation = stationService.getBestMatchingStationOrStoredStation(station);
        } catch (StationNotFoundException e) {
            out += drawUtils.messageBox(3, "Error: Station matching expression '" + station + "' not found",
                    usage1, usage2, usage3);
            return out;
        }
        
        if (!StringUtils.isEmpty(blackMarket)) {
        
	        boolean currentBlackMarket = foundStation.getBlackMarket() != null ? foundStation.getBlackMarket() : false;
	        stationService.addPropertyToStationOrientDb(foundStation, "blackMarket", new Boolean(!currentBlackMarket));
	        
	        try {
				out += stationService.findExactStationOrientDb(foundStation.getName()).toString();
			} catch (StationNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        return out;
    }
}
