package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.List;
				
public class espc_FeintPlugin extends BaseEveryFrameCombatPlugin {
	
	private String systemId;
	private ShipAPI ship;
	private float startTime = 0f;
	
	public espc_FeintPlugin(ShipAPI ship, String systemId) {
		this.ship = ship;
		this.systemId = systemId;
		startTime = Global.getCombatEngine().getTotalElapsedTime(false);
	}
	
    @Override
	public void advance(float amount, List<InputEventAPI> events) {
    	float intensity = Math.max(0f, 1f - ((Global.getCombatEngine().getTotalElapsedTime(false) - startTime) * 4f));
		ship.setJitterUnder(this, new Color(255, 20, 122, 130),
			intensity, 5, 4f, 8f);
		ship.setJitter(this, new Color(255, 20, 122, 130),
			intensity, 2, 4f, 8f);
		if (!ship.getFluxTracker().isOverloaded()) {
			ship.getMutableStats().getFluxDissipation().unmodifyMult(systemId);
			CombatEngineAPI engine = Global.getCombatEngine();
			engine.removePlugin(this);
		}
	}
}