package opentree.plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import opentree.ContextGroup;
import opentree.GraphDatabaseAgent;
import opentree.Taxonomy;
import opentree.ContextDescription;
import opentree.TaxonomyContext;
import opentree.tnrs.TNRSNameScrubber;
import opentree.tnrs.TNRSQuery;
import opentree.tnrs.TNRSResults;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.OpentreeRepresentationConverter;

public class TNRS extends ServerPlugin {

    @Description("Return information on potential matches to a search query")
    @PluginTarget(GraphDatabaseService.class)
    public Representation doTNRSForNames(
            @Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of names to be queried against the taxonomy db") @Parameter(name = "queryString") String queryString,
            @Description("A comma-delimited string of names to be queried against the taxonomy db") @Parameter(name = "contextName", optional = true) String contextName) {

        String[] searchStrings = queryString.split("\\s*\\,\\s*");
        GraphDatabaseAgent taxService = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(taxService);
        TaxonomyContext context = taxonomy.getContextByName(contextName);

        TNRSQuery tnrs = new TNRSQuery(taxonomy);
        HashSet<String> names = tnrs.stringArrayToHashset(searchStrings);
        TNRSResults results = tnrs.initialize(names, context).doFullTNRS();

        taxService.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
    }

    @Description("Return information on potential matches to a search query")
    @PluginTarget(GraphDatabaseService.class)
    public Representation doTNRSForTrees(
            @Source GraphDatabaseService graphDb,
            @Description("A string containing tree(s) in a format readable by the forester library")
                @Parameter(name = "treeString") String treeString,
            @Description("The name of the taxonomic context to use. May be omitted if not known")
                @Parameter(name = "contextName", optional = true) String contextName) throws IOException {

        // Write tree string to temp file for ParserUtils. This is a hack, it would be better to just feed the parser
        // the treeString directly, but all the appropriate methods seem to want files
        String tempFileName = "/tmp/tempTree"; // need to use timestamp to avoid overwriting on asynchronous requests
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
        String[] cleanNames = TNRSNameScrubber.scrubNames(phys[0].getAllExternalNodeNames());
        
        // connect to taxonomy db
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);
        TaxonomyContext context = taxonomy.getContextByName(contextName);

        // TEST
        System.out.println(cleanNames[0] + " " + context);

        // do TNRS
        TNRSQuery tnrs = new TNRSQuery(taxonomy);
        HashSet<String> names = tnrs.stringArrayToHashset(cleanNames);
        TNRSResults results = tnrs.initialize(names, context).doFullTNRS();

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
