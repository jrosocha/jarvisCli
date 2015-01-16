# jarvis
Elite Dangerous Neo4j/Elite OCR Trading PoweredCLI

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
ship 44;10;1000000  (Really ship your-cargo-rooom;your-jump-distance;cash-on-hand)

ocr --scan ( scans a configured directory for .csv files in that Elite OCR format, and pushes them to an archive folder) 

find goo ( or find the first few letters of any station you know. pin it down to one station)

go (this will compute a 1 jump trade)

go2 (computes a 2 stop within 1 jump of each other trade)

gon --jumps 2 (computes a one one stop trade within 2 jumps of your starting system)

go2n --jumps 2 (computes a 2 stop trade within 2 jumps of each stop .. 3 or more makes this unusably slow, around 20 minutes. 2 should return in 20-30 seconds)
```
