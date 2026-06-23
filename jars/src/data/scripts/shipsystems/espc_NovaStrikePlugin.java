package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
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
import org.magiclib.plugins.MagicTrailPlugin;

import data.scripts.util.MezzUtils;
import org.lazywizard.lazylib.VectorUtils;
				
public class espc_NovaStrikePlugin extends BaseEveryFrameCombatPlugin {
	
	public static final float FUSE_TIME = 1.25f;
	public static final float EXPLOSION_RADIUS = 125f;
	public static final float EXPLOSION_CORE = 100f;
	public static final float ACCEL = 5000f;
	public static final float SPEED_BOOST = 500f;
	public static final float IMPULSE_DURATION = 1f;
	public static final float DECCEL_DURATION = 3f;

	
	private static final int PARTICLE_COUNT = 128;
	private static final float PARTICLE_DUR = 0.9f;
	private static final float PARTICLE_DUR_MIN = 0.2f;
	private static final float PARTICLE_DIST_MAX = 600f;
	private static final float PARTICLE_DIST_MIN = 100f;
	private static final float DIST_MULT_MIN = 0.8f;
	private static final float DIST_MULT_MAX = 1.1f;
	private static final float PARTICLE_WIDTH_MAX = 4f;
	private static final float PARTICLE_WIDTH_MIN = 1.5f;
	private static final float PARTICLE_DIST_FORWARDS = 50f;
	
	private ArrayDeque<DamagingProjectileAPI> projs;
	private ShipAPI ship;
	private ArrayDeque<NovaStrikeInstance> strikes;
	
    private class NovaStrikeInstance {
    	private final Vector2f startPos;
    	private final float lifeStart;
    	
    	public NovaStrikeInstance(Vector2f pos) {
            lifeStart = Global.getCombatEngine().getTotalElapsedTime(false);
    		startPos = pos;
    		
    	}
    }
	
	public espc_NovaStrikePlugin(ShipAPI ship) {
		this.ship = ship;
	}
	
	public void addStrike(Vector2f loc) {
		strikes.add(new NovaStrikeInstance(loc));
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
		Iterator<NovaStrikeInstance> strikeIter = strikes.iterator();
		while (strikeIter.hasNext()) {
			NovaStrikeInstance strike = strikeIter.next();
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
		
		GL11.glEnd();
		
	}
}
