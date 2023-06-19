package data.scripts.world;

import java.util.ArrayList;
// import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
// import java.util.Set;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
// import com.fs.starfarer.api.impl.campaign.ids.People;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
// import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
// import com.fs.starfarer.api.impl.campaign.ids.Voices;

import static com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl.dedupePortraits;

import data.scripts.util.EspcOfficerFactory;

public class espc_People {
	        
	public static PersonAPI getPerson(final String id) {
		return Global.getSector().getImportantPeople().getPerson(id);
	}
	        
	public static void create(boolean isNewGame) {
		if (!isNewGame)
			createInitialPeople();
		importantFolks();
	}
	
	private static void importantFolks() {
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
			person.setImportance(PersonImportance.VERY_HIGH);
			person.addTag(Tags.CONTACT_MILITARY);
			person.addTag(Tags.CONTACT_UNDERWORLD);
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
			market.getCommDirectory().getEntryForPerson(person).setHidden(true);
			ip.addPerson(person);
        }
        
        market = Global.getSector().getEconomy().getMarket("espc_tocquiera_market");
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
			market.getCommDirectory().getEntryForPerson(person).setHidden(true);

			person = Global.getFactory().createPerson();
			person.setId("espc_anyiwo");
			person.setFaction("epsilpac");
			person.setGender(Gender.MALE);
			person.setRankId(Ranks.CITIZEN);
			person.setPostId(Ranks.POST_SCIENTIST);
			person.setImportance(PersonImportance.MEDIUM);
			person.addTag(Tags.CONTACT_TRADE);
			person.addTag(Tags.CONTACT_SCIENCE);
			person.getName().setFirst("Anyiwo");
			person.getName().setLast("");
			person.setVoice(Voices.SCIENTIST);
			person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_anyiwo"));
			// person2.getStats().setSkillLevel("espc_voice", 1);
			// person2.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
        	// market.setAdmin(person2);
        	market.getCommDirectory().addPerson(person);
        	market.addPerson(person);
			market.getCommDirectory().getEntryForPerson(person).setHidden(true);
			ip.addPerson(person);

			person = Global.getFactory().createPerson();
			person = Global.getFactory().createPerson();
			person.setId("espc_anlo");
			person.setFaction("epsilpac");
			person.setGender(Gender.FEMALE);
			person.setRankId("espc_speaker");
			person.setPostId("espc_speaker");
			person.setImportance(PersonImportance.HIGH);
			person.getName().setFirst("Anlo");
			person.getName().setLast("Uisarr");
			// person.setVoice(Voices.FAITHFUL);
			person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_anlo"));
			// person.getStats().setSkillLevel("espc_voice", 1);
			// person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
			market.setAdmin(person);
        	market.getCommDirectory().addPerson(person, 0);
        	market.addPerson(person);
			market.getCommDirectory().getEntryForPerson(person).setHidden(true);
			ip.addPerson(person);
			// ip.checkOutPerson(person, "permanent_staff");

        }
	}
	
	// vanilla boilerplate on mid-game addition: see impl.campaign.CoreLifecyclePluginImpl
	private static void createInitialPeople() {
		ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
		
		
		//List<MarketAPI> withAutoAdmins = new ArrayList<MarketAPI>();
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			
            if (!market.getFactionId().equals("epsilpac"))
                continue;
			if (market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_DO_NOT_INIT_COMM_LISTINGS))
				continue;
			
			boolean addedPerson = false;
			
			PersonAPI admin = null;
			
			LinkedHashSet<PersonAPI> randomPeople = new LinkedHashSet<PersonAPI>();
			
			
			if (market.hasIndustry(Industries.MILITARYBASE) || market.hasIndustry(Industries.HIGHCOMMAND)) {
				PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
				String rankId = Ranks.GROUND_MAJOR;
				if (market.getSize() >= 6) {
					rankId = Ranks.GROUND_GENERAL;
				} else if (market.getSize() >= 4) {
					rankId = Ranks.GROUND_COLONEL;
				}
				person.setRankId(rankId);
				person.setPostId(Ranks.POST_BASE_COMMANDER);
				if (market.getSize() >= 8) {
					person.setImportanceAndVoice(PersonImportance.VERY_HIGH, StarSystemGenerator.random);
				} else if (market.getSize() >= 6) {
					person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
				} else {
					person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
				}
				
				market.getCommDirectory().addPerson(person);
				market.addPerson(person);
				ip.addPerson(person);
				ip.getData(person).getLocation().setMarket(market);
				ip.checkOutPerson(person, "permanent_staff");
				addedPerson = true;
				randomPeople.add(person);
			}
			
			boolean hasStation = false;
			for (Industry curr : market.getIndustries()) {
				if (curr.getSpec().hasTag(Industries.TAG_STATION)) {
					hasStation = true;
					break;
				}
			}
			if (hasStation) {
				PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
				String rankId = Ranks.SPACE_COMMANDER;
				if (market.getSize() >= 6) {
					rankId = Ranks.SPACE_ADMIRAL;
				} else if (market.getSize() >= 4) {
					rankId = Ranks.SPACE_CAPTAIN;
				}
				person.setRankId(rankId);
				person.setPostId(Ranks.POST_STATION_COMMANDER);
				
				if (market.getSize() >= 8) {
					person.setImportanceAndVoice(PersonImportance.VERY_HIGH, StarSystemGenerator.random);
				} else if (market.getSize() >= 6) {
					person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
				} else {
					person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
				}
				
				market.getCommDirectory().addPerson(person);
				market.addPerson(person);
				ip.addPerson(person);
				ip.getData(person).getLocation().setMarket(market);
				ip.checkOutPerson(person, "permanent_staff");
				addedPerson = true;
				randomPeople.add(person);
				
				if (market.getPrimaryEntity().hasTag(Tags.STATION)) {
					admin = person;
				}
			}
			
			if (market.hasSpaceport()) {
				PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
				//person.setRankId(Ranks.SPACE_CAPTAIN);
				person.setPostId(Ranks.POST_PORTMASTER);
				
				if (market.getSize() >= 8) {
					person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
				} else if (market.getSize() >= 6) {
					person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
				} else if (market.getSize() >= 4) {
					person.setImportanceAndVoice(PersonImportance.LOW, StarSystemGenerator.random);
				} else {
					person.setImportanceAndVoice(PersonImportance.VERY_LOW, StarSystemGenerator.random);
				}
				
				market.getCommDirectory().addPerson(person);
				market.addPerson(person);
				ip.addPerson(person);
				ip.getData(person).getLocation().setMarket(market);
				ip.checkOutPerson(person, "permanent_staff");
				addedPerson = true;
				randomPeople.add(person);
			}
			
			if (addedPerson) {
				PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
				person.setRankId(Ranks.SPACE_COMMANDER);
				person.setPostId(Ranks.POST_SUPPLY_OFFICER);
				
				if (market.getSize() >= 6) {
					person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
				} else if (market.getSize() >= 4) {
					person.setImportanceAndVoice(PersonImportance.LOW, StarSystemGenerator.random);
				} else {
					person.setImportanceAndVoice(PersonImportance.VERY_LOW, StarSystemGenerator.random);
				}
				
				
				market.getCommDirectory().addPerson(person);
				market.addPerson(person);
				ip.addPerson(person);
				ip.getData(person).getLocation().setMarket(market);
				ip.checkOutPerson(person, "permanent_staff");
				addedPerson = true;
				randomPeople.add(person);
			}
			
			if (!addedPerson || admin == null) {
				PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
				person.setRankId(Ranks.CITIZEN);
				person.setPostId(Ranks.POST_ADMINISTRATOR);
				
				if (market.getSize() >= 8) {
					person.setImportanceAndVoice(PersonImportance.VERY_HIGH, StarSystemGenerator.random);
				} else if (market.getSize() >= 6) {
					person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
				} else {
					person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
				}
				
				market.getCommDirectory().addPerson(person);
				market.addPerson(person);
				ip.addPerson(person);
				ip.getData(person).getLocation().setMarket(market);
				ip.checkOutPerson(person, "permanent_staff");
				admin = person;
				randomPeople.add(person);
			}
			
			if (admin != null && !market.getId().equals("espc_tocquiera_market"))
				addSkillsAndAssignAdmin(market, admin);
			
			List<PersonAPI> people = new ArrayList<PersonAPI>(randomPeople);
			Iterator<PersonAPI> iter = people.iterator();
			while (iter.hasNext()) {
				PersonAPI curr = iter.next();
				if (curr == null || curr.getFaction() == null) {
					iter.remove();
					continue;
				}
				if (curr.isDefault() || curr.isAICore() || curr.isPlayer()) {
					iter.remove();
					continue;
				}
			}
			dedupePortraits(people);
		}
	}

	protected static void addSkillsAndAssignAdmin(MarketAPI market, PersonAPI admin) {
		List<String> skills = Global.getSettings().getSortedSkillIds();
//		if (!skills.contains(Skills.PLANETARY_OPERATIONS) ||
//				!skills.contains(Skills.SPACE_OPERATIONS) ||
//				!skills.contains(Skills.INDUSTRIAL_PLANNING)) {
//			return;
//		}
		if (!skills.contains(Skills.INDUSTRIAL_PLANNING)) {
			return;
		}
		
		int size = market.getSize();
		//if (size <= 4) return;
		
		int industries = 0;
		
		for (Industry curr : market.getIndustries()) {
			if (curr.isIndustry()) {
				industries++;
			}
		}
		
		
		admin.getStats().setSkipRefresh(true);
		
		if (industries >= 2 || size >= 6) {
			admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
		}
		
		admin.getStats().setSkipRefresh(false);
		admin.getStats().refreshCharacterStatsEffects();
		
		market.setAdmin(admin);
	}
	
}