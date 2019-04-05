package uk.ac.york.sepr4.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import lombok.Getter;
import lombok.Setter;
import uk.ac.york.sepr4.GameInstance;
import uk.ac.york.sepr4.io.FileManager;
import uk.ac.york.sepr4.io.MinigameInputProcessor;
import uk.ac.york.sepr4.object.entity.Player;
import uk.ac.york.sepr4.utils.StyleManager;

import java.util.Random;

public class MinigameScreen extends PirateScreen {

    private Table table, gameTable;
    @Getter
    private MinigameDifficulty difficulty;
    @Getter @Setter
    private boolean gameStarted = false, gameOver = false;

    private float enemyShootTimer, startCountdown;

    private Label gameText;
    private Image playerImage, enemyImage;

    public MinigameScreen(GameInstance gameInstance) {
        super(gameInstance, new Stage(new ScreenViewport()), FileManager.menuScreenBG);

        getInputMultiplexer().addProcessor(new MinigameInputProcessor(this));

        setEnableStatsHUD(true);

        createMenu();
        displayMenu();
    }

    @Override
    public void renderInner(float delta) {
        if(gameOver) {
            setGameStarted(false);
            startCountdown+=delta;
            if(startCountdown>3f) {
                resetGame();
            }
        }
        if(difficulty != null && gameStarted) {
            //minigame being played
            startCountdown -= delta;
            enemyShootTimer -= delta;
            if (startCountdown <= 0) {
                    gameText.setText("SHOOT! (Z)");
                    gameText.setColor(Color.RED);
                    if (enemyShootTimer <= 0) {
                        enemyShoot();
                    }
                }
        }

    }

    private void enemyShoot() {
        gameText.setText("You Lost!");
        setGameOver(true);
    }

    public void playerShoot() {
        if(startCountdown > 0) {
            //shot too early - loose!
            resetGame();
        } else {
            giveReward();
            gameText.setText("You Won!");
            setGameOver(true);
        }
    }

    private void resetGame() {
        difficulty = null;
        startCountdown = 0;
        enemyShootTimer = 0;
        gameTable = null;
        setGameStarted(false);
        setGameOver(false);
        displayMenu();
    }

    private void giveReward() {
        getGameInstance().getEntityManager().getOrCreatePlayer().addBalance(difficulty.getReward());
    }

    private void createMenu() {
        table = new Table();
        table.top();
        table.setFillParent(true);

        Label minigameText = new Label("How difficult do you want your minigame to be? Higher difficulty means higher rewards!", StyleManager.generateLabelStyle(35, Color.GRAY));
        Label instructionText = new Label("Wait for the signal, then use the Z key to shoot before your opponent does.", StyleManager.generateLabelStyle(25, Color.GRAY));

        TextButton quitMinigame = new TextButton("Exit Minigame", StyleManager.generateTBStyle(25, Color.RED, Color.GRAY));
        quitMinigame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent ev, float x, float y) {
                getGameInstance().fadeSwitchScreen(getGameInstance().getSailScreen(), true);
            }
        });
        TextButton easyMinigame = new TextButton("Easy (1 gold)", StyleManager.generateTBStyle(25, Color.GREEN, Color.GRAY));
        easyMinigame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent ev, float x, float y) {
                startGame(MinigameDifficulty.EASY);
            }
        });
        TextButton medMinigame = new TextButton("Medium (10 gold)", StyleManager.generateTBStyle(25, Color.YELLOW, Color.GRAY));
        medMinigame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent ev, float x, float y) {
                startGame(MinigameDifficulty.MEDIUM);
            }
        });
        TextButton hardMinigame = new TextButton("Hard (20 gold)", StyleManager.generateTBStyle(25, Color.RED, Color.GRAY));
        hardMinigame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent ev, float x, float y) {
                startGame(MinigameDifficulty.HARD);
            }
        });
        TextButton veryHardMinigame = new TextButton("Very Hard (50 gold)", StyleManager.generateTBStyle(25, Color.BLACK, Color.GRAY));
        veryHardMinigame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent ev, float x, float y) {
                startGame(MinigameDifficulty.VERY_HARD);
            }
        });

        table.add(minigameText).padTop(Value.percentHeight(0.05f, table)).expandX();
        table.row();
        table.add(instructionText).padTop(Value.percentHeight(0.02f, table));
        table.row();
        table.add(easyMinigame);
        table.row();
        table.add(medMinigame);
        table.row();
        table.add(hardMinigame);
        table.row();
        table.add(veryHardMinigame);
        table.row();
        table.add(quitMinigame).padTop(Value.percentHeight(0.02f, table));

    }

    private void displayMenu() {
        getStage().clear();
        getStage().addActor(table);
    }

    private void startGame(MinigameDifficulty difficulty) {
        Player player = getGameInstance().getEntityManager().getOrCreatePlayer();
        if(player.getBalance() >= difficulty.getCost()) {
            player.deductBalance(difficulty.getCost());
            setCountdowns(difficulty);
            this.difficulty = difficulty;
            getStage().clear();
            createGameUI(difficulty);
        } else {
            //TODO: SET MESSAGE CANT AFFORD
        }
    }

    private void createGameUI(MinigameDifficulty difficulty) {
        gameTable = new Table();
        gameTable.top();
        gameTable.setFillParent(true);
        gameText = new Label("Press SPACE when you're ready! Press Z to shoot!", StyleManager.generateLabelStyle(30, Color.GRAY));

        playerImage = new Image(FileManager.MINIGAME_PLAYER_1);
        enemyImage = new Image(difficulty.getEnemyHolstered());

        gameTable.add(playerImage).expandX().padTop(Value.percentHeight(0.20f, gameTable));
        gameTable.add();
        gameTable.add(enemyImage).expandX().padTop(Value.percentHeight(0.20f, gameTable));
        gameTable.row();
        gameTable.add();
        gameTable.add(gameText).expandX().padTop(Value.percentHeight(0.05f, gameTable));

        getStage().addActor(gameTable);
    }

    private void setCountdowns(MinigameDifficulty minigameDifficulty) {
        Random random = new Random();
        startCountdown = random.nextInt(3)+1;
        enemyShootTimer = startCountdown+minigameDifficulty.getCountdown();
    }
}


@Getter
enum MinigameDifficulty {
    EASY(FileManager.MINIGAME_ENEMY_EASY_1,FileManager.MINIGAME_ENEMY_EASY_2,1, 2,0.5f),
    MEDIUM(FileManager.MINIGAME_ENEMY_MED_1,FileManager.MINIGAME_ENEMY_MED_2,10, 50,0.3f),
    HARD(FileManager.MINIGAME_ENEMY_HARD_1,FileManager.MINIGAME_ENEMY_HARD_2,20, 200,0.26f),
    VERY_HARD(FileManager.MINIGAME_ENEMY_VHARD_1,FileManager.MINIGAME_ENEMY_VHARD_2,50, 500,0.23f);

    private Texture enemyHolstered, enemyShooting;
    private Integer cost, reward;
    private float countdown;

    MinigameDifficulty(Texture enemyHolstered, Texture enemyShooting, Integer cost, Integer reward, float countdown) {
        this.enemyHolstered = enemyHolstered;
        this.enemyShooting = enemyShooting;
        this.cost = cost;
        this.reward = reward;
        this.countdown = countdown;
    }
}