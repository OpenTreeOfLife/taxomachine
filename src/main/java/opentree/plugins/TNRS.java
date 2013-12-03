package opentree.plugins;

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

import opentree.taxonomy.GraphDatabaseAgent;
import opentree.taxonomy.Taxonomy;
import opentree.taxonomy.contexts.ContextDescription;
import opentree.taxonomy.contexts.ContextGroup;
import opentree.taxonomy.contexts.ContextNotFoundException;
import opentree.taxonomy.contexts.TaxonomyContext;
import opentree.tnrs.ContextResult;
import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSResults;
import opentree.tnrs.queries.MultiNameContextQuery;
import opentree.tnrs.queries.SingleNamePrefixQuery;

import org.apache.lucene.queryParser.ParseException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.OpentreeRepresentationConverter;
import org.neo4j.server.rest.repr.TNRSResultsRepresentation;

public class TNRS extends ServerPlugin {

    @Description("Find the least inclusive taxonomic context defined for the provided set of taxon names.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getContextForNames(
            @Source GraphDatabaseService graphDb,
/*            @Description("A comma-delimited string of taxon names to be queried against the taxonomy db")
            	// TODO: convert this to accept a JSON array
            	@Parameter(name = "queryString", optional = false) String queryString) { */
            @Description("An array of taxon names to be queried.")
    			@Parameter(name="names", optional = false) String[] names) {

    	Map<Object, String> idNameMap = new HashMap<Object, String>();
    	
    	// TODO: convert this to accept an array
//        String[] searchStrings = queryString.split("\\s*\\,\\s*");

		for (String name : names) {
			idNameMap.put(name, name);
		}
    	
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);

        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);
        
        Collection<Object> nameIdsNotMatched = tnrs.setSearchStrings(idNameMap).inferContextAndReturnAmbiguousNames().keySet();
        if (nameIdsNotMatched.isEmpty()) {
        	nameIdsNotMatched = (Collection<Object>) new HashSet<Object>();
        }
        TaxonomyContext inferredContext = tnrs.getContext();

        // create a container to hold the results
        ContextResult contextResult = new ContextResult(inferredContext, nameIdsNotMatched);
        
        gdb.shutdownDb();
        
        // convert the results to JSON and return them
        return OpentreeRepresentationConverter.convert(contextResult);
    	
    }
    
    @Description("Find taxonomic names matching the passed in prefix. Optimized for use with autocomplete boxes on webforms.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation autocompleteBoxQuery(
            @Source GraphDatabaseService graphDb,
            @Description("A string containing a single name (or partial name prefix) to be queried against the db") @Parameter(name = "queryString") String queryString,
    		@Description("The name of the taxonomic context to be searched") @Parameter(name = "contextName", optional = true) String contextName) throws ContextNotFoundException, ParseException {

        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);
        
        // attempt to get the named context, will throw exception if a name is supplied but no corresponding context can be found
        TaxonomyContext context = null;
        if (contextName != null) {
        	context = taxonomy.getContextByName(contextName);
        }

        SingleNamePrefixQuery query = new SingleNamePrefixQuery(taxonomy, context);
        TNRSResults results = query.setQueryString(queryString).runQuery().getResults();

        // return results if they exist, 
        if (results.iterator().hasNext()) {

        	List<TNRSMatch> matches = results.iterator().next().getMatches().getMatchList();
        	
            if (!matches.isEmpty()) {

            	Collections.sort(matches, new MatchComparator());

            	return TNRSResultsRepresentation.getMatchSetRepresentationForAutocompleteBox(matches.iterator());
            }
        }
        
        // else just return an empty JSON array
        return OpentreeRepresentationConverter.convert(new LinkedList<TNRSMatch>());
    }

	/**
	 * A small custom comparator to facilitate sorting matches for the autocomplete box.
	 * @author cody
	 */
	private static class MatchComparator implements Comparator<TNRSMatch> {
		@Override
		public int compare(TNRSMatch match1, TNRSMatch match2) {
			
			// sorts in reverse order: higher priority matches to lower indexes

			// exact matches are top priority
			if ((Boolean) match1.getIsPerfectMatch() == true && (Boolean) match2.getIsPerfectMatch() == false) {
				return -1;
			} else if ((Boolean) match1.getIsPerfectMatch() == false && (Boolean) match2.getIsPerfectMatch() == true) {
				return 1;
			}
			
			// higher taxa are higher priority
			if ((Boolean) match1.getIsHigherTaxon() == true && (Boolean) match1.getIsHigherTaxon() == false) {
				return -1;
			} else if ((Boolean) match1.getIsHigherTaxon() == false && (Boolean) match1.getIsHigherTaxon() == true) {
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
    
    @Description("Return information on potential matches to a search query")
    @PluginTarget(GraphDatabaseService.class)
    public Representation contextQueryForNames(
            @Source GraphDatabaseService graphDb,

            @Description("A comma-delimited string of taxon names to be queried against the taxonomy db. This is an alternative to the use of the 'names' parameter")
            	@Parameter(name = "queryString", optional = true) String queryString,
            @Description("The name of the taxonomic context to be searched")
            	@Parameter(name = "contextName", optional = true) String contextName,
        	@Description("An array of taxon names to be queried. This is an alternative to the use of the 'queryString' parameter")
        		@Parameter(name="names", optional = true) String[] names,
        	@Description("An array of ids to use for identifying names. These will be set in the id field of each name result. If this parameter is used, ids will be treated as strings.")
    			@Parameter(name="idStrings", optional = true) String[] idStrings,
        	@Description("An array of ids to use for identifying names. These will be set in the id field of each name result. If this parameter is used, ids will be treated as ints.")
    			@Parameter(name="idInts", optional = true) Long[] idInts,
    		@Description("Whether to include so-called 'dubious' taxa--those which are not accepted by OTT.")
            	@Parameter(name="includeDubious", optional=true) String includeDubiousStr) throws ContextNotFoundException {
    	
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);

        // check valid input on names
        String[] searchStrings = null;
        if (queryString != null && names == null) {
	        searchStrings = queryString.split("\\s*\\,\\s*");
        } else if (names != null && queryString == null) {
        	searchStrings = names;
        } else {
    		throw new IllegalArgumentException("You must provide exactly one of either the 'queryString' or 'names' parameters");
    	}
        
        // check valid input on ids
        Object[] ids = null;
        if (idInts != null && idStrings == null) {

        	ids = new Object[idInts.length];
        	int i=0;
        	for (Long id : idInts) {
        		ids[i++] = id;
        	}

        } else if (idStrings != null && idInts == null) { 
        	ids = idStrings;
        	
        } else if (idStrings != null && idInts != null) {
    		throw new IllegalArgumentException("You may provide at most one of the 'idStrings' an 'idInts' parameters");

        } else {
        	ids = searchStrings;
        }
        
        Map<Object, String> idNameMap = new HashMap<Object, String>();
    	if (ids != null) {
    		int i = 0;
            for (String name : searchStrings) {
            	try {
            		idNameMap.put(ids[i++], name);
            	} catch (ArrayIndexOutOfBoundsException ex) {
            		throw new IllegalArgumentException("There are more names than ids");
            	}
            }
            if (ids.length > i) {
        		throw new IllegalArgumentException("There are more ids than names");
            }
    	} else {
    		for (String name : searchStrings) {
    			idNameMap.put(name, name);
    		}
    	}
        
        // attempt to get the named context, will throw exception if a name is supplied but no corresponding context can be found
        TaxonomyContext context = null;
        boolean useAutoInference = true;
        if (contextName != null) {
        	context = taxonomy.getContextByName(contextName);
        	useAutoInference = false;
        }

        // parse the include dubious. we use a string because we can't do a null comparison on a boolean
        boolean includeDubious = false;
        if (includeDubiousStr != null) {
        	if (includeDubiousStr.equalsIgnoreCase("true")) {
        		includeDubious = true;
        	} else if (includeDubiousStr.equalsIgnoreCase("false")) {
        		includeDubious = false;
        	} else {
        		throw new IllegalArgumentException("The includeDubious parameter may only be set to 'true' or 'false'.");
        	}
        }

        MultiNameContextQuery mncq = new MultiNameContextQuery(taxonomy);
//        HashSet<String> namesSet = Utils.stringArrayToHashset(searchStrings);
        TNRSResults results = mncq.
        		setSearchStrings(idNameMap).
        		setContext(context).
        		setAutomaticContextInference(useAutoInference).
        		setIncludeDubious(includeDubious).
        		runQuery().
        		getResults();

        gdb.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
    }

    @Description("Return information on available taxonomic contexts")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getContextsJSON(
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
        
        return OpentreeRepresentationConverter.convert(contexts);
    }
}
