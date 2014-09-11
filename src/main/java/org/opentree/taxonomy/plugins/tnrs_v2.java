package org.opentree.taxonomy.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.lucene.queryParser.ParseException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.OTRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.TNRSResultsRepresentation;
import org.opentree.properties.OTPropertyPredicate;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.graphdb.GraphDatabaseAgent;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.contexts.ContextDescription;
import org.opentree.taxonomy.contexts.ContextGroup;
import org.opentree.taxonomy.contexts.ContextNotFoundException;
import org.opentree.taxonomy.contexts.TaxonomyContext;
import org.opentree.tnrs.ContextResult;
import org.opentree.tnrs.TNRSMatch;
import org.opentree.tnrs.TNRSMatchSet;
import org.opentree.tnrs.TNRSNameResult;
import org.opentree.tnrs.TNRSResults;
import org.opentree.tnrs.queries.MultiNameContextQuery;
import org.opentree.tnrs.queries.SingleNamePrefixQuery;

public class tnrs_v2 extends ServerPlugin {

	public static int MAX_QUERY_STRINGS = 1000;
	    
    @Description("Taxonomic contexts are available to limit the scope of TNRS searches. These contexts correspond to uncontested higher "
    		+ "taxa such as 'Animals' or 'Land plants'. This service returns a list containing all available taxonomic context "
    		+ "names, which may be used as input (via the `context_name` parameter) to limit the search scope of other services including "
    		+ "[match_names](#match_names) and [autocomplete_name](#autocomplete_name).")
    @PluginTarget(GraphDatabaseService.class)
    public Representation contexts(
            @Source GraphDatabaseService graphDb) throws IOException {
                
        // format is: HashMap<groupName, ArrayList<contextName>>
        HashMap<String, ArrayList<String>> contexts = new HashMap<String, ArrayList<String>>();

        for (ContextGroup group : ContextGroup.values()) {

            // get all the contexts for each context group
            ArrayList<String> subcontexts = new ArrayList<String>();
            for (ContextDescription cd : group.getDescriptions()) {
                subcontexts.add(cd.name);
            }
            contexts.put(group.toString(), subcontexts);
        }
        
        return OTRepresentationConverter.convert(contexts);
    }
	
    @Description("Find the least inclusive [taxonomic context](#contexts) that includes all the unambiguous names in the input set. "
    		+ "Unambiguous names are names with exact matches to non-homonym taxa. Ambiguous names (those *without* exact matches to "
    		+ "non-homonym taxa) are indicated in results.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation infer_context(
            @Source GraphDatabaseService graphDb,
            @Description("An array of taxon names to be queried.")
    			@Parameter(name="names", optional = false) String[] names) {

    	// initialize objects
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);
        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);

        // format input for the TNRS query object
    	Map<Object, String> idNameMap = new HashMap<Object, String>();
		for (String name : names) {
			idNameMap.put(name, name);
		}

		// call TNRS query, remembering names that could not be matched
		Collection<String> namesNotMatched = tnrs.setSearchStrings(idNameMap).inferContextAndReturnAmbiguousNames().values();

        // get the inferred context
        TaxonomyContext inferredContext = tnrs.getContext();

        // format results and finish
        ContextResult contextResult = new ContextResult(inferredContext, namesNotMatched);
        gdb.shutdownDb();

        return TNRSResultsRepresentation.getContextRepresentation(contextResult);
    	
    }

    @Description("Assumes the input is a taxon name that may be incomplete (i.e. the beginning of a taxon name such as 'Ast', "
    		+ "which would match 'Astelia', 'Astilbe', 'Aster', 'Asteroidea', 'Asteraceae', 'Astrantia', etc.). If the input "
    		+ "string is an exact string match to an existing taxon name, then only the exact match will be returned, (i.e. the "
    		+ "input 'Aster' will produce a single result 'Aster'). Name expansion will stop at whitespace, but spaces may be "
    		+ "included in the input, and indeed, they *must* be included in the input in order to match species names. For example, "
    		+ "'Garcinia' will only match the genus name 'Garcinia' itself, but 'Garcinia ' (note the trailing space) will match all "
    		+ "the species in the genus. Similarly, 'Garcinia m' will match all Garcinia species whose specific epithets start with "
    		+ "the letter 'm'."
    		+ "\n\n**IMPORTANT NOTE: This service should not be used for general purpose TNRS queries.** It is optimized for and "
    		+ "(obviously) intended for use *only* with autocomplete boxes on web forms. For all name matching purposes other than "
    		+ "autocompleting name fields on forms, use the [`match_names`](#match_names) service.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation autocomplete_name(@Source GraphDatabaseService graphDb,
    		
            @Description("A string containing a single name (or partial name prefix) to be queried against the db")
            @Parameter(name = "name")
            String queryString,
    		
            @Description("The name of the [taxonomic context](#contexts) to be searched")
            @Parameter(name = "context_name", optional = true)
            String contextName,
    		
            @Description("A boolean indicating whether or not suppressed taxa should be included in the results. Defaults to false "
            		+ "(suppressed taxa are not included).")
            @Parameter(name="include_dubious", optional = true)
            Boolean includeDubious) {

    	includeDubious = includeDubious == null ? false : includeDubious;
    	
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);
        HashMap<String, Object> errorResults = new HashMap<String, Object>();
        
        // attempt to get the named context, will throw exception if a name is supplied but no corresponding context can be found
        TaxonomyContext context = null;
        if (contextName != null) {
        	try {
        		context = taxonomy.getContextByName(contextName);
        	} catch (ContextNotFoundException ex) {
        		errorResults.put("error", "The context '" + contextName + " could not be found.");
        		return OTRepresentationConverter.convert(errorResults);
        	}
        }

        SingleNamePrefixQuery query = new SingleNamePrefixQuery(taxonomy, context);
        query.setIncludeDubious(includeDubious);
        TNRSResults results = query.setQueryString(queryString).runQuery().getResults();

        // return results if they exist
        if (results.iterator().hasNext()) {

        	List<TNRSMatch> matches = results.iterator().next().getMatches().getMatchList();
        	
            if (! matches.isEmpty()) {

            	Collections.sort(matches, new MatchComparator());

            	return TNRSResultsRepresentation.getMatchSetRepresentationForAutocompleteBox(matches.iterator());
            }
        }
        
        // else just return an empty JSON array
        return OTRepresentationConverter.convert(new LinkedList<String>());
    }
    
    @Description("Accepts one or more taxonomic names and returns information about potential matches for these names to known taxa in "
    		+ "OTT. This service uses taxonomic contexts to disambiguate homonyms and misspelled names; a context may be specified using "
    		+ "the `context_name` parameter. [Taxonomic contexts](#contexts) are uncontested higher taxa that have been selected to "
    		+ "allow limits to be applied to the scope of TNRS searches (e.g. 'match names only within flowering plants').\n\nIf no "
    		+ "context is specified, then the shallowest taxonomic context that contains all unambiguous names in the input set will be "
    		+ "used. A name is considered unambiguous if it is not a synonym and has only one exact match to any taxon name in the "
    		+ "entire taxonomy. Once a context has been identified (either user-specified or inferred), all taxon name matches will "
    		+ "performed only against taxa within that context.\n\nFor a list of available taxonomic contexts, see the "
    		+ "[contexts](#contexts) service.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation match_names(
            @Source GraphDatabaseService graphDb,

            @Description("The name of the taxonomic context to be searched")
            	@Parameter(name = "context_name", optional = true) String contextName,
        	@Description("An array of taxon names to be queried.")
        		@Parameter(name="names", optional = false) String[] names,
        	@Description("An array of ids to use for identifying names. These will be assigned to each name in the `names` array. If `ids` is provided, then `ids` and `names` must be identical in length.")
    			@Parameter(name="ids", optional = true) String[] ids,
        	@Description("A boolean indicating whether or not to include deprecated taxa in the search.")
    			@Parameter(name="include_deprecated", optional = true) Boolean includeDeprecated,
    		@Description("A boolean indicating whether or not to perform approximate string (a.k.a. \"fuzzy\") matching. Will greatly improve speed if this is turned OFF (false). By default, however, it is on (true).")
            	@Parameter(name="do_approximate_matching", optional = true) Boolean doFuzzyMatching,
    		@Description("Whether to include so-called 'dubious' taxa--those which are not accepted by OTT.")
            	@Parameter(name="include_dubious", optional=true) Boolean includeDubious) {
    	
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);

        // including deprecated and dubious names are turned OFF by default
        includeDeprecated = includeDeprecated == null ? false : includeDeprecated;
        includeDubious = includeDubious == null ? false : includeDubious;

        // fuzzy matching is turned ON by default
        doFuzzyMatching = doFuzzyMatching == null ? true : doFuzzyMatching;

        HashMap<String, Object> errorResults = new HashMap<String, Object>();
        if (ids == null) {
        	ids = names;

        } else if (ids.length != names.length) {
        	errorResults.put("error", "The number of names and ids does not match. If you provide ids, then you "
        			+ "must provide exactly as many ids as names.");
			return OTRepresentationConverter.convert(errorResults);
        
        } else {
        	HashSet<String> idSet = new HashSet<String>();
        	for (String id : ids) {
        		if (idSet.contains(id)) {
        			errorResults.put("error", "The id " + id + " is not unique. If you provide ids, they must be unique.");
        			return OTRepresentationConverter.convert(errorResults);
        		}
        		idSet.add(id);
        	}
        }
        
        Map<Object, String> idNameMap = new HashMap<Object, String>();
        
        for (int i = 0; i < names.length; i++) {
        	idNameMap.put(ids[i], names[i]);
        }
                
    	if (idNameMap.keySet().size() > MAX_QUERY_STRINGS) {
    		errorResults.put("error", "Queries containing more than "+MAX_QUERY_STRINGS+" strings are not supported. You may submit multiple"
    				+ "smaller queries to avoid this limit.");
            return OTRepresentationConverter.convert(errorResults);
    	}
    	
        // attempt to get the named context, will throw exception if a name is supplied but no corresponding context can be found
        TaxonomyContext context = null;
        boolean useAutoInference = true;
        if (contextName != null) {
        	try {
        		context = taxonomy.getContextByName(contextName);
        	} catch (ContextNotFoundException ex) {
        		errorResults.put("error", "The context '" + contextName + " could not be found.");
        		return OTRepresentationConverter.convert(errorResults);
        	}
        	useAutoInference = false;
        }

        MultiNameContextQuery mncq = new MultiNameContextQuery(taxonomy);
        TNRSResults results = mncq
        		.setSearchStrings(idNameMap)
        		.setContext(context)
        		.setAutomaticContextInference(useAutoInference)
        		.setIncludeDubious(includeDubious)
        		.setIncludeDeprecated(includeDeprecated)
        		.setDoFuzzyMatching(doFuzzyMatching)
        		.runQuery()
        		.getResults();

        gdb.shutdownDb();
        return TNRSResultsRepresentation.getResultsRepresentation(results);
    }
    
    /**
	 * A small custom comparator to facilitate sorting matches for the autocomplete box.
	 * @author cody
	 */
	private static class MatchComparator implements Comparator<TNRSMatch> {
		@Override
		public int compare(TNRSMatch match1, TNRSMatch match2) {
			
			// sorts in reverse order: higher priority matches to lower indexes

			/*
			// exact matches are top priority
			if (match1.getIsPerfectMatch() == true && match2.getIsPerfectMatch() == false) {
				return -1;
			} else if (match1.getIsPerfectMatch() == false && match2.getIsPerfectMatch() == true) {
				return 1;
			} */
			
			// higher taxa are higher priority
			if (match1.getIsHigherTaxon() == true && match1.getIsHigherTaxon() == false) {
				return -1;
			} else if (match1.getIsHigherTaxon() == false && match1.getIsHigherTaxon() == true) {
				return 1;
			}

			// alphabetical order by name
			char[] name1 = match1.getUniqueName().toCharArray();
			char[] name2 = match2.getUniqueName().toCharArray();
			for (int i = 0; i < name1.length; i++) {
				
				// if name 1 contains all of name 2 as a prefix but is longer, it is lower priority
				if (i >= name2.length) {
					return 1;
				}
				
				// check the current letter
				if (name1[i] > name2[i]) {
					return 1;
				} else if (name1[i] < name2[i]) {
					return -1;
				}
			}
			
			// if name 2 contains all of name 1 as a prefix but is longer, it is lower priority
			if (name2.length > name1.length) {
				return -1;
			}
			
			// hits are identical (e.g. valid homonyms...)
			return 0;
		}
	}
}