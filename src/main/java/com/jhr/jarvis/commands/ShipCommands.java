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

@Component
public class ShipCommands implements CommandMarker {
	
    @Autowired
    private ShipService shipService;
    
    @Autowired
    private Settings settings;
	
    @CliCommand(value = "ship", help = "Functions related to setting ship defaults for trading.")
    public String ocrCommands(
        @CliOption(key = { "status" }, mandatory = false, specifiedDefaultValue = "true", help = "Prints your ship") final String status,
        @CliOption(key = { "save" }, mandatory = false, help = "Save your ship. Format = cargo(int);distance(float);cash(int)") final String save
        ) throws IOException {
        
        String out = "";
        
        if (!StringUtils.isEmpty(status)) {       
            Ship ship = shipService.loadShip();
            out += ship;
        }
        
        if (!StringUtils.isEmpty(save)) {       
            try {
                String[] shipParts = save.split(";");
                Ship ship = new Ship(Integer.parseInt(shipParts[0]), Float.parseFloat(shipParts[1]), Integer.parseInt(shipParts[2]));
                ship = shipService.saveShip(ship);
                out += ship;
            } catch (Exception e) {
                out += "Error saving ship, format: cargo(int);distance(float);cash(int)";
            }
        }
        
        return out;
    }
	
}
