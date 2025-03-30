package data.scripts.campaign.skills;


import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public class espc_HumbleTastesBg_Skill {

	public static int DP_THRESHOLD = 4;
	public static float CR_PENALTY_PER_DP = 8f;
	
	public static class HumbleTastesMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		public HumbleTastesMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
			// CombatEngineAPI combatEngine = Global.getCombatEngine();
			if (ship.getFleetMember() != null && ship.getCurrentCR() > ship.getFleetMember().getRepairTracker().getMaxCR())
				ship.setCurrentCR(ship.getFleetMember().getRepairTracker().getMaxCR());
		}

	}
	
	public static class Level1 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new HumbleTastesMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(HumbleTastesMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			if (stats.getFleetMember() != null && stats.getFleetMember().getUnmodifiedDeploymentPointsCost() > DP_THRESHOLD)
			stats.getMaxCombatReadiness().modifyFlat(id, 
				(DP_THRESHOLD - stats.getFleetMember().getDeploymentPointsCost()) * CR_PENALTY_PER_DP * 0.01f, 
				"Humble Tastes");
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {
			stats.getMaxCombatReadiness().unmodify(id);
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
