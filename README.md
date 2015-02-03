# jarvis
Elite Dangerous OrientDb OCR Trading PoweredCLI

This is a learning exercise for OrientDb and spring-shell in Java. If you like these kinds of things, feel free to pull and contribute. If you are a OrientDb expert, man I need some advice, because this this is awful slow around the [:FRAMESHIFT] edge once you get past 2 jumps in a go command.

Anyways the DB looks like this:
```
(System)-[:Frameshift{ly:float}]-(System)-[](Station)-[Exchange{buyPrice:int, sellPrice:int, supply:int, demand:int, timestamp:long}]->(Commodity)
```

## Installation
* Install Elite:Dangerous .. or all of this will really be boring
* Install EliteOCR and learn how to use it. This CLI uses the csv exports. Mind what directory they get written to.

* Install Java 8 (JDK to do the compile). Its is the language this app uses.

* clone the git project from https://github.com/jrosocha/jarvis.git, you can just download the zip from github.
* cd to the cloned directory
* run gradlew or gradlew.bat (*nix or windows)

* cd build/install/jarvis/data
* edit jarvis-config.json. Change the line that reads like below to point to where yourElite OCR created .csv files go.
```
"eliteOcrScanDirectory" : "/Users/jrosocha/trade/Elite Dangerous",
```

(At this point you can copy the build/install/jarvis folder somewhere else. It contains all the artifacts you require to run this app.

* cd ../bin (or build/install/jarvis/bin for those of you who are lost)

* run jarvis (or jarvis.bat)

Typing tab will provide some help.
Tying help <command> will help more.


## Commands
A good list of things to do:

```
ship 44;10;1000000 
```
Sets your ship. This persists between restarts/etc. The commands is ship your-cargo-rooom;your-jump-distance;cash-on-hand
 
```
ocr  
```
Scans a configured directory for .csv files in that Elite OCR format, and pushes them to an archive folder if that option is enabled. jarvis-config.json hass all of those goodies.

```
find goo
```
Finds a station or stations based on user input. If you pin it down to one station, that gets set to memory and you don't need to type --start 'GOOCH TERMINAL' for the other trade commands.

```
st goo
```
Just like the find command, station (or st) also prints the table of commodity data available.

```
trade
```
This will compute a 1 jump trade from your current station with your current ship

```
trade --trades 2
```
Computes a 2 stop within 1 jump of each other trade, starting with your current station and ship. This ISNT a safe parameter. --trades 2 will take only 1-2 seconds, but --trades 3 will take like 300 seconds with --jumps 2 and a ship that can go 12 ly. Its a big universe.

```
trade --jumps n  
```
Computes a one one stop trade within n jumps of your starting system. This parameter is safe. You can try --jumps 20 if your --trades are 1

```
system aia
```
Finds all systems that start with aia

```
path --from aiabiko --to altais
```
Finds the shortest path between 2 systems given your ship's jump range.