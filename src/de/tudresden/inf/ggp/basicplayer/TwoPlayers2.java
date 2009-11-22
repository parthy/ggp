/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.model.utils.Game;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

/**
 *
 * @author konrad
 */
public class TwoPlayers2 extends AbstractStrategy {
	private List<IGameNode> queue = new LinkedList<IGameNode>();
	private List<IGameNode> currentWay = new LinkedList<IGameNode>();
	private Map<Integer, Integer> hash = new HashMap<Integer, Integer>();
	//to save the value for each Node
	private Map<Integer, Integer> values = new HashMap<Integer, Integer>();
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private long endTime;
	private boolean foundSolution = false;
	private int currentDepthLimit;
	private int expandedNodes;
	private long start;
	private IGameNode solution;
	private boolean gotFirstMove;

	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);

		logger.setLevel(Level.ALL);

		// initiate solution search
		game = initMatch.getGame();
		queue.add(game.getTree().getRootNode());
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 5000L;
		try {
			whoIsStarting(game.getTree().getRootNode());
			IDS(1);
		} catch (InterruptedException ex) {
			Logger.getLogger(TwoPlayers2.class.getName()).log(Level.SEVERE, null, ex);
		}
		hash.clear();
		//if(foundSolution) fillCurrentWay();
	}

	@Override
	public IMove getMove(IGameNode currentNode) {
		IMove lastOption = null;
		//calc last option if we get interrupted (shouldnt happen)
		try {
			lastOption = game.getRandomMove(currentNode)[playerNumber];
		} catch (InterruptedException ex) {
			Logger.getLogger(TwoPlayers2.class.getName()).log(Level.SEVERE, null, ex);
		}

		try {
			/**
			 * calcuate all children and compare the values (choose highest)
			 */
			List<IMove[]> moves = game.getCombinedMoves(currentNode);
			List<IGameNode> children = new LinkedList<IGameNode>();
			for (IMove[] move : moves) {
				children.add(game.getNextNode(currentNode, move));
			}
			//choose the one with the higest value
			IGameNode child = children.remove(0);
			if(values.containsKey(child.getState().hashCode())){
				int max = values.get(child.getState().hashCode());
				logger.info("alternative: "+max);
				while(!children.isEmpty()){
					IGameNode child2 = children.get(0);
					if(values.containsKey(child2.getState().hashCode())){
						if(values.get(child2.getState().hashCode()) > max){
							child = child2;
							max = values.get(child.getState().hashCode());
							logger.info("alternative: "+max);
						}
					}
				}
				//return how we got to the highest child
				return child.getMoves()[playerNumber];
			} else {
				//we dont have information for the child
				return lastOption;
			}

		} catch (InterruptedException ex) {
			return lastOption;
		}

	}

	void IDS(int depthLimit) throws InterruptedException {
    	currentDepthLimit = depthLimit;
		expandedNodes = 0;
		start = System.currentTimeMillis();
		IGameNode node;

		Boolean foundSomethingNew = true;

    	while(foundSomethingNew) {
			foundSomethingNew = false;

			System.err.println("currentDepth: "+currentDepthLimit);
			System.err.println("current size of hash: "+hash.size());
			System.err.println("expanded "+expandedNodes+" Nodes in "+(System.currentTimeMillis()-start)+" seconds.");
    		while(!queue.isEmpty() && (System.currentTimeMillis() < endTime)) {
    			node = queue.remove(0);

				try {
					node = game.getNextNode(node.getParent(), node.getMoves());
				} catch (Exception e2) {}

				hash.put(node.getState().hashCode(), currentDepthLimit-node.getDepth());

				/**
				 * here comes the evaluation of the nodes
				 */
				if(node.getState().isTerminal()){
					//put the values and propagate until nothing changes or root reached
					IGameNode nodeWithNewValue = node;
					int value = game.getGoalValues(node)[playerNumber];
					Boolean changedSomething = true;
					logger.log(Level.CONFIG, "we found a terminal with value"+value);

					while(changedSomething && (nodeWithNewValue != null)){
						changedSomething = false;

						try {
							nodeWithNewValue = game.getNextNode(nodeWithNewValue.getParent(), nodeWithNewValue.getMoves());
						} catch (Exception e3) {}
						//if no value is known yet
						if(!values.containsKey(nodeWithNewValue.getState().hashCode())){
							values.put(nodeWithNewValue.getState().hashCode(), value);
							changedSomething = true;
						} else {
							int oldValue = values.get(nodeWithNewValue.getState().hashCode());

							//do we have to minimize or maximize the parent ?
							//if(gotFirstMove _XOR_ ((node.getDepth() % 2)==1) )
							logger.log(Level.CONFIG, "propagating up "+nodeWithNewValue.getDepth()+" "+gotFirstMove);
							if(gotFirstMove ^ ((nodeWithNewValue.getDepth() % 2)==1) ){
								//we maximize
								if(value > oldValue){
									values.put(nodeWithNewValue.getState().hashCode(), value);
									changedSomething = true;
								}
							} else {
								//minimize
								if(value < oldValue){
									values.put(nodeWithNewValue.getState().hashCode(), value);
									changedSomething = true;
								}
							}
						}
						nodeWithNewValue = nodeWithNewValue.getParent();
					}
				//evaluation finished
				} else {

					if(node.getDepth() < this.currentDepthLimit) {
						expandedNodes++;
						// add successor nodes to queue
						List<IMove[]> moves = game.getCombinedMoves(node);
						for(IMove[] move: moves) {
							IGameNode newNode = game.getNextNode(node, move);
							if(!hash.containsKey(newNode.getState().hashCode())) {
								queue.add(newNode);
								foundSomethingNew = true;
							} else {
								if((currentDepthLimit-newNode.getDepth()) > hash.get(newNode.getState().hashCode())){
									queue.add(newNode);
								} else {
	//								foundSomethingNew = true;
								}
							}
						}
					}
				}
    		}
    		if(System.currentTimeMillis() >= endTime) return;
    		queue.clear();
    		queue.add( game.getTree().getRootNode());
    		currentDepthLimit++;
    	}
    }

	private void whoIsStarting(IGameNode node) throws InterruptedException {
		List<IMove[]> allMoves = game.getCombinedMoves(node);
		if(allMoves.size() == 1){
			//just one move possible, we cannot know who is the starter
			whoIsStarting(game.getNextNode(node, allMoves.remove(0)));
		} else {
			//someone has more than one move
			//save the first one
			IMove firstMove = allMoves.get(0)[playerNumber];
			gotFirstMove = false;
			for(IMove[] move : allMoves){
				//check if we have another possible move
				if(firstMove != move[playerNumber]){
					gotFirstMove = true;
				}
			}
		}
		logger.log(Level.INFO, "did we get the first move ? "+gotFirstMove);
	}


}
