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

public abstract class AbstractBaseQuery implements TNRSQuery {

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
    public AbstractBaseQuery(Taxonomy taxonomy) {
		clear();
		setDefaults();
		this.taxonomy = taxonomy;
		setContext(taxonomy.ALLTAXA);
	}

    /**
     * Initialize a new TNRSQuery. The context will be set to the passed `context`. If `context` is null, the context will be set to ALLTAXA.
     * @param taxonomy
     */
	public AbstractBaseQuery(Taxonomy taxonomy, TaxonomyContext context) {
		clear();
		setDefaults();
		this.taxonomy = taxonomy;
        setContext(context);
	}
	
    /**
     * Clear previous query.
     */
    @Override
    public abstract TNRSQuery clear();
    
    /**
     * Set parameters to default values.
     */
    @Override
	public abstract TNRSQuery setDefaults();
	
	/**
	 * Returns the currently set context.
	 * @return
	 */
	@Override
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
    		setContext(taxonomy.ALLTAXA);

    	} else { // we found at least one exact match
    		TaxonSet ts = new TaxonSet(tempExactMatches);
    		setContext(ts.getLICA().getLeastInclusiveContext());
    	}
    	
		return getContext();
    }

	/**
	 * Set the context to `context`. If `context` is null, the context will be set to ALLTAXA.
	 * @param context
	 */
    @Override
	public TNRSQuery setContext(TaxonomyContext c) {
		if (c == null) {
			context = taxonomy.ALLTAXA;
		} else {
			context = c;
		}
		throw new IllegalStateException(context.toString());
//		results.setContextName(context.getDescription().name);
//	    results.setGoverningCode(context.getDescription().nomenclature.code);
        
//        return this;
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
}
