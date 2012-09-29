package opentree.tnrs.adaptersupport.iplant;

import java.util.HashMap;

import org.neo4j.graphdb.Node;

import opentree.tnrs.TNRSMatchParser;

public class iPlantMatch implements TNRSMatchParser {
    public String acceptedName;
    public String sourceId;
    public String score;
    public String matchedName;
    public Annotations annotations;
    public String uri;

    public Node matchedNode;
    public String searchString;
    public boolean isSynonym = false;
    
    public Node getMatchedNode() {
        return matchedNode;
    }
    public Node getSynonymNode() {
        return null;
    }
    public String getSearchString() {
        return searchString;
    }
    public String getSourceName() {
        return "iplant";
    }
    public boolean getIsExactNode() {
        return false;
    }
    public boolean getIsApprox() {
        return true; // TODO: is this always true?
    }
    public boolean getIsSynonym() {
        return isSynonym; // need to check for these in graph
    }
    public double getScore() {
        return Double.parseDouble(score);
    }
    public HashMap<String, String> getOtherData() {
        return null; // TODO: fill these in...
    }
}
