package data.scripts.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData;

// import data.scripts.shipsystems.espc_FeintPlugin;

public class espc_FeintStats extends BaseShipSystemScript {

	private static final float FLUX_THRESHOLD = 0.8f;
	private static final float OVERLOAD_DURATION = 0.5f;
	
// 	private static final float DAMAGE_RECEIVED_MULT = 0.3f;
	private static final float DISSIPATION_MULT = 32f;
	private boolean debounce = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		ShipAPI ship = (ShipAPI) stats.getEntity();

		if (state == State.IN) {
			ship.setJitterUnder(this, new Color(255, 20, 122, 130),
				effectLevel, 5, 4f, 8f);
			ship.setJitter(this, new Color(255, 20, 122, 130),
				effectLevel, 2, 4f, 8f);
		} else {
			if (!debounce) {
				debounce = true;
				Global.getSoundPlayer().playSound("shield_burnout", 0.9f, 1.1f, ship.getLocation(), ship.getVelocity());
				ship.getMutableStats().getFluxDissipation().modifyMult(id, DISSIPATION_MULT);
				if (!ship.getFluxTracker().isOverloaded())
					ship.getFluxTracker().beginOverloadWithTotalBaseDuration(
						Math.max(0.1f, (
							OVERLOAD_DURATION * (1f + 
								ship.getMutableStats().getOverloadTimeMod().getFlatBonus() +
								ship.getMutableStats().getOverloadTimeMod().getPercentMod() / 100f
							) * ship.getMutableStats().getOverloadTimeMod().getMult()
						))
					);
				else
					ship.getFluxTracker().setOverloadDuration(
						Math.max(0.1f, (
							OVERLOAD_DURATION * (1f + 
								ship.getMutableStats().getOverloadTimeMod().getFlatBonus() +
								ship.getMutableStats().getOverloadTimeMod().getPercentMod() / 100f
							) * ship.getMutableStats().getOverloadTimeMod().getMult()
						))	
					);
				Global.getCombatEngine().addPlugin(new espc_FeintPlugin(ship, id));	
			}
		}
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		debounce = false;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != SystemState.IDLE || system.getAmmo() <= 0)
			return null;
		if (ship.getFluxTracker().getFluxLevel() >= FLUX_THRESHOLD)
			return "READY";
		else
			return "FLUX BELOW THRESHOLD";
	}
	
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return (system.getAmmo() > 0 && ship.getFluxTracker().getFluxLevel() >= FLUX_THRESHOLD);
	}
}