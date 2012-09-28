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
        return _results.iterator();
    }
    
    public void addDirectMatch(String searchString, Node matchedNode) {
        MatchDirect match = new MatchDirect(searchString, matchedNode);
//        System.out.println("added a match");
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
        private String _searchString;               // the original search text queried
        private Node _matchedNode;                  // the id of the matched node in the graph
        private String _sourceName;                 // the name of the source where the match was found
        protected boolean _isExactNode;             // indicates exact name matches to known, recognized taxa
        protected boolean _isFuzzy;                 // indicates matches that are not direct, i.e. fuzzy matches (presumably misspellings)
        protected boolean _isSynonym = false;       // indicates that this match is to a known synonym (not necessarily in the graph, nor necessarily pointing to a node in the graph)
        protected boolean _isInGraph;               // for matches to nodes that exist in the graph
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
            String desc = "";
            
            if (_isExactNode) {
                desc += "exact node match";
            } else {
                if (_isFuzzy)
                    desc += "approximate match to ";
                else
                    desc += "exact match to ";

                if (_isSynonym)
                    desc += "junior synonym ";
                else
                    desc += "non-synonym ";
                
                if (_isInGraph)
                    desc += "in graph";
                else
                    desc += "not in graph";
            }
            return desc;
        }
        
        public String toString() {
            return "Query '" + _searchString + "' matched to " + _matchedNode.getProperty("name") + " (id=" +
                    _matchedNode.getId() + ") in " + TNRSMatchSet.this.getGraphFilename() + " (" + getMatchType() + ")";
        }
    }

    /**
     *  A direct match to a recognized taxon in the graph. This is the trivial case.
     */
    private class MatchDirect extends Match {
        // a direct match to a node in the graph. no user interaction required for these
        public MatchDirect(String searchString, Node matchedNode) {
            super(searchString, matchedNode, LOCAL_SOURCE);
            _isExactNode = true;
            _isFuzzy = false;
            _isSynonym = false;
            _isInGraph = true;
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
            _isExactNode = false;
            _isSynonym = false;
       }
    }
}