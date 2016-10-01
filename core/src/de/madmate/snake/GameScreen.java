package de.madmate.snake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Created by markus on 28.09.16.
 */
public class GameScreen extends ScreenAdapter{

    private static final float MOVE_TIME = 1F;
    private static final int SNAKE_MOVEMENT = 32;
    private static final int GRID_CELL = 32;
    private static final int RIGHT = 0;
    private static final int LEFT = 1;
    private static final int UP = 2;
    private static final int DOWN = 3;
    private float timer = MOVE_TIME;
    private SpriteBatch batch;
    private Texture snakeHead, snakeBody, apple;
    private int snakeX = 0, snakeY = 0;
    private int snakeXBeforeUpdate = 0, snakeYBeforeUpdate = 0;
    private int snakeDirection = UP;
    private boolean appleAvailable = false;
    private int appleX, appleY;
    private Array<BodyPart> bodyParts = new Array<BodyPart>();
    private ShapeRenderer shapeRenderer;
    private boolean directionSet = false;
    private boolean hasHit = false;
    private BitmapFont bitmapFont;
    private GlyphLayout layout = new GlyphLayout();
    private static final String GAME_OVER_TEXT = "GAME OVER";
    private STATE state = STATE.PLAYING;
    private int score = 0;
    private static final int POINTS_PER_APPLE = 15;
    private static final float WORLD_WIDTH = 640;
    private static final float WORLD_HEIGHT = 480;
    private Viewport viewport;
    private Camera camera;


    @Override
    public void show() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(WORLD_WIDTH/2, WORLD_HEIGHT/2, 0);
        camera.update();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        bitmapFont = new BitmapFont();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        snakeHead = new Texture(Gdx.files.internal("snakehead.png"));
        apple = new Texture(Gdx.files.internal("apple.png"));
        snakeBody = new Texture(Gdx.files.internal("snakebody.png"));
    }
    @Override
    public void render(float delta) {
        switch (state) {
            case PLAYING: {
                queryInput();
                updateSnake(delta);
                checkAppleCollision();
                checkAndPlaceApple();
                break;
            }
            case GAME_OVER: {
                checkForRestart();
                break;

            }
        }

        clearScreen();
        drawGrid();
        draw();


    }
    private void updateSnake(float delta) {
        if(!hasHit) {
            timer -= delta;
            if (timer<= 0) {
                timer = MOVE_TIME;
                moveSnake();
                checkForOutOfBounds();
                updateBodyPartsPosition();
                checkSnakeBodyCollision();
                directionSet = false;
            }
        }
    }

    private void addToScore() {
        score += POINTS_PER_APPLE;
    }

    private void checkForRestart() {
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE)) doRestart();
    }

    private void doRestart() {
        state = STATE.PLAYING;
        bodyParts.clear();
        snakeDirection = RIGHT;
        directionSet = false;
        timer = MOVE_TIME;
        snakeX = 0;
        snakeY = 0;
        snakeXBeforeUpdate = 0;
        snakeYBeforeUpdate = 0;
        appleAvailable = false;
        score = 0;
    }

    private void checkSnakeBodyCollision() {
        for(BodyPart bodypart: bodyParts) {
            if(bodypart.x==snakeX && bodypart.y==snakeY) {
                state = STATE.GAME_OVER;
            }
        }
    }
    private void updateIfNotOppositeDirection(int newSnakeDirection, int oppositeDirection) {
        if (snakeDirection != oppositeDirection || bodyParts.size==0)
            snakeDirection = newSnakeDirection;
    }

    private void updateDirection(int newSnakeDirection) {
        if (!directionSet && snakeDirection != newSnakeDirection) {
            directionSet = true;
            switch (newSnakeDirection) {
                case LEFT: {
                    updateIfNotOppositeDirection(newSnakeDirection, RIGHT);
                }
                break;
                case RIGHT: {
                    updateIfNotOppositeDirection(newSnakeDirection, LEFT);
                }
                break;
                case UP: {
                    updateIfNotOppositeDirection(newSnakeDirection, DOWN);
                }
                break;
                case DOWN: {
                    updateIfNotOppositeDirection(newSnakeDirection, UP);
                }
                break;
            }
        }
    }

    //Draws a Grid
    public void drawGrid() {
        shapeRenderer.setProjectionMatrix(camera.projection);
        shapeRenderer.setTransformMatrix(camera.view);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int x=0; x < viewport.getWorldWidth(); x += GRID_CELL) {
            for (int y=0; y< viewport.getWorldHeight(); y+= GRID_CELL) {
                shapeRenderer.rect(x,y, GRID_CELL, GRID_CELL);
                shapeRenderer.setColor(0.5451f, 0.6314f, 0.7686f, 1);
            }
        }

        shapeRenderer.end();
    }

    private void drawScore() {
        if(state == STATE.PLAYING) {
            String scoreAsString = Integer.toString(score);
            layout.setText(bitmapFont, scoreAsString);
            bitmapFont.draw(batch, scoreAsString, (viewport.getWorldWidth() - layout.width) / 2, (4 * viewport.getWorldHeight() / 5) - layout.height / 2);
        }
    }

    private void clearScreen() {
        Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void draw() {
        batch.setProjectionMatrix(camera.projection);
        batch.setTransformMatrix(camera.view);
        batch.begin();
        batch.draw(snakeHead, snakeX, snakeY);
        drawScore();
        for (BodyPart bodyPart:bodyParts) {
            bodyPart.draw(batch);
        }
        if (appleAvailable) {
            batch.draw(apple, appleX, appleY);
        }
        if(state == STATE.GAME_OVER) {
            layout.setText(bitmapFont, GAME_OVER_TEXT);
            bitmapFont.draw(batch, GAME_OVER_TEXT, (viewport.getWorldWidth()-layout.width)/2, (viewport.getWorldHeight()-layout.height)/2);
        }

        batch.end();
    }

    private void checkAppleCollision() {
        if (appleAvailable && appleX == snakeX && appleY == snakeY) {
            BodyPart bodyPart = new BodyPart(snakeBody);
            bodyPart.updateBodyPosition(snakeX, snakeY);
            bodyParts.insert(0, bodyPart);
            appleAvailable = false;
            addToScore();
        }
    }

    private void checkForOutOfBounds() {
        if (snakeX >= viewport.getWorldWidth()) {
            snakeX = 0;
        }
        if (snakeX < 0) {
            snakeX = (int) viewport.getWorldWidth() - SNAKE_MOVEMENT;
        }
        if (snakeY >= viewport.getWorldHeight()) {
            snakeY = 0;
        }
        if (snakeY < 0) {
            snakeY = (int) viewport.getWorldHeight() - SNAKE_MOVEMENT;
        }
    }

    private void moveSnake() {
        snakeXBeforeUpdate = snakeX;
        snakeYBeforeUpdate = snakeY;
        switch (snakeDirection) {
            case RIGHT: {
                snakeX += SNAKE_MOVEMENT;
                return;
            }
            case LEFT: {
                snakeX -= SNAKE_MOVEMENT;
                return;
            }
            case UP: {
                snakeY += SNAKE_MOVEMENT;
                return;
            }
            case DOWN: {
                snakeY -= SNAKE_MOVEMENT;
                return;
            }
        }
    }

    private void queryInput() {
        boolean lPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean rPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean uPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean dPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        if (lPressed) updateDirection(LEFT);
        if (rPressed) updateDirection(RIGHT);
        if (uPressed) updateDirection(UP);
        if (dPressed) updateDirection(DOWN);
    }

    private void checkAndPlaceApple() {
        if (!appleAvailable) {
            do {
                appleX = MathUtils.random( (int) (viewport.getWorldWidth() / SNAKE_MOVEMENT) - 1) * SNAKE_MOVEMENT;
                appleY = MathUtils.random( (int) (viewport.getWorldHeight() / SNAKE_MOVEMENT) - 1) * SNAKE_MOVEMENT;

                appleAvailable = true;
            } while (appleX == snakeX && appleY == snakeY);
        }
    }
    private void updateBodyPartsPosition() {
        if (bodyParts.size > 0) {
            BodyPart bodyPart = bodyParts.removeIndex(0);
            bodyPart.updateBodyPosition(snakeXBeforeUpdate, snakeYBeforeUpdate);
            bodyParts.add(bodyPart);
        }
    }

    private class BodyPart {
        private int x, y;
        private Texture texture;
        public BodyPart(Texture texture) {
            this.texture = texture;
        }
        public void updateBodyPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public void draw(Batch batch) {
            if (!(x == snakeX && y == snakeY)) batch.draw(texture,
                    x, y);
        }
    }

    private enum STATE {
        PLAYING, GAME_OVER
    }
}
