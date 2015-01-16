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
	
    @CliCommand(value = "ocr", help = "usage: ocr --scan \n Scans your Elite OCR directory for more CSV files.")
    public String ocrCommands() throws IOException {
    
        String out = "";
        System.out.println("Is archive enabled: " + settings.isEliteOcrScanArchiveEnabed());
        out += eliteOcrService.scanDirectory(settings.isEliteOcrScanArchiveEnabed());
        return out;
    }
	
}
