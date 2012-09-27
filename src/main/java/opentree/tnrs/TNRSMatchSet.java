package opentree.tnrs;

import org.neo4j.graphdb.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
public class TNRSMatchSet implements Iterable<opentree.tnrs.TNRSMatchSet.Match> {
    
//    public static MatchDirect DIRECT_MATCH = getDirectMatch();

    public static final String LOCAL_SOURCE = "ottol";
    private ArrayList<Match> _results;
    private String _graphFilename;

    //    private TaxonomyExplorer taxonomy;
    
/*    public TNRSMatchSet(String _searchString, String _graphFileName) {
        searchString = _searchString;
        graphFileName = _graphFileName;
        results = new ArrayList<Match>();
        taxonomy = new TaxonomyExplorer(graphFileName);
    }
    
    public Match getBestMatch() {
        // return our guess for the 'correct' match; this functionality
        // could also be implemented as using Comparable or Comparator
        // interfaces to sort by criteria
        
        // for now just returns an empty Result
        Node empty = new Node();
        Match bestMatch = new MatchDirect(empty);
        return bestMatch;
    }
    */

    public TNRSMatchSet(String graphFilename) {
        _results = new ArrayList<Match>();
        _graphFilename = graphFilename;
    }
    
    public int length() {
        return _results.size();
    }

    public String getGraphFilename() {
        return _graphFilename;
    }
    
    public Iterator<Match> iterator() {
        return new MatchIterator();
    }

    private class MatchIterator implements Iterator<Match> {

        int i = 0;
        public boolean hasNext() {
            return i < _results.size() - 1;
        }

        public Match next() {
            return _results.get(i++);
        }

        public void remove() {
            throw new java.lang.UnsupportedOperationException("The remove method is not supported");
        }
        
    }
    
    public void addDirectMatch(String searchString, Node matchedNode) {
        MatchDirect match = new MatchDirect(searchString, matchedNode);
        _results.add(match);
    }
    
    /**
     * An abstract container type to hold results from TNRS searches. Inherited by specific
     * container types TNRSMatchDirect, TNRSMatchSynonym, and TNRSMatchUncertain.
     * 
     * There may be multiple matches for the same search string that map to different nodes
     * in the graph, so each match object is associated with a graph node.
     */
    public abstract class Match {

        // for all matches, information associating this match with a known node in the graph
        private String _searchString;           // the original search text queried
        private Node _matchedNode;               // the id of the matched node in the graph
        private String _sourceName;              // the name of the source where the match was found
        protected boolean _isSynonymMatch;       // indicates direct matches to known synonyms
        protected boolean _isDirectMatch;        // indicates direct matches to known, recognized taxa
        protected boolean _isUncertainMatch;     // indicates matches that are not direct, i.e. fuzzy matches (presumably misspellings)
        private HashMap<String,String> _responseData; // other data provided by the match source

        public Match(String searchString, Node _node, String _sourceName) {
            _searchString = searchString;
            _matchedNode = _node;
            _sourceName = _sourceName;

            // using Neo4j Node objects for now to represent the
            // matched nodes in the graph. it may be necessary
            // to create taxon objects that extend the node
            // objects though.
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

        public String getSource() {
            return _sourceName;
        }
        
        public String getMatchType() {
            if (_isDirectMatch)
                return "direct";
            else if (_isSynonymMatch)
                return "synonym";
            else if (_isUncertainMatch)
                return "uncertain";
            else
                throw new java.lang.UnsupportedOperationException("The match " + toString() + " is of unknown type. This is an error. Quitting.");
        }
        
        public String toString() {
            return _searchString + " matched to id " + _matchedNode.getId() + " in " + TNRSMatchSet.this.getGraphFilename() + " (" + getMatchType() + ")";
        }
    }

    /**
     *  A direct match to a recognized taxon in the graph. This is the trivial case.
     */
    private class MatchDirect extends Match {
        // a direct match to a node in the graph. no user interaction required for these
        public MatchDirect(String searchString, Node matchedNode) {
            super(searchString, matchedNode, LOCAL_SOURCE);
            _isDirectMatch = true;
            _isSynonymMatch = false;
            _isUncertainMatch = false;
        }
    }

    /**
     * A direct match to a known synonym. The match may occur within the taxonomy graph if
     * we have a relationship for the synonymy, or may occur via an external service that
     * directly matches the search query to a known synonym for which we have no record in
     * the graph.
     */
    private class MatchSynonym extends Match {
        private int synonymTaxNodeId;       // the id of the matched synonym node in the taxonomy graph
        private String synonymTaxName;      // the name of the matched node in the taxonomy

        public MatchSynonym(String searchString, Node matchedNode, String sourceName) {
            super(searchString, matchedNode, sourceName);
            _isDirectMatch = false;
            _isSynonymMatch = true;
            _isUncertainMatch = false;
        }
    }
    
    /**
     * A match that does not represent a direct string match to any recognized taxon or
     * known synonym. This is likely to happen due to misspellings.
     */
    private class MatchUncertain extends Match {

        public MatchUncertain(String searchString, Node matchedNode, String sourceName) {
            super(searchString, matchedNode, sourceName);
            _isDirectMatch = false;
            _isSynonymMatch = false;
            _isUncertainMatch = true;
        }        
    }
}