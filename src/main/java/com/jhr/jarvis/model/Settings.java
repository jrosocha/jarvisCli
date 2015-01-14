package com.jhr.jarvis.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class Settings {

    /**
     * @return the eliteOcrScanArchiveEnabed
     */
    public boolean isEliteOcrScanArchiveEnabed() {
        return eliteOcrScanArchiveEnabed;
    }

    private boolean loaded = false;
    
    private String test = null;
    
    private String graphDb = null;
    
    private String systemsFile = null;
    
    private String eliteOcrScanDirectory = null;
    
    private boolean eliteOcrScanArchiveEnabed = false;
    
    @PostConstruct
    public void loadSettings() throws FileNotFoundException, IOException {
        
        File settings = new File("../data/jarvis.properties");
        
        Properties p = new Properties();
        try (InputStream fis = new FileInputStream(settings)) {
            p.load(fis);
            load(p);
        } catch (Exception e) {
            e.printStackTrace();
            // try developer file for load dev
            p.load(this.getClass().getResourceAsStream("/jarvis-dev.properties"));
            load(p);
        }
        
        loaded = true;
    }
    
    private void load(Properties p) {
        test = p.getProperty("test");
        graphDb = p.getProperty("neo4j");
        systemsFile = p.getProperty("systems.file.path");
        eliteOcrScanDirectory = p.getProperty("eliteocr.directory.path");
        eliteOcrScanArchiveEnabed = Boolean.parseBoolean(p.getProperty("eliteocr.directory.archive"));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Settings [loaded=" + loaded + ", test=" + test + ", graphDb=" + graphDb + ", systemsFile=" + systemsFile + ", eliteOcrScanDirectory=" + eliteOcrScanDirectory
                + ", eliteOcrScanArchiveEnabed=" + eliteOcrScanArchiveEnabed + "]";
    }
    
    /**
     * @return the test
     */
    public String getTest() {
        return test;
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
}
