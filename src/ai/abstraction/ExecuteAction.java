/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction;

import rts.GameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import util.XMLWriter;

/**
 *
 * @author santi
 */
public class ExecuteAction extends AbstractAction {
	UnitAction action;
	UnitType type;

	boolean completed = false;

	public ExecuteAction(Unit u, UnitAction a) {
		super(u);
		type = u.getType();
		action = new UnitAction(a);
	}

	public boolean completed(GameState pgs) {
		return completed;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ExecuteAction))
			return false;
		ExecuteAction a = (ExecuteAction) o;
		if (type != a.type || action != a.action || unit.getID() != a.unit.getID())
			return false;

		return true;
	}

	public void toxml(XMLWriter w) {
		w.tagWithAttributes(action.getActionName(), "unitID=\"" + unit.getID() + "\" type=\"" + type.name + "\"");
		w.tag("/" + action.getActionName());
	}

	public UnitAction execute(GameState gs, ResourceUsage ru) {
		completed = true;
		return action;
	}
}
