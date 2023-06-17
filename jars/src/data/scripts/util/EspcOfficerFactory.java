package data.scripts.util;
// shorthand because i've no self control.

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;

public class EspcOfficerFactory {
	
	public static PersonAPI MakePilot(String first, String last, FullName.Gender gender, String personality, String portrait, String faction, int level) {
		PersonAPI pilot = Global.getSector().getFaction(faction).createRandomPerson(gender);
        pilot.getName().setFirst(first);
        pilot.getName().setLast(last);
        pilot.getName().setGender(gender);
        pilot.setPersonality(personality);
        pilot.setPortraitSprite(portrait);
        pilot.setFaction(faction);
		pilot.getStats().setLevel(level);
		return pilot;
	}
	
	public static void PopulateSkills(PersonAPI pilot, String[] addSkills, int[] skillLevels) {
		for (int i = 0; i < addSkills.length; i++)
			pilot.getStats().setSkillLevel(addSkills[i], (float) skillLevels[i]);
	}
	
}