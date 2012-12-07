package opentree.tnrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import opentree.Taxon;
import opentree.TaxonSet;
import opentree.Taxonomy;
import opentree.NodeIndexDescription;
import opentree.RelType;
import opentree.TaxonomyContext;
import opentree.utils.Levenshtein;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Provides methods and various options for performing TNRS queries.
 * @author cody hinchliff
 * 
 */
public class TNRSQueryNew {

    
//    private static final double TEMP_SCORE = 0.75;
    private static final double DEFAULT_NOMEN_CODE_SUPERMAJORITY_PROPORTION = 0.75;
    private static final double DEFAULT_MIN_SCORE = 0.01;
    private static final double PERFECT_SCORE = 1;
    private static final double DISTANT_HOMONYM_SCORE_SCALAR = 0.25;
//    private static final double DEFAULT_FUZZY_MATCH_IDENTITY = 0.8;

    private static final int SHORT_NAME_LENGTH = 9;
    private static final int MEDIUM_NAME_LENGTH = 14;
    private static final int LONG_NAME_LENGTH = 19;

    private static final String DEFAULT_TAXONOMY_NAME = "ottol";
    private static final String UNDETERMINED = "undetermined";

    private Taxonomy taxonomy;
    private TNRSResults results;
    private HashSet<String> queriedNames;
    private HashSet<Long> matchedNodeIds;
    private double minScore;

    private Index<Node> prefTaxNodesByName = taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME);
    
    public TNRSQueryNew(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
        minScore = DEFAULT_MIN_SCORE; // TODO: make it possible for client to set this
        clearResults();
    }
    
    private void clearResults() {
        queriedNames = new HashSet<String>();
        results = new TNRSResults();
        matchedNodeIds = new HashSet<Long>();
    }

    private TNRSResults getResults(String[] searchStrings, boolean exactOnly, TaxonomyContext context) {

        clearResults();
        ArrayList<String> namesWithoutDirectMatches = new ArrayList<String>();

        // add all names to be queried
        for (String name : searchStrings)
            queriedNames.add(name);
        
        if (context == null) {

            // No user-defined context, so we need to find one. First we will look for unambiguous
            // matches on entire graph (remembering direct matches to valid homonyms separately)

            // to store those matched taxa with direct (i.e. exact, n=1) hits
            LinkedList<Taxon> unambiguousTaxa = new LinkedList<Taxon>();

            // to store names of homonym matches (homonyms at the scale of all life, anyway)
            LinkedList<String> homonymNames = new LinkedList<String>();

            for (String thisName : queriedNames) {

                // Attempt to find exact matches against *ALL* preferred taxa
                IndexHits<Node> hits = prefTaxNodesByName.get("name", thisName);
                if (hits.size() == 1) { // not a homonym

                    // WE (MUST) ASSUME that users have spelled names correctly, but havoc will ensure if this assumption
                    // is violated, as mispelled names are likely to yield direct matches to distantly related taxa!

                    // add this taxon to the list of unambigous matches
                    Taxon matchedTaxon = taxonomy.getTaxon(hits.getSingle());
                    unambiguousTaxa.add(matchedTaxon);

                    // add the match to the TNRS results
                    TNRSMatchSet matches = new TNRSMatchSet();
                    matches.addMatch(new TNRSHit().
                            setMatchedTaxon(matchedTaxon).
                            setSearchString(thisName).
                            setIsPerfectMatch(true).
                            setIsApprox(false).
                            setIsHomonym(false).
                            setNomenCode(matchedTaxon.getNomenCode()).
                            setSourceName(DEFAULT_TAXONOMY_NAME).
                            setScore(PERFECT_SCORE));
                    results.addNameResult(new TNRSNameResult(thisName, matches));
                    results.addUnambiguousName(thisName);

                } else { // is a homonym match, we will come back to these once we identify a least inclusive context
                    homonymNames.add(thisName);
                }

                hits.close();
            }
            
            // find the lica of the unambiguous (non-homonym) hits
            TaxonSet ts = new TaxonSet(unambiguousTaxa);
            Taxon lica;
            if (ts.size() > 0)
                lica = ts.getLICA();
            else
                lica = taxonomy.getTaxon(taxonomy.ALLTAXA.getRootNode());

            // now we can determine a context to use from here on
            context = lica.getLeastInclusiveContext();

            // direct match the remembered homonyms against the inferred context
            for (String thisName : homonymNames) {

                // Here we are just matching the names that we *already know* (see above) are valid homonyms at the
                // scale of the entire tree. They may not be homonyms within the context we've now defined (they may
                // not even be direct matches at all)
                IndexHits<Node> hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).get("name", thisName);

                if (hits.size() < 1) {
                    // no direct matches, move on to next name
                    namesWithoutDirectMatches.add(thisName);
                    continue;

                } else {
                    // at least 1 hit; prepare to record matches
                    TNRSMatchSet matches = new TNRSMatchSet();

                    // determine within-context homonym status
                    boolean isHomonym = false;
                    if (hits.size() > 1)
                        isHomonym = true;
    
                    for (Node hit : hits) {
                        // add this match to the TNRS results
                        Taxon matchedTaxon = taxonomy.getTaxon(hit);
                        matches.addMatch(new TNRSHit().
                                setMatchedTaxon(matchedTaxon).
                                setSearchString(thisName).
                                setIsPerfectMatch(!isHomonym). // here it's either a direct match to an in-context homonym or a perfect match
                                setIsApprox(false).
                                setIsHomonym(isHomonym).
                                setNomenCode(matchedTaxon.getNomenCode()).
                                setSourceName(DEFAULT_TAXONOMY_NAME).
                                setScore(PERFECT_SCORE));
                        results.addNameResult(new TNRSNameResult(thisName, matches));
                        
                        if (isHomonym == false) {
                            unambiguousTaxa.add(matchedTaxon);
                            results.addUnambiguousName(thisName);
                        }
                    }
                }
                
                hits.close();
            }

        } else {

            // find direct hits within user-defined context, save all direct hits, including homonyms

        
        }
        
        // direct match unmatched names against context
        
        // external concept resolution for still-unmatched names? (direct match returned concepts against context)
        
        // FINALLY, fuzzy match names we couldn't otherwise match

        
        return results;
        
    }
    
    private TNRSResults getResultsOLD(String[] searchStrings, boolean exactOnly, TaxonomyContext context, TNRSAdapter... adapters) {
        
        /* 
        // prepare for initial name processing
        HashMap<String, Integer> codeFreqs = new HashMap<String, Integer>();
        LinkedList<Node> unambiguousTaxonNodes = new LinkedList<Node>();
        
        // find unambiguous names, count occurrences of nomenclatural codes within these names
        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];
            IndexHits<Node> directHits = prefTaxNodesByName.get("name", thisName);

            if (directHits.size() == 1) { // neither a homonym nor synonym
                Taxon n = new Taxon(directHits.getSingle());
                unambiguousTaxonNodes.add(n.getNode());
                _results.addUnambiguousName(thisName);

                // update the count for the nomenclatural code govering this name
                String thisCode = n.getNomenCode();
                if (codeFreqs.containsKey(thisCode)) {
                    System.out.println("Incrementing count for " + thisCode);
                    int count = codeFreqs.get(thisCode) + 1;
                    codeFreqs.put(thisCode, count);
                } else {
                    System.out.println("Starting count for " + thisCode);
                    codeFreqs.put(thisCode, 1);
                }
            }
            directHits.close();
        }
        TaxonSet ts = new TaxonSet(unambiguousTaxonNodes);
        
        // find prevalent nomenclatural code (if any)
        // TODO: make it possible for user to assign a governing code rather than guessing it
        String prevalentCode = "";
        for (Entry<String, Integer> i : codeFreqs.entrySet()) {
            System.out.println(i.getKey() + " " + i.getValue());
            if ((i.getValue() / (double)_queriedNames.size()) >= DEFAULT_NOMEN_CODE_SUPERMAJORITY_PROPORTION) {
                System.out.println(i.getKey());
                prevalentCode = i.getKey();
                break;
            }
        }
        if (prevalentCode.equals("")) {
            _results.setGoverningCode(UNDETERMINED);
        } else {
            _results.setGoverningCode(prevalentCode);
        }
        
        System.out.println("governingCode = " + _results.getGoverningCode());

        // find lica of unambiguous names
        Taxon mrca;
        if (ts.size() > 0)
            mrca = ts.getLICA();
        else
            mrca = new Taxon(_taxonomy.getLifeNode());
        
        System.out.println("mrca = " + mrca + " " + mrca.getName());
        
        // now do TNRS on each name
        for (String queriedName : _queriedNames) {

            float minIdentity = getMinIdentity(queriedName);
            boolean wasMatched = false;
            TNRSMatchSet matches = new TNRSMatchSet();
            
            // first: find possible preferred taxon nodes, use fuzzy matching if specified
            IndexHits<Node> matchedTaxNodes;
            if (exactOnly) {
                matchedTaxNodes = prefTaxNodesByName.get("name", queriedName);
            } else {
                matchedTaxNodes = prefTaxNodesByName.query(new FuzzyQuery(new Term("name", queriedName), minIdentity));
            }
            
            if (matchedTaxNodes.size() > 0) {
                wasMatched = true;

                // process all the matches to taxon nodes
                for (Node n : matchedTaxNodes) {

                    Taxon matchedTaxon = new Taxon(n);
                    double baseScore = 1;
                    double scoreModifier = 1;

                	// check if the matched node name is an exact match to the query
                    boolean isFuzzy;
                    if (matchedTaxon.getName().equals(queriedName)) {
                        isFuzzy = false;
                    } else {
                        isFuzzy = true;

                        // use edit distance to calculate base score for fuzzy matches
//                        System.out.println("comparing " + queriedName + " to " + matchedTaxon.getName());
                        double l = Levenshtein.distance(queriedName, matchedTaxon.getName());
//                        System.out.println("l = " + String.valueOf(l));
                        double s = Math.min(matchedTaxon.getName().length(), queriedName.length());
//                        System.out.println("s = " + String.valueOf(s));
                        baseScore = (s - l) / s;
//                        System.out.println("baseScore = " + String.valueOf(baseScore));
                        
                        if (matchedTaxon.isPreferredTaxChildOf(mrca) == false) {

                            ////////////////////////////////////////////////////////////////////////////////////
                            // TODO: provide mrca_hard_context option to exclude matches outside of mrca scope
                            // (needs corresponding option for providing taxon name for scoped mrca itself)
                            ////////////////////////////////////////////////////////////////////////////////////
                            
                            int d = _taxonomy.getInternodalDistThroughMRCA(n, mrca.getNode(), RelType.PREFTAXCHILDOF);
                            scoreModifier *= (1/Math.log(d)); // down-weight fuzzy matches outside of mrca scope by abs distance to mrca
                            System.out.println("scoreModifier = " + String.valueOf(scoreModifier));
                        }
                    }
                    
                    // check if the matched node is a homonym
                    IndexHits<Node> directHits = prefTaxNodesByName.get("name", n.getProperty("name"));
                    boolean isHomonym;
                    if (directHits.size() > 1) {
                        isHomonym = true;
//                        System.out.println("Comparing = '" + _results.getGoverningCode() + "' to '" + n.getProperty("taxcode") + "'");
                        if (_results.getGoverningCode().equals(new Taxon(n).getNomenCode()) == false) {
                            
                            ////////////////////////////////////////////////////////////////////////////////////
                            // TODO: provide nomen_code_hard_context option to exclude homonyms outside of nomenclature scope
                            // (needs corresponding option for providing scoped nomenclature code itself)
                            ////////////////////////////////////////////////////////////////////////////////////
                            
                            scoreModifier *= DISTANT_HOMONYM_SCORE_SCALAR; // down-weight homonyms outside prevalent nomenclature
//                            System.out.println("Distant homonym; scoreModifier adjusted down to = " + String.valueOf(scoreModifier));
                        }
                    } else {
                        isHomonym = false;
                    }
                    
                    // remember perfect matches
                    boolean isPerfectMatch = (!isHomonym && !isFuzzy) ? true : false;
           		                    
                    // add the match if it scores high enough
                    double score = baseScore * scoreModifier;
                    if (score >= _minScore) {
                        matches.addMatch(new TNRSHit().
                                setMatchedTaxon(matchedTaxon).
                                setSearchString(queriedName).
                                setIsPerfectMatch(isPerfectMatch).
                                setIsApprox(isFuzzy).
                                setIsHomonym(isHomonym).
                                setNomenCode(matchedTaxon.getNomenCode()).
                                setSourceName(DEFAULT_TAXONOMY_NAME).
                                setScore(score));
                    }

                    // remember matched taxon nodes so we don't add them again later
                    _matchedNodeIds.add(n.getId());
                }
            }

            // second: find possible preferred synonyms, use fuzzy matching if specified
            IndexHits<Node> matchedSynNodes;
            if (exactOnly)
                matchedSynNodes = prefTaxNodesBySynonym.get("name", queriedName);
            else
                matchedSynNodes = prefTaxNodesBySynonym.query(new FuzzyQuery(new Term("name", queriedName), minIdentity));

            if (matchedSynNodes.size() > 0) {
                wasMatched = true;

                // process all matches to synonym nodes
                for (Node n : matchedSynNodes) {

                    String synonymName = n.getProperty("name").toString();
                    double scoreModifier = 1;
                    double baseScore = 1;

                    // get the taxon matched by this synonym
                    Taxon matchedTaxon = new Taxon(_taxonomy.getTaxNodeForSynNode(n));
                    
                	// only add new matches to taxon nodes that are not already in the matched set
                	if (_matchedNodeIds.contains(matchedTaxon.getNode().getId()) == false) {

                        // check if the matched synonym name is an exact match to the query
                        boolean isFuzzy;
                        if (synonymName.equals(queriedName)) {
                            isFuzzy = false;
                        } else {
                            isFuzzy = true;
                            
                            // use edit distance to calculate base score for fuzzy matches
//                            System.out.println("comparing " + queriedName + " to " + synonymName);
                            double l = Levenshtein.distance(queriedName, synonymName);
//                            System.out.println("l = " + String.valueOf(l));
//                            System.out.println("length of queried name = " + queriedName.length());
//                            System.out.println("length of synonym name = " + synonymName.length());
                            double s = Math.min(synonymName.length(), queriedName.length());
//                            System.out.println("s = " + String.valueOf(s));
                            baseScore = (s - l) / s;
//                            System.out.println("baseScore = " + String.valueOf(baseScore));
                            
                            
                            if (matchedTaxon.isPreferredTaxChildOf(mrca) == false) {

                                System.out.println(matchedTaxon.getName() + " is not a child of " + mrca.getName());
                                ////////////////////////////////////////////////////////////////////////////////////
                                // TODO: provide mrca_hard_context option to exclude matches outside of mrca scope
                                // (needs corresponding option for providing taxon name for scoped mrca itself)
                                ////////////////////////////////////////////////////////////////////////////////////
                                
                                int d = _taxonomy.getInternodalDistThroughMRCA(matchedTaxon.getNode(), mrca.getNode(), RelType.PREFTAXCHILDOF);
//                                System.out.println("d = " + String.valueOf(d));
                                scoreModifier *= (1/Math.log(d)); // down-weight fuzzy matches outside of mrca scope by abs distance to mrca
//                                System.out.println("scoreModifier = " + String.valueOf(scoreModifier));

                            }
                        }

                        // add the match
                        double score = baseScore * scoreModifier;
                        if (score >= _minScore) {
                            matches.addMatch(new TNRSHit().
                                    setMatchedTaxon(matchedTaxon).
                                    setSearchString(queriedName).
                                    setIsApprox(isFuzzy).
                                    setIsSynonym(true).
                                    setIsPerfectMatch(false).
                                    setNomenCode(matchedTaxon.getNomenCode()).
                                    setSourceName(DEFAULT_TAXONOMY_NAME).
                                    setScore(score));
                        }
                        
                        _matchedNodeIds.add(n.getId());
                	}
                }
            }
            
            if (wasMatched) {
                // record all results found for this
                _results.addNameResult(new TNRSNameResult(queriedName, matches));

            } else {
                // remember names we couldn't match
                _results.addUnmatchedName(queriedName);
            }
        }

        // at the moment we are only using internal matching; not worth the extra time for external services
        // TODO: make this an optional feature
        // TODO: make this accessible via other methods
        boolean useExternalServices = false;
        if (useExternalServices) {
	        // fourth: call passed adapters for help with names we couldn't match
	        if (_results.getUnmatchedNames().size() > 0) {
	            for (int i = 0; i < adapters.length; i++) {
	                adapters[i].doQuery(_results.getUnmatchedNames(), _taxonomy, _results);
	            }
	        }
        }

        return _results; */

        return null;

    }

    /**
     * Returns a minimum identity score used for fuzzy matching that limits the number
     * of edit differences based on the length of the string. 
     * 
     * @param name
     * @return minIdentity
     */
    public float getMinIdentity(String name) {
        
        float ql = name.length();

        int maxEdits = 4;

        if (ql < SHORT_NAME_LENGTH)
            maxEdits = 1;
        else if (ql < MEDIUM_NAME_LENGTH)
            maxEdits = 2;
        else if (ql < LONG_NAME_LENGTH)
            maxEdits = 3;
            
        return (ql - (maxEdits + 1)) / ql;
    }
    
    // TODO: create independent methods that leverage external adapters to match names

    // methods providing access to different query options follow
    
    /**
     * Returns *ONLY* exact matches to `searchStrings` within `context`.
     *         
     * @param searchStrings
     * @param context
     * @return results
     * 
     */
    public TNRSResults matchExact(String[] searchStrings, TaxonomyContext context) {
        boolean exactOnly = true;
        return getResults(searchStrings, exactOnly, context);
    }

    /**
     * Returns *ONLY* exact matches to `searchStrings`; the taxonomic context is automatically determined.
     *         
     * @param searchStrings
     * @return results
     * 
     */
    public TNRSResults matchExact(String[] searchStrings) {
        boolean exactOnly = true;
        TaxonomyContext context = null;
        return getResults(searchStrings, exactOnly, context);
    }
    
    /**
     * Returns *ONLY* exact matches to `searchString` within `context`.
     *         
     * @param searchString
     * @param context
     * @return results
     * 
     */
    public TNRSResults matchExact(String searchString, TaxonomyContext context) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        boolean exactOnly = true;
        return getResults(searchStrings, exactOnly, context);
    }

    /**
     * Returns *ONLY* exact matches to `searchString`; the taxonomic context is automatically determined.
     *         
     * @param searchStrings
     * @return results
     * 
     */
    public TNRSResults matchExact(String searchString) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        boolean exactOnly = true;
        TaxonomyContext context = null;
        return getResults(searchStrings, exactOnly, context);
    }
    
    /**
     * Returns all exact and approximate matches to `searchString` within `context`.
     *         
     * @param searchStrings
     * @param context
     * @return results
     * 
     */
    public TNRSResults match(String searchString, TaxonomyContext context) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        boolean exactOnly = false;
        return getResults(searchStrings, exactOnly, context);
    }

    /**
     * Returns all exact and approximate matches to `searchString`; the taxonomic context is automatically determined.
     *         
     * @param searchStrings
     * @return results
     * 
     */
    public TNRSResults match(String searchString) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        TaxonomyContext context = null;
        boolean exactOnly = false;
        return getResults(searchStrings, exactOnly, context);
    }
    
    /**
     * Returns all exact and approximate matches to `searchStrings` within `context`.
     *
     * @param searchStrings
     * @return results
     * 
     */
    public TNRSResults match(String[] searchStrings, TaxonomyContext context) {
        boolean exactOnly = false;
        return getResults(searchStrings, exactOnly, context);
    }
    
    /**
     * Returns all exact and approximate matches to `searchStrings`; the taxonomic context is automatically determined.
     *         
     * @param searchStrings
     * @return results
     * 
     */
    public TNRSResults match(String[] searchStrings) {
        TaxonomyContext context = null;
        boolean exactOnly = false;
        return getResults(searchStrings, exactOnly, context);
    }
}
