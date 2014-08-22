package org.opentree.tnrs.queries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.TermQuery;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.TaxonSet;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.constants.TaxonomyRelType;
import org.opentree.taxonomy.contexts.TaxonomyContext;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;
import org.opentree.tnrs.TNRSHit;
import org.opentree.tnrs.TNRSMatchSet;
import org.opentree.tnrs.TNRSNameResult;
import org.opentree.tnrs.TNRSResults;
import org.opentree.utils.Levenshtein;

/**
 * Provides access to the default TNRS query, which accepts a set of taxonomic names, from which it will attempt to infer
 * the taxonomic context for the provided names, and will optimize queries based on that inferred context. This query returns a
 * fairly exhaustive set of information about the taxon hits to the queried names. It is a general-purpose option for development,
 * testing, and getting information about names in the taxonomy. More specific-purpose solutions to name querying are implemented
 * in other classes that extend the base TNRSQuery class.
 * 
 * @author cody hinchliff
 * 
 */
public class MultiNameContextQuery extends AbstractBaseQuery {
	
	private Map<Object, String> queriedNames;
    private Taxon bestGuessLICAForNames; // used for the inferred context
    private HashSet<Taxon> validTaxaWithExactMatches; // To store taxa/names for which we find direct (exact, n=1) matches

    // set during construction by setDefaults()
    private boolean contextAutoInferenceIsOn;
    private boolean doFuzzyMatching;
    private boolean includeDubious;
    private boolean includeDeprecated;
    private boolean matchSpTaxaToGenera;
    
    private Index<Node> nameIndex;
    private Index<Node> synonymIndex;
    private Index<Node> deprecatedIndex;
    
    private Map<Object, String> namesWithoutExactMatches;
    private Map<Object, String> namesWithoutApproxMatches;

    private final static double GENERIC_SP_MATCHING_SCORE_MODIFIER = 0.99; // arbitrary score modifier, high because we're pretty confident, but not 1 to indicate it was not an exact match
    
    public MultiNameContextQuery(Taxonomy taxonomy) {
    	super(taxonomy);
    }
    
    public MultiNameContextQuery(Taxonomy taxonomy, TaxonomyContext context) {
    	super(taxonomy, context);
    }

    /**
     * Initialize the query object with a set of names. Returns self on success. The result will be returned
     * with the incoming map keys set as the ids.
     * @param searchStrings
     * @param predefContext
     */
    public MultiNameContextQuery setSearchStrings(Map<Object, String> idNameMap) {
        clear();
        for (Object id : idNameMap.keySet()) {
        	queriedNames.put(id, QueryParser.escape(idNameMap.get(id)).toLowerCase());
        }
        return this;
    }

    /**
     * Set the behavior for inferring contexts. The default behavior is that context inference will always be used
     * to attempt to infer the shallowest context for the names. To avoid this, pass a value of false to this method.
     * If context inference is turned off, then matches will always be made against the currently set context, which
     * will be ALLTAXA unless it is manually changed using setContext() or inferContext().
     * 
     * @param useContextInference
     * @return
     */
    public MultiNameContextQuery setAutomaticContextInference(boolean useContextInference) {
    	this.contextAutoInferenceIsOn = useContextInference;
    	return this;
    }
    
	/**
	 * Set the context to `c`. If `c` is null, the context will be set to ALLTAXA.
	 * @param c
	 */
	public MultiNameContextQuery setContext(TaxonomyContext c) {
		super.setContext(c);
		return this;
	}
	
	/**
	 * Set the fuzzy matching behavior. If set to false, no fuzzy matching will be performed. If set to true, fuzzy
	 * matching will be performed if no exact string matches can be found. The default setting is true.
	 * @param doFuzzyMatching
	 * @return
	 */
	public MultiNameContextQuery setDoFuzzyMatching(boolean doFuzzyMatching) {
		this.doFuzzyMatching = doFuzzyMatching;
		return this;
	}
	
	/**
	 * Set the behavior for whether or not to match names of the form 'Genus sp.' to nodes with the name 'Genus'
	 * @param match
	 * @return
	 */
	public MultiNameContextQuery setMatchSpTaxaToGenera(Boolean match) {
		matchSpTaxaToGenera = match;
		return this;
	}
	
	/**
	 * Set the behavior for including dubious names in the results.
	 * @param includeDubious
	 * @return
	 */
	public MultiNameContextQuery setIncludeDubious(boolean includeDubious) {
		this.includeDubious = includeDubious;
		setIndexes();
		return this;
	}
	
	/**
	 * Set the behavior for including dubious names in the results.
	 * @param includeDubious
	 * @return
	 */
	public MultiNameContextQuery setIncludeDeprecated(boolean includeDeprecated) {
		this.includeDeprecated = includeDeprecated;
		return this;
	}
    
    /**
     * Clears the previous results and search strings. Also called by the constructor to initialize the query object.
     */
    @Override
    public MultiNameContextQuery clear() {
    	validTaxaWithExactMatches = new HashSet<Taxon>();
        bestGuessLICAForNames = null;
        results = new TNRSResults();

        queriedNames = new HashMap<Object, String>();
        namesWithoutExactMatches = new HashMap<Object, String>();
        namesWithoutApproxMatches = new HashMap<Object, String>();

        return this;
    }

    /**
     * Reset default search parameters
     */
    @Override
    public MultiNameContextQuery setDefaults() {
        contextAutoInferenceIsOn = true;
        doFuzzyMatching = true;
        includeDubious = false;
        includeDeprecated = false;
        matchSpTaxaToGenera = true;
    	return this;
    }
    
    /**
     * Perform a full TNRS query against the names set using setSearchStrings(). First matches all names to exact taxon
     * names, exact synonyms, and then to approximate taxon names and synonyms, and return the results as a TNRSResults
     * object. Will infer the context unless setAutomaticContextInference() has been turned off.
     * 
     * @return
     */
    public MultiNameContextQuery runQuery() {
    	
    	setIndexes();
    	
    	Map<Object, String> namesToMatchToTaxa =  new HashMap<Object, String>();
    	if (contextAutoInferenceIsOn) {
        	namesToMatchToTaxa = (HashMap<Object, String>) inferContextAndReturnAmbiguousNames();
        } else {
            namesToMatchToTaxa = queriedNames;
        }
        
        // direct match unmatched names within context
        getExactNameMatches(namesToMatchToTaxa);
        
        // direct match *all* names against synonyms
        getExactSynonymMatches(queriedNames);
        
        // do fuzzy matching for any names we couldn't match
        if (doFuzzyMatching) {
        	getApproxTaxnameOrSynonymMatches(namesWithoutExactMatches);
        }
        
        // record unmatchable names to results
        for (Entry<Object, String> nameEntry : doFuzzyMatching ? namesWithoutApproxMatches.entrySet() : namesWithoutExactMatches.entrySet()) {
        	results.addUnmatchedName(nameEntry.getKey(), nameEntry.getValue());
        }
        
        results.setIncludesDeprecated(includeDeprecated);
        results.setIncludesDubious(includeDubious);
        results.setIncludesApproximate(doFuzzyMatching);
        for (Entry<String, Object> entry : taxonomy.getMetadataMap().entrySet()) {
        	results.addTaxMetadataEntry(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    /**
     * Return the results of the previous query
     */
    @Override
    public TNRSResults getResults() {
        return results;
    }
   
    /**
     * Attempt to infer a context for the current set of names by looking for direct matches to the current set of search
     * strings. The resulting TaxonomyContext object is remembered internally.
     * 
     * This method will record the exact taxon matches that it finds during this process. If the `unmatchableNames` object is
     * not null, then names without exact matches will be placed in it as well.
     * 
     * Called by getTNRSResultsForSetNames().
     * 
     * @param searchStrings
     * @return the initiating NameslistStandardQuery object
     */
    public Map<Object, String> inferContextAndReturnAmbiguousNames() {

    	setIndexes();
    	
    	// we will return the names without exact matches
    	Map<Object, String> namesUnmatchableAgainstAllTaxaContext = new HashMap<Object, String>();
  	
    	for (Entry<Object, String> nameEntry : queriedNames.entrySet()) {

    		Object thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		
            // Attempt to find exact matches against *ALL* taxa
            IndexHits<Node> hits = null;
            TermQuery exactQuery = new TermQuery(new Term(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), thisName));
            TNRSMatchSet matches = new TNRSMatchSet(taxonomy);

            try {
            	hits = nameIndex.query(exactQuery);

	            if (hits.size() == 1) { // an exact match
	
	                // WE (MUST) ASSUME that users have spelled names correctly, but havoc may ensue if this assumption
	                // is violated, as slight misspellings are likely to yield direct matches to distantly related taxa.
	
	                // add this taxon to the list of unambigous matches
	                Taxon matchedTaxon = taxonomy.getTaxon(hits.getSingle());
	                validTaxaWithExactMatches.add(matchedTaxon);
	
	                // add the match to the TNRS results
	                matches.addMatch(new TNRSHit()
	                        .setMatchedTaxon(matchedTaxon)
	                        .setMatchedName(matchedTaxon.getName())
	                        .setSearchString(thisName)
	                        .setIsApprox(false)
	                        .setIsSynonym(false)
	                        .setNomenCode(matchedTaxon.getNomenCode())
	                        .setScore(PERFECT_SCORE));
	            } else {
	            	// add this here so it gets checked against other indices later
	            	// EVEN IF we match to a deprecated taxon (below)
	            	namesUnmatchableAgainstAllTaxaContext.put(thisId, thisName);
	            }

	            if (includeDeprecated) {
        			// do the deprecated search, add results
	            	
            		if (hits != null) {
            			hits.close();
            		}
            		hits = deprecatedIndex.query(exactQuery);
            		if (hits.size() > 0) {
    	                for (Node hit : hits) {
    	                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
    	                    matches.addMatch(new TNRSHit()
    	                            .setMatchedTaxon(matchedTaxon)
    	                            .setMatchedName(matchedTaxon.getName())
    	                            .setSearchString(thisName)
    	                            .setIsSynonym(false)
    	                            .setIsApprox(false)
    	                            .setScore(PERFECT_SCORE));
    	                }
            		}
	            }
	            
	            if (matches.size() > 0) { // we found a perfect match
	                results.addNameResult(new TNRSNameResult(thisId, matches));
                    results.addNameWithDirectMatch(thisId, thisName);
	            }
	            
            } finally {
            	hits.close();
            }
        }
        
        // update the LICA for the unambiguous hits. Will set the LICA to the root of the graph if there are no unambiguous hits
        updateLICA();
        
        // now set the context closest to the LICA. If the LICA is the root, this will set the context to ALLTAXA
        setContext(bestGuessLICAForNames.getLeastInclusiveContext());
        
        return namesUnmatchableAgainstAllTaxaContext;
    }
    
    /**
     * Search for exact taxon name matches to names in `searchStrings` using the context that is set. Names that without exact matches are  placed
     * in `namesUnmatchedAgainstWorkingContext`. For names that do have exact matches, we record the results and also add corresponding Taxon objects
     * to `taxaWithDirectMatches`. Finally we call `updateLICA()` to reflect any newly exact-matched taxa.
     * 
     * Called by getTNRSResultsForSetNames().
     * 
     * @param searchStrings
     */
    private void getExactNameMatches(Map<Object, String> searchStrings) {

    	// exact match the names against the context; save all hits
        for (Entry <Object, String> nameEntry : searchStrings.entrySet()) {
        	
    		Object thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		        	
            IndexHits<Node> hits = null;
            TermQuery exactQuery = new TermQuery(new Term(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), thisName));
            TNRSMatchSet matches = new TNRSMatchSet(taxonomy);
            boolean usingGenericSpMatching = false;
            
            try {
            	
            	// first do the search on the full name
            	hits = nameIndex.query(exactQuery);

            	if (hits.size() < 1 && matchSpTaxaToGenera) {
            		// if we got no hits, AND we want to attempt genus name matching, then do so...
            		// will attempt to match names of the form 'Genus sp.' to nodes with the name 'Genus'
            		String[] parts = thisName.split("\\s+");
            		String lastPart = parts[parts.length -1].toLowerCase();
            		
            		String inferredGenusName = null;

            		if (lastPart.equals("sp.")) {
            			inferredGenusName = thisName.substring(0,thisName.length()-3).trim();
            		} else if (lastPart.equals("sp")) {
            			inferredGenusName = thisName.substring(0,thisName.length()-2).trim();
            		}
            		
            		if (inferredGenusName != null) {
            			// this name seems appropriate for "Genus sp." name matching to "Genus", so go ahead
            			thisName = inferredGenusName;
                		usingGenericSpMatching = true;

                		TermQuery exactGenusNameQuery = new TermQuery(new Term(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), thisName));
                        if (hits != null) {
                        	hits.close();
                        	hits = nameIndex.query(exactGenusNameQuery);
                        }
            		}
            	}
            	
	            if (hits.size() > 0) {
	                // at least 1 hit; prepare to record matches
	
	                for (Node hit : hits) {
	                    // add this match to the match set
	                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
	                    matches.addMatch(new TNRSHit()
	                            .setMatchedTaxon(matchedTaxon)
	                            .setMatchedName(matchedTaxon.getName())
	                            .setSearchString(thisName)
	                            .setIsApprox(false)
	                            .setIsSynonym(false)
	                            .setNomenCode(matchedTaxon.getNomenCode())
	                            .setScore(usingGenericSpMatching ? PERFECT_SCORE * GENERIC_SP_MATCHING_SCORE_MODIFIER : PERFECT_SCORE));
	                    
	                    if (!usingGenericSpMatching) {
	                    	validTaxaWithExactMatches.add(matchedTaxon);
	                        results.addNameWithDirectMatch(thisId, thisName);
	                    }
	                }
	            }
	            
	            if (includeDeprecated) {
        			// do the deprecated search, add results. NOTE: we don't care about homonyms here

            		if (hits != null) {
            			hits.close();
            		}
            		hits = deprecatedIndex.query(exactQuery);
            		if (hits.size() > 0) {
    	                for (Node hit : hits) {
    	                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
    	                    matches.addMatch(new TNRSHit()
    	                            .setMatchedTaxon(matchedTaxon)
    	                            .setMatchedName(matchedTaxon.getName())
    	                            .setSearchString(thisName)
    	                            .setIsSynonym(false)
    	                            .setIsApprox(false)
    	                            .setScore(usingGenericSpMatching ? PERFECT_SCORE * GENERIC_SP_MATCHING_SCORE_MODIFIER : PERFECT_SCORE));
    	                    
    	                    if (!usingGenericSpMatching) {
	    	                    results.addNameWithDirectMatch(thisId, thisName);
    	                    }
    	                }
            		}
	            }
	            
                // add matches (if any) to the TNRS results
	            if (matches.size() > 0) {
	                results.addNameResult(new TNRSNameResult(thisId, matches));
	            } else {
	                namesWithoutExactMatches.put(thisId, thisName);
	            }

            } finally {
            	hits.close();
            }
        }
        
        // update the LICA to reflect any new exact hits
        updateLICA();
    }

    /**
     * Search for exact synonym matches to names in `searchStrings` using the context that is set. Names that do not have exact synonym matches 
     * are added to `namesWithoutEaxctSynonymMatches`. For names that do have exact synonym matches, record the results.
     * 
     * @param searchStrings
     * 
     * Called by getTNRSResultsForSetNames().
     * 
     */
    private void getExactSynonymMatches(Map<Object, String> searchStrings) {
  	
    	// exact match unmatched names against context synonym index
        for (Entry <Object, String> nameEntry : searchStrings.entrySet()) {

    		Object thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		TNRSMatchSet matches = null;

    		// use a preexisting nameresult for this name id if there is one, so we don't lose earlier matches
            if (results.containsResultWithId(thisId)) {
            	matches = results.getNameResult(thisId).getMatches();
        	} else {
                matches = new TNRSMatchSet(taxonomy);
        	}
            
            IndexHits<Node> hits = null;
            TermQuery exactQuery = new TermQuery(new Term(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), thisName));

            try {
            	
            	hits = synonymIndex.query(exactQuery);
	            if (hits.size() > 0) {
	            	// at least 1 hit; prepare to record matches

	            	for (Node synonymNode : hits) {
	                    // add this match to the match set

            			// TEMPORARY KLUDGE to prevent blowing up when a taxon node is found in the synonym index.
            			// To be removed once db is corrected (should not find taxon nodes from synonym index).
            			if (!synonymNode.hasRelationship(TaxonomyRelType.SYNONYMOF,Direction.OUTGOING)) {
            				continue;
            			}
	            		
	                	// get the synonym name that was matched
	                	String matchedSynonymName = (String) synonymNode.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName());

	                	// get the taxon node for this synonym
	                    Taxon matchedTaxon = taxonomy.getTaxon(synonymNode.getSingleRelationship(TaxonomyRelType.SYNONYMOF, Direction.OUTGOING).getEndNode());

	                    matches.addMatch(new TNRSHit()
	                            .setMatchedTaxon(matchedTaxon)
	                            .setMatchedName(matchedSynonymName)
	                            .setSearchString(thisName)
	                            .setIsApprox(false)
	                            .setIsSynonym(true)
	                            .setNomenCode(matchedTaxon.getNomenCode())
	                            .setScore(PERFECT_SCORE));
	                }
	            }
	            	
                // add matches (if any) to the TNRS results
	            if (matches.size() > 0) {
	            	
	            	// add the new name result if there wasn't one already there (if there was then we're already using it, see above)
	            	if (!results.containsResultWithId(thisId)) {
	            		results.addNameResult(new TNRSNameResult(thisId, matches));
	            	}	            		          

	            	// in case we managed to find a synonym match for a name that was not matched to a taxon...
	                namesWithoutExactMatches.remove(thisId);
	            }

            } finally {
            	hits.close();
            }
        }
    }
    
    /**
     * Search for approximate taxon name or synonym matches to names in `searchStrings`, adding names that
     * cannot be matched to `namesWithoutApproxTaxnameOrSynonymMatches`.
     * 
     * Called by getTNRSResultsForSetNames().
     * 
     * @param searchStrings
     */
    @SuppressWarnings("unchecked")
	private void getApproxTaxnameOrSynonymMatches(Map<Object, String> searchStrings) {
    	
        for (Entry <Object, String> nameEntry : searchStrings.entrySet()) {

    		Object thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		
            // fuzzy match names against ALL within-context taxa and synonyms
            float minIdentity = getMinIdentity(thisName);
            IndexHits<Node> hits = null;
            TNRSMatchSet matches = new TNRSMatchSet(taxonomy);
            FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), thisName), minIdentity);

            try {

            	// search for regular taxa, add them if there are results
            	hits = nameIndex.query(fuzzyQuery);
            	if (hits.size() > 0) {
	                // at least 1 hit; prepare to record matches
	                
	                for (Node hit : hits) {
	                    
	                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
	                    // add the match if it scores high enough
	                    double score = getScore(thisName, matchedTaxon, hit);
	                    if (score >= minScore) {
	                        matches.addMatch(new TNRSHit()
	                                .setMatchedTaxon(matchedTaxon)
	                                .setMatchedName(matchedTaxon.getName())
	                                .setSearchString(thisName)
	                                .setIsApprox(true)
	                                .setIsSynonym(false)
	                                .setNomenCode(matchedTaxon.getNomenCode())
	                                .setScore(score));
	                    }
	                }
            	}
            	
            	// search for synonyms, add them if there are results
            	hits = synonymIndex.query(fuzzyQuery);
            	if (hits.size() > 0) {
	                // at least 1 hit; prepare to record matches

            		for (Node synonymNode : hits) {
	                    
	                	// get the synonym name that was matched
	                	String matchedSynonymName = (String) synonymNode.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName());
	                	
	                	// get the associated taxon
	                	Taxon matchedTaxon = taxonomy.getTaxon(synonymNode.getSingleRelationship(TaxonomyRelType.SYNONYMOF, Direction.OUTGOING).getEndNode());
	                	
	                	// add the match if it scores high enough
	                    double score = getScore(thisName, matchedSynonymName, matchedTaxon, synonymNode);
		                if (score >= minScore) {
		                		                    
	                        matches.addMatch(new TNRSHit()
	                                .setMatchedTaxon(matchedTaxon)
	                                .setMatchedName(matchedSynonymName)
	                                .setSearchString(thisName)
	                                .setIsApprox(true)
	                                .setIsSynonym(true)
	                                .setNomenCode(matchedTaxon.getNomenCode())
	                                .setScore(score));
	                    }
	                }
            	}
            	
            	if (includeDeprecated) {
        			// do the deprecated search, add results.

            		if (hits != null) {
            			hits.close();
            		}
            		hits = deprecatedIndex.query(fuzzyQuery);
            		if (hits.size() > 0) {
    	                for (Node hit : hits) {
    	                    
    	                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
    	
    	                    // add the match if it scores high enough
    	                    double score = getScore(thisName, (String) hit.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()));
    	                    if (score >= minScore) {
    	                        matches.addMatch(new TNRSHit()
    	                                .setMatchedTaxon(matchedTaxon)
    	                                .setMatchedName(matchedTaxon.getName())
    	                                .setSearchString(thisName)
    	                                .setIsApprox(true)
    	                                .setIsSynonym(false)
    	                                .setScore(score));
    	                    }
    	                }
            		}
            	}
            	
                // add the matches (if any) to the TNRS results
                if (matches.size() > 0) {
	                results.addNameResult(new TNRSNameResult(thisId, matches));
                } else {
                    namesWithoutApproxMatches.put(thisId, thisName);
                }

            } finally {
            	if (hits != null) {
            		hits.close();
            	}
            }
        }
    }

    /**
     * Calculate scores for non-exact matches where the name to score against may be arbitarily defined (as an argument).
     * 
     * @param thisName
     * @param matchedName
     * @param matchedTaxon
     * @param hit
     * @return
     */
    private double getScore(String thisName, String matchedName, Taxon matchedTaxon, Node hit) {
    	        
    	double baseScore = getScore(thisName, matchedName);

    	// weight scores by distance outside of inferred lica (this may need to go away if it is a speed bottleneck)
        double scoreModifier = 1;
        
        if (bestGuessLICAForNames != null) {
	        if (matchedTaxon.isPreferredTaxChildOf(bestGuessLICAForNames) == false) {
	            int d = taxonomy.getInternodalDistThroughMRCA(hit, bestGuessLICAForNames.getNode(), TaxonomyRelType.PREFTAXCHILDOF);
	            scoreModifier *= (1/Math.log(d)); // down-weight fuzzy matches outside of mrca scope by abs distance to mrca
	        }
        }
        
        // return the score
        return baseScore * scoreModifier;
    	
    }

    /**
     * Calculate scores for non-exact matches where the name to score against is the name of the matched taxon.
     * 
     * @param thisName
     * @param matchedTaxon
     * @param hit
     * @return
     */
    private double getScore(String thisName, Taxon matchedTaxon, Node hit) {
    	return getScore(thisName, matchedTaxon.getName(), matchedTaxon, hit);
    }
    
    private double getScore(String searchName, String hitName) {
        // use edit distance to calculate base score for fuzzy matches
        double l = Levenshtein.distance(searchName, hitName);
        double s = Math.min(hitName.length(), searchName.length());
        return (s - l) / s;
    }
    
    /**
     * Update the inferred LICA of the known direct matches. If taxaWithDirectMatches is empty, then
     * the LICA is set to the root of the graph.
     */
    private void updateLICA() {
        // update the lica to reflect all direct hits
        TaxonSet ts = new TaxonSet(validTaxaWithExactMatches);
        if (ts.size() > 0)
            bestGuessLICAForNames = ts.getLICA();
        else
            bestGuessLICAForNames = taxonomy.getTaxon(taxonomy.ALLTAXA.getRootNode());
    }

    /**
     * Sets the index to be used for queries.
     */
    private void setIndexes() {
		deprecatedIndex = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.DEPRECATED_TAXA);
    	if (includeDubious) {
    		nameIndex = context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME);
    		synonymIndex = context.getNodeIndex(TaxonomyNodeIndex.SYNONYM_NODES_BY_SYNONYM);
    	} else {
    		nameIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME);
    		synonymIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_SYNONYM_NODES_BY_SYNONYM);
    	}
    }
}
