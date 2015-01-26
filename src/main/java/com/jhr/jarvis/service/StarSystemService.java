package com.jhr.jarvis.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.BranchState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.jhr.jarvis.exceptions.SystemNotFoundException;
import com.jhr.jarvis.model.Settings;
import com.jhr.jarvis.model.StarSystem;

@Service
public class StarSystemService {

    /**
     * list of systems loaded from the csv file, used to add new systems being added from new stations.
     */
    private Set<StarSystem> starSystemData = null;
    
    @Autowired
    private GraphDbService graphDbService;
    
    @Autowired
    private Settings settings;
    
    /**
     * Populates when a user uses the station or find command. Used for --from in the path commands
     */
    private String userLastStoredSystem = null;

    private static final DynamicRelationshipType FRAMESHIFT_REL = DynamicRelationshipType.withName("FRAMESHIFT");
    
    public Set<StarSystem> closeStarSystems(final StarSystem s, final float distance) {
        
        if (starSystemData == null) {
           return new HashSet<StarSystem>();
        }
        
        Set<StarSystem> closeSystems = starSystemData.parallelStream().filter(s2->{ 
            return Math.sqrt( (Math.pow((s.getX()-s2.getX()),2.0)+Math.pow((s.getY()-s2.getY()),2.0)+Math.pow((s.getZ()-s2.getZ()),2.0))) <= distance;
        }).collect(Collectors.toSet());
        
        return closeSystems;        
    }
    
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
     * When adding a system found via a EliteOCR import, we grab the system's coordinates 
     * from the data/System.csv file and add systems that are close to that one, so that systems without stations are
     * more likely to appear in the graph.
     * 
     * @param systemName
     * @param exactMatch if false, use String.match(regex). If true use String.equals
     * @return
     * @throws IOException
     */
    public List<StarSystem> searchSystemFileForStarSystemsByName(String systemName, boolean exactMatch) throws IOException {
        
        if (starSystemData == null) {
            loadSystems(new File(settings.getSystemsFile()));
        }
        
        List<StarSystem> systems;
        if (exactMatch) {
            systems = starSystemData.parallelStream().filter(ss->{ return ss.getName().toUpperCase().equals(systemName); }).collect(Collectors.toList());
        } else {
            systems = starSystemData.parallelStream().filter(ss->{ return ss.getName().matches(systemName); }).collect(Collectors.toList());
        }
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
        Map<String, Object> cypherParams = ImmutableMap.of("name", system.getName(), "x", system.getX(), "y", system.getY(), "z", system.getZ(), "edgeMaxDistance", settings.getLongestDistanceEdge());
        /*
         * The FOREACH madness is a hacky IF this condition is not met, don't run the create FRAMESHIFT edge
         */
        String query = "MATCH (t:System), (f:System {name:{name}, pos_x:{x}, pos_y:{y}, pos_z:{z}}) " +
                       "WHERE t.name<>f.name AND NOT (t)-[:FRAMESHIFT]-(f) " +
                       "FOREACH (ignoreMe IN CASE WHEN sqrt(((f.pos_x-t.pos_x)^2 + (f.pos_y-t.pos_y)^2 + (f.pos_z-t.pos_z)^2)) <= {edgeMaxDistance} THEN [1] ELSE [] END | " +
                       "MERGE (f)-[:FRAMESHIFT { ly: sqrt(((f.pos_x-t.pos_x)^2 + (f.pos_y-t.pos_y)^2 + (f.pos_z-t.pos_z)^2))}]->(t));";
        String out = graphDbService.runCypherWithTransaction(query, cypherParams);
        return out;
    }
    
    /**
     * Calculates shortest distance between two systems, making no jump greater than the ship's jump distance.
     * 
     * @param fromSystem
     * @param toSystem
     * @param jumpDistance 
     * @return
     */
    public String calculateShortestPathBetweenSystems(String fromSystem, String toSystem, double jumpDistance) {
        
        try (Transaction tx = graphDbService.getGraphDb().beginTx();) {
        
            Map<String, Object> cypherParams = ImmutableMap.of("fromSystem", fromSystem, "toSystem", toSystem);
            ExecutionResult result = graphDbService.getEngine().execute("MATCH (fromSystem:System),(toSystem:System) WHERE fromSystem.name = {fromSystem} AND toSystem.name = {toSystem} RETURN fromSystem, toSystem", cypherParams);
            
            Node fromSystemNode = null;
            Node toSystemNode = null;
            for (Map<String, Object> map : result) {                
                fromSystemNode = (Node) map.get("fromSystem");
                toSystemNode = (Node) map.get("toSystem");
            }
          
          LyFilteringExpander expander = new LyFilteringExpander(jumpDistance);
          PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(expander, "ly");
          WeightedPath path = finder.findSinglePath(fromSystemNode, toSystemNode);
          
          String out = "";
          for (PropertyContainer obj: path) { 
              if (obj instanceof Node) {
                  out += "("+obj.getProperty("name")+")";
              } else if (obj instanceof Relationship) {
                  Relationship rel = (Relationship) obj;
                  
                  double ly = (double)Math.round((double)rel.getProperty("ly") * 100) / 100;
                  out += "--[" + rel.getType().name() + "{ly:" + ly + "}]-->";
              }
          }
          
          out += OsUtils.LINE_SEPARATOR + "Path weight: " + path.weight();
          return out;
        }
    }
    
    /**
     * When exploring a (system)-[FRAMESHIFT]-(system) relationship, only expand the relationships whee ly <= a value
     * 
     * @author jrosocha
     *
     */
    private class LyFilteringExpander implements PathExpander {
        
        private double lyFilter;
        
        public LyFilteringExpander(double ly) {
            lyFilter = ly;
        }
        
        @Override
        public Iterable<Relationship> expand(Path neoPath, BranchState state) {
            
            List<Relationship> out = new ArrayList<>();
            for (Relationship rel: neoPath.endNode().getRelationships(FRAMESHIFT_REL, Direction.BOTH)) {
                if ((double)rel.getProperty("ly") <= lyFilter) {
                    out.add(rel);
                }               
            }
            return out;
        }

        @Override
        public PathExpander reverse() {
            System.out.println("Reverse not implemented.");
            return null;
        }
    }
    
    /**
     * Runs an exact match and a partial match looking to identify a single system.
     * If a single system is found, it is loaded into memory
     * 
     * @param partial
     * @return
     * @throws Exception
     */
    public StarSystem findUniqueSystem(String partial) throws SystemNotFoundException {
        
        StarSystem foundSystem = null;
        boolean found = false;
        
        try {
            foundSystem = findExactSystem(partial);
            found = true;
        } catch (Exception e) {
            // not an exact match. proceed
        }
     
        if (!found) {
            String query = "MATCH (system:System)"
                            + " WHERE system.name=~{systemName}"
                            + " RETURN system.name";                
    
            Map<String, Object> cypherParams = ImmutableMap.of("systemName", partial.toUpperCase() + ".*");
            List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
            
            if (results.size() == 0 || results.size() > 1 ) {
                throw new SystemNotFoundException("Unique station could not be identified for '" + partial + "'.");
            }
            
            foundSystem = new StarSystem((String) results.get(0).get("system.name"));
        }

        return foundSystem;
    }
 
    /**
     * Matches if the system exists as typed
     * 
     * @param systemName
     * @return
     * @throws Exception 
     */
    public StarSystem findExactSystem(String systemName) throws SystemNotFoundException {
        String query = "MATCH (system:System)"
                + " WHERE system.name={systemName}"
                + " RETURN system.name";                

        if (systemName == null) {
            throw new SystemNotFoundException("Exact system '" + systemName + "' could not be identified");
        }
        
        StarSystem foundSystem = null;
        Map<String, Object> cypherParams = ImmutableMap.of("systemName", systemName.toUpperCase());
        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        if (results.size() == 0 || results.size() > 1 ) {
            throw new SystemNotFoundException("Exact system '" + systemName + "' could not be identified");
        }
        foundSystem = new StarSystem((String) results.get(0).get("system.name"));
        return foundSystem;
    }

     /**
     * Matches a system starting with @param partial 
     * If more than one system is found, returns \n separated string.
     * If none is found, returns a message to that effect.
     * 
     * @param partial
     * @return
     */
    public List<StarSystem> findSystem(String partial) {
        
        String query = "MATCH (system:System)"
                        + " WHERE system.name=~{systemName}"
                        + " RETURN system.name, system.pos_x, system.pos_y, system.pos_z";                

        List<StarSystem> out = new ArrayList<>();
        Map<String, Object> cypherParams = ImmutableMap.of("systemName", (partial != null ? partial.toUpperCase() : "") + ".*");
        
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, cypherParams);
        
        
        for (Map<String, Object> res: results) {
            out.add(new StarSystem((String) res.get("system.name"), (float) res.get("system.pos_x"), (float) res.get("system.pos_y"), (float) res.get("system.pos_z")));
        }
        
        return out;
    }
    
    
    public long systemCount() {
        
        long systemCount = 0;
        String query = "MATCH (system:System)"
        + " RETURN COUNT(system) AS `SYSTEM COUNT`";
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, new HashMap<>());
        systemCount = (long) results.get(0).get("SYSTEM COUNT");
        return systemCount;        
    }

    public long shiftCount() {
        
        long shiftCount = 0;
        String query = "MATCH ()-[fs:FRAMESHIFT]->()"
                + " RETURN COUNT(fs) AS `FRAMESHIFT COUNT`";
        List<Map<String, Object>> results = graphDbService.runCypherNative(query, new HashMap<>());
        shiftCount = (long) results.get(0).get("FRAMESHIFT COUNT");
        return shiftCount;
    }
    
    private Function<String, StarSystem> parseCSVLineToSystem = line -> {
        String[] splitLine = line.split(",");
        splitLine[0] = (splitLine[0].startsWith("'") || splitLine[0].startsWith("\"")) ? splitLine[0].substring(0, splitLine[0].length() - 1) : splitLine[0];
        StarSystem s =  new StarSystem( splitLine[0].toUpperCase(), Float.parseFloat(splitLine[1]),  Float.parseFloat(splitLine[2]),  Float.parseFloat(splitLine[3]));
        return s;
    };

    /**
     * @return the userLastStoredSystem
     */
    public String getUserLastStoredSystem() {
        return userLastStoredSystem;
    }

    /**
     * @param userLastStoredSystem the userLastStoredSystem to set
     */
    public void setUserLastStoredSystem(String userLastStoredSystem) {
        this.userLastStoredSystem = userLastStoredSystem;
    }
    
}
