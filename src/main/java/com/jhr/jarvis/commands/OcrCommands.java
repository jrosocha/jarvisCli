package com.jhr.jarvis.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.service.EliteOcrService;

@Component
public class OcrCommands implements CommandMarker {
    
    @Autowired
    private Settings settings;
    
    @Autowired
    private EliteOcrService eliteOcrService;
	
    @CliCommand(value = "ocr", help = "Functions related to Elite OCR.")
    public String ocrCommands(
        @CliOption(key = { "scan" }, mandatory = false, specifiedDefaultValue = "true", help = "Recans the Elite OCR dir for files") final String scan,
        @CliOption(key = { "last" }, mandatory = false, specifiedDefaultValue = "true", help = "Provides the last scanned timestamp") final String timestamp
        ) throws IOException {
    
        String out = "";
        
        if (!StringUtils.isEmpty(scan)) {
            out += eliteOcrService.scanDirectory();
        }
        
        if (!StringUtils.isEmpty(timestamp)) {
            out += eliteOcrService.getLastScanned();
        }
        
        if (out.isEmpty()) {
            out += "Scans a directory for Elite OCR export csv, and then moves them to an archive dir." + OsUtils.LINE_SEPARATOR + "usage: ocr [--scan|--last]";
        }
        
        return out;
    }
	
}
