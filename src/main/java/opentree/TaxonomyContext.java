package opentree;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import opentree.ContextDescription;

public class TaxonomyContext {
    
    private ContextDescription contextDescription;
    private Taxonomy taxonomy;
    
    public TaxonomyContext(ContextDescription context, Taxonomy taxonomy) {
        this.contextDescription = context;
        this.taxonomy = taxonomy;      
        
    }
    
    /**
     * Return a Neo4J index of the type defined by the passed NodeIndexDescription `index`, which is intended to be limited to the scope of the initiating taxonomic context
     * @param indexDesc
     * @return nodeIndex
     */
    public Index<Node> getNodeIndex(NodeIndexDescription indexDesc) {
        String indexName = indexDesc.namePrefix + contextDescription.nameSuffix;
        return taxonomy.graphDb.getNodeIndex(indexName);
        
    }

    /**
     * Return the ContextDescription that underlies this TaxonomyContext object.
     * @return
     */
    public ContextDescription getDescription() {
        return contextDescription;
    }

    /**
     * Return the root node for this taxonomic context
     * @return rootNode
     */
    public Node getRootNode() {
        IndexHits<Node> rootMatches = taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).get("name", contextDescription.licaNodeName);
        Node rn = null;
        for (Node n : rootMatches) {
            if (n.getProperty("name").equals(taxonomy.getLifeNode().getProperty("name"))) {
                // if we find the life node, just return it
                rn = n;
                break;
            } else if (n.getProperty("taxcode").equals(contextDescription.nomenclature.code)) {
                // otherwise check the taxcode to validate that this is the correct root, in case there are valid homonyms in other nomenclatures
                rn = n;
                break;
            }
        }
        rootMatches.close();
        if (rn != null) {
            return rn;
        } else {
            throw new java.lang.IllegalStateException("Could not find the root node: " + contextDescription.licaNodeName + " in nomenclature + " + contextDescription.nomenclature.code);
        }
    }

    /**
     * a convenience wrapper for the taxNodes index .query("name", `name`) method
     * @param name
     * @return
     */
    public List<Node> findTaxNodesByName(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
        return findNodes(index, "name", name);
    }
    
    /**
     * a convenience wrapper for the prefTaxNodes index .query("name", `name`) method
     * @param name
     * @return
     */
    public List<Node> findPrefTaxNodesByName(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME);
        return findNodes(index, "name", name);

    }

    /**
     * a generic wrapper for node indexes that searches the specified index for entries with `column` matching `key`
     * @param index
     * @param column
     * @param key
     * @return
     */
    public List<Node> findNodes(Index<Node> index, String column, String key) {
        
        ArrayList<Node> foundNodes = new ArrayList<Node>();
        key = key.replace(" ", "\\ ");
        IndexHits<Node> results = index.query(column, key);
//        IndexHits<Node> results = index.get(column, key);
        for (Node n : results) {
            foundNodes.add(n);
        }
        results.close();

        return foundNodes;
    }


    /* IF THESE show common enough usage, it could be useful to provide them as well
    /**
     * a convenience wrapper for the prefSynNodes index .get("name", `name`) method
     * @param name
     * @return
     *
    public List<Node> findPrefTaxNodesBySyn(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_SYNONYM);
        return findNodes(index, "name", name);

    }

    /**
     * a convenience wrapper for the prefTaxSynNodes index .get("name", `name`) method
     * @param name
     * @return
     *
    public List<Node> findPrefTaxNodesByNameOrSyn(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
        return findNodes(index, "name", name);

    } */

}
