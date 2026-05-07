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
	        
	public static void create(boolean isNewGame, boolean isRandomSector) {
		if (!isNewGame)
			createInitialPeople(isRandomSector);
		importantFolks(isRandomSector);
	}
	
	private static void importantFolks(boolean isRandomSector) {
        final ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        
        MarketAPI market = null;
        MarketAPI gaussMarket = null;
        if (isRandomSector) {
        	for (MarketAPI thisMarket : Global.getSector().getEconomy().getMarketsCopy()) {
        		if (thisMarket.getFactionId().equals("epsilpac")) {
        			if (gaussMarket == null)
        				gaussMarket = thisMarket;
        			else if (gaussMarket.getSize() < thisMarket.getSize())
        				gaussMarket = thisMarket;
        			if (market == null)
        				market = thisMarket;
        			else if (market.equals(gaussMarket))
        				market = thisMarket;
        		}
        	}
        } else {
        	market = Global.getSector().getEconomy().getMarket("espc_lunron_market");
        	gaussMarket = Global.getSector().getEconomy().getMarket("espc_tocquiera_market");
        }
        Global.getSector().getMemoryWithoutUpdate().set("$espcGaussMarketId", gaussMarket.getId());
        Global.getSector().getMemoryWithoutUpdate().set("$espcGaussMarketName", gaussMarket.getName());
        
		PersonAPI nola = Global.getFactory().createPerson();
		nola.setId("espc_nola");
		nola.setFaction("epsilpac");
		nola.setGender(Gender.FEMALE);
		nola.setRankId(Ranks.FACTION_LEADER);
		nola.setPostId(Ranks.POST_FACTION_LEADER);
		nola.setImportance(PersonImportance.VERY_HIGH);
		nola.getName().setFirst("Nola");
		nola.getName().setLast("Ganymede");
		// nola.setVoice(Voices.SPACER);
		nola.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_nola"));
		
		EspcOfficerFactory.PopulateSkills(nola, new String[]{
			Skills.HELMSMANSHIP, Skills.FIELD_MODULATION, 
			Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS, 
			Skills.SYSTEMS_EXPERTISE, Skills.GUNNERY_IMPLANTS, 
			Skills.TACTICAL_DRILLS, Skills.COORDINATED_MANEUVERS, 
			Skills.WOLFPACK_TACTICS, Skills.CREW_TRAINING,
			Skills.ORDNANCE_EXPERTISE, Skills.FLUX_REGULATION}, new int[]{
			2, 2, 2, 
			2, 2, 2, 
			1, 1, 1, 1, 2, 1});
		
		ip.addPerson(nola);
        
		PersonAPI isa = Global.getFactory().createPerson();
		isa.setId("espc_isabelle");
		isa.setFaction("epsilpac");
		isa.setGender(Gender.FEMALE);
		isa.setRankId(Ranks.AGENT);
		isa.setPostId(Ranks.POST_AGENT);
		isa.setImportance(PersonImportance.VERY_HIGH);
		isa.addTag(Tags.CONTACT_MILITARY);
		isa.addTag(Tags.CONTACT_UNDERWORLD);
		isa.getName().setFirst("Isabelle");
		isa.getName().setLast("de' Medici");
		// isa.setVoice(Voices.BUSINESS);
		isa.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_isabelle"));
		
		EspcOfficerFactory.PopulateSkills(isa, new String[]{
			Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, 
			Skills.SYSTEMS_EXPERTISE, Skills.ENERGY_WEAPON_MASTERY, 
			Skills.ORDNANCE_EXPERTISE}, new int[]{
			2, 2,
			1, 2, 2});
		
		if (market != null) {
	    	market.getCommDirectory().addPerson(isa);
	    	market.addPerson(isa);
			market.getCommDirectory().getEntryForPerson(isa).setHidden(true);
		}
		ip.addPerson(isa);
		
		PersonAPI gauss = Global.getFactory().createPerson();
		gauss.setId("espc_gauss");
		gauss.setFaction("epsilpac");
		gauss.setGender(Gender.FEMALE);
		gauss.setRankId("espc_scientist");
		gauss.setPostId("espc_scientist");
		gauss.setImportance(PersonImportance.HIGH);
		gauss.getName().setFirst("Gauss");
		gauss.getName().setLast("");
		// gauss.setVoice(Voices.SCIENTIST);
		gauss.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_gauss"));
		
		EspcOfficerFactory.PopulateSkills(gauss, new String[]{
				Skills.TARGET_ANALYSIS, Skills.COMBAT_ENDURANCE,
				Skills.SYSTEMS_EXPERTISE, Skills.ORDNANCE_EXPERTISE}, new int[]{
				1, 2, 2, 2});
		
		PersonAPI anyiwo = Global.getFactory().createPerson();
		anyiwo.setId("espc_anyiwo");
		anyiwo.setFaction("epsilpac");
		anyiwo.setGender(Gender.ANY);
		anyiwo.setRankId(Ranks.CITIZEN);
		anyiwo.setPostId(Ranks.POST_SCIENTIST);
		anyiwo.setImportance(PersonImportance.MEDIUM);
		anyiwo.addTag(Tags.CONTACT_TRADE);
		anyiwo.addTag(Tags.CONTACT_SCIENCE);
		anyiwo.getName().setFirst("Anyiwo");
		anyiwo.getName().setLast("");
		anyiwo.setVoice(Voices.SCIENTIST);
		anyiwo.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_anyiwo"));
		// person2.getStats().setSkillLevel("espc_voice", 1);
		// person2.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
    	// gaussMarket.setAdmin(person2);

		PersonAPI anlo = Global.getFactory().createPerson();
		anlo.setId("espc_anlo");
		anlo.setFaction("epsilpac");
		anlo.setGender(Gender.FEMALE);
		anlo.setRankId("espc_speaker");
		anlo.setPostId("espc_speaker");
		anlo.setImportance(PersonImportance.HIGH);
		anlo.getName().setFirst("Anlo");
		anlo.getName().setLast("Uisarr");
		anlo.getStats().setSkillLevel("espc_voice", 1);
		// anlo.setVoice(Voices.FAITHFUL);
		anlo.setPortraitSprite(Global.getSettings().getSpriteName("characters", "espc_anlo"));
		
        if (gaussMarket != null) {
			
			gaussMarket.getCommDirectory().addPerson(gauss);
			gaussMarket.addPerson(gauss);
			ip.addPerson(gauss);
			gaussMarket.getCommDirectory().getEntryForPerson(gauss).setHidden(true);
	    	gaussMarket.getCommDirectory().addPerson(anyiwo);
	    	gaussMarket.addPerson(anyiwo);
			gaussMarket.getCommDirectory().getEntryForPerson(anyiwo).setHidden(true);
			ip.addPerson(anyiwo);

			// anlo.getStats().setSkillLevel("espc_voice", 1);
			// anlo.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
			if (!isRandomSector)
				gaussMarket.setAdmin(anlo);
        	gaussMarket.getCommDirectory().addPerson(anlo, 0);
        	gaussMarket.addPerson(anlo);
			gaussMarket.getCommDirectory().getEntryForPerson(anlo).setHidden(true);
			ip.addPerson(anlo);
			// ip.checkOutPerson(person, "permanent_staff");
        }
	}
	
	// vanilla boilerplate on mid-game addition: see impl.campaign.CoreLifecyclePluginImpl
	private static void createInitialPeople(boolean isRandomSector) {
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