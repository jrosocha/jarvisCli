package com.jhr.jarvis.commands;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.service.GraphDbService;
import com.jhr.jarvis.service.StarSystemService;

@Component
public class DevCommands implements CommandMarker {
	
    @Autowired
    private StarSystemService starSystemService;
 	
    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private ObjectMapper objectMapper;
    
	@CliCommand(value = "dev", help = "Dont mess with this.")
	public String loadSystemsToMemory(
		@CliOption(key = { "reload" }, mandatory = false, specifiedDefaultValue="true", help = "reload Systems.csv file.") final String reload,
	    @CliOption(key = { "get" }, mandatory = false, help = "Return data system from Systems.csv") final String get,
	    @CliOption(key = { "query" }, mandatory = false, help = "Raw neo4j query") final String query,
	    @CliOption(key = { "settings" }, mandatory = false, specifiedDefaultValue="true", help = "Loads/Reads the jarvis.properties file") final String properties,
	    @CliOption(key = { "wipe" }, mandatory = false, specifiedDefaultValue="true", help = "Emptys the Neo4j graph") final String wipeDb
	    ) throws IOException {
	    
	    String out = "";
	    
	    if (!StringUtils.isEmpty(reload)) {
	        starSystemService.loadSystems(new File(settings.getSystemsFile()));
	        out += "Loaded." + OsUtils.LINE_SEPARATOR;
	    }
	    
	    if (!StringUtils.isEmpty(get)) {
            List<StarSystem> ss = starSystemService.searchStarSystemsByName(get);
            if (ss == null || ss.size() == 0) {
                out += String.format("%s not found.", get) + OsUtils.LINE_SEPARATOR;
            } else {
                out += "Found: " + ss.size() + OsUtils.LINE_SEPARATOR;;
                for (StarSystem system: ss) {
                    out += system.toString() + OsUtils.LINE_SEPARATOR;
                }
            }
        }
	    
	    if (!StringUtils.isEmpty(query)) {
	        out += graphDbService.runCypherWithTransaction(query);
	    }
	    
	    if (!StringUtils.isEmpty(properties)) {
	        settings.loadSettings();
	        out += settings.toString() + OsUtils.LINE_SEPARATOR;
	    }
        
        if (!StringUtils.isEmpty(wipeDb)) {
            out += graphDbService.wipeDb();
        }
	    
	    return out;
	}
	
}
