package data.scripts.hullmods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.shipsystems.espc_AlternatorStats;
import data.scripts.util.MezzUtils;

public class espc_AlternatorInfoMod extends BaseHullMod {

	public static class AlternatorWeaponData {
		public boolean isBallistic;
		public String id;
		public String name;
		public WeaponSize size;
		public float dps;
		public int count = 1;
		public int isFragOrPD;
		public AlternatorWeaponData(String id, String name, WeaponSize size, boolean isBallistic, float dps,
			int isFragOrPD) {
			this.id = id;
			this.name = name;
			this.size = size;
			this.isBallistic = isBallistic;
			this.dps = dps;
			this.isFragOrPD = isFragOrPD;
		}
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, final ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		
		tooltip.addPara(MezzUtils.getString("espc_hullmod", "alternator_desc1"), opad, h, 
				MezzUtils.getString("espc_gencombat", "ballistic"), MezzUtils.getString("espc_gencombat", "energy"));
		tooltip.addPara(MezzUtils.getString("espc_hullmod", "alternator_desc2a"), opad,
				h, MezzUtils.getString("espc_hullmod", "alternator_desc2b"));

		float row2W = 100f;
		float row3W = 85f;
		float sizeW = width - row2W - row3W - 10f;
		
		float ballisticDPS = 0f;
		float energyDPS = 0f;
		List<AlternatorWeaponData> weapons = null;
		
		if (!isForModSpec && ship != null && !Global.CODEX_TOOLTIP_MODE) {
			weapons = new ArrayList<AlternatorWeaponData>();
			for (WeaponAPI w : ship.getAllWeapons()) {
				if (w.isDecorative() || w.getSlot() != null && w.getSlot().isSystemSlot() || 
						!w.getType().equals(WeaponType.BALLISTIC) &&
						!w.getType().equals(WeaponType.ENERGY))
					continue;
				boolean found = false;
				for (int i = 0; i < weapons.size(); i++) {
					if (weapons.get(i).id.equals(w.getId())) {
						weapons.get(i).count++;
						if (weapons.get(i).isBallistic)
							ballisticDPS += weapons.get(i).dps;
						else
							energyDPS += weapons.get(i).dps;
						found = true;
						break;
					}
				}
				if (!found) {
					float dps = w.getDerivedStats().getSustainedDps() 
						* (w.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.5f : 1f) *
						(w.hasAIHint(AIHints.PD) && !w.hasAIHint(AIHints.PD_ALSO) ? 0.5f : 1f);
					if (w.getType().equals(WeaponType.BALLISTIC))
						ballisticDPS += dps;
					else
						energyDPS += dps;
					weapons.add(new AlternatorWeaponData(
						w.getId(),
						w.getDisplayName(),
						w.getSize(),
						w.getType().equals(WeaponType.BALLISTIC),
						dps,
						(w.getDamageType().equals(DamageType.FRAGMENTATION) ? 1 : 0) +
							((w.hasAIHint(AIHints.PD) && !w.hasAIHint(AIHints.PD_ALSO)) ? 1 : 0)
					));
				}
			}
			if (ballisticDPS + energyDPS == 0f) {
				
			} else if (ballisticDPS == 0f || energyDPS == 0f) {
				tooltip.addPara(MezzUtils.getString("espc_hullmod", "alternator_inactive1"), 
					opad, Misc.getNegativeHighlightColor(), 
					String.format(MezzUtils.getString("espc_hullmod", "alternator_inactive1"), 
						ballisticDPS > 0f ? 
						MezzUtils.getString("espc_gencombat", "ballistic") : MezzUtils.getString("espc_gencombat", "energy")
					)
				);
			} else {
				Collections.sort(weapons, new Comparator<AlternatorWeaponData>() {
					public int compare(AlternatorWeaponData o1, AlternatorWeaponData o2) {
						if (o1.isBallistic != o2.isBallistic)
							return o1.isBallistic ? -1 : 1;
						if (!o1.size.equals(o2.size))
							return o1.size.compareTo(o2.size);
						return (int) Math.signum(o1.dps * o1.count - o2.dps * o2.count);
					}
				});

				tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
					20f, true, true, 
					new Object [] {
						Misc.ucFirst(MezzUtils.getString("espc_gencombat", "weapon_type")), sizeW, 
						Misc.ucFirst(MezzUtils.getString("espc_gencombat", "total_dps")), row2W, 
						Misc.ucFirst(MezzUtils.getString("espc_general", "increase")), row3W});

				tooltip.addRow(Alignment.MID, Misc.getTextColor(), 
					Misc.ucFirst(MezzUtils.getString("espc_gencombat", "ballistic")),
					Alignment.MID, Misc.getTextColor(), "" + (int) ballisticDPS,
					Alignment.MID, energyDPS / ballisticDPS >= 1.5f ? Misc.getHighlightColor() : 
						(energyDPS / ballisticDPS <= 0.75f ? Misc.getGrayColor() : Misc.getTextColor()), 
						"" + (int) Math.min(energyDPS / ballisticDPS * 100f, 200f) + "%"
				);
				tooltip.addRow(Alignment.MID, Misc.getTextColor(), 
					Misc.ucFirst(MezzUtils.getString("espc_gencombat", "energy")),
					Alignment.MID, Misc.getTextColor(), "" + (int) energyDPS,
					Alignment.MID, ballisticDPS / energyDPS >= 1.5f ? Misc.getHighlightColor() : 
						(ballisticDPS / energyDPS <= 0.75f ? Misc.getGrayColor() : Misc.getTextColor()), 
						"" + (int) Math.min(ballisticDPS / energyDPS * 100f, 200f) + "%"
				);
				
				tooltip.addTable("", 0, opad);
				tooltip.addSpacer(5f);
			}
		}
		
		tooltip.addSectionHeading(MezzUtils.getString("espc_hullmod", "alternator_table1"), Alignment.MID, opad);
		tooltip.addPara(MezzUtils.getString("espc_hullmod", "alternator_table2"), opad, h, 
			(int) (espc_AlternatorStats.FRAG_OR_PD_MULT * 100f) + "%", 
			(int) (espc_AlternatorStats.FRAG_OR_PD_MULT * espc_AlternatorStats.FRAG_OR_PD_MULT * 100f) + "%");
		tooltip.addPara(MezzUtils.getString("espc_hullmod", "alternator_table3"), opad, h, 
			String.format(MezzUtils.getString("espc_hullmod", "alternator_table4"), 
				(int) (espc_AlternatorStats.BONUS_MAX * 100f) + "%"));
		
		if (Global.CODEX_TOOLTIP_MODE) {
			tooltip.addSpacer(5f);
			return;
		}
		
		if (isForModSpec || (ship == null && !Global.CODEX_TOOLTIP_MODE)) return;
		
		tooltip.setBgAlpha(0.9f);
		
		if (ballisticDPS == 0f || energyDPS == 0f)
			return;
		
		tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
			20f, true, true, 
			new Object [] {
				Misc.ucFirst(MezzUtils.getString("espc_gencombat", "weapon")), sizeW, 
				Misc.ucFirst(MezzUtils.getString("espc_gencombat", "total_dps")), row2W, 
				Misc.ucFirst(MezzUtils.getString("espc_general", "count")), row3W});
		
		for (AlternatorWeaponData curr : weapons) {
			tooltip.addRow(Alignment.MID, Misc.getTextColor(), curr.name,
				Alignment.MID, curr.isFragOrPD == 2 ? Misc.getGrayColor() : 
					(curr.isFragOrPD == 1 ? Misc.getNegativeHighlightColor() : Misc.getTextColor()), 
					"" + (int) (curr.dps * curr.count),
				Alignment.MID, Misc.getTextColor(), "" + curr.count);
		}
		tooltip.addTable("", 0, opad);
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		
	}
	
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
	
	
}