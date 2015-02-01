package com.jhr.jarvis.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
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
    public String ocrToOrientDb() throws IOException {
    
        String out = "";
        System.out.println("Is archive enabled: " + settings.isEliteOcrScanArchiveEnabed());
        out += eliteOcrService.scanDirectoryForOrientDb(settings.isEliteOcrScanArchiveEnabed());
        return out;
    }
    
}
