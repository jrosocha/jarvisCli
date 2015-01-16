package com.jhr.jarvis.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Settings {

    @JsonIgnore
    private boolean loaded = false;
    
    private String graphDb = null;
    
    private String systemsFile = null;
    
    private String eliteOcrScanDirectory = null;
    
    private boolean eliteOcrScanArchiveEnabed = false;
    
    private int longestDistanceEdge = 25;
    
    private int closeSystemDistance = 10;
    
    @JsonIgnore
    @Autowired
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void loadSettings() throws FileNotFoundException, IOException {
        
        File settings = new File("../data/jarvis-config.json");
        Settings fileSettings = null;
        
        try {
            fileSettings = objectMapper.readValue(settings, Settings.class);
        } catch (Exception e) {
            e.printStackTrace();
            // try developer file for load dev
            fileSettings = objectMapper.readValue(this.getClass().getResourceAsStream("/jarvis-dev-config.json"), Settings.class);
        }
        
        load(fileSettings);
        
        loaded = true;
    }
    
    private void load(Settings s) {
        BeanUtils.copyProperties(s, this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * @return the loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * @return the graphDb
     */
    public String getGraphDb() {
        return graphDb;
    }   
    
    /**
     * @return the eliteOcrScanDirectory
     */
    public String getEliteOcrScanDirectory() {
        return eliteOcrScanDirectory;
    }

    /**
     * @param eliteOcrScanDirectory the eliteOcrScanDirectory to set
     */
    public void setEliteOcrScanDirectory(String eliteOcrScanDirectory) {
        this.eliteOcrScanDirectory = eliteOcrScanDirectory;
    }

    /**
     * @param graphDb the graphDb to set
     */
    public void setGraphDb(String graphDb) {
        this.graphDb = graphDb;
    }

    /**
     * @param systemsFile the systemsFile to set
     */
    public void setSystemsFile(String systemsFile) {
        this.systemsFile = systemsFile;
    }
    
    /**
     * @return the systemsFile
     */
    public String getSystemsFile() {
        return systemsFile;
    }
    
    /**
     * @return the eliteOcrScanArchiveEnabed
     */
    public boolean isEliteOcrScanArchiveEnabed() {
        return eliteOcrScanArchiveEnabed;
    }
    
    /**
     * @return the longestDistanceEdge
     */
    public int getLongestDistanceEdge() {
        return longestDistanceEdge;
    }

    /**
     * @param longestDistanceEdge the longestDistanceEdge to set
     */
    public void setLongestDistanceEdge(int longestDistanceEdge) {
        this.longestDistanceEdge = longestDistanceEdge;
    }

    /**
     * @return the closeSystemDistance
     */
    public int getCloseSystemDistance() {
        return closeSystemDistance;
    }

    /**
     * @param closeSystemDistance the closeSystemDistance to set
     */
    public void setCloseSystemDistance(int closeSystemDistance) {
        this.closeSystemDistance = closeSystemDistance;
    }
}
