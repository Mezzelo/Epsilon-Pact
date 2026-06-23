// note: initial damage reduction doesn't take effect for projectiles that are spawned in.
// as damagingprojectileapi is a subinterface of combatentityapi, we can write custom data to solve this.

package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.plugin.espc_DamageListener;
import data.scripts.util.MezzUtils;
import data.scripts.weapons.proj.espc_MinimirDistortion;

public class espc_MinimirEffect implements OnFireEffectPlugin {
	
	private espc_MinimirDistortion plugin = null;
	private int fxCheck = -1;

	public espc_MinimirDistortion getPlugin() {
		return plugin;
	}
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (weapon.getShip() == null)
    		return;
    	
    	if (fxCheck == -1) {
    		if (!weapon.getShip().hasListenerOfClass(espc_DamageListener.class))
    			weapon.getShip().addListener(new espc_DamageListener());
    			
    		if (MezzUtils.glibCheck()) {
        		for (WeaponAPI wep : weapon.getShip().getAllWeapons()) {
        			if (wep.getEffectPlugin() != null && wep.getEffectPlugin() instanceof espc_MinimirEffect &&
        				((espc_MinimirEffect) wep.getEffectPlugin()).getPlugin() != null) {
        				plugin = ((espc_MinimirEffect) wep.getEffectPlugin()).getPlugin();
        				fxCheck = 1;
        				break;
        			}
        		}
        		if (plugin == null) {
        			plugin = new espc_MinimirDistortion(weapon.getShip());
        			Global.getCombatEngine().addPlugin(plugin);
        		}
				fxCheck = 1;
    		} else
    			fxCheck = 0;
    	}
    	
    	if (fxCheck == 1)
    		plugin.addProj(proj);
    	
    }
}