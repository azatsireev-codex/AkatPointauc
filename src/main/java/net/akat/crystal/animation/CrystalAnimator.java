package net.akat.crystal.animation;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CrystalAnimator {

    private final Map<Integer, AnimatedCrystal> crystals = new HashMap<>();

    public void registerCrystal(ItemDisplay display, int index) {
        float phase = index * (float) (Math.PI / 4);
        crystals.put(display.getEntityId(), new AnimatedCrystal(display, display.getTransformation(), phase));
    }

    public void animateAll() {
        double time = System.currentTimeMillis() / 1000.0;

        double speed = 0.4;
        double amplitude = 0.15;
        float lerpFactor = 0.1f;

        for (Iterator<Map.Entry<Integer, AnimatedCrystal>> it = crystals.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, AnimatedCrystal> entry = it.next();
            AnimatedCrystal crystal = entry.getValue();
            ItemDisplay display = crystal.display();

            if (!display.isValid()) {
                it.remove();
                continue;
            }

            Transformation base = crystal.baseTransformation();
            Vector3f basePos = base.getTranslation();
            Vector3f currentPos = display.getTransformation().getTranslation();

            float targetY = basePos.y() + (float) (Math.sin(time * speed * 2 * Math.PI + crystal.phase()) * amplitude);

            Vector3f newPos = new Vector3f(
                    lerp(currentPos.x(), basePos.x(), lerpFactor),
                    lerp(currentPos.y(), targetY, lerpFactor),
                    lerp(currentPos.z(), basePos.z(), lerpFactor)
            );

            Transformation newTransform = new Transformation(
                    newPos,
                    base.getLeftRotation(),
                    base.getScale(),
                    base.getRightRotation()
            );

            display.setTransformation(newTransform);
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private record AnimatedCrystal(ItemDisplay display, Transformation baseTransformation, float phase) {}
}
