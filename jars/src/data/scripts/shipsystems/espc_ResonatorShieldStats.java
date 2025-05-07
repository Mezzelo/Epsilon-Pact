package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData;

public class espc_ResonatorShieldStats extends BaseShipSystemScript {

	private static final float DAMAGE_RECEIVED_MULT = 2f;
	private static final float BEAM_DAMAGE_RECEIVED_MULT = 0.5f;
	private static final float HARDFLUX_DISSIPATION_RATE = 2.0f;
	private static final float HARDFLUX_DISSIPATE_FRACTION = 0.85f;
	
	private static final float LOW_PASS_FLUX_MAX = 4000f;
	
	private int usableState = -1;

	private float hfLast = 0f;
	private float hfToDissipate = 0f;
	private float shieldMultOffset = 1f;
	private ShipAPI ship;
	
	public float getHfToDissipate() {
		return hfToDissipate;
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null || usableState == 0)
			return;

		CombatEngineAPI combatEngine = Global.getCombatEngine();
		float amount = combatEngine.getElapsedInLastFrame();
		if (amount == 0f)
			return;

		if (ship == null) {
			ship = (ShipAPI) stats.getEntity();
			ship.setCustomData("espc_resonatorShieldRef", this);
		}
		
		stats.getBeamShieldDamageTakenMult().modifyMult(id, 
			1f + (BEAM_DAMAGE_RECEIVED_MULT - 1f) * effectLevel);
		
		stats.getShieldDamageTakenMult().unmodify(id);
		shieldMultOffset = ship.getMutableStats().getShieldDamageTakenMult().getMult() < 1f ? 
			1f / ship.getMutableStats().getShieldDamageTakenMult().getMult() : 1f;
		
		stats.getShieldDamageTakenMult().modifyMult(id, 
			(1f + (DAMAGE_RECEIVED_MULT - 1f) * effectLevel)
			 * (1f + (shieldMultOffset - 1f) * effectLevel));
		
		hfToDissipate += Math.max((ship.getFluxTracker().getHardFlux() - hfLast) * 
			(1f - (1f - HARDFLUX_DISSIPATE_FRACTION) / shieldMultOffset) * effectLevel, 0f);

		if (hfToDissipate > 0f) {
			float dissipation = ship.getMutableStats().getFluxDissipation().getModifiedValue(), dissipate;
			dissipate = Math.min(hfToDissipate, dissipation * amount * HARDFLUX_DISSIPATION_RATE * effectLevel);
			ship.getFluxTracker().setHardFlux(Math.max(ship.getFluxTracker().getHardFlux() - dissipate, 0f));
			ship.getFluxTracker().decreaseFlux(dissipate);
			hfToDissipate = Math.max(hfToDissipate - dissipate, 0f);
			if (combatEngine.getPlayerShip() != null && combatEngine.getPlayerShip().equals(ship))
				Global.getSoundPlayer().applyLowPassFilter(
					1f - 0.2f * effectLevel * Math.min(1f, hfToDissipate / LOW_PASS_FLUX_MAX),
					1f - effectLevel * Math.min(1f, hfToDissipate / LOW_PASS_FLUX_MAX)
				);
				
		}
		
		hfLast = ship.getFluxTracker().getHardFlux();
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getShieldDamageTakenMult().unmodify(id);
		stats.getBeamShieldDamageTakenMult().unmodify(id);
		hfToDissipate = 0f;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (usableState == 0)
			return null;
		if (index == 0)
			return new StatusData("dissipating " + 
				(int) ((1f - (1f - HARDFLUX_DISSIPATE_FRACTION) / shieldMultOffset) 
				* effectLevel * 100f) + "% of hard flux received", false);
		// else if (index == 1)
		// 	return new StatusData("beam damage received -" + (int) ((1f - BEAM_DAMAGE_RECEIVED_MULT) * effectLevel * 100f) + "%", false);
		else if (index == 1)
			return new StatusData("non-beam shield damage received +" + 
				(int) ((DAMAGE_RECEIVED_MULT * shieldMultOffset - 1f) * effectLevel * 100f) + "%", true);
		
		return null;
	}
	
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (usableState == 0)
			return "NO SHIELD";
		
		if (system.getState() == SystemState.IDLE)
			return "READY";
		else
			return "";
	}
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (usableState == -1)
			usableState = ship.getShield() != null &&
				!ship.getShield().getType().equals(ShieldType.NONE) &&
				!ship.getShield().getType().equals(ShieldType.PHASE) ? 1 : 0;
		
		if (usableState == 1)
			return true;
		else
			return false;
	}
	
}
