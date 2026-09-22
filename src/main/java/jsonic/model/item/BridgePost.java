package jsonic.model.item;

/**
 * Fixed anchor at each end of a Bridge (see BridgeLog): never moves,
 * never animates. Sprite is a drum+rod strip padded so the drum lands on
 * the post's own tile once centred; isRight picks the mirrored variant.
 * Purely decorative, onCollision() is Item's no-op default. Sprites live
 * in view.itemview.BridgePostView.
 */
public class BridgePost extends Item {

    public final boolean isRight;

    public BridgePost(boolean isRight) {
        this.isRight = isRight;
        name = "BridgePost";
    }
}
