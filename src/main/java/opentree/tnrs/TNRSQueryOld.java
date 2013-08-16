package opentree.tnrs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import opentree.NodeIndexDescription;
import opentree.RelType;
import opentree.Taxon;
import opentree.TaxonSet;
import opentree.Taxonomy;
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
public class TNRSQueryOld {
/*
//    private static final double TEMP_SCORE = 0.75;
    private static final double DEFAULT_NOMEN_CODE_SUPERMAJORITY_PROPORTION = 0.75;
    private static final double DEFAULT_MIN_SCORE = 0.01;
    private static final double DISTANT_HOMONYM_SCORE_SCALAR = 0.25;
//    private static final double DEFAULT_FUZZY_MATCH_IDENTITY = 0.8;

    private static final int SHORT_NAME_LENGTH = 9;
    private static final int MEDIUM_NAME_LENGTH = 14;
    private static final int LONG_NAME_LENGTH = 19;

    private static final String DEFAULT_TAXONOMY_NAME = "ottol";
    private static final String UNDETERMINED = "undetermined";

//    private Taxonomy _taxonomy;
    private Taxonomy _taxonomy;
    private TNRSResults _results;
    private HashSet<String> _queriedNames;
    private HashSet<Long> _matchedNodeIds;
    private double _minScore;
    
    public TNRSQueryOld(Taxonomy taxonomy) {
//        _taxonomy = taxonomy;
        _taxonomy = taxonomy;
        _minScore = DEFAULT_MIN_SCORE; // TODO: make it possible for client to set this
        clearResults();
    }
    
    private void clearResults() {
        _queriedNames = new HashSet<String>();
        _results = new TNRSResults();
        _matchedNodeIds = new HashSet<Long>();
    }

    // TODO: create independent methods that leverage external adapters to match names

    /**
     * Performs an internal TNRS query for *ONLY* exact name matches to strings within `searchStrings`.
     *         
     * @param searchStrings
     * @return results
     * 
     *
    public TNRSResults getExactMatches(String[] searchStrings) {
        boolean exactOnly = true;
        return getResults(searchStrings, exactOnly, (TNRSAdapter)null);
    }

    /**
     * Performs an internal TNRS query for *ONLY* exact name matches to `searchString`.
     *         
     * @param searchString
     * @return results
     * 
     *
    public TNRSResults getExactMatches(String searchString) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        boolean exactOnly = true;
        return getResults(searchStrings, exactOnly, (TNRSAdapter)null);
    }

    /**
     * Performs a full internal TNRS query on a single name `searchString`, finding exact and approximate matches.
     *         
     * @param searchStrings
     * @return results
     * 
     *
    public TNRSResults getAllMatches(String searchString) {
        String[] searchStrings = new String[1];
        searchStrings[0] = searchString;
        boolean exactOnly = false;
        return getResults(searchStrings, exactOnly, (TNRSAdapter)null);
    }

    /**
     * Performs a full internal TNRS query on an array of names `searchStrings`, finding exact and approximate matches.
     *
     * @param searchStrings
     * @return results
     * 
     *
    public TNRSResults getAllMatches(String[] searchStrings) {
        boolean exactOnly = false;
        return getResults(searchStrings, exactOnly, (TNRSAdapter)null);
    }
    
    private TNRSResults getResults(String[] searchStrings, boolean exactOnly, TNRSAdapter... adapters) {

        clearResults();
        Index<Node> prefTaxNodesByName = _taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME);
        Index<Node> prefTaxNodesBySynonym = _taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_SYNONYM);

        // add the names to be queried
        for (String name : searchStrings)
            _queriedNames.add(name);

        // prepare for initial name processing
        HashMap<String, Integer> codeFreqs = new HashMap<String, Integer>();
        LinkedList<Node> unambiguousTaxonNodes = new LinkedList<Node>();
        
        // find unambiguous names, count occurrences of nomenclatural codes within these names
        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];
            IndexHits<Node> directHits = prefTaxNodesByName.get("name", thisName);

            if (directHits.size() == 1) { // neither a homonym nor synonym
                Taxon n = _taxonomy.getTaxon(directHits.getSingle());
                unambiguousTaxonNodes.add(n.getNode());
                _results.addNameWithDirectMatch(thisName);

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
        TaxonSet ts = new TaxonSet(unambiguousTaxonNodes, _taxonomy);
        
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
            mrca = _taxonomy.getTaxon(_taxonomy.getLifeNode());
        
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

                    Taxon matchedTaxon = _taxonomy.getTaxon(n);
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
                        if (_results.getGoverningCode().equals(_taxonomy.getTaxon(n).getNomenCode()) == false) {
                            
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
                    Taxon matchedTaxon = _taxonomy.getTaxon(_taxonomy.getTaxNodeForSynNode(n));
                    
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

        return _results;
    }

    /**
     * Returns a minimum identity score used for fuzzy matching that limits the number
     * of edit differences based on the length of the string. 
     * 
     * @param name
     * @return minIdentity
     *
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
    */
}
