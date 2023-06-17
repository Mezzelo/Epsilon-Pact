package data.scripts.util;

// import com.fs.starfarer.api.util.Misc;
// import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;
import java.awt.Color;

public class MezzUtils {
	
	// taking a 0-1 input, uses the first quarter of a sine curve for interp.
	// creates a smooth ease out without ease in, as opposed to full sine used in i.e. magiclib
	public static float halfSineOut(float interp) {
		return (float) FastTrig.sin(interp * MathUtils.FPI / 2f);
	}
	
	// ditto, but with the fourth quarter.
	public static float halfSineIn(float interp) {
		return (float) FastTrig.sin(interp * MathUtils.FPI / 2f + MathUtils.FPI * 3f / 2f) + 1f;
		
	}
	
	// debug, for A/B testing without changing the syntax lmao
	public static float dummyTween(float interp) {
		return interp;
		
	}
	
	// quad call shorthand.  size should be halved as parameter.
	public static void glSquare(float x, float y, float size) {
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex2f(x - size, y - size);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex2f(x + size, y - size);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex2f(x + size, y + size);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex2f(x - size, y + size);
	}
	
	// HSV lerp between colours.  avoids the muddying of saturation that occurs normally otherwise,
	// due to passing through the middle of the color wheel instead of around it.
	public static Color colorHSBLerp(Color color1, Color color2, float interp) {
		float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), new float[3]);
		float[] color2HSB = Color.RGBtoHSB(color1.getRed(), color2.getGreen(), color2.getBlue(), new float[3]);
		return new Color(
			Color.HSBtoRGB(
				color1HSB[0] * 1f - interp + color2HSB[0] * interp,
				color1HSB[1] * 1f - interp + color2HSB[1] * interp,
				color1HSB[2] * 1f - interp + color2HSB[2] * interp
		));
	}
	
	// sometimes HSB takes the longer route due to the cyclical nature of hue - i.e. H2 - H1 > 0.5.
	// we can force the opposite direction to solve this.
	// as we're usually tweening between known values, we use a separate func here to minimize conditionals
	// (and also if we want to make some rainbow bullshit intentionally)
	
	public static Color colorHSBLerpRev(Color color1, Color color2, float interp) {
		float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), new float[3]);
		float[] color2HSB = Color.RGBtoHSB(color1.getRed(), color2.getGreen(), color2.getBlue(), new float[3]);
		float diff = color1HSB[0] + 1f - color2HSB[0];
		return new Color(
			Color.HSBtoRGB(
				(diff * interp > color1HSB[0] ? 1f : 0f) + color1HSB[0] - diff * interp,
				color1HSB[1] * (1f - interp) + color2HSB[1] * interp,
				color1HSB[2] * (1f - interp) + color2HSB[2] * interp
		));
	}
	
	// shorthands for direct gl calls, so we don't have to middle-man the parameters.
	
	public static void colorHSBLerp4UB(Color color1, Color color2, int alpha1, int alpha2, float interp) {
		float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), new float[3]);
		float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), new float[3]);
		Color colorLerp = new Color(
			Color.HSBtoRGB(
				color1HSB[0] * (1f - interp) + color2HSB[0] * interp,
				color1HSB[1] * (1f - interp) + color2HSB[1] * interp,
				color1HSB[2] * (1f - interp) + color2HSB[2] * interp
		));
		GL11.glColor4ub(
			(byte) colorLerp.getRed(), (byte) colorLerp.getGreen(), (byte) colorLerp.getBlue(), (byte) ((int) (alpha1 * (1f - interp)) + (int) (alpha2 * interp))
		);
	}
	
	// single alpha input to cut down a little
	public static void colorHSBLerp4UB(Color color1, Color color2, int alpha, float interp) {
		float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), new float[3]);
		float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), new float[3]);
		Color colorLerp = new Color(
			Color.HSBtoRGB(
				color1HSB[0] * (1f - interp) + color2HSB[0] * interp,
				color1HSB[1] * (1f - interp) + color2HSB[1] * interp,
				color1HSB[2] * (1f - interp) + color2HSB[2] * interp
		));
		GL11.glColor4ub(
			(byte) colorLerp.getRed(), (byte) colorLerp.getGreen(), (byte) colorLerp.getBlue(), (byte) alpha
		);
	}
	
	public static void colorHSBLerp4UBRev(Color color1, Color color2, int alpha1, int alpha2, float interp) {
		float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), new float[3]);
		float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), new float[3]);
		float diff = color1HSB[0] + 1f - color2HSB[0];
		Color colorLerp = new Color(
			Color.HSBtoRGB(
				(diff * interp > color1HSB[0] ? 1f : 0f) + color1HSB[0] - diff * interp,
				color1HSB[1] * (1f - interp) + color2HSB[1] * interp,
				color1HSB[2] * (1f - interp) + color2HSB[2] * interp
		));
		GL11.glColor4ub(
			(byte) colorLerp.getRed(), (byte) colorLerp.getGreen(), (byte) colorLerp.getBlue(), (byte) ((int) (alpha1 * (1f - interp)) + (int) (alpha2 * interp))
		);
	}
	
	public static void colorHSBLerp4UBRev(Color color1, Color color2, int alpha, float interp) {
		float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), new float[3]);
		float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), new float[3]);
		float diff = color1HSB[0] + 1f - color2HSB[0];
		Color colorLerp = new Color(
			Color.HSBtoRGB(
				(diff * interp > color1HSB[0] ? 1f : 0f) + color1HSB[0] - diff * interp,
				color1HSB[1] * (1f - interp) + color2HSB[1] * interp,
				color1HSB[2] * (1f - interp) + color2HSB[2] * interp
		));
		GL11.glColor4ub(
			(byte) colorLerp.getRed(), (byte) colorLerp.getGreen(), (byte) colorLerp.getBlue(), (byte) alpha
		);
	}
}