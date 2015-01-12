# jarvis
Elite Dangerous Neo4j/Elite OCR Trading PoweredCLI

* Install Elite:Dangerous .. or all of this will really be boring
* Install EliteOCR and learn how to use it. This CLI uses the csv exports. Mind what directory they get written to.

* Install Gradle. It is the build tool this app uses.
* Install Java 8. Its is the language this app uses.

* clone the git project from https://github.com/jrosocha/jarvis.git
* cd to the cloned directory
* run gradle

* cd build/install/jarvis/data
* edit jarvis.properties. Change the line that reads like below to point to where yourElite OCR created .csv files go.

    # a folder that will contain all of the csv files created by elite ocr
    eliteocr.directory.path=/Users/jrosocha/trade/Elite Dangerous

* cd ../bin (or build/install/jarvis/bin for those of you who are lost)

* run jarvis

Jarvis will scan your OCR directory on a 30 second interval. 
You can force Jarvis to scan by typing 'ocr --scan' 
Jarvis will move the scanned csv files to a folder in your OCR dir named archive.

Typing tab will provide some help.
