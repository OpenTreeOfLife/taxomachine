package opentree.tnrs.adaptersupport.iplant;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;

import opentree.tnrs.TNRSHit;

public class iPlantHit {
    public String acceptedName;
    public String sourceId;
    public String score;
    public String matchedName;
    public Annotations annotations;
    public String uri;
    
    public Map<String, String> getData() {
        HashMap<String,String> data = new HashMap<String,String>();

        data.put("acceptedName", acceptedName);
        data.put("sourceId", sourceId);
        data.put("score", score);
        data.put("matchedName", matchedName);
        data.put("uri", uri);
        data.put("annotations", annotations.Authority);
                
        return data;
    }
}
