package cassebrique;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    private static final double STEP = 1.0 / 120.0; // pas de simulation fixe

    private final Game game = new Game();
    private final Renderer renderer = new Renderer();
    private AnimationTimer timer;

    @Override
    public void start(final Stage stage) {
        Storage.load(game);
        final Sounds sounds = new Sounds();
        game.sfx = sounds::play;

        final Canvas canvas = new Canvas(Game.W, Game.H);
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #04050d;");
        final Scene scene = new Scene(root, 1024, 700);
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        // Souris : conversion pixels fenetre -> coordonnees logiques du jeu
        scene.setOnMouseMoved(e -> game.mouseMoved(toLogicalX(canvas, e.getSceneX()), toLogicalY(canvas, e.getSceneY())));
        scene.setOnMouseDragged(e -> game.mouseMoved(toLogicalX(canvas, e.getSceneX()), toLogicalY(canvas, e.getSceneY())));
        scene.setOnMousePressed(e -> game.click(toLogicalX(canvas, e.getSceneX()), toLogicalY(canvas, e.getSceneY())));
        // Curseur qui sort de la fenetre : la raquette va jusqu'au bord par lequel il est sorti
        scene.setOnMouseExited(e -> {
            double sx = Math.max(0, Math.min(scene.getWidth(), e.getSceneX()));
            game.mouseExited(toLogicalX(canvas, sx));
        });

        scene.setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.LEFT || k == KeyCode.A || k == KeyCode.Q) game.setLeft(true);
            else if (k == KeyCode.RIGHT || k == KeyCode.D) game.setRight(true);
            else if (k == KeyCode.SPACE || k == KeyCode.ENTER) game.action();
            else if (k == KeyCode.P || k == KeyCode.ESCAPE) game.togglePause();
            else if (k == KeyCode.M) game.backToMenu();
            else if (k == KeyCode.S) {
                game.muted = !game.muted;
                game.saveDirty = true;
            } else if (k == KeyCode.F || k == KeyCode.F11) stage.setFullScreen(!stage.isFullScreen());
        });
        scene.setOnKeyReleased(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.LEFT || k == KeyCode.A || k == KeyCode.Q) game.setLeft(false);
            else if (k == KeyCode.RIGHT || k == KeyCode.D) game.setRight(false);
        });

        // Pause automatique si la fenetre perd le focus
        stage.focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                game.setLeft(false);
                game.setRight(false);
                game.pauseIfPlaying();
            }
        });

        timer = new AnimationTimer() {
            private long last = 0;
            private double acc = 0;
            private boolean cursorHidden = false;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double dt = Math.min(0.1, (now - last) / 1e9);
                last = now;
                acc += dt;
                while (acc >= STEP) {
                    game.update(STEP);
                    acc -= STEP;
                }
                renderer.render(gc, game, canvas.getWidth(), canvas.getHeight());

                if (game.saveDirty) {
                    game.saveDirty = false;
                    Storage.save(game);
                }
                boolean hide = game.state == Game.State.PLAYING || game.state == Game.State.READY;
                if (hide != cursorHidden) {
                    scene.setCursor(hide ? Cursor.NONE : Cursor.DEFAULT);
                    cursorHidden = hide;
                }
            }
        };
        timer.start();

        stage.setTitle("Casse-Brique");
        stage.setMinWidth(640);
        stage.setMinHeight(460);
        stage.setFullScreenExitHint("F11 pour quitter le plein \u00e9cran");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (timer != null) timer.stop();
    }

    private static double scale(Canvas c) {
        return Math.min(c.getWidth() / Game.W, c.getHeight() / Game.H);
    }

    private static double toLogicalX(Canvas c, double sceneX) {
        double s = scale(c);
        return (sceneX - (c.getWidth() - Game.W * s) / 2) / s;
    }

    private static double toLogicalY(Canvas c, double sceneY) {
        double s = scale(c);
        return (sceneY - (c.getHeight() - Game.H * s) / 2) / s;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
