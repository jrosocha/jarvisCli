package com.jhr.jarvis.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.StarSystem;

@Service
public class StarSystemService {

    private Set<StarSystem> starSystemData = null;
    
    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
 
    /**
     * Loads the Systems.csv file to memory for use in identifying x,y,z coords for use when adding systems to the graph.
     * 
     * @param systemsCsvFile
     * @throws IOException
     */
    public synchronized void loadSystems(File systemsCsvFile) throws IOException {
        Set<StarSystem> c = Files.lines(systemsCsvFile.toPath()).parallel().map(parseCSVLineToSystem).collect(Collectors.toSet());
        starSystemData = c;
    }
    
    /**
     * searches for systems with a given regex.
     * 
     * @param regex
     * @return
     * @throws IOException
     */
    public List<StarSystem> searchStarSystemsByName(String regex) throws IOException {
        
        if (starSystemData == null) {
            loadSystems(new File(settings.getSystemsFile()));
        }
        
        List<StarSystem> systems = starSystemData.parallelStream().filter(ss->{ return ss.getName().matches(regex); }).collect(Collectors.toList());
        return systems;
    }
    
    /**
     * Adds the system to the graph
     * 
     * @param system
     * @return
     */
    public String mergeSystem(StarSystem system) {
        Map<String, Object> cypherParams = ImmutableMap.of("name", system.getName(), "x", system.getX(), "y", system.getY(), "z", system.getZ());
        String out = graphDbService.runCypherWithTransaction("MERGE (system:System {name:{name}, pos_x:{x}, pos_y:{y}, pos_z:{z}});", cypherParams);
        return out;
    }
    
    /**
     * Creates 'FRAMESHIFT' edges between this system and all other systems if they do not exist. Calculates distances.
     * 
     * @param system
     * @return
     */
    public String createLyEdgesForSystem(StarSystem system) {
        Map<String, Object> cypherParams = ImmutableMap.of("name", system.getName(), "x", system.getX(), "y", system.getY(), "z", system.getZ());
        String query = "MATCH (t:System), (f:System {name:{name}, pos_x:{x}, pos_y:{y}, pos_z:{z}}) " +
                       "WHERE t.name<>f.name AND NOT (t)-[:FRAMESHIFT]-(f) " +
                       "MERGE (f)-[:FRAMESHIFT { ly: sqrt(((f.pos_x-t.pos_x)^2 + (f.pos_y-t.pos_y)^2 + (f.pos_z-t.pos_z)^2))}]->(t);";
        String out = graphDbService.runCypherWithTransaction(query, cypherParams);
        return out;
    }
    
    private Function<String, StarSystem> parseCSVLineToSystem = line -> {
        String[] splitLine = line.split(",");
        splitLine[0] = (splitLine[0].startsWith("'") || splitLine[0].startsWith("\"")) ? splitLine[0].substring(0, splitLine[0].length() - 1) : splitLine[0];
        StarSystem s =  new StarSystem( splitLine[0].toUpperCase(), Float.parseFloat(splitLine[1]),  Float.parseFloat(splitLine[2]),  Float.parseFloat(splitLine[3]));
        return s;
    };
    
}
