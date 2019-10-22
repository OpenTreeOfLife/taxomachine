package org.opentree.taxonomy;

import jade.tree.JadeTree;
import jade.tree.TreePrinter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
//import java.util.NoSuchElementException;





import java.util.Map;




//import org.apache.log4j.Logger;
import java.io.FileNotFoundException;

import org.apache.log4j.PropertyConfigurator;
//import org.forester.io.parsers.PhylogenyParser;
//import org.forester.io.parsers.util.ParserUtils;
//import org.forester.phylogeny.Phylogeny;
//import org.forester.phylogeny.PhylogenyMethods;
//import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.opentree.properties.OTVocabularyPredicate;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.opentree.taxonomy.contexts.ContextNotFoundException;
import org.opentree.taxonomy.contexts.TaxonomyContext;
import org.opentree.exceptions.MultipleHitsException;
import org.opentree.tnrs.TNRSMatch;
import org.opentree.tnrs.TNRSNameResult;
import org.opentree.tnrs.TNRSResults;
import org.opentree.tnrs.queries.MultiNameContextQuery;
import org.opentree.tnrs.queries.SimpleQuery;
import org.opentree.graphdb.GraphDatabaseAgent;

import java.io.BufferedReader;
import java.io.FileReader;

public class MainRunner {

    private static GraphDatabaseAgent taxdb;
    
    /*
     *  Initialize graph directly from OTT distribution files.
     *  Depends on the existence of the following files:
     *  1. taxonomy.tsv
     *  2. synonyms.tsv
     *  3. version.txt
     * Optional:
	 *  4. forwads.tsv  (synonyms and version should be optional as well)
     *  Will create a DB called 'ott_v[ottVersion].db' e.g. 'ott_v2.8draft5.db'
     *  Finally, it will build the contexts
    */
    // @returns 0 for success, 1 for poorly formed command, -1 for failure to complete well-formed command
    public int buildOTT(String [] args) throws FileNotFoundException, IOException, ContextNotFoundException {
        if (args.length != 2 && args.length != 3) {
            System.out.println("arguments should be: ott_directory [graph_name (defaults to 'ott_v[ottVersion].db')]");
            return 1;
        }
        
        String ottDir = args[1];
        
        if (args[1].endsWith("/") || args[1].endsWith("\\")) {
            ottDir = args[1].substring(0, args[1].length() - 1);
        }
        
        if (!new File(ottDir).exists()) {
            System.out.println("Directory '" + ottDir + "' not found. Exiting...");
            return -1;
        }
        
        String ottVersion = "";
        String taxFile = ottDir + File.separator + "taxonomy.tsv";
        String synFile = ottDir + File.separator + "synonyms.tsv";
        String aliasFile = ottDir + File.separator + "forwards.tsv";
        String versionFile = ottDir + File.separator + "version.txt";
        String graphName = "";
        
        // grab taxonomy version
        try {
            BufferedReader br = new BufferedReader(new FileReader(versionFile));
            ottVersion = br.readLine();
            br.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not open the file '" + versionFile + "'. Exiting...");
            return -1;
        } catch (IOException ioe) {
            
        }
        
        if (!ottVersion.isEmpty()) {
            ottVersion = "ott_v" + ottVersion;
            if (args.length == 3) {
                graphName = args[2];
            } else {
                graphName = ottVersion + ".db";
            }
        }
        
        // check if graph already exists. abort if it does to prevent overwriting.
        if (new File(graphName).exists()) {
            System.err.println("Graph database '" + graphName + "' already exists. Exiting...");
            return -1;
        }
        if (!new File(taxFile).exists()) {
            System.err.println("Could not open the file '" + taxFile + "'. Exiting...");
            return -1;
        }
        if (!new File(synFile).exists()) {
            System.err.println("Could not open the file '" + synFile + "'. Exiting...");
            return -1;
        }
        
        taxdb = new GraphDatabaseAgent(graphName);
        TaxonomyLoaderOTT tlo = new TaxonomyLoaderOTT(taxdb);
        Node lifeNode = tlo.getTaxonomyRootNode();
        System.out.println("Initializing ott taxonomy '" + ottVersion + "' DB from '"
            + taxFile + "' with synonyms in '" + synFile + "' to graphDB '" + graphName + "'.\n");
        
        // Load the taxonomy
        tlo.setAddSynonyms(true);
        tlo.setCreateOTTIdIndexes(true);
        tlo.setbuildPreferredIndexes(true);
        tlo.loadOTTIntoGraph(ottVersion, taxFile, synFile, aliasFile);
        
        // Build contexts
        TaxonomySynthesizer te = null;
        te = new TaxonomySynthesizer(taxdb);
        System.out.println("Building context-specific indexes (this can take a while).");
        te.makeContexts();
        
        taxdb.shutdownDb();
        
        return 0;
    }
    
	// Parse command line argument sequence

    public void taxonomyLoadParser(String[] args) throws FileNotFoundException, IOException {

        String graphname = "";
        String sourcename = "";
        String filename = "";
        String synonymfile = "";
        String aliasfile = null;
        if (args[0].equals("adddeprecated")) {
            if (args.length != 3) {
                System.out
                        .println("arguments should be: filename graphdbfolder\n");
                return;
            } else {
                filename = args[1];
                graphname = args[2];
            }
/*        } else if (args[0].equals("inittax") || args[0].equals("addtax")) {
            if (args.length != 4) {
                System.out.println("arguments should be: sourcename filename graphdbfolder\n or (for the inittax command only) you can also use:\n source-properties-file filename graphdbfolder");
                return;
            } else {
                sourcename = args[1];
                filename = args[2];
                graphname = args[3];
            } */
        } else if (args[0].equals("inittaxsyn") || args[0].equals("addtaxsyn") || args[0].equals("loadtaxsyn")) {
			switch(args.length) {
			case 6:
				aliasfile = args[4];
                graphname = args[5];
				break;
			case 5:
                graphname = args[4];
				break;
            default:
                System.out.println("arguments should be: sourcename filename synonymfile graphdbfolder");
                return;
            }
			sourcename = args[1];
			filename = args[2];
			synonymfile = args[3];
        }

        taxdb = new GraphDatabaseAgent(graphname);
        
        /*
        // ===================================== deprecated methods from days of preottol
        
        TaxonomyLoaderPreottol tld = new TaxonomyLoaderPreottol(taxdb);
        
        // currently we are assuming that we always want to add taxonomies to the root of the taxonomy
        // (i.e. the life node), but this will have to be changed to add taxonomies that are more
        // specific, such as Hibbett's fungal stuff
        TaxonomyLoaderOTT tld = new TaxonomyLoaderOTT(taxdb);
        Node lifeNode = tld.getLifeNode();
        System.out.println("life node: " + lifeNode);
        String incomingRootNodeId = null;
        if (lifeNode != null) {
        	incomingRootNodeId = String.valueOf(lifeNode.getId());
        }

        if (args[0].equals("inittax")) {
            System.out.println("initializing taxonomy from " + filename + " to " + graphname);
            if (new File(sourcename).exists()) {
                System.out.println("Sourcename \"" + sourcename + "\" is a file. This will be read as a properties file describing this source");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            } else {
                System.out.println("Sourcename \"" + sourcename + "\" is not a filepath. It will be treated as the source\'s name");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            }
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
        } else if (args[0].equals("addtax")) {
            System.out.println("adding taxonomy from " + filename + " to " + graphname);
            // tl.addAdditionalTaxonomyToGraph(sourcename, incomingRootNodeId ,filename, synonymfile);
            tld.addDisconnectedTaxonomyToGraph(sourcename, filename, synonymfile);
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
        } else if (args[0].equals("inittaxsyn")) {
            System.out.println("initializing taxonomy from " + filename + " and synonym file " + synonymfile + " to " + graphname);
            if (new File(sourcename).exists()) {
                System.out.println("Sourcename \"" + sourcename + "\" is a file. This will be read as a properties file describing this source");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            } else {
                System.out.println("Sourcename \"" + sourcename + "\" is not a filepath. It will be treated as the source\'s name");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            }
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
        } else if (args[0].equals("addtaxsyn")) {
            System.out.println("adding taxonomy from " + filename + "and synonym file " + synonymfile + " to " + graphname);
            // tl.addAdditionalTaxonomyToGraph(sourcename, incomingRootNodeId, filename,synonymfile);
            tld.addDisconnectedTaxonomyToGraph(sourcename, filename, synonymfile);
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
           */
            

        // ===================================== current methods using ott taxonomy from smasher
            
//        } else
        
        TaxonomyLoaderOTT tlo = new TaxonomyLoaderOTT(taxdb);
        Node lifeNode = tlo.getTaxonomyRootNode();
        System.out.println("life node: " + lifeNode);

        if (args[0].equals("loadtaxsyn")) {
            System.out.format("loading taxonomy %s from %s and synonym file %s to %s\n",
                              sourcename, filename, synonymfile, graphname);
            //this will create the ott relationships
            tlo.setAddSynonyms(true);
            tlo.setAddAliases(aliasfile != null);
            tlo.setCreateOTTIdIndexes(true);
            tlo.setbuildPreferredIndexes(true);
            tlo.loadOTTIntoGraph(sourcename, filename, synonymfile, aliasfile);

        } else if (args[0].equals("adddeprecated")) { 
            System.out.println("adding deprecated taxa from " + filename + " to " + graphname);
            tlo.loadDeprecatedTaxa(filename);
        
        // ================= other
            
        } else {
            System.err.println("\nERROR: not a known command");
            taxdb.shutdownDb();
            printHelp();
            System.exit(1);
        }
        taxdb.shutdownDb();
    }

    public void taxonomyQueryParser(String[] args) throws ContextNotFoundException {

        if (args[0].equals("getsubtree")) {
            if (args.length != 3) {
                System.out.println("arguments should be: graphdbfolder \"nameslist\"");
                return;
            }
        } else if (args[0].equals("checktree")) {
            if (args.length != 4) {
                System.out.println("arguments should be: treefile focalgroup graphdbfolder");
                return;
            }
/*        } else if (args[0].equals("comptaxgraph")) {
            if (args.length != 4) {
                System.out.println("arguments should be: comptaxgraph query graphdbfolder outfile");
                return;
            }
        } else if (args[0].equals("comptaxtree")) {
            if (args.length != 3) {
                System.out.println("arguments should be: comptaxtree query graphdbfolder");
                return;
            }
        } else if (args[0].equals("comptaxlist")) {
            if (args.length != 3) {
                System.out.println("arguments should be: comptaxlist query graphdbfolder");
                return;
            }
        } else if (args[0].equals("makeottol")) {
            if (args.length != 2) {
                System.out.println("arguments should be: graphdbfolder");
                return;
            }
        } else if (args[0].equals("dumpott") || args[0].equals("makeottolnamedump")) {
            if (args.length != 3) {
                System.out.println("arguments should be: graphdbfolder outfile");
                return;
            } */
        } else if (args[0].equals("makecontexts")) {
            if (args.length != 2) {
                System.out.println("arguments should be: graphdbfolder");
                return;
            }
        } else if (args[0].equals("makegenusindexes")) {
            if (args.length != 2) {
                System.out.println("arguments should be: graphdbfolder");
                return;
            }
        } else if (args.length != 3) {
            System.out.println("arguments should be: query graphdbfolder");
            return;
        }

        TaxonomySynthesizer te = null;
        SimpleQuery tnrs = null;
        Taxon taxon = null;

        if (args[0].equals("getsubtree")) {
            String graphname = args[1];
            String nameString = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            Taxonomy taxonomy = new Taxonomy(taxdb);
//            tnrs = new SimpleQuery(taxonomy);

            String[] names = nameString.split(",");
            LinkedList<Node> tNodes = new LinkedList<Node>();

            for (String taxName : names) {
                System.out.println("Searching for " + taxName);
                for (Node n : taxonomy.ALLTAXA.findTaxNodesByName(taxName)) {
                    System.out.println("adding " + n.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()) + " to taxon set");
                    tNodes.add(n);
                }
            }

            TaxonSet taxa = new TaxonSet(tNodes, taxonomy);
            JadeTree subTree = taxa.getPrefTaxSubtree();

            TreePrinter tp = new TreePrinter();
            System.out.println(tp.printNH(subTree));

/*        } else if (args[0].equals("comptaxtree")) {

            String query = args[1];
            String graphname = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There is more than one taxon with the name " + query);
            }

            System.out.println("constructing a comprehensive tax tree of " + query);
            taxon.buildTaxonomyTree();

        } else if (args[0].equals("comptaxlist")) {

            String query = args[1];
            String graphname = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("constructing a comprehensive tax tree of " + query);
            taxon.buildTreemachineTaxonList();

        } else if (args[0].equals("comptaxgraph")) {
            String query = args[1];
            String graphname = args[2];
            String outname = args[3];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("exporting the subgraph for clade " + query);
            taxon.exportGraphForClade(outname);

        } else if (args[0].equals("jsgraph")) {
            String query = args[1];
            String graphname = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("constructing json graph data for " + query);
            taxon.constructJSONGraph();

        } else if (args[0].equals("checktree")) {
            System.out.println("ERROR: this option is deprecated. use `tnrstree` option instead");
            System.out.println("\ttnrstree <treefile> <graphdbfolder> (check if the taxonomy graph contains names in treefile)");
            /*
             * String query = args[1]; String focalgroup = args[2]; String graphname = args[3]; te = new TaxonomyExplorer(graphname);
             * System.out.println("checking the names of " + query + " against the taxonomy graph"); te.checkNamesInTree(query,focalgroup);
             */

/*        } else if (args[0].equals("makeottol")) {
            String graphname = args[1];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("making ottol relationships");
            te.makePreferredOTTOLRelationshipsConflicts();
            te.makePreferredOTTOLRelationshipsNOConflicts();

        } else if (args[0].equals("makeottolnamedump")) {
            String graphname = args[1];
            String outfile = args[2];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("dumping names to: " + outfile);

            // testing
            te.OTTOLNameDump(te.ALLTAXA.findPrefTaxNodesByName("Viburnum").get(0), outfile);

            // te.makeOTTOLNameDump(te.ALLTAXA.getRootNode(), outfile); 

        } else if (args[0].equals("dumpott")) {
            String graphname = args[1];
            String outfile = args[2];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("dumping OTT relationships");
            te.dumpPreferredOTTRelationships(outfile);
            System.out.println("dumping OTT synonym relationships");
            te.dumpPreferredOTTSynonymRelationships(outfile + ".synonyms"); */
        } else if (args[0].equals("makecontexts")) {
            String graphname = args[1];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("building context-specific indexes");
            te.makeContexts();
        } else if (args[0].equals("makegenusindexes")) {
            String graphname = args[1];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("making species indexes by genus");
            te.makeGenericIndexes();
/*        } else if (args[0].equals("checknames")) {
            String sourcename = args[1];
            String graphname = args[2];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("checking names from source: " + sourcename);
            te.findEquivalentNamedNodes(sourcename); */
        } else {
            System.err.println("\nERROR: not a known command\n");
            printHelp();
            System.exit(1);
        } 
        taxdb.shutdownDb();
    }

    /* ===================== deprecated methods from the days of preottol
     * 
    /**
     * Intends to be used to compare names that are in a file with the format id parentid name to the ottol names and will output the mappings
     * 
     * @param args
     *
    public void compareNames(String args[]) {
        if (args.length != 4) {
            System.out.println("arguments should be: infile outfile graphdbfolder");
            System.exit(0);
        }
        String filename = args[1];
        String outfilename = args[2];
        String graphname = args[3];
        taxdb = new GraphDatabaseAgent(graphname);
        System.out.println("checking the names from " + filename);
        TaxonomyComparator tc = new TaxonomyComparator();
        tc.compareDontAddNamesToOTTOL(filename, outfilename, taxdb);
        taxdb.shutdownDb();
    }

    public void parseGraftByComp(String args[]) {
        if (args.length != 3) {
            System.out.println("arguments should be: graphdbfolderdom sourcenametocomp");
            System.exit(0);
        }
        System.out.println(args);
        String graphdomname = args[1];
        String sourcename = args[2];
        TaxonomyComparator tc = new TaxonomyComparator();
        GraphDatabaseAgent inga = new GraphDatabaseAgent(graphdomname);
        System.out.println("setting database: " + graphdomname);
        System.out.println("comparing source: " + sourcename);
        tc.compareGraftTaxonomyToDominant(inga, sourcename);
    }
*/
    public void recalculateMRCAS(String args[]) {
        if (args.length != 2) {
            System.out.println("arguments should be: graphdbfolder");
            System.exit(0);
        }
        System.out.println(args);
        String graphdbname = args[1];
        GraphDatabaseAgent inga = new GraphDatabaseAgent(graphdbname);
        System.out.println("setting database: " + graphdbname);
        TaxonomyLoaderOTT tlo = new TaxonomyLoaderOTT(inga);
        Node dbnode = tlo.getTaxonomyRootNode();
        System.out.println("removing mrcas");
        tlo.removeMRCAs(dbnode);
        System.out.println("adding mrcas");
        tlo.initMrcaForTipsAndPO(dbnode);
        System.out.println("verifying taxonomy");
        tlo.verifyMainTaxonomy();
    }

    public void parseTNRSRequest(String args[]) {
        if (args[0].equals("tnrsbasic")) {
            if (args.length != 3 && args.length != 4) {
                System.out.println("arguments should be: namestring graphdbfolder [context]");
                return;
            }
        } else if (args[0].equals("tnrstree")) {
            if (args.length != 3 && args.length != 4) {
                System.out.println("arguments should be: treefile graphdbfolder [context]");
                return;
            }
        }

        String graphName = args[2];

        String contextName = null;
        if (args.length == 4) {
            contextName = args[3];
        }

        taxdb = new GraphDatabaseAgent(graphName);
        Taxonomy taxonomy = new Taxonomy(taxdb);

        System.out.println("Looking for " + contextName);
        TaxonomyContext context = null;
        try {
        	context = taxonomy.getContextByName(contextName);
        } catch (ContextNotFoundException cnfx) {
            cnfx.printStackTrace();
        }

        System.out.println("Found " + context.getDescription().name);

        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);
        // TNRSAdapteriPlant iplant = new TNRSAdapteriPlant();
        TNRSResults results = null;

        if (args[0].equals("tnrsbasic")) {
            String[] searchStrings = args[1].split("\\s*\\,\\s*");

/*            HashSet<String> names = Utils.stringArrayToHashset(searchStrings);

            for (int i = 0; i < searchStrings.length; i++) {
                System.out.println(searchStrings[i]);
            } */
            
            Map<Object, String> idNameMap = new HashMap<Object, String>();
    		for (String name : searchStrings) {
    			idNameMap.put(name, name);
    		}
    		
            results = tnrs.
            		setSearchStrings(idNameMap).
            		setContext(context).
            		setAutomaticContextInference(false).
            		runQuery().
            		getResults();

        } else if (args[0].equals("tnrstree")) {

        	System.out.println("\n\nnot implemented\n\n");
//      	System.exit(0);
/*            // TODO: for files containing multiple trees, make sure to do TNRS just once
            // read in the treefile
            final File treefile = new File(args[1]);
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

            // TODO: use MRCA of tree as query context
            // TODO: use tree structure to help differentiate homonyms
            String[] tipNames = phys[0].getAllExternalNodeNames();

            // TNRSNameScrubber scrubber = new TNRSNameScrubber(searchStrings);
            String[] cleanedNames = TNRSNameScrubber.scrubBasic(tipNames);

            HashSet<String> names = Utils.stringArrayToHashset(cleanedNames);
            // scrubber.review(); // print old and cleaned names
            
            Map<Object, String> idNameMap = new HashMap<Object, String>();
    		for (String name : names) {
    			idNameMap.put(name, name);
    		}
            
            results = tnrs.
            		setSearchStrings(idNameMap).
            		setContext(context).
            		setAutomaticContextInference(false).
            		runQuery().
            		getResults(); */
        }

        for (TNRSNameResult nameResult : results) {
            System.out.println(nameResult.getId());
            for (TNRSMatch m : nameResult) {
                System.out.println("\t" + m.toString());
            }
        }

        System.out.println("\nNames that could not be matched:");
        for (Object id: results.getUnmatchedNameIds()) {
            System.out.println(id);
        }
        taxdb.shutdownDb(); 
    }

    public static void printHelp() {
        System.out.println("==========================");
        System.out.println("usage: taxomachine command options");
        System.out.println("");
        System.out.println("commands");
        System.out.println("---taxonomy---");
        
        System.out.println("\tbuildott <ott_directory> [graph_name (defaults to 'ott_v[ottVersion].db'] (build taxonomy db from ott distribution)");
//        System.out.println("\tinittax <sourcename> <filename> <graphdbfolder> (initializes the tax graph with a tax list)");
//        System.out.println("\taddtax <sourcename> <filename> <graphdbfolder> (adds a tax list into the tax graph)");
        System.out.println("\tadddeprecated <filename> <graphdbfolder> (adds the deprecated taxa in the file to the graph)");
//        System.out.println("\tinittaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (initializes the tax graph with a list and synonym file)");
        System.out.println("\tloadtaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (load ott from smasher taxonomy files)");
//        System.out.println("\taddtaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (adds a tax list and synonym file)");
//        System.out.println("\tupdatetax <filename> <sourcename> <graphdbfolder> (updates a specific source taxonomy)");
//        System.out.println("\tmakeottol <graphdbfolder> (creates the preferred ottol branches)");
//        System.out.println("\tdumpottol <graphdbfolder> <filename> (just dumps the ottol branches to a file to be ingested elsewhere)");
//        System.out.println("\tmakeottolnamedump <graphdbfolder> <filename> (dumps the recognized ottol names in a format consistent with phylotastic treestores)");
//        System.out.println("\tgraftbycomp <graphdbfolder_dom> <sourcename> (graphs an addedtaxonomy into main using the comparator)");
        System.out.println("\trecalculatemrcas <graphdbfolder> (deletes the mrca and nested mrcas and recalculates them)");
        System.out.println("\tmakecontexts <graphdbfolder> (build context-specific indexes; requires that makeottol has already been run)");
        System.out.println("\tmakegenusindexes <graphdbfolder> (build indexes of species for each genus; requires that makeottol has already been run)");
        System.out.println("\tchecknames <sourcename> <graphdbfolder>");
        System.out.println("\tcomparenames <filename> <outfile> <graphdbfolder> (compare the names from a file to the ottol names and output the mappings of names)");
        
        System.out.println("\n---taxquery---");
        System.out.println("\tcomptaxtree <name> <graphdbfolder> (construct a comprehensive tax newick)");
//        System.out.println("\tcomptaxlist <name> <graphdbfolder> (construct a comprehensive tax list in the input format expected by treemachine/taxomachine)");
        System.out.println("\tcomptaxgraph <name> <graphdbfolder> <outdotfile> (construct a comprehensive taxonomy in dot)");
        System.out.println("\tjsgraph <name> <graphdbfolder> (constructs a json file from tax graph)");
        System.out.println("\tchecktree <filename> <focalgroup> <graphdbfolder> (checks names in tree against tax graph)");
        System.out.println("\tgetsubtree <graphdbfolder> \"<nameslist>\" (find the subgraph for the specified taxa)");
        
        System.out.println("\n---taxonomic name resolution services---");
        System.out.println("\ttnrsbasic <querynames> <graphdbfolder> [contextname] (check if the taxonomy graph contains comma-delimited names)");
        System.out.println("\ttnrstree <treefile> <graphdbfolder> [contextname] (check if the taxonomy graph contains names in treefile)\n");

        System.out.println("\n---System properties that affect taxomachine behavior ---");
        System.out.println("\t-Dopentree.taxomachine.num.transactions=# Sets the maximum # of transactions that will be buffered.\n\t\t\tDefault: 10000. Use higher for better performance, and lower #s for less memory usage.");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        PropertyConfigurator.configure(System.getProperties());
        System.out.println("\ntaxomachine version 0.0.0.0.0.0.1 pre-alpha");
        // read the max # of database transactions to be buffered from a system property
        String numTransactionsProperty = System.getProperty("opentree.taxomachine.num.transactions");
        if (numTransactionsProperty != null) {
            try {
                int ntp = Integer.parseInt(numTransactionsProperty);
                if (ntp < 1) {
                    throw new NumberFormatException();
                }
                TaxonomyLoaderOTT.transaction_iter = ntp;
                TaxonomySynthesizer.transaction_iter = ntp;
                System.err.println("\n# transactions read from properties =" + ntp);
            } catch (NumberFormatException nfe) {
                System.err.println("\nExpected a positive number for the opentree.taxomachine.num.transactions property, but got \"" + numTransactionsProperty + "\"\nExiting...\n");
                System.exit(1);
            }
        }
        
        try {
            if (args.length == 0 || args[0].equals("help") || args[0].equals("-h") || args[0].equals("--help")) {
                printHelp();
                System.exit(0);
            } else if (args.length < 1) {
                System.err.println("\nERROR: expecting multiple arguments\n");
                printHelp();
                System.exit(1);
            } else {
                MainRunner mr = new MainRunner();

                // temporary function to play with TNRS name cleaning from trees
                /*
                 * if (args[0].matches("testScrub")) { mr.testSrcub(args); System.exit(0); }
                 */
                
                 if (args[0].equals("buildott")) {
                     mr.buildOTT(args);
                 } else if (args[0].equals("inittax")
                        || args[0].equals("addtax")
                        || args[0].equals("adddeprecated")
                        || args[0].equals("inittaxsyn")
                        || args[0].equals("addtaxsyn")
                        || args[0].equals("loadtaxsyn")) {
                    mr.taxonomyLoadParser(args);
                } else if (args[0].equals("comptaxtree")
                        || args[0].equals("comptaxlist")
                        || args[0].equals("comptaxgraph")
                        || args[0].equals("findcycles")
                        || args[0].equals("jsgraph")
                        || args[0].equals("checktree")
                        || args[0].equals("makeottol")
                        || args[0].equals("makeottolnamedump")
                        || args[0].equals("dumpott")
                        || args[0].equals("makecontexts")
                        || args[0].equals("makegenusindexes")
                        || args[0].equals("checknames")
                        || args[0].equals("getsubtree")) {
                    mr.taxonomyQueryParser(args);
                } else if (args[0].equals("recalculatemrcas")) {
                    mr.recalculateMRCAS(args);
/*                } else if (args[0].equals("graftbycomp")) {
                    mr.parseGraftByComp(args); */
                } else if (args[0].matches("tnrsbasic|tnrstree")) {
                    mr.parseTNRSRequest(args);
                    // TEMP
/*                } else if (args[0].equals("comparenames")) {
                    mr.compareNames(args); */
                } else {
                    System.err.println("Unrecognized command \"" + args[0] + "\"");
                    printHelp();
                    System.exit(1);
                }
            }
        } catch (Throwable x) {
            System.err.println("\nExiting due to an exception:\n " + x.getMessage() + "\nStack trace:\n");
            x.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
