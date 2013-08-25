package opentree.tnrs.queries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import opentree.ContextDescription;
import opentree.NodeIndexDescription;
import opentree.RelType;
import opentree.Taxon;
import opentree.TaxonSet;
import opentree.Taxonomy;
import opentree.TaxonomyContext;
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
    
    private final int DEFAULT_MIN_LENGTH_FOR_PREFIX_QUERY = 4;
    
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
    	this.queryString = QueryParser.escape(queryString).toLowerCase();    	
    	return this;
    }

    /**
     * Clear the results and search strings from the previous query. Also called by the constructor to initialize the object.
     */
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
     * Perform a simple query optimized for the autocomplete box on the opentree website.
     * 
     * @return
     * @throws ParseException 
     */
    @Override
    public SingleNamePrefixQuery runQuery() throws ParseException {
        
    	matches = new TNRSMatchSet(taxonomy);

    	if (queryString.length() < minLengthForQuery) {
    		return this;
    	}
    	
    	getExactNameOrSynonymMatches();
    
    	if (queryString.length() >= minLengthForPrefixQuery) {
/*	        // only attempt prefix queries on full genus names if the string is short
        	getPrefixNameOrSynonymMatches(QueryParser.escape(queryString.concat(" ")));
    	} else { */
        	getPrefixNameOrSynonymMatches();
    	}

    	// only do fuzzy queries if we haven't matched anything: they are slow!
		if (matches.size() < 1) {
    		// attempt fuzzy query
    		getApproxNameOrSynonymMatches();
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
     * Search for exact taxon name or synonym matches to names in `searchStrings` using the context that is set.
     * 
     * @param searchStrings
     */
    private void getExactNameOrSynonymMatches() {

    	TermQuery simpleQuery = new TermQuery(new Term("name", queryString));
    	IndexHits<Node> hits = null;
    	try {
    		hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM).
    				query(simpleQuery);

            boolean isHomonym = false;
            if (hits.size() > 1) {
                isHomonym = true;
            }

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
     * Attempt to find prefix query matches against the search string.
     * @throws ParseException 
     */
    private void getPrefixNameOrSynonymMatches() throws ParseException {
    	
    	PrefixQuery prefixQuery = new PrefixQuery(new Term("name", queryString));

    	IndexHits<Node> hits = null;
    	try {
    		hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM).
    				query(prefixQuery);

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
    private void getApproxNameOrSynonymMatches() {
	
    	float minIdentity = getMinIdentity(queryString);

    	// fuzzy match names against ALL within-context taxa and synonyms
    	FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("name", queryString), minIdentity);
        IndexHits<Node> hits = null;
        try {
        	hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM).
            query(fuzzyQuery);
        	
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
