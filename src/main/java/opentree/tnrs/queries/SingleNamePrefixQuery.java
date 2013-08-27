package opentree.tnrs.queries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
    
    private int minLengthForPrefixQuery;
    private final int minLengthForQuery = 2;
    
    private final int DEFAULT_MIN_LENGTH_FOR_PREFIX_QUERY = 5;
    
    private Index<Node> prefTaxNodesByNameHigher;
	private Index<Node> prefTaxNodesByNameOrSynonymHigher;
    private Index<Node> prefTaxNodesByNameGenera;
    private Index<Node> prefTaxNodesByNameSpecies;
    private Index<Node> prefTaxNodesByNameOrSynonym;
	private Index<Node> prefTaxNodesBySynonym;
	
    public SingleNamePrefixQuery(Taxonomy taxonomy) {
    	super(taxonomy);
    }
    
    public SingleNamePrefixQuery(Taxonomy taxonomy, TaxonomyContext context) {
    	super(taxonomy, context);
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
     * Set the context to be used by this query.
     */
    @Override
    public SingleNamePrefixQuery setContext(TaxonomyContext context) {
    	super.setContext(context);
        prefTaxNodesByNameHigher = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_HIGHER);
    	prefTaxNodesByNameOrSynonymHigher = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM_HIGHER);
        prefTaxNodesByNameGenera = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_GENERA);
        prefTaxNodesByNameSpecies = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_SPECIES);
        prefTaxNodesByNameOrSynonym = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
        prefTaxNodesBySynonym = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_SYNONYM);
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
	 * A query tailored for autocomplete services.
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

    		String[] parts = queryString.split("\\s+",1);

    		if (parts.length > 1) { // If the search string contains two words:

    			// Hit it against the species index.
    			getExactMatches(escapedQuery, prefTaxNodesByNameSpecies);
    			
    			if (matches.size() < 1) { // no exact hit against the species index
	    			
	    			// Hit the first word against the genus name index
	    			getExactMatches(QueryParser.escape(parts[0]), prefTaxNodesByNameGenera);

	    			if (matches.size() > 0) { // there was an exact match against the genus index
	    				
	    				// return all the species for this genus
	    				throw new IllegalArgumentException("test: this is an exact match to the genus " + matches.iterator().next().getUniqueName() + ". its species list will be returned");

	    				
	    			} else { // no exact hit for first word against the genus index

	    				// Hit it against the synonym index and the higher taxon index
		    			getExactMatches(escapedQuery, prefTaxNodesBySynonym);
		    			getExactMatches(escapedQuery, prefTaxNodesByNameHigher);
		    			
	    				if (matches.size() < 1) {
	    					
	    					// Prefix query against the higher taxon index
	    					getPrefixMatches(escapedQuery, prefTaxNodesByNameOrSynonymHigher);

		    				if (matches.size() < 1) {

		    					// last resort: fuzzy match against entire index
			    				getApproxMatches(escapedQuery, prefTaxNodesByNameOrSynonym);
		    				}
	    				}
	    			}
    			}
	    		 
    		} else { // only contains one word
    			
    			// Hit the first word against the genus index
    			getExactMatches(QueryParser.escape(parts[0]), prefTaxNodesByNameGenera);
	    		
    			if (matches.size() > 0) {
	    			// If there is an exact hit, record all the species in this genus
    				throw new IllegalArgumentException("test: this is an exact match to the genus " + matches.iterator().next().getUniqueName() + ". its species list will be returned");

    			}

				getPrefixMatches(escapedQuery, prefTaxNodesByNameOrSynonymHigher);

				if (matches.size() < 1) {
					// If there are no results, do a fuzzy search against the higher taxon index
    				getApproxMatches(escapedQuery, prefTaxNodesByNameOrSynonymHigher);
				}
    		}
    		
    	} else { // does not contain a space at all

    		getExactMatches(escapedQuery, prefTaxNodesByNameOrSynonymHigher);

    		if (matches.size() < 1) {

    			// Do a prefix query against the higher taxon index
    			getPrefixMatches(escapedQuery, prefTaxNodesByNameHigher);

    			if (matches.size() < 1) {

    				// Do a prefix query against the all taxa synonym index
    				getPrefixMatches(escapedQuery, prefTaxNodesBySynonym);
    				
    				if (matches.size() < 1) {
    					getApproxMatches(escapedQuery, prefTaxNodesByNameOrSynonymHigher);
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
    public SingleNamePrefixQuery runQueryOld() throws ParseException {
        
    	matches = new TNRSMatchSet(taxonomy);

    	if (queryString.length() < minLengthForQuery) {
    		throw new IllegalArgumentException("cannot perform query on less than " + minLengthForQuery + " characters");
    	}
    	
    	getExactMatches(queryString, prefTaxNodesByNameOrSynonym);
    
    	if (queryString.length() >= minLengthForPrefixQuery) {
        	getPrefixMatches(queryString, prefTaxNodesByNameOrSynonym);
    	}

    	// only do fuzzy queries if we haven't matched anything: they are slow!
		if (matches.size() < 1) {
    		// attempt fuzzy query
    		getApproxMatches(queryString, prefTaxNodesByNameOrSynonym);
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
    private void getExactMatches(String query, Index<Node> index) {

    	TermQuery exactQuery = new TermQuery(new Term("name", query));
    	IndexHits<Node> hits = null;
    	try {
    		hits = index.query(exactQuery);

    		boolean isHomonym = hits.size() > 1 ? true : false;

            for (Node hit : hits) {
            	if (matchedNodes.contains(hit) == false) {
	            	matchedNodes.add(hit);
	                Taxon matchedTaxon = taxonomy.getTaxon(hit);
	                matches.addMatch(new TNRSHit().
	                        setMatchedTaxon(matchedTaxon).
	                        setRank(matchedTaxon.getRank()).
	                        setIsHomonym(isHomonym));
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
	                matches.addMatch(new TNRSHit().
	                        setMatchedTaxon(matchedTaxon).
	                        setRank(matchedTaxon.getRank()).
	                        setIsHomonym(isHomonym(matchedTaxon.getName())));
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
	                matches.addMatch(new TNRSHit().
	                        setMatchedTaxon(matchedTaxon).
	                        setRank(matchedTaxon.getRank()).
	                        setIsHomonym(isHomonym(matchedTaxon.getName())));
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
	    		hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).
	    				query("name", name);
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
