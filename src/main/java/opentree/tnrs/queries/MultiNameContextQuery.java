package opentree.tnrs.queries;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;

import opentree.taxonomy.RelType;
import opentree.taxonomy.Taxon;
import opentree.taxonomy.TaxonSet;
import opentree.taxonomy.Taxonomy;
import opentree.taxonomy.contexts.ContextDescription;
import opentree.taxonomy.contexts.NodeIndexDescription;
import opentree.taxonomy.contexts.TaxonomyContext;
import opentree.tnrs.TNRSHit;
import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSResults;
import opentree.utils.Levenshtein;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.TermQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

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
    
//    private HashSet<String> queriedNames;
	private Map<String, String> queriedNames;
    private Taxon bestGuessLICAForNames; // used for the inferred context
    private HashSet<Taxon> taxaWithExactMatches; // To store taxa/names for which we can/cannot find direct (exact, n=1) matches

    // set during construction by setDefaults()
    private boolean contextAutoInferenceIsOn;
    private boolean includeDubious;
    
    // different indexes may be used depending on preferences (such as whether to include dubious taxa)
    private Index<Node> nameIndex;
    private Index<Node> synonymIndex;
    private Index<Node> nameOrSynonymIndex;
    
    // special-purpose containers used during search
//    private HashSet<String> namesUnmatchableAgainstAllTaxaContext;
//    private HashSet<String> namesWithoutExactNameMatches;
//    private HashSet<String> namesWithoutExactSynonymMatches;
//    private HashSet<String> namesWithoutApproxTaxnameOrSynonymMatches;

//    private Map<String, String> namesUnmatchableAgainstAllTaxaContext;
    private Map<String, String> namesWithoutExactNameMatches;
    private Map<String, String> namesWithoutExactSynonymMatches;
    private Map<String, String> namesWithoutApproxTaxnameOrSynonymMatches;

    
    public MultiNameContextQuery(Taxonomy taxonomy) {
    	super(taxonomy);
    }
    
    public MultiNameContextQuery(Taxonomy taxonomy, TaxonomyContext context) {
    	super(taxonomy, context);
    }
    
    /**
     * Initialize the query object with a set of names. Returns self on success. The result will be returned
     * with the original search strings as the ids.
     * @param searchStrings
     * @param predefContext
     */
    public MultiNameContextQuery setSearchStrings(Set<String> searchStrings) {
        clear();
        for (String s : searchStrings) {
        	queriedNames.put(s, QueryParser.escape(s).toLowerCase());
        }
        return this;
    }

    /**
     * Initialize the query object with a set of names. Returns self on success. The result will be returned
     * with the keys of the incoming map keys set as the ids.
     * @param searchStrings
     * @param predefContext
     */
    public MultiNameContextQuery setSearchStrings(Map<String, String> searchStringsWithIds) {
        clear();
        for (Entry<String, String> entry : searchStringsWithIds.entrySet()) {
        	queriedNames.put(entry.getKey(), QueryParser.escape(entry.getValue()).toLowerCase());
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
	 * Set the behavior for including dubious names in the results.
	 * @param includeDubious
	 * @return
	 */
	public MultiNameContextQuery setIncludeDubious(boolean includeDubious) {
		this.includeDubious = includeDubious;
		return this;
	}
    
    /**
     * Clears the previous results and search strings. Also called by the constructor to initialize the query object.
     */
    @Override
    public MultiNameContextQuery clear() {
//        queriedNames = new HashSet<String>();
    	queriedNames = new HashMap<String, String>();
        taxaWithExactMatches = new HashSet<Taxon>();
        bestGuessLICAForNames = null;
        results = new TNRSResults();
        
        // special-purpose containers used during search procedure
//        namesWithoutExactNameMatches = new HashSet<String>();
//        namesWithoutExactSynonymMatches = new HashSet<String>();
//    	namesWithoutApproxTaxnameOrSynonymMatches = new HashSet<String>();

        namesWithoutExactNameMatches = new HashMap<String, String>();
        namesWithoutExactSynonymMatches = new HashMap<String, String>();
        namesWithoutApproxTaxnameOrSynonymMatches = new HashMap<String, String>();

    	return this;
    }

    /**
     * Reset default search parameters
     */
    @Override
    public MultiNameContextQuery setDefaults() {
        contextAutoInferenceIsOn = true;
        includeDubious = false;
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
        
        // infer context if we are allowed to, and determine names to be matched against it
//        HashSet<String> namesToMatchAgainstContext = new HashSet<String>();
    	Map<String, String> namesToMatchAgainstContext =  new HashMap<String, String>();
        if (contextAutoInferenceIsOn) {
//        	namesToMatchAgainstContext = (HashSet<String>) inferContextAndReturnAmbiguousNames();
        	namesToMatchAgainstContext = (HashMap<String, String>) inferContextAndReturnAmbiguousNames();
        } else {
            namesToMatchAgainstContext = queriedNames;
        }
        
        // direct match unmatched names within context
        getExactNameMatches(namesToMatchAgainstContext);
        
        // direct match unmatched names against synonyms
        getExactSynonymMatches(namesWithoutExactNameMatches);
        
        // TODO: external concept resolution for still-unmatched names? (direct match returned concepts against context)
        // this will need an external concept-resolution service, which as yet does not seem to exist...

        // do fuzzy matching for any names we couldn't match
        getApproxTaxnameOrSynonymMatches(namesWithoutExactSynonymMatches);
        
        // TODO: last-ditch effort to match yet-unmatched names: try truncating names in case there are accession-id modifiers?

        // record unmatchable names to results
//        for (String name : namesWithoutApproxTaxnameOrSynonymMatches)
        for (Entry<String, String> nameEntry : namesWithoutApproxTaxnameOrSynonymMatches.entrySet()) {
            results.addUnmatchedName(nameEntry.getKey(), nameEntry.getValue());
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
//    public Set<String> inferContextAndReturnAmbiguousNames() {
    public Map<String, String> inferContextAndReturnAmbiguousNames() {

    	setIndexes();
    	
    	// we will return the names without exact matches
//    	HashSet<String> namesUnmatchableAgainstAllTaxaContext = new HashSet<String>();
    	Map<String, String> namesUnmatchableAgainstAllTaxaContext = new HashMap<String, String>();
    	
//    	for (String thisName : queriedNames) {
    	for (Entry<String, String> nameEntry : queriedNames.entrySet()) {

    		String thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		
            // Attempt to find exact matches against *ALL* preferred taxa
            IndexHits<Node> hits = null;
            TermQuery exactQuery = new TermQuery(new Term("name", thisName));
            try {
//            	hits = taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).query(exactQuery);
            	hits = nameIndex.query(exactQuery);

	            if (hits.size() == 1) { // an exact match
	
	                // WE (MUST) ASSUME that users have spelled names correctly, but havoc may ensue if this assumption
	                // is violated, as slight misspellings are likely to yield direct matches to distantly related taxa.
	
	                // add this taxon to the list of unambigous matches
	                Taxon matchedTaxon = taxonomy.getTaxon(hits.getSingle());
	                taxaWithExactMatches.add(matchedTaxon);
	
	                // add the match to the TNRS results
	                TNRSMatchSet matches = new TNRSMatchSet(taxonomy);
	                matches.addMatch(new TNRSHit().
	                        setMatchedTaxon(matchedTaxon).
	                        setSearchString(thisName).
	                        setIsPerfectMatch(true).
	                        setIsApprox(false).
	                        setIsHomonym(false).
	                        setNomenCode(matchedTaxon.getNomenCode()).
	                        setSourceName(DEFAULT_TAXONOMY_NAME).
	                        setScore(PERFECT_SCORE));
	
	                results.addNameResult(new TNRSNameResult(thisId, matches));
//	                results.addNameWithDirectMatch(thisName);
	                results.addNameWithDirectMatch(thisId, thisName);
	
	            } else { // is either a homonym match or there is no exact match
	            	namesUnmatchableAgainstAllTaxaContext.put(thisId, thisName);

	            }
            } finally {
            	hits.close();
            }
        }
        
        // update the LICA for the unambiguous (non-homonym) hits. Will set the LICA to the root of the graph if there are no unambiguous hits
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
//    private void getExactNameMatches(HashSet<String> searchStrings) {
    private void getExactNameMatches(Map<String, String> searchStrings) {

    	// exact match the names against the context; save all hits
//        for (String thisName : searchStrings) {
        for (Entry <String, String> nameEntry : searchStrings.entrySet()) {

    		String thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		        	
            IndexHits<Node> hits = null;
            TermQuery exactQuery = new TermQuery(new Term("name", thisName));
            try {
//            	hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).query(exactQuery);
            	hits = nameIndex.query(exactQuery);
	            if (hits.size() < 1) {
	                // no direct matches, move on to next name
//	                namesWithoutExactNameMatches.add(thisName);
	                namesWithoutExactNameMatches.put(thisId, thisName);
	                continue;
	
	            } else {
	                // at least 1 hit; prepare to record matches
	                TNRSMatchSet matches = new TNRSMatchSet(taxonomy);
	
	                // determine within-context homonym status
	                boolean isHomonym = false;
	                if (hits.size() > 1)
	                    isHomonym = true;
	
	                for (Node hit : hits) {
	                    // add this match to the match set
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
	                    
	                    if (isHomonym == false) {
	                        taxaWithExactMatches.add(matchedTaxon);
//	                        results.addNameWithDirectMatch(thisName);
	                        results.addNameWithDirectMatch(thisId, thisName);
	                    }
	                }

	                // add matches to the TNRS results
//	                results.addNameResult(new TNRSNameResult(thisName, matches));
	                results.addNameResult(new TNRSNameResult(thisId, matches));
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
//    private void getExactSynonymMatches(HashSet<String> searchStrings) {
    private void getExactSynonymMatches(Map<String, String> searchStrings) {
    	
    	// exact match unmatched names against context synonym index
//        for (String thisName : searchStrings) {
        for (Entry <String, String> nameEntry : searchStrings.entrySet()) {

    		String thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		
            IndexHits<Node> hits = null;
            TermQuery exactQuery = new TermQuery(new Term("name", thisName));
            try {
//            	hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_SYNONYM).query(exactQuery);
            	hits = synonymIndex.query(exactQuery);
	            if (hits.size() < 1) {
	                // no direct matches, move on to next name
//	                namesWithoutExactSynonymMatches.add(thisName);
	                namesWithoutExactSynonymMatches.put(thisId, thisName);
	                continue;
	
	            } else {
	                // at least 1 hit; prepare to record matches
	                TNRSMatchSet matches = new TNRSMatchSet(taxonomy);
	
	                for (Node hit : hits) {
	                    // add this match to the match set
	                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
	                    matches.addMatch(new TNRSHit().
	                            setMatchedTaxon(matchedTaxon).
	                            setSearchString(thisName).
	                            setIsPerfectMatch(false).
	                            setIsApprox(false).
	                            setIsHomonym(false).
	                            setIsSynonym(true).
	                            setNomenCode(matchedTaxon.getNomenCode()).
	                            setSourceName(DEFAULT_TAXONOMY_NAME).
	                            setScore(PERFECT_SCORE));
	                }
	
	                // add matches to the TNRS results
//	                results.addNameResult(new TNRSNameResult(thisName, matches));
	                results.addNameResult(new TNRSNameResult(thisId, matches));
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
//    private void getApproxTaxnameOrSynonymMatches(HashSet<String> searchStrings) {
    private void getApproxTaxnameOrSynonymMatches(Map<String, String> searchStrings) {
    	
//        for (String thisName : searchStrings) {
        for (Entry <String, String> nameEntry : searchStrings.entrySet()) {

    		String thisId = nameEntry.getKey();
    		String thisName = nameEntry.getValue();
    		
            // fuzzy match names against ALL within-context taxa and synonyms
            float minIdentity = getMinIdentity(thisName);
            IndexHits<Node> hits = null;
            FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("name", thisName), minIdentity);

            try {
//            	hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM).query(fuzzyQuery);
            	hits = nameOrSynonymIndex.query(fuzzyQuery);
	            if (hits.size() < 1) {
	                // no direct matches, move on to next name
//	                namesWithoutApproxTaxnameOrSynonymMatches.add(thisName);
	                namesWithoutApproxTaxnameOrSynonymMatches.put(thisId, thisName);
	                continue;
	
	            } else {
	                // at least 1 hit; prepare to record matches
	                TNRSMatchSet matches = new TNRSMatchSet(taxonomy);
	                
	                for (Node hit : hits) {
	                    
	                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
	
	                    // use edit distance to calculate base score for fuzzy matches
	                    //          System.out.println("comparing " + queriedName + " to " + matchedTaxon.getName());
	                    double l = Levenshtein.distance(thisName, matchedTaxon.getName());
	                    //          System.out.println("l = " + String.valueOf(l));
	                    double s = Math.min(matchedTaxon.getName().length(), thisName.length());
	                    //          System.out.println("s = " + String.valueOf(s));
	                    double baseScore = (s - l) / s;
	                    //          System.out.println("baseScore = " + String.valueOf(baseScore));
	                    
	                    // weight scores by distance outside of inferred lica (this may need to go away if it is a speed bottleneck)
	                    double scoreModifier = 1;
	                    if (matchedTaxon.isPreferredTaxChildOf(bestGuessLICAForNames) == false) {
	                        int d = taxonomy.getInternodalDistThroughMRCA(hit, bestGuessLICAForNames.getNode(), RelType.PREFTAXCHILDOF);
	                        scoreModifier *= (1/Math.log(d)); // down-weight fuzzy matches outside of mrca scope by abs distance to mrca
	                        System.out.println("scoreModifier = " + String.valueOf(scoreModifier));
	                    }
	                    
	                    // add the match if it scores high enough
	                    double score = baseScore * scoreModifier;
	                    if (score >= minScore) {
	                        matches.addMatch(new TNRSHit().
	                                setMatchedTaxon(matchedTaxon).
	                                setSearchString(thisName).
	                                setIsPerfectMatch(false).
	                                setIsApprox(true).
	                                setNameStatusIsKnown(false).
	                                setNomenCode(matchedTaxon.getNomenCode()).
	                                setSourceName(DEFAULT_TAXONOMY_NAME).
	                                setScore(score));
	                    }
	                }
	
	                // add the matches (if any) to the TNRS results
	                if (matches.size() > 0)
//	                    results.addNameResult(new TNRSNameResult(thisName, matches));
		                results.addNameResult(new TNRSNameResult(thisId, matches));
	                else
//	                    namesWithoutApproxTaxnameOrSynonymMatches.add(thisName);
	                    namesWithoutApproxTaxnameOrSynonymMatches.put(thisId, thisName);
	            }
            } finally {
            	hits.close();
            }
        }
    }

    /**
     * Update the inferred LICA of the known direct matches. If taxaWithDirectMatches is empty, then
     * the LICA is set to the root of the graph.
     */
    private void updateLICA() {
        // update the lica to reflect all direct hits
        TaxonSet ts = new TaxonSet(taxaWithExactMatches);
        if (ts.size() > 0)
            bestGuessLICAForNames = ts.getLICA();
        else
            bestGuessLICAForNames = taxonomy.getTaxon(taxonomy.ALLTAXA.getRootNode());
    }

    /**
     * Sets the index to be used for queries.
     */
    private void setIndexes() {
    	if (includeDubious) {
    		nameIndex = context.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
    		synonymIndex = context.getNodeIndex(NodeIndexDescription.TAXON_BY_SYNONYM);
    		nameOrSynonymIndex = context.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME_OR_SYNONYM);
    	} else {
    		nameIndex = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME);
    		synonymIndex = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_SYNONYM);
    		nameOrSynonymIndex = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
    	}
    }
}
