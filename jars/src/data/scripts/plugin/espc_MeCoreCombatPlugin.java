// unused, appears to be unnecessary with current wack-ass implementation

package data.scripts.plugin;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.espc_ModPlugin;
import exerelin.campaign.backgrounds.CharacterBackgroundUtils;
import java.util.List;

public class espc_MeCoreCombatPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private float interval = 0f;
    private boolean found = false;

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        interval = 0f;
        found = false;
    }

    public void advance(float amount, List<InputEventAPI> events) {
        if (amount == 0f || found || engine == null || !espc_ModPlugin.hasNex()
        	|| !CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing") 
        	|| engine.isMission() || Global.getCurrentState() != GameState.COMBAT ||
        	Global.getSector() == null) return;
        if (engine.isMission()) return;
        interval -= amount;
        if (interval <= 0f)
        	interval += 5f;
        else
        	return;
        
		for (ShipAPI ship : Global.getCombatEngine().getShips()) {
			if (Misc.isAutomated(ship) && ship.getCaptain() != null && ship.getCaptain().isAICore() &&
				ship.getCaptain().getAICoreId().equals("espc_meCore")) {
				if (found == false ) {
					engine.setPlayerShipExternal(ship);
					found = true;
				} else
					
				break;
			}
		}
        
    }
}