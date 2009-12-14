package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package src.de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

/**
 *
 * @author konrad
 */
public class TwoPlayerStrategy extends AbstractStrategy {
	
	private LinkedList<IGameNode> queue = new LinkedList<IGameNode>();
	private HashMap<IGameState, Integer> visitedStates = new HashMap<IGameState, Integer>();
	private HashMap<IGameState, int[]> values = new HashMap<IGameState, int[]>();
	private HashMap<IGameState, HashMap<int[], Integer>> simulationValues = new HashMap<IGameState, HashMap<int[], Integer>>();
	
	private ArrayList<IGameNode> children = new ArrayList<IGameNode>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	private int max=1;
	
	private IGameNode currentNode;
	
	private long endTime;
	private boolean searchFinished=false;
	private IGameNode node;
	
	private int[] mobilityStatistics = new int[]{0,0,0,0,0,0}; // we count the pro, con and even results for both ourselves and the opponent for mobility while simulating
	private int[] movesAtStart = new int[2];
	
	public static final int MOB_DISABLE_US = 1;
	public static final int MOB_ENABLE_US = 2;
	public static final int MOB_DISABLE_OPP = 4;
	public static final int MOB_ENABLE_OPP = 8;
	
	public static final int MOB_MOBILITY = 6;
	public static final int MOB_INVERSE = 9;
	public static final int MOB_MOBILITY_TEAM = 10;
	public static final int MOB_INV_MOB_TEAM = 5;
	private int enemyNumber;

	private SimplexSolver solver = new SimplexSolver();
	
	public void initMatch(Match initMatch) {
		match = initMatch;
		game = initMatch.getGame();
		playerNumber = game.getRoleIndex(match.getRole());
		enemyNumber = (playerNumber == 0) ? 1 : 0;
		
		currentDepthLimit = 1;
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 2000;
		try {
			IGameNode root = game.getTree().getRootNode();
			root.setPreserve(true); // we set the node to preserve so the stupid game tree won't forget about the state anymore.
			IMove[][] allMoves = game.getLegalMoves(root);
			String role = initMatch.getRole();
			int index = game.getRoleIndex(role);
			IMove[] myMoves = allMoves[index];
			if(myMoves.length <= 1) max=0;
			else if(allMoves[(index+1)%2].length > 1) { // obviously we have no turn taking game
				max = 2;
			}
			movesAtStart[0] = myMoves.length;
			movesAtStart[1] = allMoves[(index+1)%2].length;
			System.out.println("Max is set to: "+max+", we are playerIndex "+playerNumber+", thus the opponent: "+((playerNumber+1)%2));
			// new approach: search the game for half the prep time. if we didn't succeed by then, use the remaining time
			// to simulate some matches.
			while(System.currentTimeMillis() < endTime - match.getStartTime()*500) {
				simulateGame(root);
			}
			System.out.println("While simulating, I got "+simulationValues.size()+" values.");
			System.out.println("Additionally, I found out the following for mobility: ("
					+mobilityStatistics[0]+", "+mobilityStatistics[3]+") pro, ("
					+mobilityStatistics[1]+", "+mobilityStatistics[4]+") con, ("
					+mobilityStatistics[2]+", "+mobilityStatistics[5]+") even.");
			searchFinished = IDS(endTime - match.getStartTime(), root, 0);
		} catch (InterruptedException e) {}
	}

	/**
	 *
	 * @param endSearchTime
	 * @param start
	 * @param makeValueLimit
	 * @return true if search finished, else false
	 * @throws java.lang.InterruptedException
	 */
	public boolean IDS(long endSearchTime, IGameNode start, int makeValueLimit) throws InterruptedException {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.out.println("currentDepth"+currentDepthLimit);
	//		System.out.println("Now: "+System.currentTimeMillis()+", endTime: "+endSearchTime);
			System.out.println("Visited: "+nodesVisited);
			
			visitedStates.clear();
			queue.clear();
			
			queue.add(start);
			
			while(!queue.isEmpty() && System.currentTimeMillis() < endSearchTime) {
				// get next element from queue
				currentNode = queue.remove(0);
				game.regenerateNode(currentNode);
				nodesVisited++;
				//if(nodesVisited % 1000 == 0) System.out.println("visited: "+nodesVisited);
				
				// check if we get a value for the values hash
				if(currentNode.isTerminal()){
					int[] value = game.getGoalValues(currentNode);
					values.put(currentNode.getState(), value.clone());
					//System.out.println("Found a value: "+value[playerNumber]);
					makeValue(currentNode, makeValueLimit);
					continue;
				}
				makeValue(currentNode, makeValueLimit);
				// leave tracing infos
				visitedStates.put(currentNode.getState(), currentNode.getDepth()-1);
				
				children.clear();
				
				// find out possible successors
				if(currentNode.getDepth() < currentDepthLimit) {
					List<IMove[]> allMoves = game.getCombinedMoves(currentNode);
					IMove[] combMoves;
					
					for(int i=0; i<allMoves.size(); ++i) {
						combMoves = allMoves.get(i);
						IGameNode next = game.getNextNode(currentNode, combMoves);
						//next.setPreserve(true);
						game.regenerateNode(next);
						Integer foundDepth = visitedStates.get(next.getState());
						
						if(foundDepth == null || foundDepth > currentNode.getDepth()){
							queue.add(0, next);
						}
						children.add(next);
					}
				} else if(game.getCombinedMoves(currentNode).size() > 0) {
					flag = true;
				}
			}
			if(System.currentTimeMillis() >= endSearchTime){
				System.out.println("stop search because of time");
				return false;
			}
			currentDepthLimit++;
		}
		System.out.println("Search finished, values: "+values.size());

		return true;
	}

	/*
	 * Limit-capable makeValue function for use in getMove
	 */
	public void makeValue(IGameNode node, int minDepth) throws InterruptedException{
		if(node == null || node.getDepth() < minDepth) {
			return;
		}
		
		// watch out for the endTime
		if(System.currentTimeMillis() > endTime){
			System.out.println("makeValue1: stop search because of time");
			return;
		}
		game.regenerateNode(node);
		//we already have a value
		if(values.get(node.getState()) != null){
			makeValue(node.getParent(), minDepth);
			return;
		}

		int[] value = new int[]{-1, -1};
		if(max == 2){
			// no turn-taking -> do this weird simplex-stuff
			IMove[] myMoves = game.getLegalMoves(node)[playerNumber];
			IMove[] enemyMoves = game.getLegalMoves(node)[enemyNumber];

			LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

			// for each move the enemy can do
			for(int i=0; i<enemyMoves.length; i++){
				// watch out for the endTime
				if(System.currentTimeMillis() > endTime){
					System.out.println("makeValue move it: stop search because of time");
					return;
				}
				LinkedList<Float> line = new LinkedList<Float>();
				//for each move i enemy can do
				for(int j=0; j<myMoves.length; j++){
					// calculate the child
					IMove[] move = {myMoves[j], enemyMoves[i]};
					IGameNode child = game.getNextNode(node, move);
					game.regenerateNode(child);
					//child.setPreserve(true);
					// if one child has no value -> abuse
					if(values.get(child.getState()) == null){
						return;
					}
					//generate the problem
//					System.out.println("M: my "+myMoves[j]+" enemy "+enemyMoves[i]+" value "+values.get(child.getState())[playerNumber]);
					line.add(new Float(values.get(child.getState())[playerNumber]));
				}
				problem.add(line);
			}
			Float solution = solver.solve(problem);
			// i play a zero sum game
			value[playerNumber] = Math.round(solution);
			value[enemyNumber] = Math.round(solution);
	//		System.out.println("P: "+problem);
	//		System.out.println("value "+solution);
		} else {

			children.clear();
			List<IMove[]> allMoves = game.getCombinedMoves(node);
			for(IMove[] move : allMoves)
				children.add(game.getNextNode(node, move));
			for(IGameNode child : children) {
				// watch out for the endTime
				if(System.currentTimeMillis() > endTime){
					System.out.println("makeValue child it: stop search because of time");
					return;
				}
				game.regenerateNode(child);
				//child.setPreserve(true);
				// cant do anything, because we dont know the goal value of 1 child
				if(values.get(child.getState()) == null) {
					return;
				}
				// should we maximize or minimize?
				if(max == 1) { // we are max player
					if(node.getDepth()%2 == 0) { // maximize
						if(values.get(child.getState())[playerNumber] > value[playerNumber]) {
							value = values.get(child.getState()).clone();
						}
					} else { // maximize opponent
						if(values.get(child.getState())[(playerNumber+1)%2] > value[(playerNumber+1)%2]) {
							value = values.get(child.getState()).clone();
						}
					}
				} else if(max == 0) { // we are min player
					if(node.getDepth()%2 == 0) { // maximize opponent
						if(values.get(child.getState())[(playerNumber+1)%2] > value[(playerNumber+1)%2]) {
							value = values.get(child.getState()).clone();
						}
					} else { // maximize
						if(values.get(child.getState())[playerNumber] > value[playerNumber]) {
							value = values.get(child.getState()).clone();
						}
					}
				}
			}
		}
		
		if(value[0] != -1 && value[1] != -1){
			values.put(node.getState(), value);
			if(node.getParent() != null) {
				//node.getParent().setPreserve(true);
				makeValue(node.getParent(), minDepth);
			}
		}
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		IGameNode best = null;
		try {
			game.regenerateNode(arg0);
			if(!searchFinished) {
				endTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 2000;
				// a little simulation doesn't harm
				while(System.currentTimeMillis() < endTime - match.getPlayTime()*600)
					simulateGame(arg0);
				// search a bit more, from the node arg0.
				currentDepthLimit = arg0.getDepth()+1;
				//arg0.setPreserve(true);
				IDS(endTime, arg0, arg0.getDepth()+1);
			}
			//if max = 2
			Boolean foundMove = true;
			if(max == 2){
				//
				// get the probabilities from the simplexSolver
				// no turn-taking -> do this weird simplex-stuff
				IMove[] myMoves = game.getLegalMoves(arg0)[playerNumber];
				IMove[] enemyMoves = game.getLegalMoves(arg0)[enemyNumber];

				//init
				List<LinkedList<Float>> averageScore = new LinkedList<LinkedList<Float>>();
				for(int i=0; i<myMoves.length; i++){
					averageScore.add(new LinkedList<Float>());
				}


				LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

				// for each move the enemy can do
				for(int i=0; i<enemyMoves.length; i++){
					LinkedList<Float> line = new LinkedList<Float>();
					//for each move i enemy can do
					for(int j=0; j<myMoves.length; j++){
						// calculate the child
						IMove[] move = {myMoves[j], enemyMoves[i]};
						IGameNode child = game.getNextNode(arg0, move);
						//child.setPreserve(true);
						game.regenerateNode(child);
						// if one child has no value
						// -> let the normal getMove decide
						Float value;
						if(values.get(child.getState()) == null){
							foundMove = false;
							value = 49f;
						} else {
							value = new Float(values.get(child.getState())[playerNumber]);
						}

						if(simulationValues.get(child.getState()) == null){
							value += 0.49f;
						} else {
							value += 0.01f * ((int[]) (simulationValues.get(child.getState()).keySet().toArray()[0]))[playerNumber];
						}
						//generate the problem
	//					System.out.println("M: my "+myMoves[j]+" enemy "+enemyMoves[i]+" value "+values.get(child.getState())[playerNumber]);
						if(foundMove == true){
							line.add(new Float(values.get(child.getState())[playerNumber]));
						}

						averageScore.get(j).add(value);
					}
					problem.add(line);
				}
				if(foundMove == true){
					System.out.println("P: "+problem);
					LinkedList<Float> moves = solver.getMoves(problem);
					// choose the move randomly
					int rand = random.nextInt(1000);
					System.out.println("all moves: "+Arrays.asList(myMoves));
					Float currentSpace = 0f;
					IMove move = null;
					for(int i=0; i<moves.size(); i++){
						if((moves.get(i) >= 0f) && moves.get(i) <= 1f){
								System.out.println("Possible Move: "+myMoves[i]+" when "+currentSpace+" <= "+rand+" < "+(currentSpace+moves.get(i)*1000));
								if(move == null && (currentSpace <= rand) && (rand < (currentSpace+moves.get(i)*1000))){
									move = myMoves[i];
								}
								currentSpace += moves.get(i)*1000;
						} else {
								System.out.println("Possible Move: "+myMoves[i]+" dont do this");
						}
					}

					if((move == null)){
						//should not happen
						System.out.println("found no move while solving the problem");
						//let max != 2 solve it
					} else {
						return move;
					}
				} else {
					//foundMove == false
					// get the best out of average score
					IMove move = null;
					Float maximum = null;
					for(int i=0; i<averageScore.size(); i++){
						LinkedList<Float> row = averageScore.get(i);
						Float score =  0f;
						for(Float value : row){
							score += value;
						}
						score = score / row.size();
						if((move == null) || (score > maximum)){
							move = myMoves[i];
							maximum = score;
						}
						System.out.println("Possible move : "+myMoves[i]+"   Value: "+score);
					}
					return move;
				}
			}
			// max != 2 or (max==2) wasnt able to calculate a move, hrhrhr
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this, match, true));
			for(IMove[] combMove : game.getCombinedMoves(arg0)) {
				IGameNode next = game.getNextNode(arg0, combMove);
				//next.setPreserve(true);
				game.regenerateNode(next);
				System.out.print("Possible move: "+combMove[game.getRoleIndex(match.getRole())]);
				int simval = -2;
				int val[] = new int[]{-2, -2};
				try {
					simval = ((int[]) (simulationValues.get(next.getState()).keySet().toArray()[0]))[playerNumber];
				} catch(NullPointerException ex) {}
				val = values.get(next.getState());
				if(val == null) val = new int[]{-2, -2};
				System.out.println("   Value: (["+val[0]+","+val[1]+"], "+simval+")");
				childs.add(next);
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(arg0)[game.getRoleIndex(match.getRole())];
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves()[game.getRoleIndex(match.getRole())];
	}

	public HashMap<IGameState, int[]> getValues(){
		return values;
	}
	
	public HashMap<IGameState, HashMap<int[], Integer>> getSimulationValues() {
		return simulationValues;
	}
	
	public int getPlayerNumber() {
		return playerNumber;
	}
	
	/*
	 * new method designed to play a game according to either random choose or some heuristic function
	 * while doing this, we already populate the values hash with some values
	 *  (goal -> goalValue, states before goal -> average goal value achieved from this state
	 */
	private void simulateGame(IGameNode start) throws InterruptedException {
		// first we just play a game
		IGameNode currentNode = start;
		//currentNode.setPreserve(true);
		game.regenerateNode(currentNode);
		int[] value;
		while(true) {
			
			// game over?
			if(currentNode.isTerminal()) { 
				value = currentNode.getState().getGoalValues();
		//		System.out.println("Played a game and got score "+value[playerNumber]);
				break;
			}
			game.regenerateNode(currentNode);
			// try to prove that we have simultaneous moves
			int turns = 0;
			IMove[][] legalMoves = game.getLegalMoves(currentNode);
			for(int i=0; i<legalMoves.length; i++) {
				if(legalMoves[i].length > 1) turns++;
			}
			if(max < 2 && turns > 1) {
				max = 2;
				//System.out.println("We found out that the game is not turn-taking!");
			}
			
			// choose a move
			currentNode = game.getNextNode(currentNode, getMoveForSimulation(currentNode));
			//currentNode.setPreserve(true);
		}
		
		// since the game is over, we can now go all the way back and fiddle around with the goals
		HashMap<int[], Integer> existingValue = simulationValues.get(currentNode.getState());
		
		// check if mobility or inverse mobility would good to apply
		IMove[][] legals = game.getLegalMoves(currentNode.getParent());
		if(legals[playerNumber].length > movesAtStart[0] ^ value[playerNumber] < 50)
		    mobilityStatistics[0]++;
		else if(legals[playerNumber].length < movesAtStart[0] ^ value[playerNumber] >= 50)
			mobilityStatistics[1]++;
		else
			mobilityStatistics[2]++;
		
		if(legals[(playerNumber+1)%2].length < movesAtStart[1] ^ value[(playerNumber+1)%2] >= 50)
			mobilityStatistics[3]++;
		else if(legals[(playerNumber+1)%2].length > movesAtStart[1] ^ value[(playerNumber+1)%2] < 50)
			mobilityStatistics[4]++;
		else
			mobilityStatistics[5]++;
		//System.out.println("Existing Value in Hash: "+existingValue);
		
		// if there is no value yet, we put it in
		if(existingValue == null) {
			HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
			temp.put(value, 1);
			simulationValues.put(currentNode.getState(), temp);
		}
		// now we look at the parent of the goal state.
		IGameNode node = currentNode;
		while(node.getParent() != null) {
			game.regenerateNode(node);
			node = node.getParent();
			HashMap<int[], Integer> entry = simulationValues.get(node.getState());
			int[] tempVal = null;
			if(entry != null) {
				tempVal = (int[]) simulationValues.get(node.getState()).keySet().toArray()[0];
			}
			if(entry == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(value, 1);
				simulationValues.put(node.getState(), temp);
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				Integer newCount = ((Integer) simulationValues.get(node.getState()).values().toArray()[0])+1;
				for(int i=0; i<value.length; i++) {
					tempVal[i] = ((newCount-1)*tempVal[i]+value[i])/newCount;
				}
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(tempVal, newCount);
				simulationValues.put(node.getState(), temp);
			}
		}
	}
	
	/*
	 * helping function for game simulation: choose a move according to a heuristic
	 * for now we just take a random move
	 * 
	 */
	private IMove[] getMoveForSimulation(IGameNode currentNode) throws InterruptedException {
		return game.getRandomMove(currentNode);
		/*
		IGameNode best = null;
		try {
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this, match, true));
			for(IMove[] combMove : game.getCombinedMoves(currentNode)) {
				childs.add(game.getNextNode(currentNode, combMove));
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
				
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves();*/
	}
	
	public int getMobilityVariant() {
		int sum=0;
		// if the pro's for one variant is overweighing the con's and even's, we can apply that one.
		// conversely, if the con's overweigh the pro's and the even's we can apply the opposite.
		
		if(mobilityStatistics[0] > (mobilityStatistics[1]+mobilityStatistics[2])) sum += MOB_ENABLE_US;
		if((mobilityStatistics[0]+mobilityStatistics[2]) < mobilityStatistics[1]) sum += MOB_DISABLE_US;
		if(mobilityStatistics[3] > (mobilityStatistics[4]+mobilityStatistics[5])) sum += MOB_ENABLE_OPP;
		if((mobilityStatistics[3]+mobilityStatistics[4]) < mobilityStatistics[5]) sum += MOB_DISABLE_OPP;
		
		return sum;
	}
}
