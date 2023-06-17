// this could be crammed into the systemstats, but noooo.  the effects HAVE to be polished

package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
				
public class espc_OverpressureVFX extends BaseEveryFrameCombatPlugin {
	
	private ShipAPI ship;
	private ShipSystemAPI system;
	
	private boolean endSystem = false;
	
	private LinkedList<DamagingProjectileAPI> projs;
	private LinkedList<Float> trailIds;
	private LinkedList<Float> effectLevelOnFire;
	private ArrayList<Float> fadeTimes;
	
	public espc_OverpressureVFX(ShipAPI ship) {
		this.ship = ship;
		system = ship.getSystem();
		projs = new LinkedList<DamagingProjectileAPI>();
		trailIds = new LinkedList<Float>();
		effectLevelOnFire = new LinkedList<Float>();
		fadeTimes = new ArrayList<Float>();
		
	}
	
    @Override
	public void advance(float amount, List<InputEventAPI> events) {
    	CombatEngineAPI combatEngine = Global.getCombatEngine();
    	
		if (combatEngine == null || combatEngine.getElapsedInLastFrame() <= 0f || combatEngine.isPaused())
			return;
		
    	if (!endSystem && ship != null && combatEngine.isInPlay(ship) && ship.isAlive() && !ship.isHulk()) {
    		if (!endSystem && (system.getState() == SystemState.IDLE || system.getState() == SystemState.COOLDOWN))
    			endSystem = true;
    		
			Iterator<Object> entityIterator = combatEngine.getAllObjectGrid().getCheckIterator(
				ship.getLocation(), 
				(ship.getShieldRadiusEvenIfNoShield() + 60f) * 2f,
				(ship.getShieldRadiusEvenIfNoShield() + 60f) * 2f
			);
			while (entityIterator.hasNext()) {
				CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
				if (!(entity instanceof DamagingProjectileAPI))
					continue;
					
			DamagingProjectileAPI proj = (DamagingProjectileAPI) entity;
            // for (DamagingProjectileAPI proj : combatEngine.getProjectiles()) {
            	if (proj.getSource() != ship)
            		continue;
            	if (proj.getProjectileSpecId() == null || proj.didDamage() || proj.getWeapon() == null)
            		continue;
            	if (proj.getElapsed() > combatEngine.getElapsedInLastFrame() + 0.05f)
            		continue;
            	
            	if (proj.getWeapon().getType() != WeaponType.BALLISTIC || proj.getWeapon().getSize() == WeaponSize.SMALL)
            		continue;
            	
            	if (projs.contains(proj))
            		continue;
            	
            	projs.addLast(proj);
            	trailIds.addLast(MagicTrailPlugin.getUniqueID());
            	trailIds.addLast(MagicTrailPlugin.getUniqueID());
            	trailIds.addLast(MagicTrailPlugin.getUniqueID());
            	effectLevelOnFire.addLast(system.getEffectLevel());
            	fadeTimes.add(-1f);

            	// if (MathUtils.getDistanceSquared(proj.getWeapon().getFirePoint(0), proj.getLocation()) > 2500f)
            	// 	return;
            	
            	float projDamage;
            	if (proj instanceof MissileAPI)
            		projDamage = ((MissileAPI) proj).getSpec().getDamage().getBaseDamage();
            	else
            		projDamage = proj.getProjectileSpec().getDamage().getBaseDamage();
            	projDamage = Math.min(Math.max(35f, projDamage), 700f);
            	// min: needler/thumper, 50/100
            	// max: hellbore, 750 -> 500
            	
    			Global.getSoundPlayer().playSound(
    				"espc_overpressure_fire",
    				0.9f - projDamage / 1300f,
    				(0.5f + projDamage / 700f) * system.getEffectLevel(),
    				proj.getLocation(),
    				ship.getVelocity()
    			);
            	
                combatEngine.addHitParticle(
                	proj.getLocation(),
                	ship.getVelocity(),
                	50 + projDamage / 6,
                	0.6f,
                	0.15f + projDamage / 2500f,
                	new Color(255, 125, 0, 
                		(int) Math.min((projDamage / 25f) * system.getEffectLevel(), 255f)
                	)
                );
                combatEngine.addHitParticle(
                	proj.getLocation(),
                	ship.getVelocity(),
                	15 + projDamage / 16,
                	1.5f + projDamage / 500f,
                	0.05f + projDamage / 5000f,
                	new Color(255, 235, 200, 
                		(int) Math.min((projDamage / 2f) * system.getEffectLevel(), 255f)
                	)
                );
                for (int i = 0; i < projDamage / 50 + 4; i++) {
                	combatEngine.addHitParticle(
                		MathUtils.getRandomPointInCone(
                			proj.getLocation(),
                			(45f + projDamage / 25f) / (projDamage / 50f + 4f) * i,
                			proj.getFacing() - projDamage/50f - 6f,
                			proj.getFacing() + projDamage/50f + 6f
                		),
						ship.getVelocity(),
						Misc.random.nextFloat() * 35f + projDamage/20f - i,
						1,
						0.2f + 0.02f * i,
						new Color(225, (int) (Misc.random.nextFloat() * 80) + 20, 20, 
							(int) (125 * system.getEffectLevel())
						)
                	);
                }
            	
            }
    	} else if (!endSystem){
    		endSystem = true;
    	}

    	if (endSystem && projs.size() == 0) {
			combatEngine.removePlugin(this);
    	}
    	if (projs.size() == 0)
    		return;
    	
        Iterator<DamagingProjectileAPI> projIterator = projs.iterator();
        Iterator<Float> idIterator = trailIds.iterator();
        Iterator<Float> levelIterator = effectLevelOnFire.iterator();
        int i = -1;
        
        
        while (projIterator.hasNext()) {
        	i++;
        	DamagingProjectileAPI proj = (DamagingProjectileAPI) projIterator.next();
        	float trailId = (Float) idIterator.next();
        	float thisLevel = (Float) levelIterator.next();
        	if (proj == null || !combatEngine.isInPlay(proj) || proj.didDamage() || proj.isExpired()) {
        		projIterator.remove();
        		idIterator.remove();
        		idIterator.next();
        		idIterator.remove();
        		idIterator.next();
        		idIterator.remove();
        		fadeTimes.remove(i);
        		i--;
        		continue;
        	}
        	float trailId2 = (Float) idIterator.next();
        	float trailId3 = (Float) idIterator.next();
        	if (proj.isFading() && !(proj instanceof MissileAPI) && fadeTimes.get(i) < 0f)
        		fadeTimes.set(i, combatEngine.getTotalElapsedTime(false));
        	float fadeTime = fadeTimes.get(i);
        	if (fadeTime >= 0f)
        		fadeTime = Math.max(0f, 
            		1f - (combatEngine.getTotalElapsedTime(false) - fadeTime) / proj.getProjectileSpec().getFadeTime()
            	);
        	
        	// derived from magiclib's trail code, this is a cleverer solution than whatever trig i was gonna work out
        	// but why did it have like five declarations in it
        	// i might rewrite this anyway TODO ig :I
            Vector2f sidewaysVel = VectorUtils.rotate(
            	new Vector2f(0f,
                VectorUtils.rotate(
                    proj.getVelocity(),
                    -proj.getFacing(), 
                    new Vector2f()).y
            	), proj.getFacing()
            );

            MagicTrailPlugin.addTrailMemberAdvanced(
				proj,
				trailId2,	
				Global.getSettings().getSprite("fx", "base_trail_mild"),
				proj.getLocation(),
				0f,
				0f,
				proj.getFacing() - 180f,
				0f,
				0f,
				18,
				24,
				new Color(255,119,0),
            	new Color(225,60,0),
				0.55f * thisLevel * (fadeTime < 0f ? 1f : fadeTime),
				0.01f,
				0.06f,
				0.01f,
				GL11.GL_SRC_ALPHA,
				GL11.GL_ONE,
				128f,
				-128f,
				trailId2 * 128f,
				sidewaysVel,
				null,
				CombatEngineLayers.ABOVE_SHIPS_LAYER,
				1f
            );
            
            MagicTrailPlugin.addTrailMemberAdvanced(
				proj,
				trailId,	
				Global.getSettings().getSprite("fx", "base_trail_fuzzy"),
				proj.getLocation(),
				0f,
				0f,
				proj.getFacing() - 180f,
				0f,
				0f,
				12,
				64,
            	new Color(145,135,125),
            	new Color(115,115,115),
				0.325f * thisLevel * (fadeTime < 0f ? 1f : fadeTime),
				0.0f,
				0.1f,
				0.07f,
				GL11.GL_SRC_ALPHA,
				GL11.GL_ONE_MINUS_SRC_ALPHA,
				128f,
				-128f,
				trailId * 128f,
				Vector2f.add(
		            sidewaysVel,
		            MathUtils.getRandomPointInCircle(null, 24f),
		            new Vector2f()),
				null,
            	CombatEngineLayers.ABOVE_SHIPS_LAYER,
				1f
            );
            
            if (!(proj instanceof MissileAPI) && proj.getProjectileSpec().getDamage().getBaseDamage() > 105f)
            MagicTrailPlugin.addTrailMemberAdvanced(
				proj,
				trailId3,	
				Global.getSettings().getSprite("fx", "espc_trail_wispy"),
				proj.getLocation(),
				0f,
				0f,
				proj.getFacing() - 180f,
				0f,
				0f,
				12,
				72,
            	new Color(125,115,105),
            	new Color(85,85,85),
				0.45f * thisLevel * (fadeTime < 0f ? 1f : fadeTime),
				0.0f,
				0.15f,
				0.1f,
				GL11.GL_SRC_ALPHA,
				GL11.GL_ONE_MINUS_SRC_ALPHA,
				128f,
				-128f,
				trailId3 * 128f,
				Vector2f.add(
		            sidewaysVel,
		            MathUtils.getRandomPointInCircle(null, 24f),
		            new Vector2f()),
				null,
            	CombatEngineLayers.ABOVE_SHIPS_LAYER,
				1f
            );
        }
	}
}