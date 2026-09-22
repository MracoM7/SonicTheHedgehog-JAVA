package jsonic.model.entity;

/** What Camera needs to know about whatever it's following — implemented by Player. */
public interface ICameraTarget {

    public float getX();

    public float getY();

    public float getVelocityX();

    public float getVelocityY();

    public int getCurrentRadiusY();

    public boolean isLookingUp();

    public boolean isCurlingUp();

    public boolean isDying();
}
