package opentree.tnrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import opentree.BarrierNodes;
import opentree.Taxon;
import opentree.TaxonSet;
import opentree.TaxonomyExplorer;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

/**
 * @author Cody Hinchliff
 * 
 *         Provides methods and various options for performing TNRS queries.
 */

public class TNRSQuery {

    private static final double TEMP_SCORE = 0.5;

    private TaxonomyExplorer _taxonomy;
    private TNRSMatchSet _results;
    private HashSet<String> _unmatchedNames;
    private HashSet<String> _matchedNames;
    private HashSet<Long> _matchedNodeIds;
    private LinkedList<Node> _unambiguousTaxa;

    public TNRSQuery(TaxonomyExplorer taxonomy) {
        _taxonomy = taxonomy;
        _matchedNames = new HashSet<String>();
        _unambiguousTaxa = new LinkedList<Node>();
        _matchedNodeIds = new HashSet<Long>();
    }

    /**
     *  Do a TNRS query returning exact matches only for an array of names `searchStrings'
     *  
     * @param searchStrings
     * @param adapters
     * @return results
     */
    public TNRSMatchSet getExactMatches(String[] searchStrings) {
        boolean exactOnly = true;
        return getMatches(searchStrings, exactOnly, (TNRSAdapter)null);
    }

    /**
     *  Do a TNRS query returning exact matches only for a single name `searchString'
     *  
     * @param searchString
     * @param adapters
     * @return results
     */
    public TNRSMatchSet getExactMatches(String searchString) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        boolean exactOnly = true;
        return getMatches(searchStrings, exactOnly, (TNRSAdapter)null);
    }

    /**
     *  Do a TNRS query returning all matches for a single name `searchString'
     *  
     * @param searchString
     * @param adapters
     * @return results
     */
    public TNRSMatchSet getAllMatches(String searchString) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        boolean exactOnly = false;
        return getMatches(searchStrings, exactOnly, (TNRSAdapter)null);
    }

    /**
     * @param searchStrings
     * @param adapters
     * @return results
     * 
     *         Performs a TNRS query on `searchString`, a comma-delimited list of names, and loads the results of the query into a TNRSMatchSet object `results`, which is returned.
     *         `adapters` is either one or an array of TNRSAdapter objects for the sources to be queried. If no adapters are passed, then the query is only
     *         performed against the local taxonomy graph. (Adapters are currently deactivated for speed considerations).
     */
    public TNRSMatchSet getAllMatches(String[] searchStrings, TNRSAdapter... adapters) {
        boolean exactOnly = false;
        return getMatches(searchStrings, exactOnly, adapters);
    }
    
    private TNRSMatchSet getMatches(String[] searchStrings, boolean exactOnly, TNRSAdapter... adapters) {
        _results = new TNRSMatchSet();
        _unmatchedNames = new HashSet<String>();

        for (int i = 0; i < searchStrings.length; i++) {
            IndexHits<Node> directHits = _taxonomy.findTaxNodesByName(searchStrings[i]);
            if (directHits.size() == 1)
                for (Node n : directHits)
                    _unambiguousTaxa.add(n);
        }
        
        for (Node n : _unambiguousTaxa)
            System.out.println(n.getProperty("name"));
        
        TaxonSet ts = new TaxonSet(_unambiguousTaxa);
        
        Taxon mrca;
        if (ts.size() > 0)
            mrca = ts.getMRCA();
        
        BarrierNodes bn = new BarrierNodes(_taxonomy.getGraphDB());
        ArrayList<Node> barrierNodes = bn.getBarrierNodes();
        // TODO: get closest outgoing barrier node

        /* so code is in place to determine the taxon mrca of a given set of taxa. now need to determine how to
         * use this information. one possibility is to weight potential homonym matches by their distance to the
         * taxonomic mrca, with those falling within the defined ingroup having higher weight than those falling
         * outside it. another question is what set of taxa to use to determine the mrca. we can remove all
         * blatant homonyms, but what about synonyms?
         */
        
//        if (mrca != null)
//            System.out.println(mrca.getProperty("name"));
        
        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];

            // determine the min fuzzy match score based on length; shorter names
            // require lower min identities to small numbers of edit differences
            int ql = thisName.length();
            float minIdentity = 0;
            if (ql < 4)
            	minIdentity = (float)0.55;
            else if (ql < 6)
                minIdentity = (float)0.6;
            else if (ql < 8)
                minIdentity = (float)0.65;
            else if (ql < 11)
                minIdentity = (float)0.7;
            else if (ql < 14)
                minIdentity = (float)0.75;
            else
                minIdentity = (float)0.8;

            if (_matchedNames.contains(thisName)) {
                continue;
            }
            boolean wasMatched = false;

            // first: find possible preferred taxon nodes, use fuzzy matching if specified
            IndexHits<Node> matchedTaxNodes;
            if (exactOnly)
                matchedTaxNodes = _taxonomy.findTaxNodesByName(thisName);
            else
                matchedTaxNodes = _taxonomy.findTaxNodesByNameFuzzy(thisName, minIdentity);

            if (matchedTaxNodes.size() > 0) {
            	
            	for (Node n : matchedTaxNodes) {

                	// check if the name is an exact match to the query
                    boolean isFuzzy = n.getProperty("name").equals(thisName) ? false : true;

                    // check if the matched node is a homonym
                    IndexHits<Node> directHits = _taxonomy.findTaxNodesByName(n.getProperty("name").toString());
                    boolean isHomonym = directHits.size() > 1 ? true : false;

                    // TODO: if it is a homonym, find closest downstream barrier node from 
                    
                    boolean isPerfectMatch = (!isHomonym && !isFuzzy) ? true : false;
           		
                	// TODO: get the score for each hit, currently using extremely coarse scoring
                    double score = 0;
                    if (isPerfectMatch)
                    	score = 1;
                    else
                    	score = TEMP_SCORE;

                    // TODO: use tu.getDistToMRCA(n) function to adjust score for homonyms
                    
            		// add the match
                    _results.addMatch(new TNRSHit().
                            setMatchedNode(n).
                            setSearchString(thisName).
                            setIsPerfectMatch(isPerfectMatch).
                            setIsApprox(isFuzzy).
                            setIsHomonym(isHomonym).
                            setSourceName("ottol").
                            setScore(score));

                    _matchedNodeIds.add(n.getId());
                }
            	_matchedNames.add(thisName);
                wasMatched = true;
            }

            // second: find possible preferred synonyms, use fuzzy matching if specified
            IndexHits<Node> matchedSynNodes;
            if (exactOnly)
                matchedSynNodes = _taxonomy.findSynNodesByName(thisName);
            else
                matchedSynNodes = _taxonomy.findSynNodesByNameFuzzy(thisName, minIdentity);

            if (matchedSynNodes.size() > 0) {
                for (Node synNode : matchedSynNodes) {

                	// check if the name is an exact match to the query
                	boolean isApprox = synNode.getProperty("name").equals(thisName) ? false : true;
                 	
                	// find the node matched by this synonym
                	Node taxNode = _taxonomy.getTaxNodeForSynNode(synNode);
                	
                	// only add new matches to taxon nodes that are not already in the matched set
                	if (_matchedNodeIds.contains(taxNode.getId()) == false) {
                    	// TODO: get the score for each hit
                    	double score = TEMP_SCORE;

                    	// TODO: use tu.getDistToMRCA(n) function to adjust score for all synonym matches

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

                        _matchedNodeIds.add(synNode.getId());
                	}
                }
                _matchedNames.add(thisName);
                wasMatched = true;
            }
            
            // remember names we can't match within the graph
            if (wasMatched)
                _matchedNames.add(thisName);
            else
                _unmatchedNames.add(thisName);
        }

        // TODO: at the moment we are only using internal matching; not worth the extra time for external services
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

    public Set<String> getUnmatchedNames() {
        return _unmatchedNames;
    }
}
