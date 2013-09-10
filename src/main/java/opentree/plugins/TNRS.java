package opentree.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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

    @Description("Find the least inclusive taxonomic context defined for the provided set of taxon names")
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
    
    @Description("Find the least inclusive taxonomic context defined for the provided set of taxon names")
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

        SingleNamePrefixQuery snpq = new SingleNamePrefixQuery(taxonomy, context);
        TNRSResults results = snpq.setQueryString(queryString).runQuery().getResults();
 
        Iterator<TNRSNameResult> nameResultIter = results.iterator();
        // return results if they exist, otherwise spoof an empty match set to return (should just return a blank list)
        if (nameResultIter.hasNext()) {
        	return TNRSResultsRepresentation.getMatchSetRepresentationForAutocompleteBox(nameResultIter.next().getMatches());
        } else {
        	return OpentreeRepresentationConverter.convert(new LinkedList<TNRSMatch>());
        }
    }

    /*
    @Description("DEPRECATED. An alias for `contextQueryForNames`, left in for compatibility only. Use `contextQueryForNames` instead.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation doTNRSForNames(
            @Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of taxon names to be queried against the taxonomy db") @Parameter(name = "queryString") String queryString,
            @Description("The name of the taxonomic context to be searched") @Parameter(name = "contextName", optional = true) String contextName) throws ContextNotFoundException {

    	return contextQueryForNames(graphDb, queryString, contextName);
    }
    */
    
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

    /**
     * Deprecated. Never used. Use contextQueryForNames instead.
     * 
     * @param graphDb
     * @param treeString
     * @return
     * @throws IOException
     * @throws ContextNotFoundException
     */
/*    @Description("Return information on potential matches to a search query")
    @PluginTarget(GraphDatabaseService.class)
    @Deprecated
    public Representation doTNRSForTrees(
            @Source GraphDatabaseService graphDb,
            @Description("A string containing tree(s) in a format readable by the forester library")
                @Parameter(name = "treeString") String treeString/*,
            @Description("The name of the taxonomic context to use. May be omitted if not known")
                @Parameter(name = "contextName", optional = true) String contextName*) throws IOException, ContextNotFoundException {

        // Write tree string to temp file for ParserUtils. This is a hack, it would be better to just feed the parser
        // the treeString directly, but all the appropriate methods seem to want files
        String tempFileName = "/tmp/tempTree"; // TODO: need to use timestamp to avoid overwriting on asynchronous requests
        FileWriter fstream = new FileWriter(tempFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(treeString);
        out.close();

        // read in the treefile
        final File treefile = new File(tempFileName);

        PhylogenyParser parser = null;
        try {
            parser = ParserUtils.createParserDependingOnFileType(treefile, true);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        Phylogeny[] phys = null;

        try {
            phys = PhylogenyMethods.readPhylogenies(parser, treefile);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        // clean the names extracted from the treefile
        String[] cleanNames = TNRSNameScrubber.scrubBasic(phys[0].getAllExternalNodeNames());
        
        // connect to taxonomy db
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);

        // attempt to get the named context, will throw exception if a name is supplied but no corresponding context can be found
//        TaxonomyContext context = taxonomy.getContextByName(contextName);

        // do TNRS
        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);
        HashSet<String> names = Utils.stringArrayToHashset(cleanNames);
        TNRSResults results = tnrs.
        		setSearchStrings(names).
        		runQuery().
        		getResults();

        gdb.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
    } */

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
