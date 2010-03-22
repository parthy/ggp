package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package src.de.tudresden.inf.ggp.basicplayer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;

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
    private HashMap<IGameState, ValuesEntry> values = new HashMap<IGameState, ValuesEntry>();
    /**
     * This Set contains all known "good" states, i.e. terminals, states where we definitely end up with good score, and so on.
     */
    private Set<MemorizeState> memorizeStates = new HashSet<MemorizeState>();
    private int currentDepthLimit;
    private int nodesVisited;
    private int max = 1;
    private long endTime;
    private int enemyNumber;
    private SimplexSolver solver = new SimplexSolver();
    private long endSearchTime;
    private HashMap<IGameState, Integer> propagatedHash = new HashMap<IGameState, Integer>();
    Evaluator evaluator;
    
    public void setFirstEndTime(long endTime) {
        this.endTime = endTime - 600;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initMatch(Match initMatch) {
        match = initMatch;
        game = initMatch.getGame();
        playerNumber = game.getRoleIndex(match.getRole());
        enemyNumber = (playerNumber == 0) ? 1 : 0;

        // end time is set from outside
        System.out.println("\nINIT: 2playerStrategy.");

        /*
        // try to find previous results of this game in a file.
        MD5Hash hash = new MD5Hash(game.getSourceGDL().concat(String.valueOf(playerNumber)));
        try {
            FileInputStream istream = new FileInputStream(hash.toString());
            ObjectInputStream ois = new ObjectInputStream(istream);

            // if no exception occurred here, we have valid data. read out.
            HashMap<String, Integer> propagatedHashRead = (HashMap<String, Integer>) ois.readObject();
            HashMap<String, ValuesEntry> valuesRead = (HashMap<String, ValuesEntry>) ois.readObject();
            propagatedHash = new HashMap<IGameState, Integer>();
            for (Entry<String, Integer> entry : propagatedHashRead.entrySet()) {
                String fluentString = entry.getKey();
                List<String> fluentList = RuleOptimizer.getListFromFluentString(fluentString);

                List<CompoundTerm> compTermList = new ArrayList<CompoundTerm>();

                for (String fluent : fluentList) {
                    compTermList.add(EclipseConnector.getInstance().parseTerm(fluent));
                }
                HelpingStateAdapter state = new HelpingStateAdapter(EclipseConnector.getInstance(), compTermList);
                propagatedHash.put(state, entry.getValue());
            }

            for (Entry<String, ValuesEntry> entry : valuesRead.entrySet()) {
                String fluentString = entry.getKey();
                List<String> fluentList = RuleOptimizer.getListFromFluentString(fluentString);

                List<CompoundTerm> compTermList = new ArrayList<CompoundTerm>();

                for (String fluent : fluentList) {
                    compTermList.add(EclipseConnector.getInstance().parseTerm(fluent));
                }
                HelpingStateAdapter state = new HelpingStateAdapter(EclipseConnector.getInstance(), compTermList);
                values.put(state, entry.getValue());
            }
            System.out.println("Done reading hashes.");
        } catch (FileNotFoundException ex) {
            // we have no data. or the game is rewritten/scrambled whatsoever.
            System.out.println("No file found.");
        } catch (IOException e) {
            // An unspecified error occurred.
            System.out.println("IOException.");
        } catch (ClassNotFoundException e) {
            // While reading, the class was not found.
            System.out.println("Class not found.");
        } catch (Exception e) {
            // Something totally irrelevant just happened.
            System.out.println("Some exception occurred.");
            e.printStackTrace(System.out);
        }

        System.out.println("read " + propagatedHash.size() + " propagated values successfully.");
        System.out.println("read " + values.size() + " simulated values successfully.");
	*/

        // Set up the evaluator
        this.evaluator = new Evaluator(values, memorizeStates);

        currentDepthLimit = 1;
        try {
            // set the game root node
            IGameNode root = game.getTree().getRootNode();

            // find out some stuff about the moves in the root node
            IMove[][] allMoves = game.getLegalMoves(root);
            String role = initMatch.getRole();
            int index = game.getRoleIndex(role);
            IMove[] myMoves = allMoves[index];

            // before we know anything in more detail, we assume turn-taking zero-sum.
            // thus we determine the active player in the root by the amount of possible moves
            if (myMoves.length <= 1) {
                max = 0;
            } else if (allMoves[(index + 1) % 2].length > 1) { // obviously we have no turn taking game
                max = 2;
            }

            // for about half the prep time, we simulate the game tree to get some monte carlo heuristics
            // additionally, we refine our understanding of the game (zero-sum, turntaking, ...)
            while (System.currentTimeMillis() < endTime - match.getStartTime() * 500) {
                simulateGame(root);
            }
            System.out.println("While simulating, I got " + values.size() + " values.");
            if (max == 2) {
                System.out.println("Playing a game with simultaneous moves.");
            } else {
                System.out.println("Playing a game with alternating moves.");
            }

            // with the gained knowledge we start our search routine
            IDS(endTime, root);

        } catch (InterruptedException e) {
        }
    }

    /**
     *
     * @param endSearchTime
     * @param start
     * @return true if search finished, else false
     * @throws java.lang.InterruptedException
     */
    public boolean IDS(long endSearchTime, IGameNode start) throws InterruptedException {
        this.endSearchTime = endSearchTime;
        Boolean finishedSearch = false;

        try {
            boolean canSearchDeeper = true;
            while (canSearchDeeper) {

                System.out.println("currentDepth " + currentDepthLimit);
                System.out.println("Now: " + System.currentTimeMillis() + ", endTime: " + endSearchTime);
                System.out.println("Visited: " + nodesVisited);

                visitedStates.clear();
                canSearchDeeper = DLS(start, start.getDepth(), Integer.MIN_VALUE, Integer.MAX_VALUE);
                currentDepthLimit++;
            }

            finishedSearch = true;
        } catch (InterruptedException e) {
            System.out.println("couldnt finish search because of time");
        }

        System.out.println("stopped search and visited " + nodesVisited + " nodes.");

        return finishedSearch;
    }

    /**
     *
     * also uses:
     *	- currentDepthLimit
     *	- endSearchTime
     *	-
     *	- evaluation()
     *	- visitedStates
     *
     *	values.get(start_node) == max_int if search finished
     *
     * @param node to expand
     * @param depth < depthLimit
     * @return true if it could expand further but depthLimit stopped it
     */
    private Boolean DLS(IGameNode node, int depth, Integer alpha, Integer beta) throws InterruptedException {
        game.regenerateNode(node);
        
        nodesVisited++;

        if (System.currentTimeMillis() >= endSearchTime) {
//			propagatedHash.put(node.getState(), evaluateNode(node));
            // we don't need to evaluate the state, because good values for the decision
            // should be made in the last iteration
            // if we can't even make the iteration with depth limit 1 we just suck
            throw new InterruptedException("interrupted by time");
        }

        if (depth >= currentDepthLimit) {
            // reached the fringe -> ask for a evaluation
            propagatedHash.put(node.getState(), evaluateNode(node));
            // we can expand in the next iteration
            return true;
        }

        if (node.isTerminal()) {
            // remember this state as being good to reach -> goal Distance
            if (game.getGoalValues(node)[playerNumber] >= 80) {
                memorizeStates.add(new MemorizeState(node.getState(), game.getGoalValues(node)[playerNumber]));
            }
            // can also save in constant hash
            values.put(node.getState(), new ValuesEntry(game.getGoalValues(node), Integer.MAX_VALUE));
            propagatedHash.put(node.getState(), game.getGoalValues(node)[playerNumber]);
            return false;
        }

        if (visitedStates.containsKey(node.getState())) {
            Integer foundDepth = visitedStates.get(node.getState());
            if (foundDepth <= depth) {
                // have already seen this and therefore evaluated it
                // there can't be the same state twice on one path
                return false;
            }
        }

        visitedStates.put(node.getState(), node.getDepth() - 1);
        // recursion


        if (max == 2) {
            return DLS_simultaneous(node, depth);
        }

        // do not work with global variables in a recursive function
        List<IGameNode> children = new LinkedList<IGameNode>();
        List<IMove[]> moves = game.getCombinedMoves(node);
        Boolean expandFurther = false;
        for (IMove[] move : moves) {
            IGameNode child = game.getNextNode(node, move);
            children.add(child);
            if (DLS(child, depth + 1, alpha, beta)) {
                expandFurther = true;
            }

            // MiniMax
            game.regenerateNode(node);
            game.regenerateNode(child);
            Integer val = propagatedHash.get(child.getState());

            // constraint: (val != null) because of DLS(child, ... )

            // alpha: guaranteed value for max player
            //			-> the higher the better for max, worse for min
            //			-> start at positive INFINITY
            // beta: guaranteed value for min player
            //			-> the lower the better for min, worse for max
            //			-> start at negative INFINITY

            if (maximize(node)) {
                if (val >= beta) {
                    // val is greater than what the minimizing player gets in another branch
                    // -> min-player will not go into this branch
                    propagatedHash.put(node.getState(), beta);
                    break;
                }
                if (val > alpha) {
                    alpha = val;
                }
                propagatedHash.put(node.getState(), alpha);
                if (alpha == 100) {
                    break;
                }
            } else {
                //minimize
                if (val <= alpha) {
                    propagatedHash.put(node.getState(), alpha);
                    break;
                }
                if (val < beta) {
                    beta = val;
                }
                propagatedHash.put(node.getState(), beta);
                if (beta == 0) {
                    break;
                }
            }
        }
        return expandFurther;
    }

    private Boolean DLS_simultaneous(IGameNode node, int depth) throws InterruptedException {

        IMove[] myMoves = game.getLegalMoves(node)[playerNumber];
        IMove[] enemyMoves = game.getLegalMoves(node)[enemyNumber];

        LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

        // for each move the enemy can do
        Boolean expandFurther = false;
        for (int i = 0; i < myMoves.length; i++) {
            LinkedList<Float> line = new LinkedList<Float>();
            //for each move i enemy can do
            for (int j = 0; j < enemyMoves.length; j++) {
                // calculate the child
                IMove[] move = {myMoves[i], enemyMoves[j]};
                IGameNode child = game.getNextNode(node, move);

                if (DLS(child, depth + 1, Integer.MIN_VALUE, Integer.MAX_VALUE)) {
                    expandFurther = true;
                }

                game.regenerateNode(child);
                Float value = new Float(evaluateNode(child));

                line.add(value);
            }
            problem.add(line);
        }

        Float value = solver.solve(problem);

        game.regenerateNode(node);
        propagatedHash.put(node.getState(), Math.round(value));
//        System.out.println("value: "+value);

        return expandFurther;
    }

    public Integer evaluateNode(IGameNode node) {
        Integer val1 = propagatedHash.get(node.getState());
        if (val1 != null) {
            return val1;
        }

        // If the propagatedHash doesn't help us, call the evaluator to calculate some fancy stuff from simulation and "goal distance".
        return evaluator.evaluateNode(node, playerNumber);
    }

    /**
     * Helper function determining whether we have to maximize ourselves or the opponent.
     * @param depth
     * @return
     */
    private boolean maximize(IGameNode node) throws InterruptedException {
        game.regenerateNode(node);
        //	System.out.println("GUCKE HIER: "+node.getState());
        if (game.getLegalMoves(node)[playerNumber].length > 1) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IMove getMove(IGameNode current) {
    	if(Runtime.getRuntime().freeMemory() < 100*1024*1024) {
			visitedStates.clear();
			values.clear();
			propagatedHash.clear();
		}
    	
        IGameNode best = null;
        try {
            game.regenerateNode(current);
//			if(!searchFinished) {
            //       propagatedHash.clear();

            endTime = System.currentTimeMillis() + match.getPlayTime() * 1000 - 800;
            // a little simulation doesn't harm
            while (System.currentTimeMillis() < (endTime - match.getPlayTime() * 500)) {
                simulateGame(current);
            }
            System.out.println("While simulating, I raised values to " + values.size() + " .");
            // search a bit more, from the node arg0.
            // nope -> try to search the same depth like we did in the last search
            // but this time one from one step deeper, so ...
            //currentDepthLimit=current.getDepth()+1;
            currentDepthLimit = current.getDepth() + 1;

            IDS(endTime, current);

            if (max == 2) {
                return getMove_simultaneous(current);
            }

            System.out.println("best we can get: " + evaluateNode(current));
            PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this));
            for (IMove[] combMove : game.getCombinedMoves(current)) {
                IGameNode next = game.getNextNode(current, combMove);
                game.regenerateNode(next);

                System.out.print("Possible move: " + combMove[playerNumber]);

                System.out.print("   Value: (" + evaluateNode(next) + " ,");
                System.out.print(evaluator.evaluateNode(next, playerNumber) + " ,");
                System.out.println("");
                childs.add(next);
            }
            best = childs.peek();

            if (best == null) { // we didn't find anything.. (actually not possible)
                return game.getRandomMove(current)[playerNumber];
            }
        } catch (InterruptedException e) {
            System.out.println("WARNING: getMove() got interrupted");
        }

        return best.getMoves()[playerNumber];
    }

    private IMove getMove_simultaneous(IGameNode node) throws InterruptedException {
        // get the probabilities from the simplexSolver
        // no turn-taking -> do this weird simplex-stuff
        IMove[] myMoves = game.getLegalMoves(node)[playerNumber];
        IMove[] enemyMoves = game.getLegalMoves(node)[enemyNumber];

        LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

        // for each move the enemy can do
        for (int i = 0; i < myMoves.length; i++) {
            LinkedList<Float> line = new LinkedList<Float>();
            //for each move i enemy can do
            for (int j = 0; j < enemyMoves.length; j++) {
                // calculate the child
                IMove[] move = {myMoves[i], enemyMoves[j]};
                IGameNode child = game.getNextNode(node, move);

                Float value = new Float(evaluateNode(child));

                line.add(value);
            }
            problem.add(line);
        }
        System.out.println("P: " + problem);
        Float gameValue = solver.solve(problem);
        System.out.println("guaranteed value: "+gameValue);
        LinkedList<Float> moves = solver.getMoves(problem);

        // choose the move randomly
        int rand = random.nextInt(1000);
        System.out.println("move values: " + moves);
        Float currentSpace = 0f;
        IMove move = null;
        for (int i = 0; i < moves.size(); i++) {
            if ((moves.get(i) >= 0f) && moves.get(i) <= 1f) {
                System.out.println("Possible Move: " + myMoves[i] + " if (" + currentSpace + " <= " + rand + " < " + (currentSpace + moves.get(i) * 1000) + " ).");
                if (move == null && (currentSpace <= rand) && (rand < (currentSpace + moves.get(i) * 1000))) {
                    move = myMoves[i];
                }
                currentSpace += moves.get(i) * 1000;
            } else {
                System.out.println("Possible Move: " + myMoves[i] + " if (never )");
            }
        }

        if ((move == null)) {
            // should not happen
            System.out.println("WARNING: getMovesimultaneous() found no move while solving the problem");
        } else {
            return move;
        }

        // shall not come until here
        System.out.println("WARNING: getMovesimultaneous() did something unexpected: Just take the move with the highest potential");
        IMove bestmove = null;
        int bestVal = -1;
        for(IMove[] possibleMove : game.getCombinedMoves(node)) {
        	int val=-1;
        	try {
        		val = values.get(game.getNextNode(node, possibleMove).getState()).getGoalArray()[playerNumber];
        	} catch(Exception e) {
        		continue;
        	}
        	if(bestmove == null || val > bestVal) {
        		bestmove = possibleMove[playerNumber];
        		bestVal = val;
        	}
        }
        if(bestmove == null)
        	return game.getRandomMove(node)[playerNumber];
        else
        	return bestmove;
    }

    /**
     * dont use this anymore
     * instead use the evaluate function
     * @return
    public HashMap<IGameState, ValuesEntry> getValues(){
    return values;
    }
     */
    /*    public int getPlayerNumber() {
    return playerNumber;
    }
     */
    /*
     * new method designed to play a game according to either random choose or some heuristic function
     * while doing this, we already populate the values hash with some values
     *  (goal -> goalValue, states before goal -> average goal value achieved from this state
     */
    private void simulateGame(IGameNode start) throws InterruptedException {
        // first we just play a game
        IGameNode currentNode = start;

        game.regenerateNode(currentNode);
        int[] value;
        while (true) {

            // game over?
            if (currentNode.isTerminal()) {
                // remember this state as being good
                if (game.getGoalValues(currentNode)[playerNumber] <= 20 || game.getGoalValues(currentNode)[playerNumber] >= 80) {
                    memorizeStates.add(new MemorizeState(currentNode.getState(), game.getGoalValues(currentNode)[playerNumber]));
                }

                // set the value and break
                value = currentNode.getState().getGoalValues();
                break;
            }
            game.regenerateNode(currentNode);

            // try to prove that we have simultaneous moves
            int turns = 0;
            IMove[][] legalMoves = game.getLegalMoves(currentNode);
            for (int i = 0; i < legalMoves.length; i++) {
                if (legalMoves[i].length > 1) {
                    turns++;
                }
            }
            if (max < 2 && turns > 1) {
                max = 2;
            }

            // choose a move
            currentNode = game.getNextNode(currentNode, game.getRandomMove(currentNode));
        }

        // since the game is over, we can now go all the way back and fiddle around with the goals
        ValuesEntry existingValue = values.get(currentNode.getState());

        // if there is no value yet, we put it in
        if (existingValue == null) {
            ValuesEntry temp = new ValuesEntry(value, 1);
            values.put(currentNode.getState(), temp);
        }
        // now we look at the parent of the goal state.
        IGameNode node = currentNode;
        while (node.getParent() != null && node.getParent().getDepth() >= start.getDepth()) {
            game.regenerateNode(node);
            node = node.getParent();
            ValuesEntry entry = values.get(node.getState());
            if (entry == null) { // no value in there yet, so we just set the achieved goal value
                ValuesEntry temp = new ValuesEntry(value, 1);
                values.put(node.getState(), temp);
            } else { // otherwise, we build the average of the existing value and the achieved value in this particular game
                Integer newCount = values.get(node.getState()).getOccurences() + 1;
                int[] tempVal = values.get(node.getState()).getGoalArray();
                for (int i = 0; i < value.length; i++) {
                    tempVal[i] = ((newCount - 1) * tempVal[i] + value[i]) / newCount;
                }
                values.put(node.getState(), new ValuesEntry(tempVal, newCount));
            }
        }
    }

    /*
     * seemingly our destroy function
     */
    @Override
    public void dispose() {
        // new functionality: transform hash and serialize it
        HashMap<String, Integer> propagatedHashNew = new HashMap<String, Integer>();
        HashMap<String, ValuesEntry> valuesHashNew = new HashMap<String, ValuesEntry>();

        for (Entry<IGameState, Integer> entry : propagatedHash.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            propagatedHashNew.put(entry.getKey().toString(), entry.getValue());
        }

        for (Entry<IGameState, ValuesEntry> entry : values.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            valuesHashNew.put(entry.getKey().toString(), entry.getValue());
        }


        /* TODO: solve this
         * this will probably get mixed up if we take only the GDL as hash without
         * our player number
         * -> if we are the other player the values are 100-x actually
         
        try {
            MD5Hash hash = new MD5Hash(game.getSourceGDL().concat(String.valueOf(playerNumber)));
            FileOutputStream out = new FileOutputStream(hash.toString());

            ObjectOutputStream oos = new ObjectOutputStream(out);

            oos.writeObject(propagatedHashNew);
            oos.writeObject(valuesHashNew);
            oos.close();
        } catch (Exception ex) {
            System.out.println("Exception occurred while writing out values: ");
            ex.printStackTrace(System.out);
            Logger.getLogger(TwoPlayerStrategy.class.getName()).log(Level.SEVERE, null, ex);
        }
	*/
        this.queue.clear();
        this.queue = null;
        this.values.clear();
        this.values = null;
        this.visitedStates.clear();
        this.visitedStates = null;
        memorizeStates = null;
    }
}
