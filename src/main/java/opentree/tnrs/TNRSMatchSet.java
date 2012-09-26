package opentree.tnrs;

import org.neo4j.graphdb.Node;
import java.util.ArrayList;
import java.util.HashMap;

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
public class TNRSMatchSet {
    
//    public static MatchDirect DIRECT_MATCH = getDirectMatch();

    public static final String LOCAL_SOURCE = "ottol";
    
    private ArrayList<Match> results;
    private String searchString;
    private String graphFileName;
    private TaxonomyExplorer taxonomy;
    
    public TNRSMatchSet(String _searchString, String _graphFileName) {
        searchString = _searchString;
        graphFileName = _graphFileName;
        results = new ArrayList<Match>();
        taxonomy = new TaxonomyExplorer(graphFileName);
    }
    
/*    public Match getBestMatch() {
        // return our guess for the 'correct' match; this functionality
        // could also be implemented as using Comparable or Comparator
        // interfaces to sort by criteria
        
        // for now just returns an empty Result
        Node empty = new Node();
        Match bestMatch = new MatchDirect(empty);
        return bestMatch;
    }
    */
        
    public int length() {
        return results.size();
    }
    
    public String getGraphFileName() {
        return graphFileName;
    }
    
    public void addDirectMatch(Node matchedNode) {
        MatchDirect match = new MatchDirect(matchedNode);
        results.add(match);
    }
    
    /**
     * An abstract container type to hold results from TNRS searches. Inherited by specific
     * container types TNRSMatchDirect, TNRSMatchSynonym, and TNRSMatchUncertain.
     * 
     * There may be multiple matches for the same search string that map to different nodes
     * in the graph, so each match object is associated with a graph node.
     */
    private abstract class Match {

        // for all matches, information associating this match with a known node in the graph
        private Node matchedNode;               // the id of the matched node in the graph
        private String sourceName;              // the name of the source where the match was found
        protected boolean isSynonymMatch;       // indicates direct matches to known synonyms
        protected boolean isDirectMatch;        // indicates direct matches to known, recognized taxa
        protected boolean isUncertainMatch;     // indicates matches that are not direct, i.e. fuzzy matches (presumably misspellings)
        private HashMap<String,String> responseData; // other data provided by the match source

        public Match(Node _node, String _sourceName) {
            matchedNode = _node;
            sourceName = _sourceName;

            // using Neo4j Node objects for now to represent the
            // matched nodes in the graph. it may be necessary
            // to create taxon objects that extend the node
            // objects though.
        }
        
        public String getSearchString() {
            return searchString;
        }
        
        public long getMatchedNodeId() {
            return matchedNode.getId();
        }
        
        public String getSource() {
            return sourceName;
        }
        
        public String getMatchedNodeName() {
            return matchedNode.getProperty("name").toString();
        }

        public String getMatchType() {
            if (isDirectMatch)
                return "direct";
            else if (isSynonymMatch)
                return "synonym";
            else if (isUncertainMatch)
                return "uncertain";
            else
                throw new java.lang.UnsupportedOperationException("The match " + toString() + " is of unknown type. This is an error. Quitting.");
        }
        
        public String toString() {
            return searchString + " matched to id " + matchedNode.getId() + " in " + TNRSMatchSet.this.getGraphFileName() + " (" + getMatchType() + ")";
        }
    }

    /**
     *  A direct match to a recognized taxon in the graph. This is the trivial case.
     */
    private class MatchDirect extends Match {
        // a direct match to a node in the graph. no user interaction required for these
        public MatchDirect(Node _matchedNode) {
            super(_matchedNode, LOCAL_SOURCE);
            isDirectMatch = true;
            isSynonymMatch = false;
            isUncertainMatch = false;
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

        public MatchSynonym(Node _matchedNode, String _sourceName) {
            super(_matchedNode, _sourceName);
            isDirectMatch = false;
            isSynonymMatch = true;
            isUncertainMatch = false;
        }
    }
    
    /**
     * A match that does not represent a direct string match to any recognized taxon or
     * known synonym. This is likely to happen due to misspellings.
     */
    private class MatchUncertain extends Match {

        public MatchUncertain(Node _matchedNode, String _sourceName) {
            super(_matchedNode, _sourceName);
            isDirectMatch = false;
            isSynonymMatch = false;
            isUncertainMatch = true;
        }        
    }
}