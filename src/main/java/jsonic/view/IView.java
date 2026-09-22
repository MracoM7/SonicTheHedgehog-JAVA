package jsonic.view;

public interface IView {

    public void render(IRenderSnapshot snapshot);

    // static methods
    public static IView getInstance() {
        return GameView.getInstance();
    }
}
