package opentree;

import opentree.BarrierNodes.Nomenclature;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class Taxonomy {

    GraphDatabaseAgent graphDb;
    public final static float DEFAULT_FUZZYMATCH_IDENTITY = (float) 0.70;
    final static String LIFE_NODE_NAME = "life";
    public final TaxonomyContext ALLTAXA = new TaxonomyContext(ContextDescription.ALLTAXA, this);

    public Taxonomy(GraphDatabaseAgent gdb) {
        graphDb = gdb;
    }

    public static enum ContextGroup {
        LIFE,
        BACTERIA,
        ANIMALS,
        PLANTS,
        FUNGI
    }
    
    public static enum ContextDescription {

        //                  Label               Group                   Index suffix        Node name string    Nomenclature
        ALLTAXA             ("All life",        ContextGroup.LIFE,      "",                 LIFE_NODE_NAME,     Nomenclature.Undefined),

        // FUNGI
        BACTERIA            ("Bacteria",        ContextGroup.BACTERIA,  "Bacteria",         "Bactera",          Nomenclature.ICNB),

        // ANIMALS
        METAZOA             ("Animals",         ContextGroup.ANIMALS,   "Animals",          "Metazoa",          Nomenclature.ICZN),

        // FUNGI
        FUNGI               ("Fungi",           ContextGroup.FUNGI,     "Fungi",            "Fungi",            Nomenclature.ICBN),
        
        // PLANTS
        LAND_PLANTS         ("Land plants",     ContextGroup.PLANTS,    "Plants",           "Embryophyta",      Nomenclature.ICBN),
        HORNWORTS           ("Hornworts",       ContextGroup.PLANTS,    "Anthocerotophyta", "Anthocerotophyta", Nomenclature.ICBN),
        MOSSES              ("Mosses",          ContextGroup.PLANTS,    "Bryophyta",        "Bryophyta",        Nomenclature.ICBN),
        LIVERWORTS          ("Liverworts",      ContextGroup.PLANTS,    "Marchantiophyta",  "Marchantiophyta",  Nomenclature.ICBN),
        VASCULAR_PLANTS     ("Vascular plants", ContextGroup.PLANTS,    "Tracheophyta",     "Tracheophyta",     Nomenclature.ICBN),
        LYCOPHYTES          ("Club mosses",     ContextGroup.PLANTS,    "Lycopodiophyta",   "Lycopodiophyta",   Nomenclature.ICBN),
        FERNS               ("Ferns",           ContextGroup.PLANTS,    "Moniliformopses",  "Moniliformopses",  Nomenclature.ICBN),
        SEED_PLANTS         ("Seed plants",     ContextGroup.PLANTS,    "Spermatophyta",    "Spermatophyta",    Nomenclature.ICBN),
        FLOWERING_PLANTS    ("Flowering plants", ContextGroup.PLANTS,   "Magnoliophyta",    "Magnoliophyta",    Nomenclature.ICBN),
        MAGNOLIIDS          ("Magnoliids",      ContextGroup.PLANTS,    "Magnoliids",       "magnoliids",       Nomenclature.ICBN),
        MONOCOTS            ("Monocots",        ContextGroup.PLANTS,    "Monocots",         "Liliopsida",       Nomenclature.ICBN),
        EUDICOTS            ("Eudicots",        ContextGroup.PLANTS,    "Eudicots",         "eudicotyledons",   Nomenclature.ICBN),
        ASTERIDS            ("Asterids",        ContextGroup.PLANTS,    "Asterids",         "asterids",         Nomenclature.ICBN),
        ROSIDS              ("Rosids",          ContextGroup.PLANTS,    "Rosids",           "rosids",           Nomenclature.ICBN);
        
        public final String label;
        public final ContextGroup group;
        public final String nameSuffix;
        public final String nodeName;
        public final Nomenclature nomenclature;

        ContextDescription (String label, ContextGroup group, String nameSuffix, String nodeName, Nomenclature nomenclature) {
            this.label = label;
            this.group = group;
            this.nameSuffix = nameSuffix;
            this.nodeName = nodeName;
            this.nomenclature = nomenclature;
        }
    }
        
    public static enum RelTypes implements RelationshipType {
        TAXCHILDOF, // standard rel for tax db, from node to parent
        SYNONYMOF, // relationship for synonyms
        METADATAFOR, // relationship connecting a metadata node to the root of a taxonomy
        PREFTAXCHILDOF // relationship type for preferred relationships
    }
    
    // general taxonomy access methods

    public Node getLifeNode() {
        
        Node lifeNode;
        try {
            lifeNode = ALLTAXA.findTaxNodesByName(LIFE_NODE_NAME).get(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Could not get life node");
            lifeNode = null;
        }

        return lifeNode;
    }
    
    /**
     * Just get the recognized taxon node that is associated with a given synonym node.
     * 
     * @param synonymNode
     * @return taxNode
     */
    public Node getTaxNodeForSynNode(Node synonymNode) {

        TraversalDescription synonymTraversal = Traversal.description()
                .relationships(RelTypes.SYNONYMOF, Direction.OUTGOING);

        Node taxNode = null;
        for (Node tn : synonymTraversal.traverse(synonymNode).nodes())
            taxNode = tn;

        return taxNode;
    }
    
    /**
     * Finds the directed internodal distance between `n1` and `n2` along relationships of type `relType` by tracing the paths to the LICA
     * of n1 and n2. Note that this method uses direction to find the LICA, and will not behave properly if the outgoing paths along the
     * specified relType from n1 and n2 do not intersect.
     * @param n1
     * @param n2
     * @param relType
     * @return distance
     */
    public int getInternodalDistThroughMRCA(Node n1, Node n2, RelationshipType relType) {

        System.out.println("Node 1: " + n1.getProperty("name") + " " + n1.getId() + ", Node 2: " + n2.getProperty("name") + " " + n2.getId());

        TraversalDescription hierarchy = Traversal.description()
                .depthFirst()
                .relationships(relType, Direction.OUTGOING);

        Iterable<Node> firstPath = hierarchy.traverse(n1).nodes();
        Iterable<Node> secondPath = hierarchy.traverse(n2).nodes();

        int i = 0;
        boolean matched = false;
        count: for (Node n : firstPath) {

            int j = 0;
            for (Node m : secondPath) {
                if (n.getId() == m.getId())
                    matched = true;
                j++;
            }

            if (matched) {
                i += j;
                break count;
            }

            i++;
        }

        return i;
    }
    
    // wrappers for relevant underlying database methods
    
    public Node createNode() {
        return graphDb.createNode();
    }
    
    public Transaction beginTx() {
        return graphDb.beginTx();
    }
    
    public Node getNodeById(Long arg0) {
        return graphDb.getNodeById(arg0);
    }
}
