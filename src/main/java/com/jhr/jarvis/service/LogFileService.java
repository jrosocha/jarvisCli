package com.jhr.jarvis.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.jhr.jarvis.exceptions.SettingNotFoundException;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.StarSystem;
import com.jhr.jarvis.model.Station;
import com.jhr.jarvis.table.TableRenderer;
import com.jhr.jarvis.util.DrawUtils;

@Service
public class LogFileService {

    private File activeNetLogFile = null;
    
    private Thread netLogTailerThread = null;
    
    private String lastFoundSystemInNetLog = null;
    
    @Autowired 
    private Settings settings;
    
    @Autowired 
    private StarSystemService starSystemService;
    
    @Autowired
    private DrawUtils drawUtils;
    
    @Autowired
    private StationService stationService;
    
    @Scheduled(fixedRate = 15000)
    private void scheduledCheckForNewNetlogFile() {
        
        try {
            File latestNetlogFile = getLatestNetLogFile();
            if (activeNetLogFile == null || !(activeNetLogFile.getName().equals(latestNetlogFile.getName()))) {
                activeNetLogFile = latestNetlogFile;
                initNetlogTailer();
            }
        } catch (FileNotFoundException | SettingNotFoundException e) {
            e.printStackTrace();
        }
        
    }
    
    private void initNetlogTailer() {
        
        if (netLogTailerThread != null) {
            netLogTailerThread.interrupt();
            netLogTailerThread = null;
        }
        
        if (activeNetLogFile != null) {
            TailerListener listener = new NetLogTailerListener();
            Tailer tailer = new Tailer(activeNetLogFile, listener, 2000);
            this.netLogTailerThread = new Thread(tailer);
            netLogTailerThread.setDaemon(true);
            netLogTailerThread.start();
        }
    }
    
    private File getLatestNetLogFile() throws SettingNotFoundException, FileNotFoundException{
        
        File eliteDangerousAppDirectory = new File(settings.getEliteDangerousAppDirectory());
        
        if (!(eliteDangerousAppDirectory.exists() && eliteDangerousAppDirectory.isDirectory())) {
            throw new SettingNotFoundException("eliteDangerousAppDirectory is not correctly set in jarvis-config.json");
        }
        
        File logDir = new File(eliteDangerousAppDirectory, "Logs");
        if (!(logDir.exists() && logDir.isDirectory())) {
            throw new FileNotFoundException("logDir could not be found relative to " + eliteDangerousAppDirectory.getAbsolutePath());
        }
        
        List<String> logFiles = new ArrayList<>(Arrays.asList(logDir.list((File dirToFilter, String filename) -> filename.startsWith("netLog"))));
      
        if (logFiles.size() == 0) {
            throw new FileNotFoundException("No logs found in " + logDir.getAbsolutePath());
        }
        
        long lastMod = Long.MIN_VALUE;
        File newestFile = null;
        for (String fileName: logFiles) {
            File currentFile = new File(logDir, fileName);
            if (currentFile.lastModified() > lastMod) {
                newestFile = currentFile;
                lastMod = currentFile.lastModified();
            }
        }

        return newestFile;
    }
    
    public class NetLogTailerListener extends TailerListenerAdapter {
        public void handle(String line) {
            // look for a line containing: System:26(Hyroks)    
            Pattern pattern = Pattern.compile("System:\\d*\\(([A-Za-z\\s']*)\\)");
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String foundSystem =  matcher.group(1);
                if (lastFoundSystemInNetLog == null || !(lastFoundSystemInNetLog.equalsIgnoreCase(foundSystem))){
                    lastFoundSystemInNetLog = foundSystem;
                    
                    StarSystem starSystem = null;
                    try {
                        List<StarSystem> found = starSystemService.searchSystemFileForStarSystemsByName(lastFoundSystemInNetLog.toUpperCase(), true);
                        if (found.size() > 0) {
                            starSystem = found.get(0);
                            String out = OsUtils.LINE_SEPARATOR + drawUtils.messageBox(2, null, "Welcome to " + starSystem.getName());
                            Date start = new Date();
                            List<Station> stations = stationService.getStationsForSystemOrientDb(starSystem.getName());
                            List<Map<String, Object>> tableData = stations.stream().map(station->{
                                Map<String, Object> tableRow = new HashMap<>();
                                tableRow.put("STATION", station.getName());
                                tableRow.put("DAYS OLD", (new Date().getTime() - station.getDate())/1000/60/60/24 );
                                return tableRow;
                            }).collect(Collectors.toList());

                            out += OsUtils.LINE_SEPARATOR;
                            out += "SYSTEM: " + starSystem.getName() + " @ " + starSystem.getX() + ", " + starSystem.getY() + ", " + starSystem.getZ() + OsUtils.LINE_SEPARATOR ; 
                            out += OsUtils.LINE_SEPARATOR + TableRenderer.renderMapDataAsTable(tableData, ImmutableList.of("STATION", "DAYS OLD"));
                            out += OsUtils.LINE_SEPARATOR + "executed in " + (new Date().getTime() - start.getTime())/1000.0 + " seconds.";
                            System.out.println(out);
                            
                        } else {
                            String out = OsUtils.LINE_SEPARATOR + drawUtils.messageBox(2, "Sir. An error has occured.", "Sir, I could not identify '" + foundSystem + "' in your Systems.csv. This will result in lost data.");
                            System.out.println(out);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }
            }
        }
    }
}
