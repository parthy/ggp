package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.IStrategy;
import org.eclipse.palamedes.gdl.core.simulation.Match;

/**
 *
 * @author konrad
 */
public class MainStrategy implements IStrategy {

	private IStrategy myStrategy;
	private long firstEndTime;
	
	public MainStrategy(long endTime) {
		this.firstEndTime = endTime;
	}
	
	public void initMatch(Match initMatch) {
		if(initMatch.getGame().getRoleCount() == 1){
			this.myStrategy = new OnePlayerSearch();
			((OnePlayerSearch) myStrategy).setFirstEndTime(firstEndTime);
		} else if(initMatch.getGame().getRoleCount() == 2){
			this.myStrategy = new TwoPlayerStrategy();
			((TwoPlayerStrategy) myStrategy).setFirstEndTime(firstEndTime);
		} else {
			this.myStrategy = new MultiPlayerStrategy();
			((MultiPlayerStrategy) myStrategy).setFirstEndTime(firstEndTime);
		}
		
		myStrategy.initMatch(initMatch);
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		return this.myStrategy.getMove(arg0);
	}

	public void dispose() {
		System.out.println("Free Memory before gc: "+Runtime.getRuntime().freeMemory());
		this.myStrategy.dispose();
		myStrategy = null;
		System.gc();
		System.out.println("Free Memory after gc: "+Runtime.getRuntime().freeMemory());
	}

	public double getReliabilityValue() {
		return this.getReliabilityValue();
	}

	public double getExpectedGoalValue() {
		return this.getExpectedGoalValue();
	}

	@Override
	public double getHeuristicValue(IGameNode arg0) {
		return myStrategy.getHeuristicValue(arg0);
	}

}
