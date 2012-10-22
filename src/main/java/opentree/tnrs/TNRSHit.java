package opentree.tnrs;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;

/**
 * TNRSHit objects are mutable precursors to TNRSMatch objects. They are intended to be used as organizational containers, which temporarily hold
 * information about matches, and they can be passed to the TNRSMatchSet method addMatch() in order to an actual TNRSMatch object to the set. They
 * should not be used for other purposes.
 * 
 * @author Cody Hinchliff
 *
 */
public class TNRSHit {

    Node _matchedNode;
    Node _synonymNode;
    String _searchString;
    String _sourceName;
    boolean _isHomonym;
    boolean _isExactNode;
    boolean _isApprox;
    boolean _isSynonym;
    HashMap<String,String> _otherData;
    double _score;

    public TNRSHit() {
        _matchedNode = null;
        _synonymNode = null;
        _searchString = "";
        _sourceName = "";
        _isHomonym = false;
        _isExactNode = false;
        _isApprox = false;
        _isSynonym = false;
        _otherData = null;
        _score = -1;
    }
    
    public TNRSHit setMatchedNode(Node matchedNode) {
        _matchedNode = matchedNode;
        return this;
    }

    public TNRSHit setSynonymNode(Node synonymNode) {
        _matchedNode = synonymNode;
        return this;
    }

    public TNRSHit setSearchString(String searchString) {
        _searchString = searchString;
        return this;
    }

    public TNRSHit setSourceName(String sourceName) {
        _sourceName = sourceName;
        return this;
    }

    public TNRSHit setIsHomonym(boolean isHomonym) {
        _isHomonym = isHomonym;
        return this;
    }

    public TNRSHit setIsExactNode(boolean isExactNode) {
        _isExactNode = isExactNode;
        return this;
    }

    public TNRSHit setIsApprox(boolean isApprox) {
        _isApprox = isApprox;
        return this;
    }

    public TNRSHit setIsSynonym(boolean isSynonym) {
        _isSynonym = isSynonym;
        return this;
    }

    public TNRSHit setOtherData(Map<String,String> otherData) {
        _otherData = (HashMap<String, String>)otherData;
        return this;
    }

    public TNRSHit setScore(double score) {
        _score = score;
        return this;
    }
    
    public Node getMatchedNode() {
        return _matchedNode;
    }    
    
    public String getSearchString() {
        return _searchString;
    }
    
    public boolean getIsHomonym() {
        return _isHomonym;
    }
        
    public Node getSynonymNode() {
        // the matched synonym node (if any)
        return _synonymNode;
    }

    public String getSourceName() {
        // the name of the source where the match was found
        return _sourceName;
    }

    public boolean getIsExactNode() {
        // is this an exact direct match to a graph node?
        return _isExactNode;
    }

    public boolean getIsApprox() {
        // whether the match is a fuzzy match (presumably misspelling)
        return _isApprox;
    }

    public boolean getIsSynonym() {
        // indicates that this match points to a known synonym
        return _isSynonym;
    }

    public double getScore() {
        // the score of this match
        return _score;
    }

    public Map<String, String> getOtherData() {
        // any other data
        return _otherData;
    }

}
