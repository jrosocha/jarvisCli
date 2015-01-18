package com.jhr.jarvis.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Service;

import com.jhr.jarvis.model.Commodity;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;

@Service
@DependsOn({"settings"})
public class EliteOcrService {

    /**
     * @return the lastScanned
     */
    public Date getLastScanned() {
        return lastScanned;
    }

    @Autowired
    private Settings settings;
    
    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private StarSystemService starSystemService;
    
    @Autowired
    private StationService stationService;
    
    private Date lastScanned = null;
    
    public synchronized String scanDirectory(boolean doArchive) throws IOException {
        
        Date start = new Date();
        lastScanned = new Date();
        
        File eliteOcrDir = new File(settings.getEliteOcrScanDirectory());
        File eliteOcrArchiveDir = new File(eliteOcrDir, "archive");
        if (!eliteOcrArchiveDir.exists()) {
            eliteOcrArchiveDir.mkdir();
        }
        
        String out = "";
        
        File[] filesInOcrDir = eliteOcrDir.listFiles();
        for (int i = 0; i < filesInOcrDir.length; i++) {
            if (filesInOcrDir[i].isFile() && filesInOcrDir[i].getName().endsWith(".csv")) {               
                out += processEliteOcrCSVFile(filesInOcrDir[i]);
                // archive the read file
                if (doArchive) {
                    try {
                        filesInOcrDir[i].renameTo(new File(eliteOcrArchiveDir, filesInOcrDir[i].getName()));   
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        out += "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds";
        return out;
    }
    
    private String processEliteOcrCSVFile(File in) throws IOException {
        
        StarSystem currentSystem = null;
        Station currentStation = null;
        String out = "";
        
        int systems = 0;
        int stations = 0;
        int commodities = 0;
        int exchanges = 0;
        
        Date start = new Date();
        boolean clearedStationExistingExchanges = false;
        
        try (BufferedReader br = new BufferedReader(new FileReader(in))) {
            String line;
            // line examples (header and example)
            // System;Station;Commodity;Sell;Buy;Demand;;Supply;;Date;
            // Chemaku;Kaku Orbital;Hydrogen Fuel;106;112;;;617885;Med;2015-01-08T01:58:48+00:00;    
            
            while ((line = br.readLine()) != null) {
                String[] splitLine = line.split(";");
                
                if (currentSystem == null || !currentSystem.getName().equals(splitLine[0].toUpperCase())) {
                    List<StarSystem> matchingSystems = starSystemService.searchStarSystemsByName(splitLine[0].toUpperCase(), true);
                    if (matchingSystems.size() == 0) {
                        // log an error?
                        continue;
                    } else if (matchingSystems.size() > 1) {
                        // log an error?
                    }
                    
                    currentSystem = matchingSystems.get(0);
                    Set<StarSystem> closeSystems = starSystemService.closeStarSystems(currentSystem, settings.getCloseSystemDistance());
                    closeSystems.add(currentSystem);

                    for (StarSystem system: closeSystems) {
                        systems++;
                        starSystemService.mergeSystem(system);
                        starSystemService.createLyEdgesForSystem(system);
                    }
                    
                }
                
                if (currentStation == null || !currentStation.getName().equals(splitLine[1].toUpperCase())) {
                    currentStation = new Station(splitLine[1].toUpperCase());
                    stations++;
                    stationService.createStationIfNotExists(currentSystem, currentStation);
                }
                
                Commodity currentCommodity = new Commodity(splitLine[2].toUpperCase());
                commodities++;
                stationService.createCommodityIfNotExists(currentCommodity);
                
                if (!clearedStationExistingExchanges) {
                    stationService.clearStationOfExchanges(currentStation);
                    clearedStationExistingExchanges = true;
                }
                
                int buyPrice = StringUtils.isEmpty(splitLine[3]) ? 0 : Integer.parseInt(splitLine[3]);
                int sellPrice = StringUtils.isEmpty(splitLine[4]) ? 0 : Integer.parseInt(splitLine[4]);
                int supply = StringUtils.isEmpty(splitLine[7]) ? 0 : Integer.parseInt(splitLine[7]);
                int demand = StringUtils.isEmpty(splitLine[5]) ? 0 : Integer.parseInt(splitLine[5]);
                long date = StringUtils.isEmpty(splitLine[9]) ? 0 : parseCSVDateFormat(splitLine[9]).getTime();
                
                exchanges++;
                stationService.createCommodityExchangeRelationship(currentStation, currentCommodity, buyPrice, sellPrice, supply, demand, date); 
            }
        }
        
        System.out.println("loaded " + in.getAbsolutePath());
        System.out.println("systems added or updated " + systems);
        System.out.println("stations added or updated " + stations);
        System.out.println("commodities added or updated " + commodities);
        System.out.println("exchanges added or updated " + exchanges);
        System.out.println("activity completed in " + ((new Date().getTime() - start.getTime())/1000.0) + " seconds" + OsUtils.LINE_SEPARATOR);
        
        return out;
    };
    
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private Date parseCSVDateFormat(String date) {
        //2015-01-03T19:10:25+00:00
        try {
            return FORMAT.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }
    
    
    
}
