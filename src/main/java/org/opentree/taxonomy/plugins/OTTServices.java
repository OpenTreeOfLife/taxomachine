package org.opentree.taxonomy.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.OTRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;
import org.opentree.taxonomy.GraphDatabaseAgent;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;

public class OTTServices extends ServerPlugin {

    @Description("Get information about a recognized taxon.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getDeprecatedTaxa (
    		@Source GraphDatabaseService graphDb,
    		@Description("The OTT id of the taxon of interest.") @Parameter(name="ottId", optional=false) Long ottId) {

    	List<HashMap<String,Object>> deprecatedTaxa = new LinkedList<HashMap<String, Object>>();
    	
    	IndexHits<Node> dTax = new GraphDatabaseAgent(graphDb)
    			.getNodeIndex(TaxonomyNodeIndex.DEPRECATED_TAXA.indexName())
    			.query(new MatchAllDocsQuery());
    	
    	for (Node d : dTax) {
    		HashMap<String, Object> dMap = new HashMap<String, Object>();
    		dMap.put(OTVocabularyPredicate.OT_OTT_ID.propertyName(), d.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
    		dMap.put(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), d.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
    		dMap.put(TaxonomyProperty.REASON.propertyName(), d.getProperty(TaxonomyProperty.REASON.propertyName()));
    		dMap.put(TaxonomyProperty.SOURCE_INFO.propertyName(), d.getProperty(TaxonomyProperty.SOURCE_INFO.propertyName()));
    		deprecatedTaxa.add(dMap);
    	}
    	
    	return OTRepresentationConverter.convert(deprecatedTaxa);
    }
    
    @Description("Get information about a recognized taxon.")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getTaxonInfo (
    		@Source GraphDatabaseService graphDb,
    		@Description("The OTT id of the taxon of interest.") @Parameter(name="ottId", optional=false) Long ottId) {
    	
    	HashMap<String, Object> results = new HashMap<String, Object>();
    	
    	Taxonomy t = new Taxonomy(graphDb);
    	Taxon match = t.getTaxonForOTTId(ottId);
    	
    	if (match != null) {
		addPropertyFromNode(match.getNode(), "name", results);
    		addPropertyFromNode(match.getNode(), "rank", results);
    		addPropertyFromNode(match.getNode(), "source", results);
    		addPropertyFromNode(match.getNode(), "uniqname", results);
    		addPropertyFromNode(match.getNode(), OTVocabularyPredicate.OT_OTT_ID.propertyName(), results);
    		results.put("node_id", match.getNode().getId());
    	
    		HashSet<String> synonyms = new HashSet<String>();
	    	for (Node n : match.getSynonymNodes()) {
	    		synonyms.add((String) n.getProperty("name"));
	    	}
	    	results.put("synonyms", synonyms);
    	
    	}

    	return OTRepresentationConverter.convert(results);
    }
    
    
    private void addPropertyFromNode(Node node, String property, Map<String, Object> map) {
		map.put(property, node.getProperty(property));
    }

}
