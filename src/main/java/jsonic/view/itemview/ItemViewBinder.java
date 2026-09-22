package jsonic.view.itemview;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jsonic.model.item.BridgeLog;
import jsonic.model.item.BridgePost;
import jsonic.model.item.BuzzBomberShot;
import jsonic.model.item.FlowerPurple;
import jsonic.model.item.FlowerYellow;
import jsonic.model.item.Goal;
import jsonic.model.item.Item;
import jsonic.model.item.Ring;
import jsonic.model.item.Rock;
import jsonic.model.item.ScatteredRing;
import jsonic.model.item.Spike;
import jsonic.model.item.Spring;

/**
 * Maps each concrete Item subclass to the single ItemView instance that owns its sprite(s) -
 * mirrors JRoyale's EntityViewBinder (one View per type, looked up by the renderer instead of
 * held by the Model instance itself).
 */
public class ItemViewBinder {

    private static ItemViewBinder instance = null;

    private final Map<Class<? extends Item>, ItemView> views = new HashMap<>();

    private ItemViewBinder() {
        RingView ringView = register(Ring.class, RingView::new);
        if (ringView != null) views.put(ScatteredRing.class, ringView); // same sprite/animation as Ring

        register(Spike.class, SpikeView::new);
        register(Rock.class, RockView::new);
        register(Spring.class, SpringView::new);
        register(Goal.class, GoalView::new);
        register(FlowerYellow.class, FlowerYellowView::new);
        register(FlowerPurple.class, FlowerPurpleView::new);
        register(BridgeLog.class, BridgeLogView::new);
        register(BridgePost.class, BridgePostView::new);
        register(BuzzBomberShot.class, BuzzBomberShotView::new);
    }

    // Isolates one failing view constructor so it can't stop every other item type from getting
    // a view, or leave `instance` unassigned and force the whole binder to rebuild every draw call.
    private <T extends ItemView> T register(Class<? extends Item> type, Supplier<T> factory) {
        try {
            T view = factory.get();
            views.put(type, view);
            return view;
        } catch (RuntimeException e) {
            System.err.println("ItemViewBinder: failed to create view for " + type.getSimpleName() + ": " + e);
            return null;
        }
    }

    public ItemView getViewFor(Item item) {
        return views.get(item.getClass());
    }

    // static methods
    public static ItemViewBinder getInstance() {
        if (instance == null) {
            instance = new ItemViewBinder();
        }
        return instance;
    }
}
