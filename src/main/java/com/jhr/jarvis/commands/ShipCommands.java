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
	
//    @CliCommand(value = "ship", help = "Functions related to setting ship defaults for trading.")
//    public String ocrCommands(
//        @CliOption(key = { "status" }, mandatory = false, specifiedDefaultValue = "true", help = "Prints your ship") final String status,
//        @CliOption(key = { "save" }, mandatory = false, help = "Save your ship. Format = cargo(int);distance(float);cash(int)") final String save
//        ) throws IOException {
//        
//        String out = "";
//        
//        if (!StringUtils.isEmpty(status)) {       
//            Ship ship = shipService.loadShip();
//            out += ship;
//        }
//        
//        if (!StringUtils.isEmpty(save)) {       
//            try {
//                String[] shipParts = save.split(";");
//                Ship ship = new Ship(Integer.parseInt(shipParts[0]), Float.parseFloat(shipParts[1]), Integer.parseInt(shipParts[2]));
//                ship = shipService.saveShip(ship);
//                out += ship;
//            } catch (Exception e) {
//                out += "Error saving ship, format: cargo(int);distance(float);cash(int)";
//            }
//        }
//        
//        if (StringUtils.isEmpty(out)) {
//            out += "format: cargo(int);distance(float);cash(int)" + OsUtils.LINE_SEPARATOR + shipService.loadShip();
//        }
//        
//        return out;
//    }
    
    @CliCommand(value = "ship", help = "dont know")
    public String obtainHelp(
            @CliOption(key = { "", "command" }, optionContext = "disable-string-converter availableCommands", help = "Command name to provide help for") String buffer) {
        
        String out = "";
        try {
            String[] shipParts = buffer.split(";");
            Ship ship = new Ship(Integer.parseInt(shipParts[0]), Float.parseFloat(shipParts[1]), Integer.parseInt(shipParts[2]));
            ship = shipService.saveShip(ship);
            out += ship;
        } catch (Exception e) {
            try {
                out += "format: ship cargo(int);distance(float);cash(int)" + OsUtils.LINE_SEPARATOR + shipService.loadShip();
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
