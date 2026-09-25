package jsonic.utils;

/** Every level in the game, as LevelConfig instances. Add a level here and nowhere else. */
public final class LevelRegistry {

    private LevelRegistry() {}

    // the one level used for the exam demo
    public static final LevelConfig LEVEL_01 = new LevelConfig(
        "GREEN HILL ZONE",
        "/res/levels/green_hill/map.csv",
        "/res/levels/green_hill/visuals.png",
        "/res/levels/green_hill/backgrounds/",
        "/res/levels/green_hill/splash.png",
        "/res/levels/green_hill/music.wav",
        "/res/levels/green_hill/passed.png",
        2,  // startCol
        58  // startRow
    );

    // pure physics test course: every slope angle, a rectangular loop, a full
    // loop and a semi-loop arc, no background art - meant to run with the debug overlay on
    public static final LevelConfig LEVEL_TEST = new LevelConfig(
        "TEST COURSE",
        "/res/levels/test/map.csv",
        null,
        null, // no parallax scenery for a physics test course
        null, // no splash card either
        "/res/levels/test/music.wav",
        "/res/levels/test/passed.png",
        2,  // startCol
        58  // startRow, same spawn clearance as LEVEL_01
    );

    // Every selectable level, in menu order - the single source of truth MenuState and
    // TitleRenderer read from, so adding a level here is the only place that needs touching.
    // LEVEL_TEST only makes sense with the debug overlay available to read it, so it's hidden
    // from public builds instead of listed there with no way to see what it's testing.
    public static final LevelConfig[] ALL = GameConstants.DEBUG_ENABLED
        ? new LevelConfig[] { LEVEL_01, LEVEL_TEST }
        : new LevelConfig[] { LEVEL_01 };

}
