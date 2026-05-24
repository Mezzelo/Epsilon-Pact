package data.scripts.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
/*
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
*/
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MezzUtils;

import java.awt.*;
import java.util.Random;

public class espc_MeCorePlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {
	/*
	public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
		PersonAPI person = Global.getFactory().createPerson();
		person.setFaction(factionId);
		person.setAICoreId(aiCoreId);
		
		person.getStats().setSkipRefresh(true);
		
		person.setName(Global.getSector().getPlayerPerson().getName());
		person.setPortraitSprite(Global.getSector().getPlayerPerson().getPortraitSprite());
		person.getStats().setLevel(Global.getSector().getPlayerPerson().getStats().getLevel());
		MutableCharacterStatsAPI stats = Global.getSector().getPlayerPerson().getStats();
		for (SkillLevelAPI skill : stats.getSkillsCopy())
			person.getStats().setSkillLevel(skill.getSkill().getId(), skill.getLevel());
		
		person.setPersonality(Personalities.RECKLESS);
		person.setRankId(Ranks.SPACE_CAPTAIN);
		person.setPostId(null);
		
		person.getStats().setSkipRefresh(false);
		
		return person;
	}*/
	
	public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
		return Global.getSector().getPlayerPerson();
	}

	@Override
	public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
		float opad = 10f;
		Color text = person.getFaction().getBaseUIColor();
		Color bg = person.getFaction().getDarkUIColor();
		tooltip.addSectionHeading(MezzUtils.getString("espc_nexbackgrounds", "mecoreheading"), text, bg, Alignment.MID, 20);
		tooltip.addPara(MezzUtils.getString("espc_nexbackgrounds", "mecoredesc1") +
			MezzUtils.getString("espc_nexbackgrounds", "mecoredesc2"), opad, Misc.getHighlightColor());
	}

}
