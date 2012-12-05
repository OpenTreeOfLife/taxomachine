package opentree;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import opentree.Taxonomy.ContextDescription;

public class TaxonomyContext {

    /**
     * Defines all the indexes that are used by the taxonomy contexts. Each one of these name prefixes is combined with the a name suffix from the Taxonomy.ContextDescription enum to yield an index name.
     * @author cody hinchliff
     *
     */
    public static enum NodeIndex {
        TAXON_BY_NAME               ("taxNodesByName"),         // all taxon nodes by name
        TAXON_BY_SYNONYM            ("taxNodesBySyn"),          // all taxon nodes by synonym
        PREFERRED_TAXON_BY_NAME     ("prefTaxNodesByName"),     // taxon nodes with preferred relationships by name
        PREFERRED_TAXON_BY_SYNONYM  ("prefTaxNodesBySyn"),      // taxon nodes with preferred relationships by synonym
        PREFERRED_TAXON_BY_NAME_OR_SYNONYM  ("prefTaxNodesByNameOrSyn"),
        
        TAX_SOURCES                 ("taxSources"),             // ?
        TAX_STATUS                  ("taxStatus"),
        TAX_RANK                    ("taxRank");

        public final String namePrefix;

        NodeIndex(String namePrefix) {
            this.namePrefix = namePrefix;
        }
    }

    public static enum RelationshipIndex {
        SOURCE_TYPE ("taxSources");

        public final String namePrefix;

        RelationshipIndex(String namePrefix) {
            this.namePrefix = namePrefix;
        }
    }
    
    private ContextDescription _context;
    private Taxonomy _taxonomy;
    
    TaxonomyContext(ContextDescription context, Taxonomy taxonomy) {
        _context = context;
        _taxonomy = taxonomy;
    }
    
    public Index<Node> getNodeIndex(NodeIndex index) {
        String indexName = index.namePrefix + _context.nameSuffix;
        return _taxonomy.graphDb.getNodeIndex(indexName);
    }

    /**
     * a convenience wrapper for the taxNodes index .get("name", `name`) method
     * @param name
     * @return
     */
    public List<Node> findTaxNodesByName(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndex.TAXON_BY_NAME);
        return findNodes(index, "name", name);

    }
    
    /**
     * a convenience wrapper for the prefTaxNodes index .get("name", `name`) method
     * @param name
     * @return
     */
    public List<Node> findPrefTaxNodesByName(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndex.PREFERRED_TAXON_BY_NAME);
        return findNodes(index, "name", name);

    }

    /**
     * a convenience wrapper for the prefSynNodes index .get("name", `name`) method
     * @param name
     * @return
     */
    public List<Node> findPrefTaxNodesBySyn(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndex.PREFERRED_TAXON_BY_SYNONYM);
        return findNodes(index, "name", name);

    }

    /**
     * a convenience wrapper for the prefTaxSynNodes index .get("name", `name`) method
     * @param name
     * @return
     */
    public List<Node> findPrefTaxNodesByNameOrSyn(String name) {
        Index<Node> index = (Index<Node>) getNodeIndex(NodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
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
}
