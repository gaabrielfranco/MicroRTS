/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction.partialobservability;

import java.util.List;

import ai.abstraction.LightRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import rts.GameState;
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
	Long unitID;
	UnitAction possibleAction;

	public RandomScript(UnitTypeTable a_utt) {
		this(a_utt, new AStarPathFinding());
	}

	public RandomScript(UnitTypeTable a_utt, Long uID, UnitAction a) {
		this(a_utt, new AStarPathFinding());

		this.unitID = uID;
		this.possibleAction = a;
	}

	public RandomScript(UnitTypeTable a_utt, PathFinding a_pf) {
		super(a_utt, a_pf);
	}

	public void reset() {
		super.reset();
	}

	public AI clone() {
		return new RandomScript(utt, unitID, possibleAction);
	}

	public Long getUnitID() {
		return this.unitID;
	}

	public UnitAction getAct() {
		return this.possibleAction;
	}

	@Override
	public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
		if (u.getID() == this.unitID)
			addAction(u, this.possibleAction);
		//else System.out.println("MEL RUIM");
	}

	@Override
	public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
		if (u.getID() == this.unitID)
			addAction(u, this.possibleAction);
		//else System.out.println("BAS RUIM");
	}

	@Override
	public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
		if (u.getID() == this.unitID)
			addAction(u, this.possibleAction);
		//else System.out.println("BAR RUIM");
	}

	@Override
	public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
		for (Unit u : workers) {
			if (u.getID() == this.unitID) {
				addAction(u, this.possibleAction);
			} //else System.out.println("WORK RUIM");
		}
	}
}
