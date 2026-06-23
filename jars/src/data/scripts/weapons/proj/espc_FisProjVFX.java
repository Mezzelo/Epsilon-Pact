// deterministic sim of single frame renders to achieve more complex particle behaviour
// applying easing curves is instrumental to achieving the specific visuals of this effect,
// and don't appear to be available in the utilities i've found.  attempting to use as little memory as possible here.
// direct render calls has this very performant - 50x fire rate with minimal impact on weaker hardware,
// although lmk if you run into issues.

// the moment the conditions change (i.e. projectile changes trajectory or speed) we abort,
// rather than deal with desync at the cost of more complexity

// you could almost certainly do this faster on the gpu with shaders
// don't have time to study em rn.  there's worse performance hogs.
package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;

import org.lwjgl.opengl.GL11;

import data.scripts.util.MezzUtils;
				
public class espc_FisProjVFX extends BaseEveryFrameCombatPlugin {
	
	private static final float BASE_PROJ_SPEED = 700f;
	
	private static final float PARTICLE_DUR_BASE = 1f;
	private static final float PARTICLE_INTERVAL = 0.005f;
	private static final Color COLOR_INNER = new Color(255, 235, 220);
	private static final Color COLOR_MID = new Color(200, 80, 255);
	private static final Color COLOR_OUTER = new Color(85, 45, 255);
	
	private ArrayDeque<FisProj> projs;
	private ShipAPI ship;
	
    public espc_FisProjVFX(ShipAPI ship) {
    	this.ship = ship;
    	projs = new ArrayDeque<FisProj>();
    }
    
    public void addProj(DamagingProjectileAPI proj) {
    	projs.add(new FisProj(proj));
    }
	
	private class FisProjInterval { 
    	private final Vector2f pos;
    	private final float ang;
    	private final Vector2f vel;
    	private final float time;
    	
    	private FisProjInterval(Vector2f pos, float ang, Vector2f vel, float time) {
            this.pos = new Vector2f(pos.x, pos.y);
            this.ang = ang;
            this.vel = new Vector2f(vel.x, vel.y);
            this.time = time;
    	}
	}
    
    private class FisProj {
    	
    	private final DamagingProjectileAPI proj;
    	private ArrayDeque<FisProjInterval> velDatas;
    	
    	private final float effectStart;
    	private float effectEnd = -1f;
    	private int buffer = 0;
    	
    	private final float rand;
    	private final Long randSeed;
    	
    	private FisProj(DamagingProjectileAPI proj) {
            this.proj = proj;
            effectStart = Global.getCombatEngine().getTotalElapsedTime(false);
            velDatas = new ArrayDeque<FisProjInterval>();
            velDatas.addLast(new FisProjInterval(proj.getLocation(), proj.getFacing(),
            	Misc.ZERO,
            	effectStart));
    		this.rand = Misc.random.nextFloat();
    		this.randSeed = Misc.random.nextLong();
    	}
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
					ship.removeCustomData("espc_FisProjPlugin");
				engine.removePlugin(this);
			}
			return;
		}
		Iterator<FisProj> projIter = projs.iterator();
		while (projIter.hasNext()) {
			FisProj proj = projIter.next();
			if ((proj.proj == null || !engine.isEntityInPlay(proj.proj) || proj.proj.didDamage()) && proj.effectEnd <= 0f) {
				proj.effectEnd = Global.getCombatEngine().getTotalElapsedTime(false);
				if (proj.proj != null) {
					FisProjInterval velData = proj.velDatas.getLast();
					if (velData.time == proj.effectStart && velData.vel.equals(Misc.ZERO))
						velData.vel.set(proj.proj.getVelocity().x, proj.proj.getVelocity().y);
				}
			}
			else if (proj.effectEnd <= 0f) {
				
				FisProjInterval velData = proj.velDatas.getLast();
				if (velData.time == proj.effectStart && velData.vel.equals(Misc.ZERO))
					velData.vel.set(proj.proj.getVelocity().x, proj.proj.getVelocity().y);
				
				if (Math.abs(velData.ang - proj.proj.getFacing()) > 0.001f ||
					Math.abs(velData.vel.x + velData.vel.y - proj.proj.getVelocity().x - proj.proj.getVelocity().y) > 0.01f)
					proj.buffer = 2;
				else if (proj.buffer > 0)
					proj.buffer--;
				
				if (proj.buffer > 0)
					proj.velDatas.addLast(new FisProjInterval(
						new Vector2f(proj.proj.getLocation().x, proj.proj.getLocation().y), 
						proj.proj.getFacing(), 
						new Vector2f(proj.proj.getVelocity().x, proj.proj.getVelocity().y),
						engine.getTotalElapsedTime(false)));
			}
				
			if (proj.effectEnd > 0f && engine.getTotalElapsedTime(false) > proj.effectEnd + PARTICLE_DUR_BASE)
				projIter.remove();
		}
		
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		if (projs.size() == 0)
			return;
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		
		for (FisProj proj : projs) {
			
			Iterator<FisProjInterval> dataIter = proj.velDatas.iterator();
			FisProjInterval data = dataIter.next();
			while (cTime - PARTICLE_DUR_BASE > data.time && dataIter.hasNext()) {
				dataIter.remove();
				data = dataIter.next();
			}
			
			if (!engine.getViewport().isNearViewport(proj.proj.getLocation(), BASE_PROJ_SPEED * 2f))
				continue;
			
			float iMin = Math.max(cTime - PARTICLE_DUR_BASE - proj.effectStart, 0f);
			iMin = iMin - iMin % PARTICLE_INTERVAL;
			float iMax = proj.effectEnd <= 0f ? cTime - proj.effectStart: proj.effectEnd - proj.effectStart;
			Random particleNoise = new Random(proj.randSeed);
			FisProjInterval next = dataIter.hasNext() ? dataIter.next() : null;
			
			for (float i = 0f; i < iMax; i += PARTICLE_INTERVAL) {
				float randX = particleNoise.nextFloat();
				float randY = particleNoise.nextFloat();
				float randZ = particleNoise.nextFloat();
				if (i < iMin)
					continue;
				
				while (dataIter.hasNext() && next.time <= proj.effectStart + i) {
					data = next;
					next = dataIter.next();
				}
				
				// basically a layer of pseudo-random-enough bullshit noise over a chaotic trig func
				// grants a nice degree of uniformity to the patterns
				// random distribution is square instead of circular, but it's not really noticeable /cope
				float particleX = data.pos.x + data.vel.x * (i - data.time + proj.effectStart) +
					(float) ((FastTrig.sin(((i + proj.rand * 5f) * (15f + proj.rand * 20f))) * 30f + randX * 36f - 18f) 
					* FastTrig.cos(Math.toRadians(data.ang - 90f))
						* MezzUtils.halfSineOut((cTime - i - proj.effectStart)/PARTICLE_DUR_BASE)
					+ (FastTrig.cos(((i + proj.rand * 7f) * (20f + proj.rand * 30f))) * 65f + randY * 36f - 18f) 
						* FastTrig.cos(Math.toRadians(data.ang))
						* MezzUtils.halfSineIn((cTime - i - proj.effectStart)/PARTICLE_DUR_BASE));
				float particleY = data.pos.y + data.vel.y * (i - data.time + proj.effectStart) + 
					(float) ((FastTrig.sin(((i + proj.rand * 5f) * (15f + proj.rand * 20f))) * 30f + randX * 36f - 18f) 
					* FastTrig.sin(Math.toRadians(data.ang - 90f))
						* MezzUtils.halfSineOut((cTime - i - proj.effectStart)/PARTICLE_DUR_BASE)
					+ (FastTrig.cos(((i + proj.rand * 7f) * (20f + proj.rand * 30f))) * 65f + randY * 36f - 18f) 
						* FastTrig.sin(Math.toRadians(data.ang))
						* MezzUtils.halfSineIn((cTime - i - proj.effectStart)/PARTICLE_DUR_BASE));
				float particleSize = 6f + randZ * 8f;
				
				// hybrid time+displacement approach for color, creating a hotter core
				if ((cTime - i - proj.effectStart) / PARTICLE_DUR_BASE * (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f < 0.7f)
					MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
						(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + proj.effectStart) * 2f), 
						0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
						(cTime - i - proj.effectStart) / PARTICLE_DUR_BASE * 
						(Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f / 0.7f
					);
				else
					MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER,
						(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + proj.effectStart) * 2f), 
						0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
						Math.min(((cTime - i - proj.effectStart) / PARTICLE_DUR_BASE * 
						(Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f - 0.7f) * 7f / 0.3f, 1f)
					);
				
				MezzUtils.glSquare(particleX, particleY, particleSize/2f);
				
			}
			
			if (proj.effectEnd > 0f) {
				
				// no need to reset seed: we already iterate fully through it prior.
				if (dataIter.hasNext())
					data = proj.velDatas.getLast();
				
				for (float i = 0; i < 18; i ++) {
					// slightly different application of our noise here, but the same set of sequences suffices.
					float randX = particleNoise.nextFloat();
					float randY = particleNoise.nextFloat();
					float randZ = particleNoise.nextFloat();
				
					float particleX = data.pos.x + data.vel.x * 
						(proj.effectEnd - data.time) + data.vel.x/(2f + randX * 7f) * (cTime - proj.effectEnd) + 
						MezzUtils.halfSineOut((cTime - proj.effectEnd)/PARTICLE_DUR_BASE) * 
						(float) FastTrig.cos(Math.toRadians(data.ang - 55f + 110f * randY)) * 25f;
					float particleY = data.pos.y + data.vel.y * 
						(proj.effectEnd - data.time) + data.vel.y/(2f + randX * 7f) * (cTime - proj.effectEnd) + 
						MezzUtils.halfSineOut((cTime - proj.effectEnd)/PARTICLE_DUR_BASE) * 
						(float) FastTrig.sin(Math.toRadians(data.ang - 55f + 110f * randY)) * 25f;
					float particleSize = 9f + randZ * 6f;
					
					if (Math.abs(randY - 0.5f) < 0.25f)
						MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
							(int)(MathUtils.clamp((proj.effectEnd + PARTICLE_DUR_BASE - cTime - i/24f) * 2f, 
							0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
							Math.abs(randY - 0.5f) * 4f
						);
					else
						MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER, 
							(int)(MathUtils.clamp((proj.effectEnd + PARTICLE_DUR_BASE - cTime - i/24f) * 2f, 
							0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
							(Math.abs(randY - 0.5f) - 0.25f) * 4f
						);

					MezzUtils.glSquare(particleX, particleY, particleSize/2f);
				
				}
			}
		}
		
		GL11.glEnd();
		
	}
}
