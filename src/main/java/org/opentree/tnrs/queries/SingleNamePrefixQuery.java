package org.opentree.tnrs.queries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.Version;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.TaxonomyRelType;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.TaxonSet;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.contexts.ContextDescription;
import org.opentree.taxonomy.contexts.TaxonomyContext;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;
import org.opentree.tnrs.TNRSHit;
import org.opentree.tnrs.TNRSMatch;
import org.opentree.tnrs.TNRSMatchSet;
import org.opentree.tnrs.TNRSNameResult;
import org.opentree.tnrs.TNRSResults;
import org.opentree.utils.Levenshtein;

/**
 * Provides access to a tnrs query tailored for autocomplete boxes.
 * 
 * @author cody hinchliff
 * 
 */
public class SingleNamePrefixQuery extends AbstractBaseQuery {

	private String queryString;
    private TNRSMatchSet matches;
    private HashMap<String, Boolean> homonyms;
    private HashSet<Node> matchedNodes;
    private boolean includeDubious;
    
    private int minLengthForPrefixQuery;
    private final int minLengthForQuery = 2;
    
    private final int DEFAULT_MIN_LENGTH_FOR_PREFIX_QUERY = 3;

    private Index<Node> speciesNodesByGenus;
    private Index<Node> taxNodesByName;
    private Index<Node> taxNodesByNameHigher;
	private Index<Node> taxNodesByNameOrSynonymHigher;
    private Index<Node> taxNodesByNameGenera;
    private Index<Node> taxNodesByNameSpecies;
    private Index<Node> taxNodesByNameOrSynonym;
	private Index<Node> taxNodesBySynonym;
    
    private Index<Node> prefSpeciesNodesByGenus;
    private Index<Node> prefTaxNodesByName;
    private Index<Node> prefTaxNodesByNameHigher;
	private Index<Node> prefTaxNodesByNameOrSynonymHigher;
    private Index<Node> prefTaxNodesByNameGenera;
    private Index<Node> prefTaxNodesByNameSpecies;
    private Index<Node> prefTaxNodesByNameOrSynonym;
	private Index<Node> prefTaxNodesBySynonym;
	
    public SingleNamePrefixQuery(Taxonomy taxonomy) {
    	super(taxonomy);
    	speciesNodesByGenus = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.SPECIES_BY_GENUS);
    	prefSpeciesNodesByGenus = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_SPECIES_BY_GENUS);
    }
    
    public SingleNamePrefixQuery(Taxonomy taxonomy, TaxonomyContext context) {
    	super(taxonomy, context);
    	speciesNodesByGenus = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.SPECIES_BY_GENUS);
    	prefSpeciesNodesByGenus = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_SPECIES_BY_GENUS);
    }

    /**
     * Initialize the query object with a query string.
     * @param queryString
     */
    public SingleNamePrefixQuery setQueryString(String queryString) {

    	// index is lower case, query string should be as well!
//    	this.queryString = QueryParser.escape(queryString).toLowerCase();    	
    	this.queryString = queryString.toLowerCase();
    	return this;
    }
    
    /**
     * Set the behavior whether or not to include suppressed taxa.
     * @param includeDubious
     */
	public SingleNamePrefixQuery setIncludeDubiousTaxa(boolean includeDubious) {
		this.includeDubious = includeDubious;
		return this;
	}

    /**
     * Set the context to be used by this query.
     */
    @Override
    public SingleNamePrefixQuery setContext(TaxonomyContext c) {
    	super.setContext(c);
        
    	taxNodesByName = this.context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME);
    	taxNodesByNameHigher = this.context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_HIGHER);
    	taxNodesByNameOrSynonymHigher = this.context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_OR_SYNONYM_HIGHER);
        taxNodesByNameGenera = this.context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_GENERA);
        taxNodesByNameSpecies = this.context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_SPECIES);
        taxNodesByNameOrSynonym = this.context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_OR_SYNONYM);
        taxNodesBySynonym = this.context.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_SYNONYM);
        
    	prefTaxNodesByName = this.context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME);
    	prefTaxNodesByNameHigher = this.context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_HIGHER);
    	prefTaxNodesByNameOrSynonymHigher = this.context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM_HIGHER);
        prefTaxNodesByNameGenera = this.context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_GENERA);
        prefTaxNodesByNameSpecies = this.context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_SPECIES);
        prefTaxNodesByNameOrSynonym = this.context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
        prefTaxNodesBySynonym = this.context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_SYNONYM);
    	return this;
    }
    
    /**
     * Clear the results and search strings from the previous query. Also called by the constructor to initialize the object.
     */
    @Override
    public SingleNamePrefixQuery clear() {
    	this.queryString = "";
    	this.homonyms = new HashMap<String, Boolean>();
    	this.matchedNodes = new HashSet<Node>();
    	this.results = new TNRSResults();
    	this.matches = new TNRSMatchSet(taxonomy);
    	return this;
    }
    
    /**
     * Return the search settings parameters to their default values
     */
	@Override
	public SingleNamePrefixQuery setDefaults() {
		minLengthForPrefixQuery = DEFAULT_MIN_LENGTH_FOR_PREFIX_QUERY;
		return this;
	}

	/**
	 * A query tailored for autocomplete services. Attempts to maximize the relevance of matches by predicting user interaction
	 * with an autocompleting search field.
	 */
    @Override
	public SingleNamePrefixQuery runQuery() {

		String escapedQuery = QueryParser.escape(queryString);
    	
    	boolean hasSpace = false;
    	for (Character a : queryString.toCharArray()) {
    		if (a.equals(' ')) {
    			hasSpace = true;
    			break;
    		}
    	}
		
    	if (hasSpace) {
    		
    		String[] parts = queryString.split("\\s+",2);

			// Hit it against the species index.
			getExactMatches(escapedQuery, includeDubious ? taxNodesByNameSpecies : prefTaxNodesByNameSpecies, true);
			
			if (matches.size() < 1) { // no exact hit against the species index
				
    			// Hit the first word against the genus name index
    			getExactMatches(QueryParser.escape(parts[0]), includeDubious ? taxNodesByNameGenera : prefTaxNodesByNameGenera, true);

    			if (matches.size() > 0) { // the first word was an exact match against the genus index
    				
    				LinkedList<TNRSMatch> genusMatches = new LinkedList<TNRSMatch>();
    				for (TNRSMatch match : matches) {
    					genusMatches.add(match);
    				}
    				
    				// erase the genus matches, since we're looking for more specific matches using the provided epithet
    				matches = new TNRSMatchSet(taxonomy);
    				
    				// retrieve all the species/subspecific taxa in each matched genus (may be homonym)
    				for (TNRSMatch genusMatch : genusMatches) {

    					Index<Node> nodeByGenusIndex = includeDubious ? speciesNodesByGenus : prefSpeciesNodesByGenus;
    					IndexHits<Node> speciesHits = nodeByGenusIndex.get(TaxonomyProperty.PARENT_GENUS_OTT_ID.propertyName(), genusMatch.getMatchedNode().getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));

	    				try {
	    				
		    				String genusName = String.valueOf(genusMatch.getMatchedNode().getProperty(TaxonomyProperty.NAME.propertyName()));
		    				char[] searchEpithet = parts[1].trim().toCharArray();
		    				
		    				// add any species to the results whose name matches the second search term prefix
		    				for (Node sp : speciesHits) {
		    					
		    					// use substring position to get just the epithet of this species match
		    					char[] hitName = String.valueOf(sp.getProperty(TaxonomyProperty.NAME.propertyName())).substring(genusName.length()+1).toCharArray();

		    					// check if the epithet matches
		    					boolean prefixMatch = true;
		    					for (int i = 0; i < searchEpithet.length; i++) {
		    						
		    						if (i < hitName.length) {
			    						if (hitName[i] != searchEpithet[i]) {
			    							prefixMatch = false;
			    							break;
			    						}
		    						} else { // if the hit name is shorter than the incoming prefix, it isn't a match
		    							prefixMatch = false;
		    							break;
		    						}
		    					}
		    					if (prefixMatch) {
		    						matches.addMatch(new TNRSHit()
		    								.setMatchedTaxon(new Taxon(sp, taxonomy))
		    								.setIsHomonym(isHomonym(String.valueOf(sp.getProperty(TaxonomyProperty.NAME.propertyName()))))
		    								.setRank(String.valueOf(sp.getProperty(TaxonomyProperty.RANK.propertyName())))
		    								.setIsDubious((Boolean) sp.getProperty(TaxonomyProperty.DUBIOUS.propertyName())))
		    								;
		    					}
		    				}
	    				} finally {
	    					speciesHits.close();
	    				}

    				}
    				
    			} else { // no exact hit for first word against the genus index

    				// Hit it against the synonym index and the higher taxon index
	    			getExactMatches(escapedQuery, includeDubious ? taxNodesBySynonym : prefTaxNodesBySynonym, false);
	    			getExactMatches(escapedQuery, includeDubious ? taxNodesByNameHigher : prefTaxNodesByNameHigher, true);
	    			
    				if (matches.size() < 1) {
    					
    					// Prefix query against the higher taxon index
    					getPrefixMatches(escapedQuery, includeDubious ? taxNodesByNameOrSynonymHigher : prefTaxNodesByNameOrSynonymHigher);

	    				if (matches.size() < 1) {

	    					// last resort: fuzzy match against entire index
		    				getApproxMatches(escapedQuery, includeDubious ? taxNodesByNameOrSynonym : prefTaxNodesByNameOrSynonym);
	    				}
    				}
    			}
			}	
    	} else { // does not contain a space at all

    		getExactMatches(escapedQuery, includeDubious ? taxNodesByNameOrSynonymHigher : prefTaxNodesByNameOrSynonymHigher, true);

    		if (matches.size() < 1) {

    			// Do a prefix query against the higher taxon index
    			getPrefixMatches(escapedQuery, includeDubious ? taxNodesByNameHigher : prefTaxNodesByNameHigher);

    			if (matches.size() < 1) {

    				// Do a prefix query against the all taxa synonym index
    				getPrefixMatches(escapedQuery, includeDubious ? taxNodesBySynonym : prefTaxNodesBySynonym);
    				
    				if (matches.size() < 1) {
    					getApproxMatches(escapedQuery, includeDubious ? taxNodesByNameOrSynonymHigher : prefTaxNodesByNameOrSynonymHigher);
    				}
    			}
    		}
    	}
    	
		return this; 
	}

    /**
     * Perform a simple query optimized for the autocomplete box on the opentree website.
     * 
     * @return
     * @throws ParseException 
     */
    @Deprecated
    public SingleNamePrefixQuery runQueryOld() throws ParseException {
        
    	matches = new TNRSMatchSet(taxonomy);

    	if (queryString.length() < minLengthForQuery) {
    		throw new IllegalArgumentException("cannot perform query on less than " + minLengthForQuery + " characters");
    	}
    	
    	getExactMatches(queryString, includeDubious ? taxNodesByNameOrSynonym : prefTaxNodesByNameOrSynonym, true);
    
    	if (queryString.length() >= minLengthForPrefixQuery) {
        	getPrefixMatches(queryString, includeDubious ? taxNodesByNameOrSynonym : prefTaxNodesByNameOrSynonym);
    	}

    	// only do fuzzy queries if we haven't matched anything: they are slow!
		if (matches.size() < 1) {
    		// attempt fuzzy query
    		getApproxMatches(queryString, includeDubious ? taxNodesByNameOrSynonym : prefTaxNodesByNameOrSynonym);
		}
    	
    	return this;
    }
    
    /**
     * Get the results of the last query
     */
    @Override
    public TNRSResults getResults() {
    	results.addNameResult(new TNRSNameResult(queryString, matches));
        return results;
    }
    
    /**
     * Search for exact taxon name or synonym matches to names in `searchStrings` using the provided index.
     * 
     * @param searchStrings
     */
    private void getExactMatches(String query, Index<Node> index, boolean labelExactMatches) {

    	TermQuery exactQuery = new TermQuery(new Term("name", query));
    	IndexHits<Node> hits = null;
    	try {
    		hits = index.query(exactQuery);

    		boolean isHomonym = hits.size() > 1 ? true : false;

            for (Node hit : hits) {
            	if (matchedNodes.contains(hit) == false) {
	            	matchedNodes.add(hit);
	                Taxon matchedTaxon = taxonomy.getTaxon(hit);
	                matches.addMatch(new TNRSHit()
	                        .setMatchedTaxon(matchedTaxon)
	                        .setRank(matchedTaxon.getRank())
	                        .setIsHomonym(isHomonym)
	                        .setIsPerfectMatch(labelExactMatches)
	                        .setIsDubious((Boolean) matchedTaxon.getNode().hasProperty(TaxonomyProperty.DUBIOUS.propertyName())));
            	}
            }
    	} finally {
    		hits.close();
    	}
    }
    
    /**
     * Attempt to find prefix query matches against the search string. Does not attempt to query for string shorter than
     * the specified minimum length. To set the minimum length, use setMinLengthForPrefixQuery.
     */
    private void getPrefixMatches(String query, Index<Node> index) {
    	
    	if (query.length() < minLengthForPrefixQuery) {
    		return;
    	}
    	
    	PrefixQuery prefixQuery = new PrefixQuery(new Term("name", query));

    	IndexHits<Node> hits = null;
    	try {
    		hits = index.query(prefixQuery);

            for (Node hit : hits) {
            	if (matchedNodes.contains(hit) == false) {
	            	matchedNodes.add(hit);
	                Taxon matchedTaxon = taxonomy.getTaxon(hit);
	                matches.addMatch(new TNRSHit()
	                        .setMatchedTaxon(matchedTaxon)
	                        .setRank(matchedTaxon.getRank())
	                        .setIsHomonym(isHomonym(matchedTaxon.getName()))
	                        .setIsDubious((Boolean) matchedTaxon.getNode().hasProperty(TaxonomyProperty.DUBIOUS.propertyName())));
            	}
            }
    	} finally {
    		hits.close();
    	}
    }

    /**
     * Search for approximate taxon name or synonym matches against `queryString`.
     */
    private void getApproxMatches(String query, Index<Node> index) {
	
    	float minIdentity = getMinIdentity(query);

    	// fuzzy match names against ALL within-context taxa and synonyms
    	FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("name", query), minIdentity);
        IndexHits<Node> hits = null;
        try {
        	hits = index.query(fuzzyQuery);
        	
            for (Node hit : hits) {               
            	if (matchedNodes.contains(hit) == false) {
	            	matchedNodes.add(hit);
	                Taxon matchedTaxon = taxonomy.getTaxon(hit);
	                matches.addMatch(new TNRSHit()
	                        .setMatchedTaxon(matchedTaxon)
	                        .setRank(matchedTaxon.getRank())
	                        .setIsHomonym(isHomonym(matchedTaxon.getName()))
	                        .setIsDubious((Boolean) matchedTaxon.getNode().hasProperty(TaxonomyProperty.DUBIOUS.propertyName())));
            	}
            }
        } finally {
        	hits.close();
        }
    }
    
    /**
     * Just check if this name is a homonym (i.e. is an exact match to two or more taxa within the context).
     * Remembers results in the homonyms HashSet for speed.
     * @param name
     * @return
     */
    private boolean isHomonym(String name) {

    	boolean isHomonym = false;

    	if (homonyms.containsKey(name)) {
    		isHomonym = homonyms.get(name);

    	} else {
	    	IndexHits<Node> hits = null;
	    	try {
	    		hits = (includeDubious ? taxNodesByName : prefTaxNodesByName).query("name", name);
		        if (hits.size() > 1) {
		            isHomonym = true;
		        }
		        homonyms.put(name, isHomonym);
	    	} finally {
	    		hits.close();
	    	}
    	}
    	
    	return isHomonym;
    }
}
