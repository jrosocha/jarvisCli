# jarvis
Elite Dangerous Neo4j/Elite OCR Trading PoweredCLI

This is a learning exercise for Neo4j and spring-shell in Java. If you like these kinds of things, feel free to pull and contribute. If you are a Neo4j expert, man I need some advice, because this this is awful slow around the [:FRAMESHIFT] edge.

Anyways the DB looks like this:
```
(:System)-[:FRAMESHIFY{ly:float}]-(:System)-[](:Station)-[:EXCHANGE{buyPrice:int, sellPrice:int, supply:int}]->(:Commodity)
```

* Install Elite:Dangerous .. or all of this will really be boring
* Install EliteOCR and learn how to use it. This CLI uses the csv exports. Mind what directory they get written to.

* Install Java 8. Its is the language this app uses.

* clone the git project from https://github.com/jrosocha/jarvis.git, you can just download the zip from github.
* cd to the cloned directory
* run gradlew or gradlew.bat (*nix or windows)

* cd build/install/jarvis/data
* edit jarvis-config.json. Change the line that reads like below to point to where yourElite OCR created .csv files go.
```
"eliteOcrScanDirectory" : "/Users/jrosocha/trade/Elite Dangerous",
```

* cd ../bin (or build/install/jarvis/bin for those of you who are lost)

* run jarvis (or jarvis.bat)

Typing tab will provide some help.
Tying help <command> will help more.

A good list of things to do:

```
ship 44;10;1000000 
```
Sets your ship. This persists between restarts/etc. The commands is ship your-cargo-rooom;your-jump-distance;cash-on-hand
 
```
ocr --scan  
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
go
```
This will compute a 1 jump trade from your current station with your current ship

```
go2
```
Computes a 2 stop within 1 jump of each other trade, starting with your current station and ship

```
gon --jumps n 
```
Computes a one one stop trade within n jumps of your starting system. This takes a bit of time. On my system with --jumps 3, ~40 seconds.

```
go2n --jumps 2 
```
Computes a 2 stop trade within 2 jumps of each stop .. 3 or more makes this unusably slow, around 20 minutes. 2 should return in ~60 seconds)
