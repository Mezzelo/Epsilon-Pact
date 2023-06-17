package data.campaign.ids;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
// import com.fs.starfarer.api.impl.campaign.ids.People;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
// import com.fs.starfarer.api.impl.campaign.ids.Voices;

import data.scripts.util.EspcOfficerFactory;

public class espc_People {
	        
	public static PersonAPI getPerson(final String id) {
		return Global.getSector().getImportantPeople().getPerson(id);
	}
	        
	public static void create() {
        final ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        
        MarketAPI market =  Global.getSector().getEconomy().getMarket("espc_lunron_market");
        if (market != null) {
			PersonAPI person = Global.getFactory().createPerson();
			person.setId("espc_nola");
			person.setFaction("epsilpac");
			person.setGender(Gender.FEMALE);
			person.setRankId(Ranks.FACTION_LEADER);
			person.setPostId(Ranks.POST_FACTION_LEADER);
			person.setImportance(PersonImportance.VERY_HIGH);
			person.getName().setFirst("Nola");
			person.getName().setLast("Ganymede");
			// person.setVoice(Voices.SPACER);
			person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_nola"));
			
			EspcOfficerFactory.PopulateSkills(person, new String[]{
				Skills.HELMSMANSHIP, Skills.FIELD_MODULATION, 
				Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS, 
				Skills.SYSTEMS_EXPERTISE, Skills.GUNNERY_IMPLANTS, 
				Skills.TACTICAL_DRILLS, Skills.COORDINATED_MANEUVERS, 
				Skills.WOLFPACK_TACTICS, Skills.CREW_TRAINING,
				Skills.ORDNANCE_EXPERTISE, Skills.FLUX_REGULATION}, new int[]{
				2, 2, 2, 
				2, 2, 2, 
				1, 1, 1, 1, 2, 1});
			
			ip.addPerson(person);

        }
        
        market =  Global.getSector().getEconomy().getMarket("espc_bruniel_market");
        if (market != null) {
			PersonAPI person = Global.getFactory().createPerson();
			person = Global.getFactory().createPerson();
			person.setId("espc_isabelle");
			person.setFaction("epsilpac");
			person.setGender(Gender.FEMALE);
			person.setRankId(Ranks.AGENT);
			person.setPostId(Ranks.POST_AGENT);
			person.setImportance(PersonImportance.HIGH);
			person.getName().setFirst("Isabelle");
			person.getName().setLast("de' Medici");
			// person.setVoice(Voices.BUSINESS);
			person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_isabelle"));
			
			EspcOfficerFactory.PopulateSkills(person, new String[]{
				Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, 
				Skills.SYSTEMS_EXPERTISE, Skills.ENERGY_WEAPON_MASTERY, 
				Skills.ORDNANCE_EXPERTISE}, new int[]{
				2, 2,
				1, 2, 2});
			
        	market.getCommDirectory().addPerson(person);
        	market.addPerson(person);
        	
			ip.addPerson(person);
        }
        
        market =  Global.getSector().getEconomy().getMarket("espc_tocquiera_market");
        if (market != null) {
			PersonAPI person = Global.getFactory().createPerson();
			person = Global.getFactory().createPerson();
			person.setId("espc_gauss");
			person.setFaction("epsilpac");
			person.setGender(Gender.FEMALE);
			person.setRankId("espc_scientist");
			person.setPostId("espc_scientist");
			person.setImportance(PersonImportance.HIGH);
			person.getName().setFirst("Gauss");
			person.getName().setLast("");
			// person.setVoice(Voices.SCIENTIST);
			person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_gauss"));
			
			EspcOfficerFactory.PopulateSkills(person, new String[]{
					Skills.TARGET_ANALYSIS, Skills.COMBAT_ENDURANCE,
					Skills.SYSTEMS_EXPERTISE, Skills.ORDNANCE_EXPERTISE}, new int[]{
					1, 2, 2, 2});
			
        	market.getCommDirectory().addPerson(person);
        	market.addPerson(person);
			ip.addPerson(person);

			PersonAPI person2 = Global.getFactory().createPerson();
			person2 = Global.getFactory().createPerson();
			person2.setId("espc_anlo");
			person2.setFaction("epsilpac");
			person2.setGender(Gender.FEMALE);
			person2.setRankId("espc_speaker");
			person2.setPostId("espc_speaker");
			person2.setImportance(PersonImportance.HIGH);
			person2.getName().setFirst("Anlo");
			person2.getName().setLast("Uisarr");
			// person.setVoice(Voices.FAITHFUL);
			person2.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_anlo"));
			// person2.getStats().setSkillLevel("espc_voice", 1);
			// person2.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
        	// market.setAdmin(person2);
        	market.getCommDirectory().addPerson(person2);
        	market.addPerson(person2);
			market.getCommDirectory().getEntryForPerson(person2).setHidden(true);
			ip.addPerson(person2);

        }
	}
}