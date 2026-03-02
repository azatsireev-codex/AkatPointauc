package net.akat.crystal.reward;

import java.util.Random;

public class ConfigBasedRewardStrategy implements CrystalRewardStrategy {

    private final int min;
    private final int max;
    private final Random random = new Random();

    public ConfigBasedRewardStrategy(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public int getRewardAmount() {
        if (max <= min) return min;
        return random.nextInt(max - min + 1) + min;
    }
}
