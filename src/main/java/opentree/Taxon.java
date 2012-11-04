package opentree;

import jade.tree.JadeNode;
import jade.tree.JadeTree;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import opentree.TaxonomyBase.RelTypes;
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

public class Taxon extends TaxonomyBase {

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
//                .relationships( RelTypes.PREFTAXCHILDOF, Direction.OUTGOING ); // TODO: use preferred rels (have to have preottol to get them)
                .relationships( RelTypes.TAXCHILDOF, Direction.OUTGOING );
        
        Long pid = parent.getNode().getId();
//        System.out.println("mrca = " + parent.getName());
        
        for (Node n : hierarchy.traverse(taxNode).nodes()) {
//            System.out.println("comparing " + n.getProperty("name") + " to " + parent.getName());
            if (n.getId() == pid) {
                return true;
            }
        }
        
        return false;
    }

    
    /**
     * Writes a dot file for the taxonomy graph that is rooted at `clade_name`
     * 
     * @param clade_name
     *            - the name of the internal node in taxNodeIndex that will be the root of the subtree that is written
     * @param out_filepath
     *            - the filepath to create
     * @todo support other graph file formats
     */
    public void exportGraphForClade(String out_filepath) {

        // TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelTypes.TAXCHILDOF,Direction.INCOMING );
        PrintWriter out_file;
        try {
            out_file = new PrintWriter(new FileWriter(out_filepath));
            out_file.write("strict digraph  {\n\trankdir = RL ;\n");
            HashMap<String, String> src2style = new HashMap<String, String>();
            HashMap<Node, String> nd2dot_name = new HashMap<Node, String>();
            int count = 0;

            // TODO: switch this to use TraversalDescription object
            for (Node nd : taxNode.traverse(Traverser.Order.BREADTH_FIRST,
                    StopEvaluator.END_OF_GRAPH,
                    ReturnableEvaluator.ALL,
                    RelTypes.TAXCHILDOF,
                    Direction.INCOMING)) {
                for (Relationship rel : nd.getRelationships(RelTypes.TAXCHILDOF, Direction.INCOMING)) {
                    count += 1;
                    Node rel_start = rel.getStartNode();
                    String rel_start_name = ((String) rel_start.getProperty("name"));
                    String rel_start_dot_name = nd2dot_name.get(rel_start);
                    if (rel_start_dot_name == null) {
                        rel_start_dot_name = "n" + (1 + nd2dot_name.size());
                        nd2dot_name.put(rel_start, rel_start_dot_name);
                        out_file.write("\t" + rel_start_dot_name + " [label=\"" + rel_start_name + "\"] ;\n");
                    }
                    Node rel_end = rel.getEndNode();
                    String rel_end_name = ((String) rel_end.getProperty("name"));
                    String rel_end_dot_name = nd2dot_name.get(rel_end);
                    if (rel_end_dot_name == null) {
                        rel_end_dot_name = "n" + (1 + nd2dot_name.size());
                        nd2dot_name.put(rel_end, rel_end_dot_name);
                        out_file.write("\t" + rel_end_dot_name + " [label=\"" + rel_end_name + "\"] ;\n");
                    }
                    String rel_source = ((String) rel.getProperty("source"));
                    String edge_style = src2style.get(rel_source);
                    if (edge_style == null) {
                        edge_style = "color=black"; // @TMP
                        src2style.put(rel_source, edge_style);
                    }
                    out_file.write("\t" + rel_start_dot_name + " -> " + rel_end_dot_name + " [" + edge_style + "] ;\n");
                }
            }
            out_file.write("}\n");
            out_file.close();
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
        for (Relationship friendrel : CHILDOF_TRAVERSAL.traverse(taxNode).relationships()) {
            count += 1;
            if (nodes.containsKey(friendrel.getStartNode()) == false) {
                JadeNode node = new JadeNode();
                node.setName(((String) friendrel.getStartNode().getProperty("name")).replace(" ", "_").replace(",", "_").replace(")", "_").replace("(", "_")
                        .replace(":", "_"));
                nodes.put(friendrel.getStartNode(), node);
            }
            if (nodes.containsKey(friendrel.getEndNode()) == false) {
                JadeNode node = new JadeNode();
                node.setName(((String) friendrel.getEndNode().getProperty("name")).replace(" ", "_").replace(",", "_").replace(")", "_").replace("(", "_")
                        .replace(":", "_"));
                nodes.put(friendrel.getEndNode(), node);
            }
            nodes.get(friendrel.getEndNode()).addChild(nodes.get(friendrel.getStartNode()));
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
        for (Node friendnode : CHILDOF_TRAVERSAL.traverse(taxNode).nodes()) {
            if (friendnode.hasRelationship(Direction.INCOMING)) {
                nodenumbers.put(friendnode, count);
                numbernodes.put(count, friendnode);
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
        TaxonomyExplorer te = new TaxonomyExplorer();
        TNRSQuery tnrs = new TNRSQuery(te);

        Taxon lonicera = new Taxon(tnrs.getExactMatches("Lonicera").iterator().next().getMatches().iterator().next().getMatchedNode());
        lonicera.buildTaxonomyTree();

        shutdownDB();
    }
}
