package player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleOptimizer {
	public String reorderGDL(String gameDescription) {
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
    	p = Pattern.compile("^[ ]*(\\([^\\(\\?\\)]*\\)) ");
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
    		rulesString += " "+optimizeRule(rule);
    	}
		
    	System.out.println(rulesString);
    	return rulesString;
	}
    
    private String optimizeRule(String rule) {
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
	    			
	    			// if we know all needed vars or we have to deal with true or does, we move it.
	    			if((knownVars.containsAll(neededVars) && body.indexOf(literal) > toInsertAfter) || (toInsertAfter == -1 && literal.matches(".*(true|does).*"))) {
	    				collector.add(literal);
	    				it.remove();
	    			}
	    		}
	    		
	    		// let's move distinct clauses to the beginning of the collector in order to throw out unhelpful solutions ASAP.
	    		Iterator<String> it2 = collector.iterator();
	    		ArrayList<String> collector2 = new ArrayList<String>();
	    		while(it2.hasNext()) {
	    			String cur = it2.next();
	    			if(cur.matches(".*(distinct|true|does).*")) {
	    				it2.remove();
	    				collector2.add(0, cur);
	    			} else {
	    				collector2.add(cur);
	    			}
	    		}
	    		
	    		if(toInsertAfter == -1) { // if we have to move to the beginning, just add the remaining body to the collector. 
	    			collector2.addAll(body);
	    			body = new ArrayList<String>(collector2);
	    		} else { // otherwise, add the collector to the front part of the list, then the remaining body. 
					if(toInsertAfter+1 > body.size()) toInsertAfter = body.size()-1;
					ArrayList<String> newList = new ArrayList<String>(body.subList(0, toInsertAfter+1));
	    			newList.addAll(collector2);
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
    
    private ArrayList<String> getVarsFromLiteral(String lit) {
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
    
    public static List<String> getListFromFluentString(String fluentString) {
		List<String> list = new ArrayList<String>();
		
		boolean opened=false;
		int openPars=0;
		fluentString = fluentString.substring(1, fluentString.length()-1)+" ";
		
		while(true) {
			//System.out.println("Parsing "+fluentString);
			for(int i=0; i<fluentString.length(); i++) {
				if(fluentString.charAt(i) == '(') {
					opened = true;
					openPars++;
					continue;
				}
				if(fluentString.charAt(i) == ')') {
					openPars--;
					continue;
				}
				if(opened && openPars == 0) {
					//System.out.println("Found Fluent: "+fluentString.substring(0, i));
					list.add(fluentString.substring(0, i));
					fluentString = fluentString.substring(i+1);
					opened = false;
					break;
				}
			}
			if(fluentString.length() < 3) break;
		}
		
		return list;
	}

	public static int calculateTruthDegree(String candidate, String goal) {
		int truth = 0;
		
		//System.out.println("Calculating truth of \n"+candidate+"\n with respect to \n"+goal);
		
		List<String> fluentsC = getListFromFluentString(candidate);
		List<String> fluentsG = getListFromFluentString(goal);
		
		// We walk trough all the goal's fluents and search for them in the candidate's list.
		// If we find one, we add the percentage of one true fluent to the truth value.
		for(String fluent : fluentsG) {
			if(fluentsC.contains(fluent))
				truth += (int) (100/fluentsG.size());
		}
		
		//System.out.println("Came up with "+truth);
		
		return truth;
	}
}
