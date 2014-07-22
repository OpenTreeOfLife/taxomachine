package org.opentree.taxonomy.plugins;

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
import org.neo4j.server.rest.repr.OTRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;
import org.opentree.graphdb.GraphDatabaseAgent;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.OTTFlag;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.constants.TaxonomyRelType;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;

public class OTTServices extends ServerPlugin {

    @Description("Get information about the taxonomy itself.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getTaxonomyInfo (@Source GraphDatabaseService graphDb) {
    	return OTRepresentationConverter.convert(new Taxonomy(graphDb).getMetadataMap());
    }
	
    @Description("Get a list of deprecated ott ids and the names to which they correspond.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getDeprecatedTaxa (@Source GraphDatabaseService graphDb) {

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
    
    @Description("Get information about a known taxon. If the option to include info about the lineage is used, the information will be provided " +
    			 "in an ordered array, with the least inclusive taxa at lower indices (i.e. higher indices are higher taxa).")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getTaxonInfo (
    		@Source GraphDatabaseService graphDb,
    		@Description("The OTT id of the taxon of interest.") @Parameter(name="ottId", optional=false) Long ottId,
    		@Description("Whether or not to include information about all the higher level taxa that include this one.") @Parameter(name="includeLineage", optional=true) Boolean includeLineage) {
    	
    	HashMap<String, Object> results = new HashMap<String, Object>();
    	
    	Taxonomy t = new Taxonomy(graphDb);
    	Taxon match = t.getTaxonForOTTId(ottId);
    	
    	if (match != null) {
    		
    		Node n = match.getNode();

    		if (match.isDeprecated()) {
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
	    		for (Node m : match.getSynonymNodes()) {
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
    	}

    	return OTRepresentationConverter.convert(results);
    }
    
    private void addTaxonInfo(Node n, HashMap<String, Object> results) {

    	results.put("node_id", n.getId());
		addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_ID.propertyName(), results);
		addPropertyFromNode(n, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), results);
//		addPropertyFromNode(n, TaxonomyProperty.SOURCE.propertyName(), results);
		addPropertyFromNode(n, TaxonomyProperty.RANK.propertyName(), results);
		addPropertyFromNode(n, TaxonomyProperty.UNIQUE_NAME.propertyName(), results);

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
