// got a little carried away here.  worth it, though
// still using the placeholder flechette sprite as a catch-all
// if i care enough to load textures for each one that means sorting by projectile type to reduce calls/instantiation
// wildly disproportionate amounts of work & computational effort.  *fuck that* this shit's already extra!!

package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;

// import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

// import java.util.Random;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MezzUtils;

import org.lwjgl.util.vector.Vector2f;
// import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;

import org.lwjgl.opengl.GL11;

// import data.scripts.util.MezzUtils;
				
public class espc_CollapseVFX extends BaseEveryFrameCombatPlugin {
	
	private LinkedList<StaticProjSprite> freezeProjs;
	private ShipAPI target;
	private ShipAPI ship;
	private Vector2f jitter;
	private float startTime = -1f;
	private float removeTime = -1f;
	private float thisInterval;
	
	private static final float PARTICLE_DURATION = 1.5f;
	private static final float SPAWN_RADIUS = 80f;
	private static final float PARTICLE_TRAVEL = 30f;
	private static final float PARTICLE_INTERVAL = 0.02f;
	private static final float FADE_TIME = 0.3f;
	private static final float COLLAPSE_TIME = 0.95f;
	private static final float PARTICLE_SIZE_MIN = 4f;
	private static final float PARTICLE_SIZE_DIFF = 2f;

    public espc_CollapseVFX(ShipAPI target, ShipAPI user, float startTime) {
    	this.target = target;
    	this.ship = user;
    	freezeProjs = new LinkedList<StaticProjSprite>();
    	jitter = new Vector2f();
    	this.startTime = startTime;

    	// max radius: paragon, 270
    	// min: xyphos, 30
    	thisInterval = PARTICLE_INTERVAL / target.getShieldRadiusEvenIfNoShield() * 250f;
    }
	
	private class StaticProjSprite {
// 		public String sprite;
		public Vector2f location;
		public float facing;
		public float length;
		public float width;
		public StaticProjSprite(
			ProjectileSpecAPI projSpec,
			Vector2f location,
			float facing
			) {
			// this.sprite = projSpec.getBulletSpriteName();
			this.location = Vector2f.sub(location, target.getLocation(), new Vector2f());
			this.facing = -facing + 90f;
			this.width = projSpec.getWidth();
			if (projSpec.getLength() <= 0f || projSpec.getSpawnType() == ProjectileSpawnType.PLASMA)
				this.length = this.width;
			else
				this.length = projSpec.getLength();
			// Global.getLogger(espc_StaticVFX.class).info("sprite: " + sprite);
		}
		
		public StaticProjSprite(
			MissileAPI missile,
			Vector2f location,
			float facing
			) {
			// this.sprite = projSpec.getBulletSpriteName();
			this.location = Vector2f.sub(location, target.getLocation(), new Vector2f());
			this.facing = -facing + 90f;
			this.length = missile.getSpriteAPI().getHeight() * 1.5f;
			this.width = missile.getSpriteAPI().getWidth() * 1.5f;
		}
	}
	
	public void FreezeProj(DamagingProjectileAPI thisProj, boolean isMissile) {
		StaticProjSprite entry;
		if (!isMissile)
			entry = new StaticProjSprite(
				thisProj.getProjectileSpec(),
				thisProj.getLocation(),
				thisProj.getFacing()
			);
		else
			entry = new StaticProjSprite(
				(MissileAPI) thisProj,
				thisProj.getLocation(),
				thisProj.getFacing()
			);
		freezeProjs.addLast(entry);
	}

	public void releaseVisuals() {
		freezeProjs.clear();
	}
	
	public void setToRemove(CombatEngineAPI combatEngine) {
		removeTime = combatEngine.getTotalElapsedTime(false);
	}
	
	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
    	CombatEngineAPI combatEngine = Global.getCombatEngine();
    	if (amount <= 0f || combatEngine == null || combatEngine.isPaused())
    		return;
    	
    	jitter = new Vector2f(
			Misc.random.nextFloat() * 10f - 5f,
			Misc.random.nextFloat() * 10f - 5f
		);
    	if (removeTime > 0f)
    		jitter.scale(1f + (combatEngine.getTotalElapsedTime(false) - removeTime) / COLLAPSE_TIME);
    	
    	// normally i would assume unapply handles this, but who knows :)
    	if (removeTime < 0f && (ship == null || !combatEngine.isInPlay(ship)))
    		setToRemove(combatEngine);
    	
    	if (removeTime > 0f && combatEngine.getTotalElapsedTime(false) > removeTime + COLLAPSE_TIME) {
    		if (target != null && combatEngine.isInPlay(target))
    			target.removeCustomData("espc_collapse");
			combatEngine.removePlugin(this);
    	}
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
    	CombatEngineAPI combatEngine = Global.getCombatEngine();
    	if (target == null || combatEngine == null || !combatEngine.isInPlay(target))
    		return;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        // guessing i need to close the draw and bind texture for each sprite
        // would require sorting entries to minimize calls - placeholder sabot sprite works well enough lol
		SpriteAPI sprite = Global.getSettings().getSprite("graphics/missiles/missile_sabot_warhead.png");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		
		for (int i = 0; i < 2; i++) {
			Vector2f loc = new Vector2f(target.getLocation().x, target.getLocation().y);
			if (i == 0) {
				Vector2f.add(
					loc, 
					jitter,
					loc
				);
				GL11.glColor4ub(
					(byte) 25, (byte) 75, (byte) 255, (byte) 200
				);
			} else
				GL11.glColor4ub(
					(byte) 125, (byte) 190, (byte) 255, (byte) 200
				);
			
			Iterator<StaticProjSprite> freezeProjIterator = freezeProjs.iterator();
			while (freezeProjIterator.hasNext()) {
				
				StaticProjSprite thisSprite = (StaticProjSprite) freezeProjIterator.next();

				GL11.glTexCoord2f(0, 0);
				GL11.glVertex2f(
					loc.x + thisSprite.location.x - thisSprite.width / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
						- thisSprite.length / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing)),
						loc.y + thisSprite.location.y - thisSprite.length / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
						+ thisSprite.width / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing))
				);
				GL11.glTexCoord2f(1, 0);
				GL11.glVertex2f(
						loc.x + thisSprite.location.x + thisSprite.width / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
					- thisSprite.length / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing)),
					loc.y + thisSprite.location.y - thisSprite.length / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
					- thisSprite.width / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing))
				);
				GL11.glTexCoord2f(1, 1);
				GL11.glVertex2f(
						loc.x + thisSprite.location.x + thisSprite.width / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
					+ thisSprite.length / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing)),
					loc.y + thisSprite.location.y + thisSprite.length / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
					- thisSprite.width / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing))
				);
				GL11.glTexCoord2f(0, 1);
				GL11.glVertex2f(
						loc.x + thisSprite.location.x - thisSprite.width / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
					+ thisSprite.length / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing)),
					loc.y + thisSprite.location.y + thisSprite.length / 2f * (float) FastTrig.cos(Math.toRadians(thisSprite.facing))
					+ thisSprite.width / 2f * (float) FastTrig.sin(Math.toRadians(thisSprite.facing))
				);
			}
		}
		
		GL11.glEnd();

		SpriteAPI sprite2 = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite2.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		
		Random angleRand = new Random((long) startTime);
		Random sizeRand = new Random((long) startTime);
		float currTime = combatEngine.getTotalElapsedTime(false);
		float alphaMult = 1f;
		float distMult = 1f;
		if (removeTime > 0f) {
			alphaMult = Math.max(0f, 1f - (currTime - removeTime)/COLLAPSE_TIME);
			distMult = 1f - MezzUtils.halfSineIn((currTime - removeTime)/COLLAPSE_TIME);
		}

		Vector2f loc = new Vector2f(target.getLocation().x, target.getLocation().y);
		
		for (float i = startTime; i < currTime; i+= thisInterval) {
			if (i < currTime - PARTICLE_DURATION) {
				angleRand.nextFloat();
				sizeRand.nextFloat();
				continue;
			}
				
			float ang = angleRand.nextFloat() * 360f;
			float sizeDiff = sizeRand.nextFloat() * PARTICLE_SIZE_DIFF;
			
			float lifetime = currTime - i;
			float alpha = Math.min(1f, 
				(PARTICLE_DURATION - Math.abs(lifetime * 2f - PARTICLE_DURATION))/FADE_TIME
			);
			
			GL11.glColor4ub(
				(byte) 95, (byte) 160, (byte) 255, (byte) (250 * alpha * alphaMult)
			);
			
			float particleX = loc.x + (float) FastTrig.cos(Math.toRadians(ang)) * distMult * (
				SPAWN_RADIUS + target.getShieldRadiusEvenIfNoShield() +
				PARTICLE_TRAVEL * MezzUtils.halfSineOut(MezzUtils.halfSineOut(lifetime/PARTICLE_DURATION))
			);
			float particleY = loc.y + (float) FastTrig.sin(Math.toRadians(ang)) * distMult * (
				SPAWN_RADIUS + target.getShieldRadiusEvenIfNoShield() +
				PARTICLE_TRAVEL * MezzUtils.halfSineOut(MezzUtils.halfSineOut(lifetime/PARTICLE_DURATION))
			);
			
			MezzUtils.glSquare(particleX, particleY, PARTICLE_SIZE_MIN + sizeDiff);
			
		}
		GL11.glEnd();
		
	}
}
