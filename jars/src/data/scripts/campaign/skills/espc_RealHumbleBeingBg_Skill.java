package data.scripts.campaign.skills;

import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.FleetTotalItem;
import com.fs.starfarer.api.characters.FleetTotalSource;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_RealHumbleBeingBg_Skill {
	
	public static float MAX_CR_BONUS = 100f;
	public static int MAX_LEVEL_BONUS = 1;
	public static int OFFICER_CAP_PENALTY = -8;
	
	public static int RecomputeOfficerCap(MutableCharacterStatsAPI stats) {
		int pactOfficerCount = 0;
		if (stats.getFleet() != null && stats.getFleet().getFleetData() != null) {
			for (OfficerDataAPI officer: stats.getFleet().getFleetData().getOfficersCopy()) {
    			boolean found = false;
        		for (SkillLevelAPI skill : officer.getPerson().getStats().getSkillsCopy()) {
        			if (!found && skill.getSkill().getId().contains("espc") && skill.getLevel() > 0 &&
        				skill.getSkill().isCombatOfficerSkill()) {
        				pactOfficerCount++;
        				found = true;
        			}
        		}
			}
		}
		return pactOfficerCount;
	}
	
	public static class Level1 extends BaseSkillEffectDescription implements ShipSkillEffect, FleetTotalSource {
		
		public FleetTotalItem getFleetTotalItem() {
			return getAutomatedPointsTotal();
		}
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			if (Misc.isAutomated(stats) && 
				!Automated.isAutomatedNoPenalty(stats)) {
				float crBonus = computeAndCacheThresholdBonus(stats, "espc_auto_cr_humble", MAX_CR_BONUS, ThresholdBonusType.AUTOMATED_POINTS);
				stats.getMaxCombatReadiness().modifyFlat(id, crBonus * 0.01f, "Led by a Real Human Being");
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
			stats.getDynamic().getMod(Stats.OFFICER_MAX_LEVEL_MOD).modifyFlat(id, MAX_LEVEL_BONUS);
			stats.getOfficerNumber().modifyFlat(id, Math.min(OFFICER_CAP_PENALTY + RecomputeOfficerCap(stats), 0));
		}

		public void unapply(MutableCharacterStatsAPI stats, String id) {
			stats.getDynamic().getMod(Stats.OFFICER_MAX_LEVEL_MOD).unmodify(id);
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
	
}
