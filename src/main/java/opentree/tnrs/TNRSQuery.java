package opentree.tnrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
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
    private static final double NOMEN_CODE_SUPERMAJORITY_PROPORTION = 0.75;
    private static final String UNDETERMINED = "undetermined";

    private TaxonomyExplorer _taxonomy;
    private TNRSResults _results;
    private HashSet<String> _queriedNames;
//    private HashSet<String> _unmatchedNames;
//    private HashSet<String> _matchedNames;
//    private HashSet<Long> _matchedNodeIds;
//    private LinkedList<Node> _unambiguousTaxonNodes;
    
    public TNRSQuery(TaxonomyExplorer taxonomy) {
        _taxonomy = taxonomy;
        initialize();
    }
    
    private void initialize() {
        _queriedNames = new HashSet<String>();
        _results = new TNRSResults();
//        _matchedNames = new HashSet<String>();
//        _matchedNodeIds = new HashSet<Long>();
//        _unmatchedNames = new HashSet<String>();
    }
    
    /**
     *  Do a TNRS query returning exact matches only for an array of names `searchStrings'
     *  
     * @param searchStrings
     * @param adapters
     * @return results
     */
    public TNRSResults getExactMatches(String[] searchStrings) {
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
    public TNRSResults getExactMatches(String searchString) {
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
    public TNRSResults getAllMatches(String searchString) {
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
    public TNRSResults getAllMatches(String[] searchStrings, TNRSAdapter... adapters) {
        boolean exactOnly = false;
        return getMatches(searchStrings, exactOnly, adapters);
    }
    
    private TNRSResults getMatches(String[] searchStrings, boolean exactOnly, TNRSAdapter... adapters) {

        initialize();

        // add the names to be queried
        for (String name : searchStrings)
            _queriedNames.add(name);

        // prepare for initial name processing
        HashMap<String, Integer> codeFreqs = new HashMap<String, Integer>();
        LinkedList<Node> unambiguousTaxonNodes = new LinkedList<Node>();
        
        // find unambiguous names, count occurrences of nomenclatural codes within these names
        for (int i = 0; i < searchStrings.length; i++) {
            IndexHits<Node> directHits = _taxonomy.findTaxNodesByName(searchStrings[i]);

            if (directHits.size() == 1) { // neither a homonym nor synonym
                for (Node n : directHits) {
                    unambiguousTaxonNodes.add(n);

                    // update the count for the nomenclatural code govering this name
                    String thisCode = n.getProperty("taxCode").toString();
                    if (codeFreqs.containsKey(thisCode)) {
                        int count = codeFreqs.get(thisCode) + 1;
                        codeFreqs.put(thisCode, count);
                    } else {
                        codeFreqs.put(thisCode, 1);
                    }
                }
            }
        }
        TaxonSet ts = new TaxonSet(unambiguousTaxonNodes);
        
        // find mrca of unambiguous names
        Taxon mrca;
        if (ts.size() > 0)
            mrca = ts.getMRCA();
        else
            mrca = new Taxon(_taxonomy.getLifeNode());

        // find prevalent nomenclatural code (if any)
        String prevalentCode = "";
        for (Entry<String, Integer> i : codeFreqs.entrySet()) {
            // TODO: make it possible for user to set req'd supermajority prop via query data
            // actually, should probably make it possible to assign a context name rather than setting this value
            if ((i.getValue() / _queriedNames.size()) >= NOMEN_CODE_SUPERMAJORITY_PROPORTION) {
                prevalentCode = i.getKey();
                break;
            }
        }
        if (prevalentCode.equals("")) {
            _results.setGoverningCode(UNDETERMINED);
        } else {
            _results.setGoverningCode(prevalentCode);
        }

        // now do TNRS on each name
        for (String thisName : _queriedNames) {

            // prepare for TNRS on this name
            float minIdentity = getMinIdentity(thisName);
            boolean wasMatched = false;
            TNRSMatchSet matches = new TNRSMatchSet();
            
            // first: find possible preferred taxon nodes, use fuzzy matching if specified
            IndexHits<Node> matchedTaxNodes;
            if (exactOnly)
                matchedTaxNodes = _taxonomy.findTaxNodesByName(thisName);
            else
                matchedTaxNodes = _taxonomy.findTaxNodesByNameFuzzy(thisName, minIdentity);

            if (matchedTaxNodes.size() > 0) {
            	
            	for (Node n : matchedTaxNodes) {

                    double scoreModifier = 1;

                	// check if the name is an exact match to the query
                    boolean isFuzzy = n.getProperty("name").equals(thisName) ? false : true;

                    // check if the matched node is a homonym
                    IndexHits<Node> directHits = _taxonomy.findTaxNodesByName(n.getProperty("name").toString());
                    boolean isHomonym;
                    if (directHits.size() > 1) {
                        isHomonym = true;
                        if (_results.getGoverningCode() != n.getProperty("taxCode"))
                            scoreModifier *= 0.5; // de-weight homonyms outside prevalent nomenclature
                    } else {
                        isHomonym = false;
                    }
                    
                    // remember perfect matches
                    boolean isPerfectMatch = (!isHomonym && !isFuzzy) ? true : false;
           		
                	// TODO: get the score for each hit, currently using extremely coarse scoring
                    double baseScore = 0;
                    if (isPerfectMatch)
                    	baseScore = 1;
                    else
                    	baseScore = TEMP_SCORE;

                    // weight fuzzy matches by abs distance to mrca
                    
                    
            		// add the match
                    matches.addMatch(new TNRSHit().
                            setMatchedNode(n).
                            setSearchString(thisName).
                            setIsPerfectMatch(isPerfectMatch).
                            setIsApprox(isFuzzy).
                            setIsHomonym(isHomonym).
                            setSourceName("ottol").
                            setScore(baseScore * scoreModifier));

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

    public float getMinIdentity(String name) {

        // The required minimum identity for fuzzy matches is based on the length of the string. 
        // Shorter names need lower scores to work well, because fewer edit differences have a
        // bigger impact on the the identity proportion when compared with other strings.
        
        int ql = name.length();

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
        
        return minId;
    }
}
