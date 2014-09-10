package org.opentree.taxonomy.plugins;

import jade.tree.JadeNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.JadeNodeRepresentation;
import org.neo4j.server.rest.repr.OTRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;
import org.opentree.exceptions.TaxonNotFoundException;
import org.opentree.graphdb.GraphDatabaseAgent;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.OTTFlag;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.TaxonSet;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.constants.TaxonomyRelType;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;

public class taxonomy extends ServerPlugin {

    @Description("Get matadata and information about the taxonomy itself. Currently the recorded metadata is fairly sparse, but it "
    		+ "should include (at least) the version, and the location from which the taxonomy itself can be downloaded.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation about (@Source GraphDatabaseService graphDb) {
    	return OTRepresentationConverter.convert(new Taxonomy(graphDb).getMetadataMap());
    }
	
    @Description("Get a list of deprecated ott ids and the names to which they correspond. Deprecated ott ids are ott ids that were "
    		+ "included in a previous version of the OTT taxonomy, but which have been retired and no longer exist in subsequent "
    		+ "versions. Deprecated ids are thus known to be unstable across versions and should not be used. In most cases, the "
    		+ "reason for deprecation is that the taxonomic names have either ceased to exist, or were duplicates of other names, and "
    		+ "have since been combined. This service is provided as a convenience. The canonical list of deprecated ids is available "
    		+ "from http://file.opentreeoflife.org/.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation deprecated_taxa (@Source GraphDatabaseService graphDb) {

    	List<HashMap<String,Object>> deprecatedTaxa = new LinkedList<HashMap<String, Object>>();
    	
    	IndexHits<Node> dTaxNodes = new Taxonomy(new GraphDatabaseAgent(graphDb)).ALLTAXA
    			.getNodeIndex(TaxonomyNodeIndex.DEPRECATED_TAXA)
    			.query(new MatchAllDocsQuery());
    	
    	for (Node d : dTaxNodes) {
    		HashMap<String, Object> dMap = new HashMap<String, Object>();
    		
    		addPropertyFromNode(d, OTVocabularyPredicate.OT_OTT_ID.propertyName(), dMap);
    		addPropertyFromNode(d, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), dMap);
    		addPropertyFromNode(d, TaxonomyProperty.REASON.propertyName(), dMap);
    		addPropertyFromNode(d, TaxonomyProperty.SOURCE_INFO.propertyName(), dMap);
    		deprecatedTaxa.add(dMap);
    	}
    	
    	return OTRepresentationConverter.convert(deprecatedTaxa);
    }
    
    @Description("Extract and return the inclusive taxonomic subtree i.e. (a subset of the taxonomy) below a given taxon. "
    		+ "The taxonomy subtree is returned in newick format.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation subtree (@Source GraphDatabaseService graphDb,
		@Description("The OTT id of the taxon of interest.") @Parameter(name="ott_id", optional=false) Long ottId) {
    	
    	Taxonomy taxonomy = new Taxonomy(graphDb);
    	HashMap<String,Object> results = new HashMap<String, Object>();

    	results.put("subtree", taxonomy.getTaxonForOTTId(ottId).getTaxonomySubtree().getRoot().getNewick(false));
    	
    	return OTRepresentationConverter.convert(results);
    }
    
    @Description("Return information about the least inclusive common ancestral taxon (the LICA) of the identified taxa. A "
    		+ "taxonomic LICA is analogous to a most recent common ancestor (MRCA) in a phylogenetic tree. For example, the "
    		+ "LICA for the taxa 'Pan' and 'Lemur' is 'Primates'.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation lica (@Source GraphDatabaseService graphDb,
		@Description("The ott ids for the taxa of interest.") @Parameter(name="ott_ids", optional=false) Long[] ottIds,
		@Description("Whether or not to include information about the higher level taxa that include the identified LICA.") @Parameter(name="include_lineage", optional=true) Boolean includeLineage) {

    	Taxonomy taxonomy = new Taxonomy(graphDb);
    	HashMap<String,Object> results = new HashMap<String, Object>();

    	if (ottIds.length < 1) {
    		throw new IllegalArgumentException("You must provide at least one ott id");
    	}
    	
    	LinkedList<Taxon> taxa = new LinkedList<Taxon>();
    	LinkedList<Long> ottIdsNotFound = new LinkedList<Long>();
    	for (Long o : ottIds) {
    		Taxon t = taxonomy.getTaxonForOTTId(o);
    		if (t != null) {
    			taxa.add(t);
    		} else {
    			ottIdsNotFound.add(o);
    		}
    	}
    	
    	if (taxa.size() > 0) {
    		TaxonSet ts = new TaxonSet(taxa);
        	results.put("lica", getTaxonInfo(ts.getLICA(), includeLineage));
        	results.put("ott_ids_not_found", ottIdsNotFound);
    	} else {
    		throw new IllegalArgumentException("None of the ott ids provided could be matched to known taxa.");
    	}
    	
    	return OTRepresentationConverter.convert(results);
    }
    
    @Description("Get information about a known taxon in the taxonomy.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation taxon (
    		@Source GraphDatabaseService graphDb,
    		@Description("The OTT id of the taxon of interest.") @Parameter(name="ott_id", optional=false) Long ottId,
    		@Description("Whether or not to include information about all the higher level taxa that include this one. "
    		+ "By default, this option is set to false. If it is set to true, the lineage will be provided in an ordered array, "
    		+ "with the least inclusive taxa at lower indices (i.e. higher indices are higher taxa).") @Parameter(name="include_lineage", optional=true) Boolean includeLineage) throws TaxonNotFoundException {
    	
    	HashMap<String, Object> results = new HashMap<String, Object>();
    	
    	Taxonomy t = new Taxonomy(graphDb);
    	Taxon match = t.getTaxonForOTTId(ottId);
    	
    	if (match != null) {
    		results = (HashMap<String, Object>) getTaxonInfo(match, includeLineage);
    	} else {
    		throw new TaxonNotFoundException(ottId);
    	}

    	return OTRepresentationConverter.convert(results);
    }
    
    @Description("Return a list of taxonomic flags used in this database, including the number of taxa to which each flag "
    		+ "has been assigned.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation flags (@Source GraphDatabaseService graphDb) {

    	HashMap<String, Object> results = new HashMap<String, Object>();
    	
    	Taxonomy t = new Taxonomy(graphDb);
		for (OTTFlag f : OTTFlag.values()) {
	    	IndexHits<Node> hits = t.taxaByFlag.query("flag", f.label);
	    	results.put(f.label, hits.size());
		}
    	
    	return OTRepresentationConverter.convert(results);
    }
    

    private Map<String,Object> getTaxonInfo(Taxon t, Boolean includeLineage) {
    	
    	HashMap<String, Object> results = new HashMap<String, Object>();
		Node n = t.getNode();

		if (t.isDeprecated()) {
			// for deprecated ids, add only appropriate properties
	    	results.put("node_id", n.getId());
			addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_ID.propertyName(), results);
			addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), results);
			addPropertyFromNode(n, TaxonomyProperty.REASON.propertyName(), results);
			addPropertyFromNode(n, TaxonomyProperty.SOURCE_INFO.propertyName(), results);
			results.put("flags", Arrays.asList(new String[] {TaxonomyProperty.DEPRECATED.toString()}));
			
		} else {
			// not deprecated, add regular info
			addTaxonInfo(n, results);
    		
    		HashSet<String> synonyms = new HashSet<String>();
    		for (Node m : t.getSynonymNodes()) {
    			synonyms.add((String) m.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()));
    		}
    		results.put("synonyms", synonyms);
    		
    		if (includeLineage != null && includeLineage == true) {
    			Node p = n;
    			LinkedList<HashMap<String,Object>> lineage = new LinkedList<HashMap<String, Object>>();
    			while (p.hasRelationship(TaxonomyRelType.TAXCHILDOF, Direction.OUTGOING)) {
    				p = p.getSingleRelationship(TaxonomyRelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
    				HashMap<String,Object> info = new HashMap<String, Object>();
    				addTaxonInfo(p, info);
    				lineage.add(info);
    			}
    			results.put("taxonomic_lineage", lineage);
    		}
		}

		return results;
    }
    
    private void addTaxonInfo(Node n, HashMap<String, Object> results) {

    	// TODO: need to update this to use the TaxonomyProperty enum once the taxonomy uniqname field is changed to this format
    	results.put("unique_name", n.getProperty(TaxonomyProperty.UNIQUE_NAME.propertyName()));

    	results.put("node_id", n.getId());
    	addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_ID.propertyName(), results);
		addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), results);
//		addPropertyFromNode(n, TaxonomyProperty.SOURCE.propertyName(), results);
		addPropertyFromNode(n, TaxonomyProperty.RANK.propertyName(), results);

		HashSet<String> flags = new HashSet<String>();
		for (OTTFlag flag : OTTFlag.values()) {
			if (n.hasProperty(flag.label)) {
				flags.add(flag.toString());
			}
		}
		results.put("flags", flags);
    }
    
    private void addPropertyFromNode(Node node, String property, Map<String, Object> map) {
		map.put(property, node.getProperty(property));
    }

}
