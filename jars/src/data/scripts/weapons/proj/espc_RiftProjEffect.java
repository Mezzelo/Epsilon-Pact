package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

import java.awt.Color;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;
				
public class espc_RiftProjEffect implements OnHitEffectPlugin {
	
    @Override
    public void onHit(
        	DamagingProjectileAPI proj, 
        	CombatEntityAPI target, 
        	Vector2f point, 
        	boolean shieldHit, 
        	ApplyDamageResultAPI damage, 
        	CombatEngineAPI engine
	) {
		if (target instanceof DamagingProjectileAPI && !proj.didDamage())
			return;
		
		if (target instanceof ShipAPI && ((ShipAPI) target).getHullSize() == HullSize.FIGHTER &&
			proj.getHitpoints() > 0f)
			return;
		
		if (((DamagingProjectileAPI) proj).getProjectileSpecId().equals("espc_riftpike_shot")) {
			engine.addSwirlyNebulaParticle(point, Misc.ZERO, 60f, 3f, 0.5f, 0.8f, 0.7f, new Color(163,0,255), true);
			engine.spawnExplosion(point, Misc.ZERO, new Color(255,180,250), 105f, 0.7f);
			Global.getSoundPlayer().playSound(
				"rifttorpedo_explosion",
				//"riftcascade_rift",
				1.5f, 1f, point, Misc.ZERO);
		} else {
			engine.addSwirlyNebulaParticle(point, Misc.ZERO, 30f, 2.5f, 0.5f, 0.8f, 0.5f, new Color(163,0,255), true);
			engine.spawnExplosion(point, Misc.ZERO, new Color(255,180,250), 45f, 0.4f);
			Global.getSoundPlayer().playSound(
				"rifttorpedo_explosion",
				//"riftcascade_rift",
				1.7f, 0.4f, point, Misc.ZERO);
		}
    }
	
}
