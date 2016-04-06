package org.opentree.taxonomy.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.mortbay.log.Log;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.OTRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.BadInputException;
import org.opentree.graphdb.GraphDatabaseAgent;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.LabelFormat;
import org.opentree.taxonomy.OTTFlag;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.TaxonSet;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.constants.TaxonomyRelType;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;

import jade.tree.JadeTree;

public class taxonomy_v3 extends ServerPlugin {

    @Description("Return metadata and information about the taxonomy itself. Currently the available metadata is fairly sparse, but "
    		+ "includes (at least) the version, and the location from which the complete taxonomy source files can be downloaded.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation about (@Source GraphDatabaseService graphDb) {
        Map<String, Object> meta = new Taxonomy(graphDb).getMetadataMap();
    	return OTRepresentationConverter.convert(meta);
    }

    static int SUBTREE_LIMIT = 50000;

    @Description("Extract and return the inclusive taxonomic subtree i.e. (a subset of the taxonomy) below a given taxon. "
    		+ "The taxonomy subtree is returned in newick format.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation subtree (@Source GraphDatabaseService graphDb,

    		@Description("The OTT id of the taxon of interest.")
    		@Parameter(name="ott_id", optional=false)
    		Long ottId,

    		@Description("The format for the labels. If provided, this must be one of the options: [\"name\", "
    				+ "\"id\", \"name_and_id\",\"original_name\"], indicating whether the node labels should contain "
    				+ "(respectively) just the cleaned name (i.e. with punctuation and whitespace replaced with underscores), "
    				+ "just the ott id, the cleaned name and as well as the ott id (default), or the original name without "
    				+ "punctuation removed.")
    		@Parameter(name="label_format", optional=true)
    		String labelFormatStr)
        throws BadInputException
    {

    	labelFormatStr = labelFormatStr == null ? LabelFormat.NAME_AND_ID.toString() : labelFormatStr;

    	Taxonomy taxonomy = new Taxonomy(graphDb);
    	HashMap<String,Object> results = new HashMap<String, Object>();

    	LabelFormat format = null;
    	for (LabelFormat f : LabelFormat.values()) {
    		if (f.toString().toLowerCase().equals(labelFormatStr.toLowerCase())) {
    			format = f;
    		}
    	}

    	if (format == null) {
            throw new BadInputException("The specified label type '" + labelFormatStr + "' was not recognized.");

    	} else {
    		Taxon match = taxonomy.getTaxonForOTTId(ottId);
            if (match == null)
                // Should be a 400
                throw new BadInputException(
                            String.format("The specified taxon %s was not found.", ottId));
            else {
                JadeTree tree = match.getTaxonomySubtree(format);
                int count = tree.nodeCount();
                if (count > SUBTREE_LIMIT)
                    throw new BadInputException(
                                String.format("The requested subtree exceeds the limit of %s taxa.",
                                              count));
                else
                    results.put("newick", tree.getRoot().getNewick(false)+";");
            }
    	}

    	return OTRepresentationConverter.convert(results);
    }

    @Description("Return information about the most recent common ancestor (MRCA) of the identified taxa. "
            + "For example, the "
    		+ "MRCA of the taxa 'Pan' and 'Lemur' in the taxonomy represented by the newick string "
    		+ "'(((Pan,Homo,Gorilla)Hominidae,Gibbon)Hominoidea,Lemur)Primates' is 'Primates'.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation mrca (@Source GraphDatabaseService graphDb,

    	@Description("The ott ids (in an array) for the taxa whose MRCA is to be found.")
    	@Parameter(name="ott_ids", optional=false)
    	Long[] ottIds,

    	@Description("Whether or not to include information about the higher level taxa that include the identified MRCA. "
				+ "By default, this option is set to false. If it is set to true, the lineage will be provided in an ordered array, "
				+ "with the least inclusive taxa at lower indices (i.e. higher indices are higher taxa).")
    	@Parameter(name="include_lineage", optional=true)
    	Boolean includeLineage) 
        throws BadInputException
    {
    	Taxonomy taxonomy = new Taxonomy(graphDb);
    	HashMap<String,Object> results = new HashMap<String, Object>();

    	if (ottIds.length < 1) {
    		throw new BadInputException("You must provide at least one ott id");
    	}

    	LinkedList<Taxon> taxa = new LinkedList<Taxon>();
    	LinkedList<Long> ottIdsNotFound = new LinkedList<Long>();
    	for (Long o : ottIds) {
    		Taxon tax = taxonomy.getTaxonForOTTId(o);
    		if (tax != null) {
    			taxa.add(tax);
    		} else {
    			ottIdsNotFound.add(o);
    		}
    	}

        if (ottIdsNotFound.size() > 0) {
            java.io.StringWriter sw = new java.io.StringWriter();
            sw.write("No taxa found with these OTT ids: ");
            try {
                org.json.simple.JSONArray.writeJSONString(ottIdsNotFound, sw);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            throw new BadInputException(sw.toString());
            // was: results.put("ott_ids_not_found", ottIdsNotFound);
        }
    	if (taxa.size() > 0) {
    		TaxonSet ts = new TaxonSet(taxa);
        	results.put("mrca", getTaxonInfo(ts.getLICA(), includeLineage, false));
    	} else {
    		throw new BadInputException("None of the OTT ids provided could be matched to known taxa");
    	}

    	return OTRepresentationConverter.convert(results);
    }

    @Description("Get information about a known taxon in the taxonomy.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation taxon_info (@Source GraphDatabaseService graphDb,

    		@Description("The OTT id of the taxon of interest.")
    		@Parameter(name="ott_id", optional=false)
    		Long ottId,

    		@Description("Provide a list of terminal OTT ids contained by this taxon.")
    		@Parameter(name="include_terminal_descendants", optional=true)
    		Boolean listDescendants,

    		@Description("Whether or not to include information about all the higher level taxa that include this one. "
    		+ "By default, this option is set to false. If it is set to true, the lineage will be provided in an ordered array, "
    		+ "with the least inclusive taxa at lower indices (i.e. higher indices are higher taxa).")
    		@Parameter(name="include_lineage", optional=true)
    		Boolean includeLineage,

    		@Description("Whether or not to include information about all the children of this taxon. "
    		+ "By default, this option is set to false. If it is set to true, the children will be provided in an array.")
    		@Parameter(name="include_children", optional=true)
    		Boolean includeChildren)
        throws BadInputException
    {

    	listDescendants = listDescendants == null ? false : listDescendants;

    	HashMap<String, Object> results = new HashMap<String, Object>();

    	Taxonomy tax = new Taxonomy(graphDb);
    	Taxon match = tax.getTaxonForOTTId(ottId);

    	if (match != null) {
    		results = (HashMap<String, Object>) getTaxonInfo(match, includeLineage, includeChildren);
    		if (listDescendants) {
                        LabelFormat tipFormat = LabelFormat.ID;  //API specifies ids; could support alternate labels
                        results.put("terminal_descendants",
                                    match.getTaxonomyTerminals(tipFormat));
    	    	}

    	} else {
    		throw new BadInputException("The OTT id " + String.valueOf(ottId) + " could not be found");
    	}

    	return OTRepresentationConverter.convert(results);
    }

    @Description("Return a list of all taxonomic flags used in this database, including the number of taxa to which each flag "
    		+ "has been assigned.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation flags (@Source GraphDatabaseService graphDb) {

    	HashMap<String, Object> results = new HashMap<String, Object>();

    	Taxonomy tax = new Taxonomy(graphDb);
		for (OTTFlag f : OTTFlag.values()) {
	    	IndexHits<Node> hits = tax.taxaByFlag.query("flag", f.label);
	    	results.put(f.label, hits.size());
		}

    	return OTRepresentationConverter.convert(results);
    }

    // Utility to generate a taxon-description-blob

    private Map<String,Object> getTaxonInfo(Taxon t, Boolean includeLineage, Boolean includeChildren) {

    	HashMap<String, Object> results = new HashMap<String, Object>();
		Node n = t.getNode();

		if (t.isDeprecated()) {
			// for deprecated ids, add only appropriate properties
			addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_ID.propertyName(), "ott_id", results);
            addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), "name", results);
			addPropertyFromNode(n, TaxonomyProperty.REASON.propertyName(), results);
			addPropertyFromNode(n, TaxonomyProperty.INPUT_SOURCES.propertyName(), results);
			results.put("flags", Arrays.asList(new String[] {TaxonomyProperty.DEPRECATED.toString()}));
            // is_suppressed is required by spec.  This seems wrong somehow.
			results.put("is_suppressed", Boolean.TRUE);
            // synonyms is required by spec.  This seems wrong somehow.
            results.put("synonyms", new HashSet<String>());  // No synonyms for deprecated

		} else {
			// not deprecated, add regular info
			addTaxonInfo(n, results);

    		if (includeLineage != null && includeLineage == true) {
    			Node p = n;
    			LinkedList<HashMap<String,Object>> lineage = new LinkedList<HashMap<String, Object>>();
                String ottprop = OTVocabularyPredicate.OT_OTT_ID.propertyName();
    			while (p.hasRelationship(TaxonomyRelType.TAXCHILDOF, Direction.OUTGOING)) {
    				p = p.getSingleRelationship(TaxonomyRelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
    				HashMap<String,Object> info = new HashMap<String, Object>();
    				addTaxonInfo(p, info);
    				lineage.add(info);
    			}
    			results.put("lineage", lineage);
    		}

            if (includeChildren != null && includeChildren == true) {
    			LinkedList<HashMap<String,Object>> children = new LinkedList<HashMap<String, Object>>();
                for (Relationship child : n.getRelationships(TaxonomyRelType.TAXCHILDOF, Direction.INCOMING)) {
                    Node c = child.getStartNode();
    				HashMap<String,Object> info = new HashMap<String, Object>();
    				addTaxonInfo(c, info);
    				children.add(info);
                }
    			results.put("children", children);
            }
		}

		return results;
    }

    // Compare addTaxonInfo in class TNRSResultsRepresentation

    private void addTaxonInfo(Node n, HashMap<String, Object> results) {

    	addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_ID.propertyName(), "ott_id", results);

        String nameProperty = OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName();
		addPropertyFromNode(n, nameProperty, "name", results);
		addPropertyFromNode(n, TaxonomyProperty.RANK.propertyName(), results);
		addTaxSources(n, results);

    	// TODO: need to update this to use the TaxonomyProperty enum once the taxonomy uniqname field is changed to this format
        String uname = (String) n.getProperty(TaxonomyProperty.UNIQUE_NAME.propertyName());
        if (uname.length() == 0)
            uname = (String) n.getProperty(nameProperty);
    	results.put("unique_name", uname);

        Boolean isSuppressed = Boolean.FALSE;
        if (n.hasProperty(TaxonomyProperty.DUBIOUS.propertyName()))
            isSuppressed = (Boolean) n.getProperty(TaxonomyProperty.DUBIOUS.propertyName());

        // Do this only if property is present on node?  and is true?
        results.put("is_suppressed", isSuppressed);

		HashSet<String> flags = new HashSet<String>();
		for (OTTFlag flag : OTTFlag.values()) {
			if (n.hasProperty(flag.label)) {
				flags.add(flag.toString());
			}
		}
		results.put("flags", flags);

        HashSet<String> synonyms = new HashSet<String>();
        for (Node m : Taxon.getSynonymNodes(n)) {
            if (m != n)
                synonyms.add((String) m.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()));
        }
        results.put("synonyms", synonyms);
    }

    private void addPropertyFromNode(Node node, String property, Map<String, Object> map) {
		map.put(property, node.getProperty(property));
    }

    private void addPropertyFromNode(Node node, String property, String resultName, Map<String, Object> map) {
		map.put(resultName, node.getProperty(property));
    }


    //parses an input_sources string (e.g. "ncbi:1234,if:5678") into a List<String>
    //Because the elements are curieoruri, (http://www.w3.org/TR/rdfa-core/#s_curieprocessing)
    //it doesn't make sense to parse any deeper here
    //TODO maybe propagate "tax_sources" back through INPUT_SOURCES property
    private void addTaxSources(Node node, Map<String, Object> resultsMap){
        String property = TaxonomyProperty.INPUT_SOURCES.propertyName();
        if (node.getProperty(property) != null){
            String[] sources = ((String)node.getProperty(property)).split(",");
            resultsMap.put("tax_sources",Arrays.asList(sources));
        }
    }
 

}
