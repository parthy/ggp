package player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.palamedes.gdl.connection.Message;
import org.eclipse.palamedes.gdl.connection.Player;
import org.eclipse.palamedes.gdl.core.model.GameFactory;
import org.eclipse.palamedes.gdl.core.model.IGame;
import org.eclipse.palamedes.gdl.core.model.utils.Game;
import org.eclipse.palamedes.gdl.core.simulation.IStrategy;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;
import org.eclipse.palamedes.gdl.core.simulation.strategies.SMonteCarlo;

public final class MyPlayer extends Player {
	public static IStrategy strategy;
    /**
     * This method is called when a new match begins.
	 *
     * <br/>
     * msg="(START MATCHID ROLE GAMEDESCRIPTION STARTCLOCK PLAYCLOCK)"<br/>
     * e.g. msg="(START tictactoe1 white ((role white) (role black) ...) 1800 120)" means:
     * <ul>
     *   <li>the current match is called "tictactoe1"</li>
     *   <li>your role is "white",</li>
     *   <li>
     *     after at most 1800 seconds, you have to return from the
     *     commandStart method
     *   </li>
     *   <li>for each move you have 120 seconds</li>
     * </ul>
     */
    public void commandStart(Message msg){

        // create the clock
        int playClock  = msg.getPlayClock()  - 1;
        int startClock = msg.getStartClock() - 1;

        System.out.println( "Start Clock " + startClock );
        System.out.println( "Play  Clock " + playClock  );

        // get the game from the database
        /** XXX: You can change here between GameFactory.JAVAPROVER, GameFactory.JOCULAR
         *       and GameFactory.PROLOG. Both Stanford resolvers have some
         *       drawbacks. Currently JAVAPROVER is slower whereas JOCULAR
         *       does not support the full range of special characters like '+'.
         *       GameFactory.PROLOG is probably the fastest option, but you need
         *       to have Eclipse-Prolog installed (http://www.eclipse-clp.org/). */
        GameFactory factory = GameFactory.getInstance();
        IGame runningGame = factory.createGame( GameFactory.PROLOGPROVER,
                                                    MyPlayer.reorderGDL(msg.getGameDescription()) );
        System.out.println("MyPlayer created the game.");

        
        /** XXX: If you implement another strategy here is the place to instantiate it */
		/** and if you wanna use a heuristic here is also the place to put it in */

        strategy = new MainStrategy();

        System.out.println( "MyPlayer created the strategy "      +
                            strategy.getClass().getSimpleName() +
                            "." );

        System.out.println( "MyPlayer starts contemplate while doing yoga." );

        // create a match
        realMatch = createRealMatch( msg.getMatchId(),
                                   	 runningGame,
                                   	 strategy,
                                   	 msg.getRole(),
                                   	 startClock,
                                   	 playClock );
        
        System.out.println( "MyPlayer created the match." );
        System.out.println( "MyPlayer is prepared to start the game." );
        System.out.println("stats:"+runningGame.getStatistic());
    }

    public static String reorderGDL(String gameDescription) {
    	ArrayList<String> facts = new ArrayList<String>();
    	ArrayList<String> rules = new ArrayList<String>();
    	String rulesString = gameDescription;
    	
    	// Search string for facts and init statements
    	Pattern p = Pattern.compile("(\\(init \\([^\\(]*\\)\\)) ");
    	Matcher m = p.matcher(rulesString);
    	//System.out.println("--- INIT ---");
    	while(m.find()) {
    		//System.out.println(rulesString.substring(m.start(1), m.end(1)));
    		facts.add(rulesString.substring(m.start(1), m.end(1)));
    		if(m.start(1) > 0 && m.end(1) < rulesString.length())
    			rulesString = rulesString.substring(0, m.start(1)-1).concat(rulesString.substring(m.end(1)));
    		else if(m.start(1) > 0 && m.end(1) >= rulesString.length())
    			rulesString = rulesString.substring(0, m.start(1)-1);
    		else if(m.start(1) <= 0 && m.end(1) < rulesString.length())
    			rulesString = rulesString.substring(m.end(1));
    		m = p.matcher(rulesString);
    	}
    	//System.out.println("--- OTHER FACTS ---");
    	p = Pattern.compile("(\\([^\\(\\?\\)]*\\)) ");
    	m = p.matcher(rulesString);
    	while(m.find()) {
    		//System.out.println(rulesString.substring(m.start(1), m.end(1))+", "+m.start(1)+":"+m.end(1));
    		facts.add(rulesString.substring(m.start(1), m.end(1)));
    		if(m.start(1) > 0 && m.end(1) < rulesString.length())
    			rulesString = rulesString.substring(0, m.start(1)-1).concat(rulesString.substring(m.end(1)));
    		else if(m.start(1) > 0 && m.end(1) >= rulesString.length())
    			rulesString = rulesString.substring(0, m.start(1)-1);
    		else if(m.start(1) <= 0 && m.end(1) < rulesString.length())
    			rulesString = rulesString.substring(m.end(1));
    		m = p.matcher(rulesString);
    	}
    	
    	// Now we only have rules. Iterate them and try to reorder there
    	p = Pattern.compile("(\\(<= [^<=]*\\)) \\(<=");
    	m = p.matcher(rulesString);
    	//System.out.println("--- RULES ---");
    	while(m.find()) {
    		//System.out.println(rulesString.substring(m.start(1), m.end(1)));
    		rules.add(rulesString.substring(m.start(1), m.end(1)));
    		if(m.start(1) > 0 && m.end(1) < rulesString.length())
    			rulesString = rulesString.substring(0, m.start(1)-1).concat(rulesString.substring(m.end(1)));
    		else if(m.start(1) > 0 && m.end(1) >= rulesString.length())
    			rulesString = rulesString.substring(0, m.start(1)-1);
    		else if(m.start(1) <= 0 && m.end(1) < rulesString.length())
    			rulesString = rulesString.substring(m.end(1));
    		m = p.matcher(rulesString);
    	}
    	// what remains is the last rule with a space at the beginning.
    	rules.add(rulesString.substring(1));
    	
    	// add facts to the game string
    	rulesString = "";
    	for(String fact : facts) {
    		rulesString += fact+" ";
    	}
    	
    	for(String rule : rules) {
    		rulesString += " "+MyPlayer.optimizeRule(rule);
    	}
		
    	System.out.println(rulesString);
    	return rulesString;
	}
    
    public static String optimizeRule(String rule) {
    	//System.out.println("Optimizing "+rule);
    	int opens = -1, lastPos=-1;
    	boolean started=false;
    	String head="";
    	ArrayList<String> body = new ArrayList<String>();
    	for(int i=0; i<rule.length(); i++) {
    		if(rule.substring(i, i+1).equals(" ")) continue;
    		if(rule.substring(i, i+1).equals("(")) opens++;
    		if(rule.substring(i, i+1).equals(")")) { if(!started) started = true; opens--; }
    		if(opens == 0 && started) { // one section done
    			if(head.equals("")) { // we are done with the head 
    				head = rule.substring(0, i+1);
    				lastPos = i+2;
    				//System.out.println("Found head: "+head);
    			} else { // we found a literal
    				body.add(rule.substring(lastPos, i+1));
    				//System.out.println("Found body literal: "+rule.substring(lastPos, i+1));
    				lastPos = i+2;
    			}
    		}
    	}
    	if(body.size() == 1) {
    		//System.out.println("Nothing to optimize, returning: "+head.concat(" "+body.get(0))+")");
    		rule = head.concat(" "+body.get(0));
    	}
    	else {
    		rule = head;
    		
    		// determine known vars from head
    		ArrayList<String> knownVars = getVarsFromLiteral(head);
    		//System.out.println("From head we know the vars: "+knownVars);
    		
    		// walk through body and place those literals at the beginning for which we know all vars.
    		boolean goFurther = true;
    		int toInsertAfter = -1;
    		while(goFurther) {
    			goFurther = false;
    			//System.out.println(knownVars+", "+toInsertAfter);
    			//System.out.println(body);
	    		Iterator<String> it = body.iterator();
	    		ArrayList<String> collector = new ArrayList<String>();
	    		while(it.hasNext()) {
	    			String literal = it.next();
	    			ArrayList<String> neededVars = getVarsFromLiteral(literal);
	    			if(knownVars.containsAll(neededVars) && body.indexOf(literal) > toInsertAfter) {
	    				collector.add(literal);
	    				it.remove();
	    			}
	    		}
	    		if(toInsertAfter == -1) { // if we have to move to the beginning, just add the remaining body to the collector. 
	    			collector.addAll(body);
	    			body = new ArrayList<String>(collector);
	    		} else { // otherwise, add the collector to the front part of the list, then the remaining body. 
	    			ArrayList<String> newList = new ArrayList<String>(body.subList(0, toInsertAfter+1));
	    			newList.addAll(collector);
	    			newList.addAll(body.subList(toInsertAfter+1, body.size()));
	    			body = new ArrayList<String>(newList);
	    		}
	    		//System.out.println("=> "+body);
	    		
	    		// afterwards find the position, where we first get new variables.
	    		for(int i=0; i<body.size(); i++) {
	    			if(!knownVars.containsAll(getVarsFromLiteral(body.get(i)))) {
	    				toInsertAfter = i;
	    				goFurther = true;
	    				knownVars.addAll(getVarsFromLiteral(body.get(i)));
	    				break;
	    			}
	    		}
    		}
    		for(String b : body) {
    			rule += " "+b;
    		}
    		//System.out.println("Returning "+rule+")");
    	}
    	return rule+")";
    }
    
    public static ArrayList<String> getVarsFromLiteral(String lit) {
    	ArrayList<String> knownVars = new ArrayList<String>();
		Pattern p = Pattern.compile("\\?([^\\)])[ \\)]");
		Matcher m = p.matcher(lit);
		while(m.find()) {
			knownVars.add(lit.substring(m.start(1), m.end(1)));
			lit = lit.substring(m.end(1));
			m = p.matcher(lit);
		}
		
		return knownVars;
    }
    
	/**
     * This method is called once for each move<br/>
     * <br/>
     * msg = "(PLAY MATCHID JOINTMOVE)<br/>
     * JOINTMOVE will be NIL for the first PLAY message and the list of the
     * moves of all the players in the previous state<br/>
     * e.g. msg="(PLAY tictactoe1 NIL)" for the first PLAY message
     *   or msg="(PLAY tictactoe1 ((MARK 1 2) NOOP))" if white marked cell (1,2)
     *   and black did a "noop".<br/>
     * @return the move of this player
     */
    public String commandPlay(Message msg){
        checkMatchId(msg);

        // only make a turn if the move list is not empty
        // is it empty it means we hit the first play message, setting the
        // initial state is done while constructing the match object
        if ( msg.hasMoves() ){
            System.out.println( "Moves from GameMaster: " +
                                Arrays.toString(msg.getMoves()) );
           
            String[] prepared = prepareMoves(msg);

            // got the initial NIL in case of length == 0
            if (prepared.length != 0)

                // prepare moves
                realMatch.makeTurn( prepared );

        }

        // real work is done here
        String move = realMatch.getMove().getMove().toUpperCase();

        // logging the resulting move
        System.out.println( "Our move: " + move );
        System.out.println("stats:"+((Game)realMatch.getGame()).getStatistic());
        return move;
    }


    /**
     * This method is called if the match is over
     *
     * msg="(STOP MATCHID JOINTMOVE)
     */
    public void commandStop(Message msg){
        checkMatchId(msg);

        // adds the moves to the match
        if ( msg.hasMoves() )

            // real work is done here
            realMatch.makeTurn( prepareMoves(msg) );

        // check if we agree to be in a final state and log the result
        if ( !realMatch.getCurrentNode().isTerminal() )
            System.out.println( "Game stopped but not finished" );
        else{
            System.out.println( "Game finished" );
			try {
	        	int[] goalvalues;
				goalvalues = realMatch.getGame().getGoalValues(realMatch.getCurrentNode());
	        	System.out.println( "goal:" );
	        	for(int i=0;i<goalvalues.length;++i){
	        		System.out.print( goalvalues[i]+" " );
	        	}
	        	System.out.println();
			} catch (InterruptedException e) {
				System.out.println("Timeout while computing goal values:");
				e.printStackTrace();
			}
        }
        
        // destroy everything in the strategy
        strategy.dispose();
	strategy = null;
    }


}
