package com.jhr.jarvis.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhr.jarvis.model.Settings;

@Service
public class AvoidStationService {

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private Settings settings;
    
    private List<String> avoidStations = null;  
    
    /**
     * @return the avoidStations
     */
    public List<String> getAvoidStations() {     

        if (avoidStations == null) {
            try {
                avoidStations = objectMapper.readValue(new File(settings.getAvoidStationsFile()),new TypeReference<List<String>>() {});
            } catch (Exception e) {
                avoidStations = new ArrayList<>();
            }
        }
        return avoidStations;
    }

    /**
     * @param avoidStations the avoidStations to set
     */
    public void saveAvoidStations() {
        
        try {
            objectMapper.writeValue(new File(settings.getAvoidStationsFile()), avoidStations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
