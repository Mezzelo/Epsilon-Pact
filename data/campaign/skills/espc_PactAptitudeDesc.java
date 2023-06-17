package data.campaign.skills;

import java.awt.Color;

import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.util.Misc;

public class espc_PactAptitudeDesc {
	public static class Level0 implements DescriptionSkillEffect {
		public String getString() {
			//return "The maximum level of all skills governed by this aptitude is limited to the level of the aptitude.";
			return BaseIntelPlugin.BULLET + "The skills in this tree cannot be learned independently\n"
				+BaseIntelPlugin.BULLET + "They may only be unlocked through other means\n"
				// +BaseIntelPlugin.BULLET + "Reaching the top tier requires 4 skill points, plus 1 more to take one of the top skills\n"
				// +BaseIntelPlugin.BULLET + "Taking the second top tier skill requires an additional 2 points spent in lower tier skills\n"
				// +BaseIntelPlugin.BULLET + "Skills that only affect the piloted ship can be made \"elite\" at the cost of a " + Misc.STORY + " point\n"
				// +BaseIntelPlugin.BULLET + "Skills that have been made elite in the past can be re-made elite at no cost\n"
			
			;
		}
		public Color[] getHighlightColors() {
			Color h = Misc.getHighlightColor();
			Color s = Misc.getStoryOptionColor();
			return new Color[] {h, h, h, s};
		}
		// "" + Misc.STORY + " point"
		public String[] getHighlights() {
			return new String[] {"cannot", "other", "means"};
		}
		public Color getTextColor() {
			return Misc.getTextColor();
			//return null;
		}
	}
}
