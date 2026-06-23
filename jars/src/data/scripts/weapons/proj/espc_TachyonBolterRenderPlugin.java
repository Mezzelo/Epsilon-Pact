package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import data.scripts.util.MezzUtils;
import data.scripts.weapons.espc_TachyonBolterEffect;
				
public class espc_TachyonBolterRenderPlugin extends BaseEveryFrameCombatPlugin {
	
	private static final int PARTICLE_COUNT = 64;
	private static final float PARTICLE_DUR = 0.6f;
	private static final float PARTICLE_DUR_MIN = 0.2f;
	private static final float PARTICLE_DIST_MAX = 400f;
	private static final float PARTICLE_DIST_MIN = 50f;
	private static final float DIST_MULT_MIN = 0.8f;
	private static final float DIST_MULT_MAX = 1.1f;
	private static final float PARTICLE_WIDTH_MAX = 3f;
	private static final float PARTICLE_WIDTH_MIN = 1.5f;
	private static final float PARTICLE_DIST_FORWARDS = 50f;
	
	private static final int SPARK_PARTICLE_COUNT = 32;
	private static final float SPARK_DELAY = 0.05f;
	private static final float SPARK_BEARING_MAX = 20f;
	private static final float SPARK_ANGLE_SPREAD = 35f;
	private static final float SPARK_PARTICLE_DUR = 0.7f;
	private static final float SPARK_PARTICLE_DUR_MIN = 0.4f;
	private static final float SPARK_PARTICLE_DIST_MAX = 250f;
	private static final float SPARK_PARTICLE_DIST_MAX_MAX = 350f;
	private static final float SPARK_PARTICLE_DIST_MIN = 100f;
	
	private ArrayDeque<DamagingProjectileAPI> projs;
	private ArrayDeque<WeaponAPI> weps;
	private ArrayDeque<TachyonBolterImpact> impacts;
	private ArrayDeque<TachyonBolterSpark> sparks;
	
    private class TachyonBolterSpark {
    	private final float effectStart;
    	private final WeaponAPI weapon;
    	private final Vector2f vel;
    	private final float startAng;
    	private final float distMax;
    	private final float angSinY;
    	private final float angCosY;
    	
    	public TachyonBolterSpark(WeaponAPI wep, Vector2f vel) {
            this.effectStart = Global.getCombatEngine().getTotalElapsedTime(false);
    		this.weapon = wep;
    		this.startAng = (Misc.random.nextFloat() * 2f - 1f) * SPARK_BEARING_MAX + wep.getCurrAngle();
    		this.angSinY = (float) FastTrig.sin(Math.toRadians(startAng));
    		this.angCosY = (float) FastTrig.cos(Math.toRadians(startAng));
    		this.vel = vel;
    		this.distMax = Misc.random.nextFloat() * (SPARK_PARTICLE_DIST_MAX_MAX - SPARK_PARTICLE_DIST_MAX) 
    			+ SPARK_PARTICLE_DIST_MAX;
    		
    	}
    }
	
    private class TachyonBolterImpact {
    	private final Vector2f startPos;
    	private final float startAng;
    	private final float effectStart;
    	private final float angSinY;
    	private final float angCosY;
    	private final float distMax;
    	private final float durMax;
    	
    	public TachyonBolterImpact(Vector2f pos, float angle) {
            effectStart = Global.getCombatEngine().getTotalElapsedTime(false);
    		startPos = pos;
    		startAng = angle;
    		angSinY = (float) FastTrig.sin(Math.toRadians(startAng));
    		angCosY = (float) FastTrig.cos(Math.toRadians(startAng));
    		distMax = Misc.random.nextFloat() * (DIST_MULT_MAX - DIST_MULT_MIN) * PARTICLE_DIST_MAX + DIST_MULT_MIN;
    		durMax = PARTICLE_DUR;
    		
    	}
    }
	
	public espc_TachyonBolterRenderPlugin(espc_TachyonBolterEffect plugin) {
		projs = new ArrayDeque<DamagingProjectileAPI>();
		weps = new ArrayDeque<WeaponAPI>();
		impacts = new ArrayDeque<TachyonBolterImpact>();
		sparks = new ArrayDeque<TachyonBolterSpark>();
	}
	
	public void addWeapon(WeaponAPI wep) {
		weps.add(wep);
	}
	
	public void addProj(DamagingProjectileAPI proj) {
		projs.addLast(proj);
	}
	
	public void addImpact(Vector2f point, float ang) {
		impacts.addLast(new TachyonBolterImpact(point, ang));
	}
	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
		if (amount <= 0f)
			return;
    	if (Global.getCombatEngine().isPaused())
    		return;
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null)
			return;
		if (projs.size() == 0 && weps.size() == 0 && impacts.size() == 0) {
			engine.removePlugin(this);
			return;
		}
		
		
		if (projs.size() > 0) {
			Iterator<DamagingProjectileAPI> iter = projs.iterator();
			while (iter.hasNext()) {
				DamagingProjectileAPI proj = iter.next();
				if (proj == null || !engine.isEntityInPlay(proj)) {
					iter.remove();
					continue;
				}
			}
		}
		if (weps.size() > 0) {
			Iterator<WeaponAPI> wepIter = weps.iterator();
			while (wepIter.hasNext()) {
				WeaponAPI wep = wepIter.next();
				if (wep == null || wep.getShip() == null ||
					!engine.isEntityInPlay(wep.getShip())) {
					wepIter.remove();
					continue;
				}
				if (wep.getChargeLevel() > 0f && wep.getCooldown() <= 0f)
					continue;
			}
		}
		if (impacts.size() == 0)
			return;
		Iterator<TachyonBolterImpact> impactIter = impacts.iterator();
		while (impactIter.hasNext()) {
			TachyonBolterImpact impact = impactIter.next();
			if (engine.getTotalElapsedTime(false) > impact.effectStart + impact.durMax)
				impactIter.remove();
		}
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);
		Random jitterRand = new Random((long) (cTime * 600f));

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		
		if (weps.size() > 0) {
			Iterator<WeaponAPI> wepIter = weps.iterator();
			while (wepIter.hasNext()) {
				WeaponAPI wep = wepIter.next();
				if (wep.getChargeLevel() >= 1f) {
					
				}
	
			}
		}

		Iterator<DamagingProjectileAPI> iter = projs.iterator();
		while (iter.hasNext()) {
			DamagingProjectileAPI proj = iter.next();
			GL11.glColor4ub(
				(byte) 255, (byte) 255, (byte) 255, (byte) 255);
			MezzUtils.glSquare(proj.getLocation().x, proj.getLocation().y, 
				Math.min(proj.getDamageAmount() / espc_TachyonBolterEffect.BASE_DAMAGE, 1f) * 
				(10f * jitterRand.nextFloat() + 2f));
			GL11.glColor4ub(
				(byte) 175, (byte) 55, (byte) 255, (byte) 205);
			MezzUtils.glSquare(proj.getLocation().x, proj.getLocation().y, 
				Math.min(proj.getDamageAmount() / espc_TachyonBolterEffect.BASE_DAMAGE, 1f) * 
				(35f * jitterRand.nextFloat() + 7f));
			GL11.glColor4ub(
				(byte) 85, (byte) 25, (byte) 215, (byte) 65);
				MezzUtils.glSquare(proj.getLocation().x, proj.getLocation().y, 
				Math.min(proj.getDamageAmount() / espc_TachyonBolterEffect.BASE_DAMAGE, 1f) * 
				(70f * jitterRand.nextFloat() + 15f));
			
		}
		
		for (TachyonBolterImpact impact: impacts) {
			if (!engine.getViewport().isNearViewport(impact.startPos, 500f)) {
				jitterRand.nextFloat();
				jitterRand.nextFloat();
				continue;
			}
			
			Random rand = new Random((long) (impact.effectStart + impact.startPos.x * 10f + impact.startPos.y * 10f));
			
			for (int i = 0; i < PARTICLE_COUNT; i ++) {
				float dur = MezzUtils.halfSineIn(rand.nextFloat()) * (impact.durMax - PARTICLE_DUR_MIN) + PARTICLE_DUR_MIN;
				float ang = rand.nextFloat() * 360f;
				float distRand = rand.nextFloat();
				float dist = distRand * (impact.distMax - PARTICLE_DIST_MIN) + PARTICLE_DIST_MIN;
				if (cTime - impact.effectStart > dur)
					continue;
				float randCos = (float) FastTrig.cos((ang + impact.startAng) * MathUtils.FPI / 180f);
				float randSin = (float) FastTrig.sin((ang + impact.startAng) * MathUtils.FPI / 180f);
				float locSin = (float) FastTrig.sin(ang * MathUtils.FPI / 180f);
				float interp = Math.min((cTime - impact.effectStart)/dur, 1f);
				float scale = 1f - 
					Math.min(Math.max((cTime - impact.effectStart - dur / 2f) * 2f / dur, 0f), 1f);
				
				float width = (PARTICLE_WIDTH_MAX - PARTICLE_WIDTH_MIN) * jitterRand.nextFloat() + PARTICLE_WIDTH_MIN;
				float opacity = Math.max(0.1f, jitterRand.nextFloat() * 2.5f - 0.5f);
				
				float x = impact.startPos.x + 
					MezzUtils.halfSineOut(interp) * randCos * dist * (0.65f + Math.abs(locSin) * 0.35f) +
					MezzUtils.halfSineIn(interp) * impact.angCosY * PARTICLE_DIST_FORWARDS * (distRand * 0.7f + 0.3f);
				float y = impact.startPos.y +
					MezzUtils.halfSineOut(interp) * randSin * dist * (0.65f + Math.abs(locSin) * 0.35f) +
					MezzUtils.halfSineIn(interp) * impact.angSinY * PARTICLE_DIST_FORWARDS * (distRand * 0.7f + 0.3f);
				
				GL11.glColor4ub(
					(byte) 255, (byte) 255, (byte) 255, (byte) 255);
				MezzUtils.glSquare(x, y, scale * width * (opacity * 0.5f + 0.5f));
				MezzUtils.colorHSBLerp4UB(
					new Color(175, 55, 255), 
					new Color(85, 25, 215), 
					(int) (205 * opacity), (int) (205 * opacity), MezzUtils.halfSineOut(interp));
				MezzUtils.glSquare(x, y, scale * width * 2f);
				MezzUtils.colorHSBLerp4UB(
					new Color(175, 55, 255), 
					new Color(85, 25, 255), 
					125, 125, MezzUtils.halfSineOut(interp));
				MezzUtils.glSquare(x, y, scale * width * 4.5f * (opacity * 0.5f + 0.5f));
				
			}
		}
		
		GL11.glEnd();
		
	}
}
