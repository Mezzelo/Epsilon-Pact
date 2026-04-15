package data.scripts.campaign.skills;


import java.util.List;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public class espc_BattleTempo {

	public static float MAX_SPEED_BONUS = 10f;
	public static float MANEUVERABILITY_BONUS = 20f;
	public static float FIRE_RATE_BONUS = 10f;
	public static float FLUX_DISSIPATION_BONUS = 10f;
	public static float DAMAGE_TAKEN_REDUCTION = 10f;
	public static float UNPAUSE_TIME_MAX = 20f;
	
	public static class BattleTempoMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		private float bonusTime = UNPAUSE_TIME_MAX;
		private float lastTime = 0f;
		private boolean markForReset = false;
		public BattleTempoMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
			if (ship.getOwner() != 0) {
				ship.getListenerManager().removeListenerOfClass(BattleTempoMod.class);
				return;
			}
			float time = Global.getCombatEngine().getTotalElapsedTime(true);
			float diff = time - lastTime;
			lastTime = time;
			// ignore for the first 5 secs of the ship being deployed
			if (ship.getFullTimeDeployed() > 5f && diff > amount + 0.2f) {
				bonusTime = 0f;
			} else {
				if (bonusTime >= UNPAUSE_TIME_MAX)
					return;
				bonusTime = Math.min(bonusTime + amount, UNPAUSE_TIME_MAX);
			}

			float mult = bonusTime / UNPAUSE_TIME_MAX;
			MutableShipStatsAPI stats = ship.getMutableStats();
			stats.getFluxDissipation().modifyPercent(id, FLUX_DISSIPATION_BONUS * mult);
			stats.getBallisticRoFMult().modifyPercent(id, FIRE_RATE_BONUS * mult);
			stats.getEnergyRoFMult().modifyPercent(id, FIRE_RATE_BONUS * mult);
			stats.getMaxSpeed().modifyPercent(id, MAX_SPEED_BONUS * mult);
			stats.getAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
			stats.getDeceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
			stats.getTurnAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * 2f * mult);
			stats.getMaxTurnRate().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
			stats.getArmorDamageTakenMult().modifyMult(
					id, 1f - DAMAGE_TAKEN_REDUCTION / 100f * mult);
			stats.getHullDamageTakenMult().modifyMult(
					id, 1f - DAMAGE_TAKEN_REDUCTION / 100f * mult);
			stats.getShieldDamageTakenMult().modifyMult(
				id, 1f - DAMAGE_TAKEN_REDUCTION / 100f * mult);
		}

	}
	
	public static class Level1 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new BattleTempoMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(BattleTempoMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getFluxDissipation().modifyPercent(id, FLUX_DISSIPATION_BONUS);
			stats.getBallisticRoFMult().modifyPercent(id, FIRE_RATE_BONUS);
			stats.getEnergyRoFMult().modifyPercent(id, FIRE_RATE_BONUS);
			stats.getMaxSpeed().modifyPercent(id, MAX_SPEED_BONUS);
			stats.getAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS);
			stats.getDeceleration().modifyPercent(id, MANEUVERABILITY_BONUS);
			stats.getTurnAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS);
			stats.getMaxTurnRate().modifyPercent(id, MANEUVERABILITY_BONUS);
			stats.getArmorDamageTakenMult().modifyMult(
					id, 1f - DAMAGE_TAKEN_REDUCTION / 100f);
			stats.getHullDamageTakenMult().modifyMult(
					id, 1f - DAMAGE_TAKEN_REDUCTION / 100f);
			stats.getShieldDamageTakenMult().modifyMult(
				id, 1f - DAMAGE_TAKEN_REDUCTION / 100f);
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {
			stats.getFluxDissipation().unmodify(id);
			stats.getMaxSpeed().unmodify(id);
			stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
			stats.getTurnAcceleration().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
			stats.getArmorDamageTakenMult().unmodify(id);
			stats.getHullDamageTakenMult().unmodify(id);
			stats.getShieldDamageTakenMult().unmodify(id);
		}

		public String getEffectDescription(float level) {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
	}
}
