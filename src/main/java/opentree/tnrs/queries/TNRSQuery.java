package opentree.tnrs.queries;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import opentree.Taxon;
import opentree.TaxonSet;
import opentree.Taxonomy;
import opentree.TaxonomyContext;
import opentree.tnrs.TNRSResults;

public abstract class TNRSQuery {

	// used for fuzzy matches
    protected static final double DEFAULT_MIN_SCORE = 0.01;
    protected static final double PERFECT_SCORE = 1;
    protected static final int SHORT_NAME_LENGTH = 9;
    protected static final int MEDIUM_NAME_LENGTH = 14;
    protected static final int LONG_NAME_LENGTH = 19;
    protected static final String DEFAULT_TAXONOMY_NAME = "ottol";
    protected double minScore;
	
    // essential container variables and objects
	protected Taxonomy taxonomy;
    protected Index<Node> prefTaxNodesByName;
    protected TaxonomyContext context;
    protected TNRSResults results;
    
    /**
     * Initialize a new TNRSQuery. The context will be set to ALLTAXA.
     * @param taxonomy
     */
    public TNRSQuery(Taxonomy taxonomy) {
		initialize();
		setContextAbstract(taxonomy.ALLTAXA);
	}

    /**
     * Initialize a new TNRSQuery. The context will be set to the passed `context`. If `context` is null, the context will be set to ALLTAXA.
     * @param taxonomy
     */
	public TNRSQuery(Taxonomy taxonomy, TaxonomyContext context) {
		initialize();
        setContextAbstract(context);
	}
	
	/**
	 * Returns the currently set context.
	 * @return
	 */
	public TaxonomyContext getContext() {
		return context;
	}
	
    /**
     * Will infer the taxonomic context of the provided set of names using exact matches to taxon names.
     * The resulting TaxonomyContext object is remembered internally and is also returned.
     */
    public TaxonomyContext inferContext(Set<String> names) {
    	
    	HashSet<Taxon> tempExactMatches = new HashSet<Taxon>();
    	
    	for (String thisName : names) {

    		// attempt to find exact matches within the ALLTAXA context
            IndexHits<Node> hits = prefTaxNodesByName.query("name", thisName.replace(" ", "\\ "));
            try {
	            if (hits.size() == 1) { // an exact match
	                Taxon matchedTaxon = taxonomy.getTaxon(hits.getSingle());
	                tempExactMatches.add(matchedTaxon);
	            }
            } finally {
            	hits.close();
            }
        }
        
    	if (tempExactMatches.isEmpty()) { // there were not exact name matches, so we have no context
    		setContextAbstract(taxonomy.ALLTAXA);

    	} else { // we found at least one exact match
    		TaxonSet ts = new TaxonSet(tempExactMatches);
    		setContextAbstract(ts.getLICA().getLeastInclusiveContext());
    	}
    	
		return getContext();
    }

    /*
     * Returns a boolean indicating whether the context has been intentionally set using either inferContext() or setContext().
     * If the context has not been set, then it will be the default ALLTAXA context. This method is used by various searching
     * methods that will attempt to infer a context if it has not already been intentionally set.
     * 
     * @return
     *
    public boolean contextIsDefault() {
    	return contextIsDefault;
    } */
    
	// several different very simple queries follow

	/*
     * Returns *ONLY* exact matches to a single string `searchString` using the ALLTAXA context.
     *         
     * @param searchStrings
     * @return results
     * 
     *
    public TNRSResults matchExact(String searchString) {
        HashSet<String> name = new HashSet<String>();
        name.add(searchString);
        return matchExact(name, taxonomy.ALLTAXA);
    } */
    
    /*
     * Returns *ONLY* exact matches to `searchStrings` using the context `context`.
     *         
     * @param searchStrings
     * @param context
     * @return results
     * 
     *
    public TNRSResults matchExact(Set<String> searchStrings, TaxonomyContext context) {
        
//        initialize(searchStrings, context);
    	setSearchStrings(searchStrings);
    	setContext(context);

        for (String name : queriedNames) {
            System.out.println(name);
        }
        
        // match names against context
        HashSet<String> namesWithoutDirectTaxnameMatches = new HashSet<String>();
        getExactTaxonMatches(queriedNames, namesWithoutDirectTaxnameMatches);

        // record the names that couldn't be matched
        for (String name : namesWithoutDirectTaxnameMatches) {
            results.addUnmatchedName(name);
        }
        
        return results;
    } */

	/**
	 * Set the context to `context`. If `context` is null, the context will be set to ALLTAXA. Protected because it should be wrapped in
	 * an individual method in non-abstract query classes, which should return the query object. See MultiNameContextQuery or SimpleQuery
	 * for examples.
	 * @param context
	 */
	protected void setContextAbstract(TaxonomyContext context) {
		if (context == null) {
			this.context = taxonomy.ALLTAXA;
		} else {
			this.context = context;
		}
		results.setContextName(this.context.getDescription().name);
        results.setGoverningCode(context.getDescription().nomenclature.code);
	}
	
    /**
     * Returns a minimum identity score used for fuzzy matching that limits the number of edit differences
     * based on the length of the string. 
     * 
     * @param name
     * @return minIdentity
     */
    protected float getMinIdentity(String name) {
        
        float ql = name.length();

        int maxEdits = 4; // used for names longer than LONG_NAME_LENGTH

        if (ql < SHORT_NAME_LENGTH)
            maxEdits = 1;
        else if (ql < MEDIUM_NAME_LENGTH)
            maxEdits = 2;
        else if (ql < LONG_NAME_LENGTH)
            maxEdits = 3;
            
        return (ql - (maxEdits + 1)) / ql;
    }
    
    /**
     * Initialize containers and essential variables. Called by constructors.
     */
	private void initialize() {
        minScore = DEFAULT_MIN_SCORE; // TODO: make it possible for client to set this
        results = new TNRSResults();
        context = null;
	}
}
