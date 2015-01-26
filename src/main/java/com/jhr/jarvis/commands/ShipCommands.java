package com.jhr.jarvis.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;
import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.util.DrawUtils;

@Component
public class ShipCommands implements CommandMarker {
	
    @Autowired
    private ShipService shipService;
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private DrawUtils drawUtils;
    
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
                out += drawUtils.messageBox(3, 
                        "Error: Invalid Use of 'ship' Command",
                        "Usage:   ship cargo;distance;cash", 
                        "Example: ship 44;10.0;1000000",
                        shipService.loadShip().toString());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        
        return out;
    }
	
}
