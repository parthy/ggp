package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGameState;

public interface IHeuristic {
	public int calculateHeuristic(IGameState state);
}
