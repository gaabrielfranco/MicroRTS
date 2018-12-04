/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction.partialobservability;

import java.util.LinkedList;
import java.util.List;

import ai.abstraction.AbstractAction;
import ai.abstraction.Harvest;
import ai.abstraction.RangedRush;
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
 * @author santi
 */
public class PORangedRushV2 extends RangedRush {
	
	GameState gameState;
	Unit unit;
	UnitAction possibleAction;

    public PORangedRushV2(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }
    
    public PORangedRushV2(UnitTypeTable a_utt, GameState gs, Unit u, UnitAction a) {
    	this(a_utt, new AStarPathFinding());

    	gameState = gs;
    	unit = u;
    	possibleAction = a;
    }
    
    public PORangedRushV2(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_utt, a_pf);
    }

    public void reset() {
    	super.reset();
    }

    public AI clone() {
        return new PORangedRushV2(utt, pf);
    }
    
    public Unit getUnit()
    {
    	return unit;
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        
        if (unit == u && gameState == gs)
        {
        	addAction(u, possibleAction);
        	return;
        }
        
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        } else if (gs instanceof PartiallyObservableGameState) {
            PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
            // there are no enemies, so we need to explore (find the nearest non-observable place):
            int closest_x = 0;
            int closest_y = 0;
            closestDistance = -1;
            for(int i = 0;i<pgs.getHeight();i++) {
                for(int j = 0;j<pgs.getWidth();j++) {
                    if (!pogs.observable(j, i)) {
                        int d = (u.getX() - j)*(u.getX() - j) + (u.getY() - i)*(u.getY() - i);
                        if (closestDistance == -1 || d<closestDistance) {
                            closest_x = j;
                            closest_y = i;
                            closestDistance = d;
                        }
                    }
                }
            }
            if (closestDistance!=-1) {
                move(u, closest_x, closest_y);
            }
        }
    }
    
    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        if (unit == u && gameState.getPhysicalGameState() == pgs)
        {
        	addAction(u, possibleAction);
        	return;
        }
        
        int nworkers = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
        }
        if (nworkers < 1 && p.getResources() >= workerType.cost) {
            train(u, workerType);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        if (unit == u && gameState.getPhysicalGameState() == pgs)
        {
        	addAction(u, possibleAction);
        	return;
        }
        
        if (p.getResources() >= rangedType.cost) {
            train(u, rangedType);
        }
    }
    
    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
        int nbases = 0;
        int nbarracks = 0;

        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        freeWorkers.addAll(workers);

        if (workers.isEmpty()) {
            return;
        }
        
        for(int i = 0; i < freeWorkers.size(); i++)
        {
        	Unit u = freeWorkers.get(i);
        	if (unit == u && gameState.getPhysicalGameState() == pgs)
            {
            	addAction(u, possibleAction);
            	freeWorkers.remove(i);
            	break;
            }
        }

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases == 0 && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += baseType.cost;
            }
        }

        if (nbarracks == 0 && !freeWorkers.isEmpty()) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += barracksType.cost;
            }
        }


        // harvest with all the free workers:
        for (Unit u : freeWorkers) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isResource) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) {
                AbstractAction aa = getAbstractAction(u);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                } else {
                    harvest(u, closestResource, closestBase);
                }
            }
        }
    }

}
