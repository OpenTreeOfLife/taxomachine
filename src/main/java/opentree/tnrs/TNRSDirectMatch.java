package opentree.tnrs;

import java.util.HashMap;

import org.neo4j.graphdb.Node;

public class TNRSDirectMatch implements TNRSMatchParser {

    Node _matchedNode;
    String _searchString;
    
    public TNRSDirectMatch(String searchString, Node matchedNode) {
        _searchString = searchString;
        _matchedNode = matchedNode;
    }
    
    public Node getMatchedNode() {
        return _matchedNode;
    }

    public Node getSynonymNode() {
        return null;
    }
    
    public String getSearchString() {
        return _searchString;
    }
    
    public String getSourceName() {
        return "ottol";
    }

    public boolean getIsExactNode() {
        return true;
    }    
    
    public boolean getIsApprox() {
        return false;
    }
    
    public boolean getIsSynonym() {
        return false;
    }
    
    public double getScore() {
        return 1;
    }
    
    public HashMap<String, String> getOtherData() {
        return null;
    }
}
