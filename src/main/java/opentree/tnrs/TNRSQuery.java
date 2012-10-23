package opentree.tnrs;

import java.util.HashMap;
import java.util.HashSet;

import opentree.TaxonomyBase;
import opentree.TaxonomyExplorer;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.Traversal;

/**
 * @author Cody Hinchliff
 * 
 *         Provides methods and various options for performing TNRS queries.
 */

public class TNRSQuery {

    private static final double TEMP_SCORE = 0.5;

//    private String _graphName;
    private TaxonomyExplorer _taxonomy;
    private TNRSMatchSet _results;
    private HashSet<String> _unmatchedNames;
    private HashMap<String, Boolean> _matchedNames;

    public TNRSQuery(TaxonomyExplorer taxonomy) {
//        _graphName = graphName;
        _taxonomy = taxonomy;
        _matchedNames = new HashMap<String, Boolean>();
    }

    /**
     * @param searchString
     * @param adapters
     * @return results
     * 
     *         Performs a TNRS query on 'searchString', and loads the results of the query into a TNRSMatchSet object called 'results', which is returned.
     *         'adapters' is either one or an array of TNRSAdapter objects for the sources to be queried. If no adapters are passed, then the query is only
     *         performed against the local taxonomy graph.
     */
    public TNRSMatchSet getMatches(String[] searchStrings, TNRSAdapter... adapters) {
        _results = new TNRSMatchSet();
        _unmatchedNames = new HashSet<String>();

        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];

            // determine the min fuzzy match score based on length; shorter names
            // require lower min identities to small numbers of edit differences
            int ql = thisName.length();
            float minId = 0;
            if (ql < 4)
            	minId = (float)0.55;
            else if (ql < 6)
            	minId = (float)0.6;
            else if (ql < 8)
            	minId = (float)0.65;
            else if (ql < 11)
            	minId = (float)0.7;
            else if (ql < 14)
            	minId = (float)0.75;
            else
            	minId = (float)0.8;

            if (_matchedNames.containsKey(thisName)) {
                continue;
            }
            boolean wasMatched = false;

            // first: use fuzzy matching to find possible preferred taxon nodes
            IndexHits<Node> matchedTaxNodes = _taxonomy.findTaxNodeByNameFuzzy(thisName, minId);
            if (matchedTaxNodes.size() > 0) {
            	
            	for (Node n : matchedTaxNodes) {

                	// check if the name is an exact match to the query
                    boolean isFuzzy = n.getProperty("name").equals(thisName) ? false : true;
            		
                    // check if the matched node is a homonym
                    IndexHits<Node> directMatches = _taxonomy.findTaxNodeByName(n.getProperty("name").toString());
                    boolean isHomonym = directMatches.size() > 1 ? true : false;
                    
                    boolean isPerfectMatch = !(isHomonym || isFuzzy) ? true : false;
           		
                	// TODO: get the score for each hit
                    double score = 0;
                    if (isPerfectMatch)
                    	score = 1;
                    else
                    	score = TEMP_SCORE;

            		// add the match
                    _results.addMatch(new TNRSHit().
                            setMatchedNode(n).
                            setSearchString(thisName).
                            setIsPerfectMatch(isPerfectMatch).
                            setIsApprox(isFuzzy).
                            setIsHomonym(isHomonym).
                            setSourceName("ottol").
                            setScore(score));
                }
                _matchedNames.put(thisName, true);
                wasMatched = true;
            }

            // second: use fuzzy matching to find possible preferred synonyms
            IndexHits<Node> matchedSynNodes = _taxonomy.findSynNodeByNameFuzzy(thisName, minId);
            if (matchedSynNodes.size() > 0) {
                for (Node synNode : matchedSynNodes) {

                	// check if the name is an exact match to the query
                	boolean isApprox = synNode.getProperty("name").equals(thisName) ? false : true;
                 	
                	// find the node matched by this synonym
                	Node taxNode = _taxonomy.getTaxNodeForSynNode(synNode);
                	
                	// TODO: get the score for each hit
                	double score = TEMP_SCORE;
                	
                    // add the match
                    _results.addMatch(new TNRSHit().
                            setMatchedNode(taxNode).
                            setSynonymNode(synNode).
                            setSearchString(thisName).
                            setIsApprox(isApprox).
                            setIsSynonym(true).
                            setIsPerfectMatch(false).
                            setSourceName("ottol").
                            setScore(score));
                }
                _matchedNames.put(thisName, true);
                wasMatched = true;
            }
/*
            // third: check for fuzzy matches to recognized taxa
            IndexHits<Node> fuzzyTaxMatches = _taxonomy.findTaxNodeByNameFuzzy(thisName);
            if (fuzzyTaxMatches.size() > 0) {
                for (Node n : fuzzyTaxMatches) {

                    // check to see if the fuzzily matched node is a homonym
                    IndexHits<Node> directMatches = _taxonomy.findTaxNodeByName(n.getProperty("name").toString());
                    boolean isHomonym = false;
                    if (directMatches.size() > 1)
                        isHomonym = true;

                    // add the match
                    _results.addMatch(new TNRSHit().
                            setIsApprox(true).
                            setIsHomonym(isHomonym).
                            setMatchedNode(n).
                            setScore(TEMP_SCORE). // TODO: record scores with fuzzy matches
                            setSearchString(thisName).
                            setSourceName("ottol"));
                }
                wasMatched = true;
            }

            // fourth: check for fuzzy matches to synonyms
            IndexHits<Node> fuzzySynMatches = _taxonomy.findSynNodeByNameFuzzy(thisName);
            if (fuzzySynMatches.size() > 0) {
                for (Node synNode : fuzzySynMatches) {
                	// find the node matched by this synonym
                	Node taxNode = _taxonomy.getTaxNodeForSynNode(synNode);
                	
                    // add the match
                    _results.addMatch(new TNRSHit().
                    		setIsApprox(true).
                            setMatchedNode(taxNode).
                            setSynonymNode(synNode).
                            setSearchString(thisName).
                            setIsSynonym(true).
                            setSourceName("ottol").
                            setScore(TEMP_SCORE));  // TODO: record scores with fuzzy matches
                }
                wasMatched = true;
            } */
            
            // remember names we can't match within the graph
            if (wasMatched)
                _matchedNames.put(thisName, true);
            else
                _unmatchedNames.add(thisName);
        }

        // TODO: at the moment we are just using internal matching for speed considerations
        boolean useExternalServices = false;
        if (useExternalServices) {
	        // fourth: call passed adapters for help with names we couldn't match
	        if (_unmatchedNames.size() > 0) {
	            for (int i = 0; i < adapters.length; i++) {
	                adapters[i].doQuery(_unmatchedNames, _taxonomy, _results);
	            }
	        }
        }

        _taxonomy.shutdownDB();
        return _results;
    }

    public HashSet<String> getUnmatchedNames() {
        return _unmatchedNames;
    }
}
