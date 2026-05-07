package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.FleetTotalItem;
import com.fs.starfarer.api.characters.FleetTotalSource;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_RealHumanBeingBg_Skill {
	
	public static float MAX_CR_BONUS = 300f;
	
	public static class Level1 extends BaseSkillEffectDescription implements ShipSkillEffect, FleetTotalSource {
		
		public FleetTotalItem getFleetTotalItem() {
			return getAutomatedPointsTotal();
		}
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			if (Misc.isAutomated(stats) && 
					!Automated.isAutomatedNoPenalty(stats)) {
				float crBonus = computeAndCacheThresholdBonus(stats, "espc_auto_cr_humble", MAX_CR_BONUS, ThresholdBonusType.AUTOMATED_POINTS);
				stats.getMaxCombatReadiness().modifyFlat(id, Math.min(crBonus * 0.01f, 1f), "Led by a Real Human Being");
			}
		}
			
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getMaxCombatReadiness().unmodifyFlat(id);
		}
		
		public String getEffectDescription(float level) {
			return null;
		}
			
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.ALL_SHIPS;
		}
	}
	
	public static class Level2 implements CharacterStatsSkillEffect {

		public void apply(MutableCharacterStatsAPI stats, String id, float level) {
			stats.getOfficerNumber().modifyFlat(id, -8);
		}

		public void unapply(MutableCharacterStatsAPI stats, String id) {
			stats.getOfficerNumber().unmodify(id);
		}

		public String getEffectDescription(float level) {
			return null;
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.NONE;
		}
	}
	
	public static class AutoshipsOnlyMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		public AutoshipsOnlyMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
			// CombatEngineAPI combatEngine = Global.getCombatEngine();
			if (!ship.isShuttlePod() && !Misc.isAutomated(ship) && ship.getCurrentCR() > 0f &&
				!ship.getVariant().hasHullMod(HullMods.NEURAL_INTERFACE))
				ship.setCurrentCR(0f);
		}

	}
	
	public static class Level3 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new AutoshipsOnlyMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(AutoshipsOnlyMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			if (stats.getFleetMember() != null && !Misc.isAutomated(stats.getFleetMember()))
			stats.getMaxCombatReadiness().modifyFlat(id, 
				-1f, 
				"You can't pilot this!");
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
	
	public static class Level5 extends BaseSkillEffectDescription implements CharacterStatsSkillEffect {

		public void apply(MutableCharacterStatsAPI stats, String id, float level) {
			if (stats.isPlayerStats()) {
				Misc.getAllowedRecoveryTags().add(Tags.AUTOMATED_RECOVERABLE);
			}
		}

		public void unapply(MutableCharacterStatsAPI stats, String id) {
			if (stats.isPlayerStats()) {
				Misc.getAllowedRecoveryTags().remove(Tags.AUTOMATED_RECOVERABLE);
			}
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
