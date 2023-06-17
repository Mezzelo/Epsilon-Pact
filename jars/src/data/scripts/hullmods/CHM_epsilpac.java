package data.scripts.hullmods;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class CHM_epsilpac extends BaseHullMod {
    
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        Global.getCombatEngine().addPlugin(new CHM_epsilpacPlugin(ship));
	}
    
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0)
            return ("70%");
        else if (index == 1)
            return ("30%");
        else if (index == 2)
            return ("20/15/10/5%");
        else if (index == 3)
            return ("90%");
        else if (index == 4)
            return ("10%");

        return null;
    }
	
    
    @Override
    public void addPostDescriptionSection(final TooltipMakerAPI tooltip, final ShipAPI.HullSize hullSize, final ShipAPI ship, final float width, final boolean isForModSpec) {
        tooltip.addPara("%s", 6f, Misc.getGrayColor(), "At the end of everything, hold onto anything.");
    }
    
    @Override
    public Color getBorderColor() {
        return new Color(147, 102, 50, 0);
    }

    @Override
    public Color getNameColor() {
        return new Color(10, 119, 158,255);
    }
}