package opentree.tnrs;

import java.util.HashSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import opentree.NodeIndexDescription;
import opentree.Taxon;
import opentree.Taxonomy;
import opentree.TaxonomyContext;

public class SimpleQuery extends TNRSQuery {

	public SimpleQuery(Taxonomy taxonomy) {
		super(taxonomy);
	}
	
	public SimpleQuery(Taxonomy taxonomy, TaxonomyContext context) {
		super(taxonomy, context);
	}

	/*
     * Returns *ONLY* exact matches to a single string `searchString`; the context is determined automatically.
     *         
     * @param searchStrings
     * @return results
     * 
     *
    public TNRSResults matchExact(String searchString) {
        HashSet<String> name = new HashSet<String>();
        name.add(searchString);
        return matchExact(name, null);
    } */
    
    /**
     * Returns *ONLY* exact matches to `searchStrings` within `context`.
     *         
     * @param searchStrings
     * @param context
     * @return results
     * @throws MultipleHitsException 
     * 
     */
    public TNRSResults matchExact(String searchString) {
        
//        initialize(searchStrings, context);

//        for (String name : queriedNames) {
//            System.out.println(name);
//        }
        
    	results = new TNRSResults();
        // match names against context
//        HashSet<String> namesWithoutDirectTaxnameMatches = new HashSet<String>();
//        getExactTaxonMatches(queriedNames, namesWithoutDirectTaxnameMatches);

    	// TODO: make sure the spaces are escaped appropriately
    	IndexHits<Node> hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).query("name", searchString); //.replace(" ", "\\ "));

         try {
        	 // at least 1 hit; prepare to record matches
        	 TNRSMatchSet matches = new TNRSMatchSet();

        	 for (Node hit : hits) {
                // add this match to the match set
                Taxon matchedTaxon = taxonomy.getTaxon(hit);
                matches.addMatch(new TNRSHit().setMatchedTaxon(matchedTaxon));
        	 }

        	 // add matches to the TNRS results
            results.addNameResult(new TNRSNameResult(searchString, matches));
	            
         } finally {
         	hits.close();
         }
    	
        // record the names that couldn't be matched
//        for (String name : namesWithoutDirectTaxnameMatches) {
//	           results.addUnmatchedName(name);
//        }
        
        return results;
    }	
}
