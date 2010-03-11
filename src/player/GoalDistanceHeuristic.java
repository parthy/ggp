package player;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.palamedes.gdl.core.model.IFluent;
import org.eclipse.palamedes.gdl.core.model.IGameNode;

public class GoalDistanceHeuristic implements IHeuristic {

	private String gdl;
	
	public GoalDistanceHeuristic(String gdlSource) {
		this.gdl = gdlSource;
	}
	
	
	@Override
	public int calculateHeuristic(IGameNode node) {
		return calculateHeuristic(node, "");
	}
	
	public int calculateHeuristic(IGameNode node, String stepcounter) {
		String state = node.getState().toString();
		
		RuleOptimizer ruler = new RuleOptimizer();
		String rules = ruler.reorderGDL(this.gdl);
		
		List<String> toHave = ruler.goalRules;
		int count = 0;
		int fulfilled = 0;
		for(String rule : toHave) {
			if(stepcounter.length() > 1 && rule.matches(".*"+stepcounter+".*"))
				continue;
			else {
				count++;
				//System.out.println("test");
				if(rule.matches("\\(true (.*)\\)"))
					rule = rule.substring(5, rule.length()-1);
				if(rule.substring(0, 1).equals(" "))
					rule = rule.substring(1);
				//System.out.println("Does "+"\\(.*"+rule+".*\\)"+" match "+state+" ?");
				if(state.matches("\\(.*"+rule+".*\\)"))
					fulfilled++;
				else
					System.out.println("no");
			}
		}
		int value = (int) ((new Float(fulfilled))/(new Float(count))*100.0);
		return value;
	}

}
