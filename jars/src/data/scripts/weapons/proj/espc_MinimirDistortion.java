package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import data.scripts.util.espc_DistWrapper;
				
public class espc_MinimirDistortion extends BaseEveryFrameCombatPlugin {
	
	private final ShipAPI ship;
	// could just use the type straight, butt his has the nice convenience of bundling it with an entity reference
	private ArrayDeque<espc_DistWrapper> projs;
	
    public espc_MinimirDistortion(ShipAPI ship) {
    	this.ship = ship;
		projs = new ArrayDeque<espc_DistWrapper>();
    }
	
	public void addProj(DamagingProjectileAPI proj) {
		espc_DistWrapper distortion = new espc_DistWrapper(true,
        	proj.getLocation(), Misc.ZERO, 5f, 30f, -1f, 0.05f, -1f, 0, -1);
		distortion.setEntity(proj);
		projs.addLast(distortion);
	}
	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
    	if (amount <= 0f)
    		return;
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused())
			return;
		if (projs.size() == 0) {
			if (ship == null || !engine.isInPlay(ship) || ship.isHulk() || !ship.isAlive()) {
				if (ship != null)
					ship.removeCustomData("espc_MinimirDistortionPlugin");
				engine.removePlugin(this);
			}
			return;
		}
		Iterator<espc_DistWrapper> iter = projs.iterator();
		while (iter.hasNext()) {
			espc_DistWrapper distortion = iter.next();
			DamagingProjectileAPI proj = (DamagingProjectileAPI) distortion.getEntity();
			if (proj == null || !engine.isInPlay(proj) || proj.isExpired() || proj.didDamage()) {
				distortion.setLifetime(0.1f);
				distortion.setFade(0.1f, 0);
				iter.remove();
				continue;
			}
			if (proj.isFading()) {
				distortion.setLifetime(0.175f);
				distortion.setFade(0.175f, 1);
				distortion.setVelocity(proj.getVelocity());
				iter.remove();
				continue;
			}
			distortion.setLocation(proj.getLocation());
		}
	}
}
