/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction.partialobservability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import ai.abstraction.AbstractAction;
import ai.abstraction.Harvest;
import ai.abstraction.LightRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/**
 *
 * @author gabriel
 */
public class RandomScript extends LightRush {
	HashMap<Long, List<UnitAction>> possibleActions;

    public RandomScript(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }
    
    public RandomScript(UnitTypeTable a_utt, HashMap<Long, List<UnitAction>> act) {
    	this(a_utt, new AStarPathFinding());

    	possibleActions = new HashMap<Long, List<UnitAction>>(act);
    }
    
    
    public RandomScript(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_utt, a_pf);
    }
    

    public void reset() {
    	super.reset();
    }

    public AI clone() {
        return new RandomScript(utt, pf);
    }
    
    public HashMap<Long, List<UnitAction>> getPossibleActions()
    {
    	System.out.println(possibleActions.size());
    	return possibleActions;
    }
    
    @Override
    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
    	System.out.println(possibleActions.size());
    	int randomPos = ThreadLocalRandom.current().nextInt(0, possibleActions.get(u.getID()).size());
    	addAction(u, possibleActions.get(u.getID()).get(randomPos));
    	possibleActions.get(u.getID()).remove(randomPos);
    }
    
    @Override
    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
    	System.out.println(possibleActions.size());
    	int randomPos = ThreadLocalRandom.current().nextInt(0, possibleActions.get(u.getID()).size());
    	addAction(u, possibleActions.get(u.getID()).get(randomPos));
    	possibleActions.get(u.getID()).remove(randomPos);
    }
    
    @Override
    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
    	System.out.println(possibleActions.size());
    	int randomPos = ThreadLocalRandom.current().nextInt(0, possibleActions.get(u.getID()).size());
    	addAction(u, possibleActions.get(u.getID()).get(randomPos));
    	possibleActions.get(u.getID()).remove(randomPos);
    }
    
    @Override
    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
    	System.out.println(possibleActions.size());
    	for (Unit u: workers)
    	{
        	int randomPos = ThreadLocalRandom.current().nextInt(0, possibleActions.get(u.getID()).size());
        	addAction(u, possibleActions.get(u.getID()).get(randomPos));
        	possibleActions.get(u.getID()).remove(randomPos);
    	}
    }
}
