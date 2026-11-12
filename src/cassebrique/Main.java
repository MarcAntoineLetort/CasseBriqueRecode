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

        scene.setOnKeyPressed(e -> onKey(stage, e.getCode(), true));
        scene.setOnKeyReleased(e -> onKey(stage, e.getCode(), false));

        // Pause automatique si la fenetre perd le focus
        stage.focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                game.clearKeys();
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
                // En duel (clavier seul) le curseur reste visible ; en solo il se cache pendant le jeu.
                boolean hide = !game.twoPlayer
                        && (game.state == Game.State.PLAYING || game.state == Game.State.READY);
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

    /** Toutes les touches : deplacement (solo ou duel), actions, menu. */
    private void onKey(Stage stage, KeyCode k, boolean down) {
        if (game.state == Game.State.MENU) {
            if (!down) return;
            if (k == KeyCode.LEFT || k == KeyCode.PAGE_UP) game.changePage(-1);
            else if (k == KeyCode.RIGHT || k == KeyCode.PAGE_DOWN) game.changePage(1);
            else if (k == KeyCode.SPACE || k == KeyCode.ENTER) game.action(0);
            else common(stage, k);
            return;
        }

        // Deplacements (relaches pris en compte dans tous les etats)
        if (game.twoPlayer) {
            if (k == KeyCode.Z || k == KeyCode.W) game.setMove(0, -1, down);       // blanc : haut
            else if (k == KeyCode.S) game.setMove(0, 1, down);                      // blanc : bas
            else if (k == KeyCode.UP) game.setMove(1, -1, down);                    // noir : haut
            else if (k == KeyCode.DOWN) game.setMove(1, 1, down);                   // noir : bas
        } else {
            if (k == KeyCode.LEFT || k == KeyCode.A || k == KeyCode.Q) game.setMove(0, -1, down);
            else if (k == KeyCode.RIGHT || k == KeyCode.D) game.setMove(0, 1, down);
        }
        if (!down) return;

        if (k == KeyCode.SPACE) game.action(1);       // joueur blanc / solo
        else if (k == KeyCode.ENTER) game.action(2);  // joueur noir / solo
        else if (k == KeyCode.P || k == KeyCode.ESCAPE) game.togglePause();
        else if (k == KeyCode.M) game.backToMenu();
        else common(stage, k);
    }

    private void common(Stage stage, KeyCode k) {
        if (k == KeyCode.N) {
            game.muted = !game.muted;
            game.saveDirty = true;
        } else if (k == KeyCode.F || k == KeyCode.F11) {
            stage.setFullScreen(!stage.isFullScreen());
        }
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
