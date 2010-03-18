package player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		
		RuleOptimizer ruler = RuleOptimizer.getInstance();
		if(!ruler.optimized) {
			ruler.reorderGDL(gdl);
		}
		
		List<String> toHave = ruler.goalRules;
		List<String> toNotHave = ruler.negGoalRules;

		int count = 0;
		int fulfilled = 0;
		for(String rule : toHave) {
			if((stepcounter.length() > 1 && rule.matches(".*"+stepcounter+".*")) || rule.length() < 2)
				continue;
			else {
				//System.out.println("test");
				if(rule.matches("\\(true (.*)\\)"))
					rule = rule.substring(5, rule.length()-1);
				else
					continue;
				
				String curRule = rule;
				// check for spaces where there shouldn't be any
				if(rule.substring(0, 1).equals(" "))
					curRule = rule.substring(1);
				
				if(curRule.substring(curRule.length()-2, curRule.length()-1).equals(" "))
					curRule = curRule.substring(0, curRule.length()-1);
				
				//System.out.println("Does "+"\\(.*"+rule+".*\\)"+" match "+state+" ?");
				String regex = Pattern.compile("\\?[a-zA-Z0-9]+").matcher(curRule).replaceAll("[^\\\\( ]*");
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(state);
				if(rule.matches(".*\\?[a-zA-Z0-9]+.*")) {
					count += state.length();
					while(m.find()) {
						fulfilled += regex.length();
					}
				} else {
					count++;
					if(!stepcounter.equals("") && state.matches(".*room a.*")) {
						System.out.println("Yep");
					}
					if(state.matches(".*"+regex+".*"))
						fulfilled++;
				}
			}
		}
		for(String rule : toNotHave) {
			if(stepcounter.length() > 1 && rule.matches(".*"+stepcounter+".*"))
				continue;
			else {
				count++;
				//System.out.println("test");
				if(rule.matches("\\(true (.*)\\)"))
					rule = rule.substring(5, rule.length()-1);
				else
					continue;
				
				String curRule = rule;
				// check for spaces where there shouldn't be any
				if(rule.substring(0, 1).equals(" "))
					curRule = rule.substring(1);
				
				if(curRule.substring(curRule.length()-2, curRule.length()-1).equals(" "))
					curRule = curRule.substring(0, curRule.length()-1);
				
				String regex = Pattern.compile("\\?[a-zA-Z0-9]+").matcher(curRule).replaceAll("[^\\\\( ]*");
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(state);
				if(rule.matches("\\?[a-zA-Z0-9]+")) {
					fulfilled += state.length();
					count += state.length();
					while(m.find()) {
						fulfilled -= regex.length();
					}
				} else {
					count++;
					if(!state.matches(".*"+regex+".*"))
						fulfilled++;
				}
				
				
				
				/*if(!state.matches("\\(.*"+rule.replaceAll("\\?[a-zA-Z0-9]+", ".*")+".*\\)"))
					fulfilled++;*/
			}
		}
		Float f = new Float(((new Float(fulfilled))/(new Float(count))*100.0));
		int value = Math.round(f);
		if(value < 0 || value > 100) {
			System.out.println("Found weird value: "+value);
		}
		if(!stepcounter.equals("") && state.matches(".*room a.*")) {
			return value;
		}
		return value;
	}

}
