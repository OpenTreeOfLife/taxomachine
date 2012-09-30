package opentree.tnrs;

import org.neo4j.graphdb.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import opentree.taxonomy.TaxonomyExplorer;

/**
 * @author Cody Hinchliff
 * 
 * A set of matches for a single query.  simple container to hold results;
 * essentially a wrapper for an ArrayList of TNRSMatch objects that represent
 * individual matches to the search string. Implements useful procedures like
 * finding the best match, specialized sorting (e.g. by type of result), etc.
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
    
    public int length() {
        return _matches.size();
    }

    public String getGraphFilename() {
        return _graphFilename;
    }
    
    public Iterator<TNRSMatch> iterator() {
        return _matches.iterator();
    }
        
    public void addMatch(TNRSHit m) {
        Match match = new Match(m);
        _matches.add(match);
    }
        
    /**
     * An abstract container type to hold results from TNRS searches.
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
        
        public String getSearchString() {
            return _searchString;
        }
        
        public long getMatchedNodeId() {
            return _matchedNode.getId();
        }

        public String getMatchedNodeName() {
            return _matchedNode.getProperty("name").toString();
        }
        
        public Node getMatchedNode() {
            return _matchedNode;
        }
        
        public String getSource() {
            return _sourceName;
        }
        
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