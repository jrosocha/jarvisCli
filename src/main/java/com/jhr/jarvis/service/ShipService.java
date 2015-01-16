package com.jhr.jarvis.service;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.Ship;

@Service
public class ShipService {

    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private File shipFile = new File("../data/ship");
    
    public boolean isShipEmpty(Ship ship) {
        if (ship == null || ship.getCargoSpace() == 0 || ship.getCash() == 0 || ship.getJumpDistance() == 0) {
            return true;
        }
        return false;
    }
    
    public Ship saveShip(Ship ship) throws JsonGenerationException, JsonMappingException, IOException {
        
        if (shipFile.exists()) {
            shipFile.delete();
        }
        
        objectMapper.writeValue(shipFile, ship);
        
        return ship;
    }
    
    public Ship loadShip() throws JsonParseException, JsonMappingException, IOException {
        if (shipFile.exists()) {
            return objectMapper.readValue(shipFile, Ship.class);
        }
        
        return saveShip(new Ship());
    }
    
    
    
    
}
