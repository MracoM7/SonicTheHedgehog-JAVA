package jsonic.view.enemyview;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jsonic.model.enemy.BuzzBomber;
import jsonic.model.enemy.Chopper;
import jsonic.model.enemy.Enemy;
import jsonic.model.enemy.Motobug;

/** Maps each concrete Enemy subclass to the single EnemyView instance that owns its sprite(s), looked up by the renderer instead of held by the Model instance itself. */
public class EnemyViewBinder {

    private static EnemyViewBinder instance = null;

    private final Map<Class<? extends Enemy>, EnemyView> views = new HashMap<>();

    private EnemyViewBinder() {
        register(Motobug.class, MotobugView::new);
        register(BuzzBomber.class, BuzzBomberView::new);
        register(Chopper.class, ChopperView::new);
    }

    // Isolates one failing view constructor so it can't stop every other enemy type from getting
    // a view, or leave `instance` unassigned and force the whole binder to rebuild every draw call.
    private <T extends EnemyView> T register(Class<? extends Enemy> type, Supplier<T> factory) {
        try {
            T view = factory.get();
            views.put(type, view);
            return view;
        } catch (RuntimeException e) {
            System.err.println("EnemyViewBinder: failed to create view for " + type.getSimpleName() + ": " + e);
            return null;
        }
    }

    public EnemyView getViewFor(Enemy enemy) {
        return views.get(enemy.getClass());
    }

    // static methods
    public static EnemyViewBinder getInstance() {
        if (instance == null) {
            instance = new EnemyViewBinder();
        }
        return instance;
    }
}
