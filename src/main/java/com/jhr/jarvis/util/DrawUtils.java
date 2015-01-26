package com.jhr.jarvis.util;

import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

@Component
public class DrawUtils {

    public static final int MAX = 130;
    
    public String messageBox(int pad, String header, String ... messages) {

        int maxMessageLength = generateMaxLineLength(messages);
        if (header != null && header.length() > maxMessageLength) {
            maxMessageLength = header.length() < MAX ? header.length() : MAX;
        }
        
        String tableHead = generateTableHead(maxMessageLength, pad);
        String out = "";
        
        if (header != null) {
            out += tableHead;
            out += generateLine(maxMessageLength, pad, header);     
        }
        
        out += tableHead;
        for (String message: messages) {
            out += generateLine(maxMessageLength, pad, message);
        }
        out += tableHead;
        
        return out;
    }
    
    protected int generateMaxLineLength(String[] messages) {
        int maxMessageLength = 0;
        for(String msg : messages){
            if (msg.length() > maxMessageLength) {
                maxMessageLength = msg.length();
                if (maxMessageLength > MAX) {
                    maxMessageLength = MAX;
                    break;
                }
            }
         }
        return maxMessageLength;
    }
    
    protected String generateLine(int maxlineLength, int pad, String message) {
        String out = ""; 
        String padS = "";
        
        for (int i = 0; i < pad; i++) {
            padS += " ";
        }
        
        String messageFormatted = "";
        if (message.length() > maxlineLength) {
            messageFormatted += message.substring(0, maxlineLength);
        } else {
            messageFormatted += message;
            for (int i = 0; i < ((maxlineLength) - message.length()); i++) {
                messageFormatted += " ";
            }
        }
        
        out += "|" + padS + messageFormatted + padS + "|" + OsUtils.LINE_SEPARATOR;
        return out;
    }
    
    protected String generateTableHead(int lineLength, int pad) {
        String tableHead = "+";
        for (int i = 0; i < lineLength + (pad * 2); i++) {
            tableHead += "-";
        }
        tableHead += "+" + OsUtils.LINE_SEPARATOR;
        return tableHead;
    }
    
    
    
}
