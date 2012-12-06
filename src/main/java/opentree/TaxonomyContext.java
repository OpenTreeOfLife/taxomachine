package opentree;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import opentree.ContextDescription;

public class TaxonomyContext {
    
    private ContextDescription _contextDescription;
    private Taxonomy _taxonomy;
    
    TaxonomyContext(ContextDescription context, Taxonomy taxonomy) {
        _contextDescription = context;
        _taxonomy = taxonomy;      
        
    }
    
    /**
     * Return a Neo4J index of the type defined by the passed NodeIndexDescription `index`, which is intended to be limited to the scope of the initiating taxonomic context
     * @param indexDesc
     * @return nodeIndex
     */
    public Index<Node> getNodeIndex(NodeIndexDescription indexDesc) {
        String indexName = indexDesc.namePrefix + _contextDescription.nameSuffix;
        return _taxonomy.graphDb.getNodeIndex(indexName);
        
    }
    
    /**
     * Return the root node for this taxonomic context
     * @return rootNode
     */
    public Node getRootNode() {
//        System.out.println(_contextDescription.licaNodeName);
        IndexHits<Node> rootMatch = _taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).get("name", _contextDescription.licaNodeName);
//        System.out.println(rootMatch.toString() + " length " + rootMatch.size());

        Node rn = rootMatch.getSingle();
        rootMatch.close();

//        System.out.println(rn.toString() + " " + rn.getProperty("name"));
        return rn;
    }
    

    /**
     * a convenience wrapper for the taxNodes index .get("name", `name`) method
     * @param name
     * @return
     */
    public List<Node> findTaxNodesByName(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
        return findNodes(index, "name", name);
    }
    
    /**
     * a convenience wrapper for the prefTaxNodes index .get("name", `name`) method
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
        
        IndexHits<Node> results = index.get(column, key);
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
