package com.jhr.jarvis.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.core.SimpleParser;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.service.ShipService;

@Component
public class ShipCommands implements CommandMarker {
	
    @Autowired
    private ShipService shipService;
    
    @Autowired
    private Settings settings;
    
    @CliCommand(value = "ship", help = "usage: ship cargo;distance;cash \n Saves your ship for future commands.")
    public String obtainHelp(
            @CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "cargo;distance;cash") String buffer) {
        
        String out = "";
        try {
            String[] shipParts = buffer.split(";");
            Ship ship = new Ship(Integer.parseInt(shipParts[0]), Float.parseFloat(shipParts[1]), Integer.parseInt(shipParts[2]));
            ship = shipService.saveShip(ship);
            out += ship;
        } catch (Exception e) {
            try {
                out += "usage: ship cargo;distance;cash" + OsUtils.LINE_SEPARATOR + shipService.loadShip();
            } catch (JsonParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (JsonMappingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        
        return out;
    }
	
}
