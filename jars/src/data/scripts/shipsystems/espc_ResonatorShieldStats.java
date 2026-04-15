package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData;

public class espc_ResonatorShieldStats extends BaseShipSystemScript {

	private static final float DAMAGE_RECEIVED_MULT = 1.5f;
	private static final float HARDFLUX_DISSIPATION_RATE = 2.0f;
	private static final float HARDFLUX_DISSIPATE_FRACTION = 0.8f;
	private static final float AI_HULL_BACK = 0.3f;
	private static final float AI_FLUX_BACK = 0.45f;
	private static final float AI_FLUX_FORCE_ENGAGE = 0.2f;
	
	private static final float LOW_PASS_FLUX_MAX = 4000f;
	
	private int usableState = -1;

	private float hfLast = 0f;
	private float hfToDissipate = 0f;
	private float shieldMultOffset = 1f;
	private ShipAPI ship;
	private ShipAIConfig origConfig = null;
	private boolean timidOrCautious = false;
	
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
			
			if (ship.getShipAI() != null && ship.getShipAI().getConfig() != null)
				if (ship.getShipAI().getConfig().personalityOverride != null &&
					(ship.getShipAI().getConfig().personalityOverride.equals(Personalities.TIMID) || 
					ship.getShipAI().getConfig().personalityOverride.equals(Personalities.CAUTIOUS))) {
					timidOrCautious = true;
				} else {
					ShipAIConfig config = ship.getShipAI().getConfig();
					origConfig = config.clone();
				}
		}
		
		stats.getShieldDamageTakenMult().unmodify(id);
		shieldMultOffset = ship.getMutableStats().getShieldDamageTakenMult().getMult() < 1f ? 
			1f / ship.getMutableStats().getShieldDamageTakenMult().getMult() : 1f;
		
		float shieldMult = (1f + (DAMAGE_RECEIVED_MULT - 1f) * effectLevel)
			* (1f + (shieldMultOffset - 1f) * effectLevel);
		stats.getShieldDamageTakenMult().modifyMult(id, shieldMult);
		
		stats.getBeamShieldDamageTakenMult().modifyMult(id, 1f / shieldMult);
		
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
		
		if (timidOrCautious)
			return;
		
		if (origConfig != null &&
			((ShipAPI) stats.getEntity()).getShipAI() != null) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			ShipAIConfig config = ship.getShipAI().getConfig();
			if (config != null) {
				if (effectLevel > 0 && ship.getFluxLevel() < AI_FLUX_BACK &&
						ship.getHullLevel() > AI_HULL_BACK) {
						config.personalityOverride = Personalities.RECKLESS;
						config.alwaysStrafeOffensively = true;
						if (ship.getFluxLevel() < AI_FLUX_FORCE_ENGAGE)
							config.backingOffWhileNotVentingAllowed = false;
						else
							config.backingOffWhileNotVentingAllowed = true;
						config.turnToFaceWithUndamagedArmor = false;
						config.burnDriveIgnoreEnemies = true;
					} else {
						config.copyFrom(origConfig);
					}
			}
		}
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getShieldDamageTakenMult().unmodify(id);
		stats.getBeamShieldDamageTakenMult().unmodify(id);
		hfToDissipate = 0f;
		if (timidOrCautious)
			return;
		if (origConfig != null && ((ShipAPI) stats.getEntity()).getShipAI() != null) {
			ShipAIConfig config = ((ShipAPI) stats.getEntity()).getShipAI().getConfig();
			if (config != null) {
				config.copyFrom(origConfig);
			}
		}
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
