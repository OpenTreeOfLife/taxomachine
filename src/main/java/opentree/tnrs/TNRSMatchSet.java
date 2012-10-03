package opentree.tnrs;

import org.neo4j.graphdb.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author Cody Hinchliff
 * 
 * A set of matches resulting from a single TNRS query (which may have contained many names). These are simple containers to hold results, which
 * they provide access to in the form of TNRSMatch objects representing individual hits for a given search string to some recognized name or synonym.
 *
 */
public class TNRSMatchSet implements Iterable<TNRSMatch> {
    
    private List<TNRSMatch> _matches;
    private String _graphFilename;

    // TODO: add sort features

    public TNRSMatchSet(String graphFilename) {
        _matches = new ArrayList<TNRSMatch>();
        _graphFilename = graphFilename;
    }
    
    /**
     * @return the number of matches in the set
     */
    public int length() {
        return _matches.size();
    }

    /**
     * @return the name of the queried graph
     */
    public String getGraphFilename() {
        return _graphFilename;
    }

    /**
     * @return an iterator of TNRSMatch objects containing all the matches in this set
     */
    public Iterator<TNRSMatch> iterator() {
        return _matches.iterator();
    }

    /**
     * Adds a match to the set, using the data within by the passed TNRSHit object.
     * @param TNRSHit to be added
     */
    public void addMatch(TNRSHit m) {
        Match match = new Match(m);
        _matches.add(match);
    }
        
    /**
     * An internal container compatible with the TNRSMatch specification.
     */
    private class Match extends TNRSMatch {

        // for all matches, information associating this match with a known node in the graph
        private String _searchString = "";             // the original search text queried
        private Node _matchedNode;                     // the recognized taxon node we matched
        private Node _synonymNode;                      // the synonym node we matched
        private String _sourceName = "";               // the name of the source where the match was found
        private boolean _isExactNode;                  // whether this is an exact match to known, recognized taxon
        private boolean _isApprox;                     // whether this is a fuzzy match (presumably misspellings)
        private boolean _isSynonym;                    // whether the match points to a known synonym (not necessarily in the graph, nor necessarily pointing to a node in the graph)
        private boolean _isHomonym;                     // whether the match points to a known homonym
        private double _score;                         // the score of this match
        private HashMap<String,String> _otherData;     // other data provided by the match source
        
        public Match(TNRSHit m) {
            _matchedNode = m.getMatchedNode();
            _synonymNode = m.getSynonymNode();
            _isExactNode = m.getIsExactNode();
            _isApprox = m.getIsApprox();
            _isSynonym = m.getIsSynonym();
            _isHomonym = m.getIsHomonym();
            _sourceName = m.getSourceName();
            _searchString = m.getSearchString();
            _score = m.getScore();
            _otherData = (HashMap<String,String>)m.getOtherData();
        }
        
        /**
         * @return the original search string which produced this match
         */
        public String getSearchString() {
            return _searchString;
        }
        
        /**
         * @return the graph database id of node to which this match points
         */
        public long getMatchedNodeId() {
            return _matchedNode.getId();
        }

        /**
         * @return the name of the graph node to which this match points
         */
        public String getMatchedNodeName() {
            return _matchedNode.getProperty("name").toString();
        }
        
        /**
         * @return the Neo4j Node object for the recognized name to which this graph points
         */
        public Node getMatchedNode() {
            return _matchedNode;
        }
        
        /**
         * @return the TNRS source that produced this match
         */
        public String getSource() {
            return _sourceName;
        }
        
        /**
         * @return a textual description of the type of match this is
         */
        public String getMatchType() {
            String desc = "";
            
            if (_isExactNode) {
                desc += "exact node match";
            } else {
                if (_isApprox)
                    desc += "approximate match to ";
                else
                    desc += "exact match to ";

                if (_isSynonym)
                    desc += "junior synonym";
                else
                    desc += "non-synonym";
            }

            if (_isHomonym)
                desc += "; also a homonym";

            return desc;
        }
        
        @Override
        public String toString() {
            return "Query '" + _searchString + "' matched by " + _sourceName + " to " + _matchedNode.getProperty("name") + " (id=" +
                    _matchedNode.getId() + ") in " + TNRSMatchSet.this.getGraphFilename() + " (" + getMatchType() + ")";
        }
    }
}