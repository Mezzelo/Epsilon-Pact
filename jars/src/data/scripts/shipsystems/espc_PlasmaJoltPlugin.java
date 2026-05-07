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
import com.fs.starfarer.api.input.InputEventAPI;

import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
				
public class espc_PlasmaJoltPlugin extends BaseEveryFrameCombatPlugin {
	
	private ShipAPI ship;
	private ShipSystemAPI system;
	
	public static final float DAMAGE_BONUS = 1f;
	public static final float VELOCITY_BONUS = 1f;
	public static final float VELOCITY_MAX = 6000f;
	
	private boolean markForRemove = false;
	
	private LinkedList<DamagingProjectileAPI> projs;
	private ArrayDeque<Float> trailIds;
	private ArrayDeque<Float> effectLevelOnFire;
	private ArrayList<Float> fadeTimes;
	
	public espc_PlasmaJoltPlugin(ShipAPI ship) {
		this.ship = ship;
		system = ship.getSystem();
		projs = new LinkedList<DamagingProjectileAPI>();
		trailIds = new ArrayDeque<Float>();
		effectLevelOnFire = new ArrayDeque<Float>();
		fadeTimes = new ArrayList<Float>();
		
	}
	
    @Override
	public void advance(float amount, List<InputEventAPI> events) {
    	CombatEngineAPI combatEngine = Global.getCombatEngine();
    	
		if (combatEngine == null || combatEngine.getElapsedInLastFrame() <= 0f || combatEngine.isPaused())
			return;
		
		if (!markForRemove && system != null && !system.isActive() && projs.size() == 0)
			return;
		
		if (!markForRemove && (ship == null || !combatEngine.isEntityInPlay(ship) || !ship.isAlive() || ship.isHulk()))
			markForRemove = true;
		
    	if (!markForRemove && system.isActive()) {
			Iterator<Object> entityIterator = combatEngine.getAllObjectGrid().getCheckIterator(
				ship.getLocation(), 
				(ship.getShieldRadiusEvenIfNoShield() + 100f) * 2f,
				(ship.getShieldRadiusEvenIfNoShield() + 100f) * 2f
			);
			while (entityIterator.hasNext()) {
				CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
				if (!(entity instanceof DamagingProjectileAPI))
					continue;
					
			DamagingProjectileAPI proj = (DamagingProjectileAPI) entity;
            	if (proj.getSource() != ship)
            		continue;
            	if (proj.getProjectileSpecId() == null || proj.didDamage() || proj.getWeapon() == null)
            		continue;
            	
            	if (projs.contains(proj))
            		continue;
            	
            	proj.setDamageAmount(proj.getDamageAmount() * (1f + DAMAGE_BONUS));
            	float vel = Math.min(proj.getVelocity().length() * (1f + VELOCITY_BONUS), VELOCITY_MAX) / proj.getVelocity().length();
            	if (vel > 1f)
            		proj.getVelocity().scale(vel);
            	// you should add like a shockwave fx probably.
            	
            	projs.addLast(proj);
            	trailIds.addLast(MagicTrailPlugin.getUniqueID());
            	effectLevelOnFire.addLast(system.getEffectLevel());
            	fadeTimes.add(-1f);
            	
            }
    	}

    	if (projs.size() == 0) {
			if (markForRemove)
				combatEngine.removePlugin(this);
			return;
    	}
    	
        Iterator<DamagingProjectileAPI> projIterator = projs.iterator();
        Iterator<Float> idIterator = trailIds.iterator();
        Iterator<Float> levelIterator = effectLevelOnFire.iterator();
        int i = -1;
        
        while (projIterator.hasNext()) {
        	i++;
        	DamagingProjectileAPI proj = (DamagingProjectileAPI) projIterator.next();
        	float thisLevel = (Float) levelIterator.next();
        	float trailId = (Float) idIterator.next();
        	if (proj == null || !combatEngine.isEntityInPlay(proj) || proj.didDamage() || proj.isExpired()) {
        		projIterator.remove();
        		idIterator.remove();
        		fadeTimes.remove(i);
        		i--;
        		continue;
        	}
        	if (proj.isFading() && !(proj instanceof MissileAPI) && fadeTimes.get(i) < 0f)
        		fadeTimes.set(i, combatEngine.getTotalElapsedTime(false));
        	float fadeTime = fadeTimes.get(i);
        	if (fadeTime >= 0f)
        		fadeTime = Math.max(0f, 
            		1f - (combatEngine.getTotalElapsedTime(false) - fadeTime) / proj.getProjectileSpec().getFadeTime()
            	);
        	
        	// derived from magiclib's trail code, this is a cleverer solution than whatever trig i was gonna work out
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
				trailId,	
				Global.getSettings().getSprite("fx", "base_trail_mild"),
				proj.getLocation(),
				0f,
				0f,
				proj.getFacing() - 180f,
				0f,
				0f,
				18,
				24,
				new Color(30,120,255),
            	new Color(30,60,225),
				0.95f * thisLevel * (fadeTime < 0f ? 1f : fadeTime),
				0.01f,
				0.06f,
				0.01f,
				GL11.GL_SRC_ALPHA,
				GL11.GL_ONE,
				128f,
				-128f,
				trailId * 128f,
				sidewaysVel,
				null,
				CombatEngineLayers.ABOVE_SHIPS_LAYER,
				1f
            );
        }
	}
}