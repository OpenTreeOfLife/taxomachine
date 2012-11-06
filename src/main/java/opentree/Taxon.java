package opentree;

import jade.tree.JadeNode;
import jade.tree.JadeTree;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import opentree.Taxonomy.RelTypes;
import opentree.tnrs.MultipleHitsException;
import opentree.tnrs.TNRSQuery;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class Taxon {

    private final Node taxNode;
    private ChildNumberEvaluator cne;
    private SpeciesEvaluator se;

    public Taxon(Node n) {
        taxNode = n;
    }

    public Node getNode() {
        return taxNode;
    }

    public String getName() {
        return taxNode.getProperty("name").toString();
    }
    
    public boolean isPreferredTaxChildOf(Taxon parent) {
    
        TraversalDescription hierarchy = Traversal.description()
                .depthFirst()
                .relationships( RelTypes.PREFTAXCHILDOF, Direction.OUTGOING );
        
        Long pid = parent.getNode().getId();
        
        for (Node n : hierarchy.traverse(taxNode).nodes())
            if (n.getId() == pid)
                return true;
        
        return false;
    }

    
    /**
     * Writes a dot file for the taxonomy graph that is rooted at `clade_name`
     * 
     * @param clade_name
     *            - the name of the internal node in taxNodeIndex that will be the root of the subtree that is written
     * @param outFilepath
     *            - the filepath to create
     * @todo support other graph file formats
     */
    public void exportGraphForClade(String outFilepath) {

        PrintWriter outFile;
        try {
            outFile = new PrintWriter(new FileWriter(outFilepath));
            outFile.write("strict digraph  {\n\trankdir = RL ;\n");
            HashMap<String, String> sourceToStyleMap = new HashMap<String, String>();
            HashMap<Node, String> nodeToNameMap = new HashMap<Node, String>();
//            int count = 0;

            // TODO: switch this to use TraversalDescription object
/*            for (Node nd : taxNode.traverse(Traverser.Order.BREADTH_FIRST,
                    StopEvaluator.END_OF_GRAPH,
                    ReturnableEvaluator.ALL,
                    RelTypes.TAXCHILDOF,
                    Direction.INCOMING)) */ 
            for (Node nd : Traversal.description()
                .relationships(RelTypes.PREFTAXCHILDOF, Direction.INCOMING).breadthFirst().traverse(taxNode).nodes()) {
                for (Relationship rel : nd.getRelationships(RelTypes.TAXCHILDOF, Direction.INCOMING)) {
//                    count += 1;
                    Node startNode = rel.getStartNode();
                    String sName = ((String) startNode.getProperty("name"));
                    String sDotName = nodeToNameMap.get(startNode);
                    if (sDotName == null) {
                        sDotName = "n" + (1 + nodeToNameMap.size());
                        nodeToNameMap.put(startNode, sDotName);
                        outFile.write("\t" + sDotName + " [label=\"" + sName + "\"] ;\n");
                    }
                    Node endNode = rel.getEndNode();
                    String eName = ((String) endNode.getProperty("name"));
                    String eDotName = nodeToNameMap.get(endNode);
                    if (eDotName == null) {
                        eDotName = "n" + (1 + nodeToNameMap.size());
                        nodeToNameMap.put(endNode, eDotName);
                        outFile.write("\t" + eDotName + " [label=\"" + eName + "\"] ;\n");
                    }
                    String relSource = ((String) rel.getProperty("source"));
                    String edgeStyle = sourceToStyleMap.get(relSource);
                    if (edgeStyle == null) {
                        edgeStyle = "color=black"; // @TMP
                        sourceToStyleMap.put(relSource, edgeStyle);
                    }
                    outFile.write("\t" + sDotName + " -> " + eDotName + " [" + edgeStyle + "] ;\n");
                }
            }
            outFile.write("}\n");
            outFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This essentially uses every relationship and constructs a newick tree (hardcoded to taxtree.tre file)
     * 
     * It would be trivial to only include certain relationship sources @ name the name of the internal node that will be the root of the subtree that is
     * written
     */
    public void buildTaxonomyTree() {

        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
                .relationships(RelTypes.TAXCHILDOF, Direction.INCOMING);
        System.out.println(taxNode.getProperty("name"));
        JadeNode root = new JadeNode();
        root.setName(((String) taxNode.getProperty("name")).replace(" ", "_"));
        HashMap<Node, JadeNode> nodes = new HashMap<Node, JadeNode>();
        nodes.put(taxNode, root);
        int count = 0;
        for (Relationship rel : CHILDOF_TRAVERSAL.traverse(taxNode).relationships()) {
            count += 1;
            if (nodes.containsKey(rel.getStartNode()) == false) {
                JadeNode node = new JadeNode();
                node.setName(((String) rel.getStartNode().getProperty("name")).replace(" ", "_").replace(",", "_").replace(")", "_").replace("(", "_")
                        .replace(":", "_"));
                nodes.put(rel.getStartNode(), node);
            }
            if (nodes.containsKey(rel.getEndNode()) == false) {
                JadeNode node = new JadeNode();
                node.setName(((String) rel.getEndNode().getProperty("name")).replace(" ", "_").replace(",", "_").replace(")", "_").replace("(", "_")
                        .replace(":", "_"));
                nodes.put(rel.getEndNode(), node);
            }
            nodes.get(rel.getEndNode()).addChild(nodes.get(rel.getStartNode()));
            if (count % 100000 == 0)
                System.out.println(count);
        }
        JadeTree tree = new JadeTree(root);
        PrintWriter outFile;
        try {
            outFile = new PrintWriter(new FileWriter("taxtree.tre"));
            outFile.write(tree.getRoot().getNewick(false));
            outFile.write(";\n");
            outFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void constructJSONGraph() {

        System.out.println(taxNode.getProperty("name"));
        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
                .relationships(RelTypes.TAXCHILDOF, Direction.INCOMING);
        HashMap<Node, Integer> nodenumbers = new HashMap<Node, Integer>();
        HashMap<Integer, Node> numbernodes = new HashMap<Integer, Node>();
        int count = 0;
        for (Node n : CHILDOF_TRAVERSAL.traverse(taxNode).nodes()) {
            if (n.hasRelationship(Direction.INCOMING)) {
                nodenumbers.put(n, count);
                numbernodes.put(count, n);
                count += 1;
            }
        }
        PrintWriter outFile;
        try {
            outFile = new PrintWriter(new FileWriter("graph_data.js"));
            outFile.write("{\"nodes\":[");
            for (int i = 0; i < count; i++) {
                Node tnode = numbernodes.get(i);
                outFile.write("{\"name\":\"" + tnode.getProperty("name") + "");
                outFile.write("\",\"group\":" + nodenumbers.get(tnode) + "");
                outFile.write("},");
            }
            outFile.write("],\"links\":[");
            for (Node tnode : nodenumbers.keySet()) {
                for (Relationship trel : tnode.getRelationships(Direction.OUTGOING)) {
                    outFile.write("{\"source\":" + nodenumbers.get(trel.getStartNode()) + "");
                    outFile.write(",\"target\":" + nodenumbers.get(trel.getEndNode()) + "");
                    outFile.write(",\"value\":" + 1 + "");
                    outFile.write("},");
                }
            }
            outFile.write("]");
            outFile.write("}\n");
            outFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String constructJSONAltRels(String domsource, ArrayList<Long> altrels) {
        cne.setStartNode(taxNode);
        cne.setChildThreshold(200);
        se.setStartNode(taxNode);
        int maxdepth = 3;
        boolean taxonomy = true;
        RelationshipType defaultchildtype = RelTypes.TAXCHILDOF;
        RelationshipType defaultsourcetype = RelTypes.TAXCHILDOF;
        String sourcename = "ottol";
        if (domsource != null)
            sourcename = domsource;

        PathFinder<Path> pf = GraphAlgoFactory.shortestPath(Traversal.pathExpanderForTypes(defaultchildtype, Direction.OUTGOING), 100);
        JadeNode root = new JadeNode();
        if (taxonomy == false)
            root.setName((String) taxNode.getProperty("name"));
        else
            root.setName((String) taxNode.getProperty("name"));
        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
                .relationships(defaultchildtype, Direction.INCOMING);
        ArrayList<Node> visited = new ArrayList<Node>();
        ArrayList<Relationship> keepers = new ArrayList<Relationship>();
        HashMap<Node, JadeNode> nodejademap = new HashMap<Node, JadeNode>();
        HashMap<JadeNode, Node> jadeparentmap = new HashMap<JadeNode, Node>();
        nodejademap.put(taxNode, root);
        root.assocObject("nodeid", taxNode.getId());
        // These are the altrels that actually made it in the tree
        ArrayList<Long> returnrels = new ArrayList<Long>();
        for (Node friendnode : CHILDOF_TRAVERSAL.depthFirst().evaluator(Evaluators.toDepth(maxdepth)).evaluator(cne).evaluator(se).traverse(taxNode).nodes()) {
            // System.out.println("visiting: "+friendnode.getProperty("name"));
            if (friendnode == taxNode)
                continue;
            Relationship keep = null;
            Relationship spreferred = null;
            Relationship preferred = null;

            for (Relationship rel : friendnode.getRelationships(Direction.OUTGOING, defaultsourcetype)) {
                if (preferred == null)
                    preferred = rel;
                if (altrels.contains(rel.getId())) {
                    keep = rel;
                    returnrels.add(rel.getId());
                    break;
                } else {
                    if (((String) rel.getProperty("source")).compareTo(sourcename) == 0) {
                        spreferred = rel;
                        break;
                    }
                    /*
                     * just for last ditch efforts if(pf.findSinglePath(rel.getEndNode(), firstNode) != null || visited.contains(rel.getEndNode())){ preferred =
                     * rel; }
                     */
                }
            }
            if (keep == null) {
                keep = spreferred;// prefer the source rel after an alt
                if (keep == null) {
                    continue;// if the node is not part of the main source just continue without making it
                    // keep = preferred;//fall back on anything
                }
            }
            JadeNode newnode = new JadeNode();
            if (taxonomy == false) {
                if (friendnode.hasProperty("name")) {
                    newnode.setName((String) friendnode.getProperty("name"));
                    newnode.setName(newnode.getName().replace("(", "_").replace(")", "_").replace(" ", "_").replace(":", "_"));
                }
            } else {
                newnode.setName(((String) friendnode.getProperty("name")).replace("(", "_").replace(")", "_").replace(" ", "_").replace(":", "_"));
            }

            newnode.assocObject("nodeid", friendnode.getId());

            ArrayList<Relationship> conflictrels = new ArrayList<Relationship>();
            for (Relationship rel : friendnode.getRelationships(Direction.OUTGOING, defaultsourcetype)) {
                if (rel.getEndNode().getId() != keep.getEndNode().getId() && conflictrels.contains(rel) == false) {
                    // check for nested conflicts
                    // if(pf.findSinglePath(keep.getEndNode(), rel.getEndNode())==null)
                    conflictrels.add(rel);
                }
            }
            newnode.assocObject("conflictrels", conflictrels);
            nodejademap.put(friendnode, newnode);
            keepers.add(keep);
            visited.add(friendnode);
            if (taxNode != friendnode && pf.findSinglePath(keep.getStartNode(), taxNode) != null) {
                jadeparentmap.put(newnode, keep.getEndNode());
            }
        }
        // build tree and work with conflicts
        System.out.println("root " + root.getChildCount());
        for (JadeNode jn : jadeparentmap.keySet()) {
            if (jn.getObject("conflictrels") != null) {
                String confstr = "";
                @SuppressWarnings("unchecked")
                ArrayList<Relationship> cr = (ArrayList<Relationship>) jn.getObject("conflictrels");
                if (cr.size() > 0) {
                    confstr += ", \"altrels\": [";
                    for (int i = 0; i < cr.size(); i++) {
                        String namestr = "";
                        if (taxonomy == false) {
                            if (cr.get(i).getEndNode().hasProperty("name"))
                                namestr = (String) cr.get(i).getEndNode().getProperty("name");
                        } else {
                            namestr = (String) cr.get(i).getEndNode().getProperty("name");
                        }
                        confstr += "{\"parentname\": \"" + namestr + "\",\"parentid\":\"" + cr.get(i).getEndNode().getId() + "\",\"altrelid\":\""
                                + cr.get(i).getId() + "\",\"source\":\"" + cr.get(i).getProperty("source") + "\"}";
                        if (i + 1 != cr.size())
                            confstr += ",";
                    }
                    confstr += "]\n";
                    jn.assocObject("jsonprint", confstr);
                }
            }
            try {
                // System.out.println(jn.getName()+" "+nodejademap.get(jadeparentmap.get(jn)).getName());
                nodejademap.get(jadeparentmap.get(jn)).addChild(jn);
            } catch (java.lang.NullPointerException npe) {
                continue;
            }
        }
        System.out.println("root " + root.getChildCount());

        // get the parent so we can move back one node
        Node parFirstNode = null;
        for (Relationship rels : taxNode.getRelationships(Direction.OUTGOING, defaultsourcetype)) {
            if (((String) rels.getProperty("source")).compareTo(sourcename) == 0) {
                parFirstNode = rels.getEndNode();
                break;
            }
        }
        JadeNode beforeroot = new JadeNode();
        if (parFirstNode != null) {
            String namestr = "";
            if (taxonomy == false) {
                if (parFirstNode.hasProperty("name"))
                    namestr = (String) parFirstNode.getProperty("name");
            } else {
                namestr = (String) parFirstNode.getProperty("name");
            }
            beforeroot.assocObject("nodeid", parFirstNode.getId());
            beforeroot.setName(namestr);
            beforeroot.addChild(root);
        } else {
            beforeroot = root;
        }
        beforeroot.assocObject("nodedepth", beforeroot.getNodeMaxDepth());

        // construct the final string
        JadeTree tree = new JadeTree(beforeroot);
        String ret = "[\n";
        ret += tree.getRoot().getJSON(false);
        ret += ",{\"domsource\":\"" + sourcename + "\"}]\n";
        return ret;
    }

    public void runittest() {

        String pathToGraphDb = "";
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(pathToGraphDb);
        TNRSQuery tnrs = new TNRSQuery(new Taxonomy(gdb));

        Taxon lonicera = null;
        try {
            lonicera = new Taxon(tnrs.getExactMatches("Lonicera").getSingleMatch().getMatchedNode());
        } catch (MultipleHitsException e) {
            e.printStackTrace();
        }

        lonicera.buildTaxonomyTree();

        gdb.shutdownDb();
    }
}
