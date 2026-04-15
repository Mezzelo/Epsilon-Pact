package data.scripts.util;
// shorthand because i've no self control.

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;

@SuppressWarnings("serial")
public class espc_DoctrineProgression {
	
	public static class DoctrineShift {
		public String[] hullAdditions;
		public String[] hullRemovals;
		public String[] weaponAdditions;
		public String[] weaponRemovals;
		public Map<String, Float> variantFrequencies;
		
		public DoctrineShift(String[] hullAdditions) {
			this.hullAdditions = hullAdditions;
		}
		
		public DoctrineShift(String[] hullAdditions, String[] hullRemovals, String[] weaponAdditions, String[] weaponRemovals,
			Map<String, Float> variantFrequencies) {
			this.hullAdditions = hullAdditions;
			this.hullRemovals = hullRemovals;
			this.weaponAdditions = weaponAdditions;
			this.weaponRemovals = weaponRemovals;
			this.variantFrequencies = variantFrequencies;
		}
	}
	
	public static class DoctrineShiftFaction {
		public String faction;
		public String[] hulls;
		public DoctrineShiftFaction(String faction, String[] hulls) {
			this.faction = faction;
			this.hulls = hulls;
		}
	}
	
	public static Map<Integer, DoctrineShift> knowledge = new HashMap<Integer, DoctrineShift>();
	static {
		knowledge.put(-1, new DoctrineShift(
			new String[] {"wolf", "brawler", "tempest", "shrike", "fury", "apogee", "eradicator", "champion", "aurora", "dram", "mule"}, 
			new String[] {"espc_picket", "espc_sentry", "espc_warden", "espc_bastillon", "espc_berserker", "espc_rampart",
				"glimmer", "lumen_espc", "fulgent_espc", "scintilla_espc", "brilliant_espc", "apex", "nova_espc", "radiant_espc",
				"jackalope", "songbird", "flagbearer", "militia", "ember", "pilgrim", "observer", "chorale", "amanuensis", 
				"anubis", "wolf_espc"}, 
			new String[] {"hveldriver", "heavymauler", "hil"}, 
			new String[] {"espc_riftspear", "espc_riftpike", "espc_fission", "espc_mkl", "espc_flak",
				"espc_aa", "espc_remdriver", "espc_remmortar", "espc_minimir", "espc_minimirdual", "espc_gatling"}, 
			new HashMap<String, Float>() { {
				put("lasher_espc_Strike", 0.0f);
				put("lasher_espc_Strike_Dated", 8.0f);
				
				put("espc_rondel_Standard", 10.0f);
				put("espc_rondel_Assault", 0.0f);
				put("espc_rondel_Anti_Shield", 0.0f);
				
				put("hammerhead_espc_Elite_Common", 5.0f);
				put("hammerhead_espc_Support_Common", 5.0f);
				put("hammerhead_espc_Elite", 0.0f);
				put("hammerhead_espc_Support", 0.0f);
				
				put("sunder_espc_Assault_Common", 2.0f);
				put("sunder_espc_Strike_Common", 1.0f);
				put("sunder_espc_Support_Common", 2.0f);
				put("sunder_espc_Assault", 0.0f);
				put("sunder_espc_Strike", 0.0f);
				put("sunder_espc_Support", 0.0f);
				}
			}
		));
		knowledge.put(6, new DoctrineShift(
			new String[] {"jackalope", "militia"}, 
			null, 
			new String[] {"espc_aa"},
			null, 
			new HashMap<String, Float>() { {
				put("espc_militia_Standard", 0.0f);
				put("espc_militia_Support", 0.0f);
				put("espc_militia_Standard_Common", 5.0f);
				put("espc_militia_Support_Common", 5.0f);
				put("espc_jackalope_Escort", 2.0f);
				put("espc_jackalope_Escort_Pact", 0.0f);
				put("espc_jackalope_Anti_Armor", 3.0f);
				put("espc_jackalope_Anti_Armor_Pact", 0.0f);
				put("espc_jackalope_Strike", 0.0f);
				}
			}
		));
		knowledge.put(12, new DoctrineShift(
			new String[] {"pilgrim", "chorale", "ember"}, 
			new String[] {"mule"}, 
			null,
			null,
			new HashMap<String, Float>() { {
				put("espc_pilgrim_Assault_Common", 3.0f);
				put("espc_pilgrim_Escort_Common", 2.0f);
				put("espc_pilgrim_Strike_Common", 3.0f);
				put("espc_pilgrim_Support_Common", 2.0f);
				put("espc_pilgrim_Assault", 0.0f);
				put("espc_pilgrim_Escort", 0.0f);
				put("espc_pilgrim_Strike", 0.0f);
				put("espc_pilgrim_Support", 0.0f);
				
				put("espc_chorale_Common", 2.0f);
				put("espc_chorale_Assault", 0.0f);
				put("espc_chorale_Elite_Support", 0.0f);
				put("espc_chorale_Elite", 0.0f);
				put("espc_chorale_Strike", 1.0f);
				put("espc_chorale_Support", 0.0f);
				put("espc_chorale_Suppressive", 0.0f);
				put("espc_chorale_Suppressive_Common", 1.0f);
				}
			}
		));
		knowledge.put(18, new DoctrineShift(
			new String[] {"songbird", "anubis",
			"espc_picket", "espc_warden"}, 
			new String[] {"dram"}, 
			new String[] {"espc_remdriver", "espc_remmortar", "espc_flak"},
			new String[] {"hveldriver", "heavymauler"}, 
			new HashMap<String, Float>() { {
				put("espc_picket_Anti_Armor", 0.0f);
				put("espc_picket_Strike", 10.0f);
				put("espc_picket_Anti_Armor", 4.0f);
				put("espc_picket_Strike", 6.0f);
				
				put("espc_warden_Assault", 0.0f);
				put("espc_warden_Attack", 2.0f);
				put("espc_warden_Strike", 2.0f);
				put("espc_warden_Ranged", 2.0f);
				
				put("espc_rondel_Standard", 5.0f);
				put("espc_rondel_Anti_Shield", 5.0f);
				
				put("espc_jackalope_Escort", 0.0f);
				put("espc_jackalope_Escort_Pact", 2.0f);
				
				put("hammerhead_espc_Support_Common", 0.0f);
				put("hammerhead_espc_Support", 5.0f);
				
				put("espc_militia_Support", 5.0f);
				put("espc_militia_Support_Common", 0.0f);
				
				put("espc_pilgrim_Assault_Common", 0.0f);
				put("espc_pilgrim_Escort_Common", 0.0f);
				put("espc_pilgrim_Assault", 3.0f);
				put("espc_pilgrim_Escort", 2.0f);
				
				put("espc_chorale_Common", 0.0f);
				put("espc_chorale_Suppressive", 1.0f);
				put("espc_chorale_Suppressive_Common", 0.0f);
				}
			}
		));
		knowledge.put(24, new DoctrineShift(
			new String[] {"flagbearer", "observer", "wolf_espc",
			"espc_sentry", "espc_bastillon", "espc_berserker", "glimmer", "lumen_espc"}, 
			new String[] {"wolf", "brawler", "tempest"}, 
			new String[] {"espc_riftspear", "espc_minimir", "espc_minimirdual", "espc_mkl", "espc_gatling"},
			null, 
			new HashMap<String, Float>() { {
				put("espc_picket_Anti_Armor", 4.0f);
				put("espc_picket_Strike", 6.0f);
				
				put("lasher_espc_Strike", 8.0f);
				put("lasher_espc_Strike_Dated", 0.0f);
				
				put("espc_warden_Assault", 2.0f);
				put("espc_warden_Support", 2.0f);
				
				put("espc_rondel_Standard", 0.0f);
				put("espc_rondel_Assault", 3.0f);
				put("espc_rondel_Anti_Shield", 7.0f);
				
				put("espc_jackalope_Escort_Pact", 1.0f);
				put("espc_jackalope_Anti_Armor", 1.0f);
				put("espc_jackalope_Anti_Armor_Pact", 1.0f);
				put("espc_jackalope_Strike", 2.0f);
				
				put("hammerhead_espc_Elite_Common", 0.0f);
				put("hammerhead_espc_Support_Common", 0.0f);
				put("hammerhead_espc_Elite", 2.0f);
				put("hammerhead_espc_Support", 3.0f);
				
				put("sunder_espc_Assault_Common", 0.0f);
				put("sunder_espc_Assault", 2.0f);
				
				put("espc_militia_Standard", 5.0f);
				put("espc_militia_Standard_Common", 0.0f);
				
				put("espc_pilgrim_Strike_Common", 0.0f);
				put("espc_pilgrim_Support_Common", 0.0f);
				put("espc_pilgrim_Strike", 3.0f);
				put("espc_pilgrim_Support", 2.0f);
				
				put("espc_observer_Beam", 2.0f);
				put("espc_observer_Assault", 4.0f);
				put("espc_observer_Strike_Common", 4.0f);
				put("espc_observer_Assault", 0.0f);
				put("espc_observer_Strike", 0.0f);
				put("espc_observer_Suppressive", 0.0f);
				
				put("espc_chorale_Support", 1.0f);
				}
			}
		));
		knowledge.put(30, new DoctrineShift(
				new String[] {"espc_rampart", "fulgent_espc", "scintilla_espc"}, 
				new String[] {"shrike", "eradicator", "champion", "apogee"}, 
				null,
				null, 
				new HashMap<String, Float>() { {
					}
				}
			));
		knowledge.put(36, new DoctrineShift(
			new String[] {"amanuensis", "brilliant_espc",
			"apex"}, 
			new String[] {"fury", "aurora"}, 
			new String[] {"espc_riftpike", "espc_fission"},
			null, 
			new HashMap<String, Float>() { {
				put("sunder_espc_Strike_Common", 0.0f);
				put("sunder_espc_Support_Common", 0.0f);
				put("sunder_espc_Strike", 1.0f);
				put("sunder_espc_Support", 2.0f);
				
				put("espc_observer_Beam", 0.0f);
				put("espc_observer_Strike_Common", 0.0f);
				put("espc_observer_Strike", 4.0f);
				put("espc_observer_Suppressive", 2.0f);
				
				put("espc_chorale_Assault", 3.0f);
				put("espc_chorale_Elite", 1.0f);
				put("espc_chorale_Elite_Support", 3.0f);
				}
			}
		));
		knowledge.put(48, new DoctrineShift(
			new String[] {"nova_espc", "radiant_espc"}));
	}
	
	public static Map<Integer, DoctrineShiftFaction[]> knowledgeForeign = new HashMap<Integer, DoctrineShiftFaction[]>();
	static {
		knowledgeForeign.put(6, new DoctrineShiftFaction[] {
				new DoctrineShiftFaction("derelict", new String[] {"espc_picket", "espc_warden"})
			});
		knowledgeForeign.put(12, new DoctrineShiftFaction[] {
			new DoctrineShiftFaction("luddic_path", new String[] {"espc_jackalope_lp"}),
			new DoctrineShiftFaction("hegemony", new String[] {"espc_rondel_h"}),
			new DoctrineShiftFaction("independent", new String[] {"espc_ember_c"}),
			new DoctrineShiftFaction("derelict", new String[] {"espc_sentry", "espc_bastillon", "espc_berserker"}),
			new DoctrineShiftFaction("pirates", new String[] {"WEP_espc_as"}),
		});
		knowledgeForeign.put(18, new DoctrineShiftFaction[] {
			new DoctrineShiftFaction("pirates", new String[] {"espc_ember_p", "WEP_espc_amflamersolo"}),
			new DoctrineShiftFaction("independent", new String[] {"espc_ember_c"}),
			new DoctrineShiftFaction("derelict", new String[] {"espc_rampart"})
		});
		knowledgeForeign.put(24, new DoctrineShiftFaction[] {
			new DoctrineShiftFaction("hegemony", new String[] {"espc_pilgrim_h"}),
			new DoctrineShiftFaction("luddic_path", new String[] {"WEP_espc_amflamersolo"}),
		});
		knowledgeForeign.put(30, new DoctrineShiftFaction[] {
			new DoctrineShiftFaction("tritachyon", new String[] {"espc_observer_tt"}),
		});
	}
	
	protected static void scratchpad() {
		Global.getSector().getFaction("epsilpac").getVariantOverrides().put("wolf_espc_Assault", 2f);
	}
	
}