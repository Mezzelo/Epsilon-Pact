package data.campaign.skills;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

// import java.awt.Color;

import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.MarketSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
// import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
// import com.fs.starfarer.api.ui.TooltipMakerAPI;
// import com.fs.starfarer.api.util.Misc;

// import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;

public class espc_Voice {
	
	public static int SUPPLY_BONUS = 1;
	public static int STABILITY_BONUS = 5;
	
	public static class Level1 implements CharacterStatsSkillEffect {
		public void apply(MutableCharacterStatsAPI stats, String id, float level) {
			stats.getDynamic().getMod(Stats.SUPPLY_BONUS_MOD).modifyFlat(id, SUPPLY_BONUS);
		}

		public void unapply(MutableCharacterStatsAPI stats, String id) {
			stats.getDynamic().getMod(Stats.SUPPLY_BONUS_MOD).unmodifyFlat(id);
		}
		
		public String getEffectDescription(float level) {
			return "All industries supply " + SUPPLY_BONUS + " more unit of all the commodities they produce\n";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.GOVERNED_OUTPOST;
		}
	}
	
	public static class Level2 implements MarketSkillEffect {

		public void apply(MarketAPI market, String id, float level) {
			market.getStability().modifyFlat(id, STABILITY_BONUS, "Voice of Many");
		}

		public void unapply(MarketAPI market, String id) {
			market.getStability().unmodifyFlat(id);
		}
		
		public String getEffectDescription(float level) {
			return "+" + STABILITY_BONUS + " stability";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.GOVERNED_OUTPOST;
		}
	}
	
	public static class Level3 implements MarketSkillEffect {

		public void apply(MarketAPI market, String id, float level) {
		}

		public void unapply(MarketAPI market, String id) {
		}
		
		public String getEffectDescription(float level) {
			return "Tribal Enclaves grant a significant bonus to ground defenses";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.GOVERNED_OUTPOST;
		}
	}
	
}


