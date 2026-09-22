package jsonic.model.entity;

/** Only effects an Item can apply to the collector at the moment of collection/collision. */
public interface ICollector {

    public void addRing();

    /** `amount` is currently unused (Sonic's hurt model is binary), kept for future variable-damage sources. */
    public void takeDamage(int amount);

    public void reachGoal();

    /** A spring's effect: overrides vertical velocity and forces the player airborne. */
    public void launch(float velocityY);

    /** Unconditional small upward bump, unlike takeDamage() — used by Spike so the player can't rest on it. */
    public void bounce();
}
