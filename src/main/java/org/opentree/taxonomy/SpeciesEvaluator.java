package org.opentree.taxonomy;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.opentree.taxonomy.constants.TaxonomyRelType;

/**
 * A Neo4j Traversal Evaluator which include:
 *		paths that end with a TAXCHILDOF startNode, and
 *		paths are the TAX parent of other nodes.
 */
public class SpeciesEvaluator implements Evaluator{
	Node startNode = null;
	public void setStartNode(Node n){
		startNode = n;
	}
	public Evaluation evaluate(Path arg0) {
		boolean parent_startnode = false;
		
		for(Relationship rel: arg0.endNode().getRelationships(Direction.OUTGOING, TaxonomyRelType.TAXCHILDOF)){
			if (rel.getEndNode().getId()==startNode.getId()){
				parent_startnode = true;
				break;
			}
		}
		if(parent_startnode == true){
			return Evaluation.INCLUDE_AND_CONTINUE;
		}else if(arg0.endNode().hasRelationship(Direction.INCOMING,TaxonomyRelType.TAXCHILDOF)){
			return Evaluation.INCLUDE_AND_CONTINUE;
		}
		return Evaluation.EXCLUDE_AND_PRUNE;
	}
}
