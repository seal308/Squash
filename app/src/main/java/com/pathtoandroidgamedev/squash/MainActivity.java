package com.pathtoandroidgamedev.squash;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Random;

public class MainActivity extends Activity {

    Canvas canvas;
    SquashCourtView squashCourtView;

    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    // for getting display details like # of pixels
    Display display;
    Point size;
    int screenWidth;
    int screenHeight;

    // game objects
    int racketWidth;
    int racketHeight;
    Point racketPosition;

    Point ballPosition;
    int ballWidth;

    boolean ballIsMovingLeft;
    boolean ballIsMovingRight;
    boolean ballIsMovingUp;
    boolean ballIsMovingDown;

    // for racket movement
    boolean racketIsMovingLeft;
    boolean racketIsMovingRight;

    // stats
    long lastFrameTime;
    int fps;
    int score;
    int lives;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        squashCourtView = new SquashCourtView(this);
        setContentView(squashCourtView);

        int sample1 = -1;
        int sample2 = -1;
        int sample3 = -1;
        int sample4 = -1;


        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try
        {
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundPool.load(descriptor, 0);
        }catch (IOException e)
        {

        }

        //could make this use getters and setters
        // don't want anyone changing screen size
        display = getWindowManager().getDefaultDisplay();
        size = new Point(); // point type has an x and y member variable
        display.getSize(size); // size now holds the width and height of display
        screenWidth = size.x;
        screenHeight = size.y;

        //game objects
        racketPosition = new Point();
        racketPosition.x = screenWidth/2;
        racketPosition.y = screenHeight/2;
        racketWidth = screenWidth/8;
        racketHeight = 10;

        ballWidth = screenWidth/35;
        ballPosition = new Point();
        ballPosition.x = screenWidth / 2;
        ballPosition.y = 1 + ballWidth;

        lives = 3;


    }

    class SquashCourtView extends SurfaceView implements Runnable{

        Thread ourThread = null; //runnable interface provides thread which we overwrite.
        SurfaceHolder ourHolder;
        // volatile means we will be able to changed its values from outside and inside our thread
        volatile boolean playingSquash;
        Paint paint;

        public SquashCourtView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
            ballIsMovingDown = true;

            //send the ball in random direction
            Random randomNumber = new Random();
            int ballDirection = randomNumber.nextInt(3);
            switch(ballDirection){
                case 0:
                    ballIsMovingLeft = true;
                    ballIsMovingRight = false;
                    break;
                case 1:
                    ballIsMovingRight = true;
                    ballIsMovingLeft = false;
                    break;
                case 2: //move straight down
                    ballIsMovingLeft = false;
                    ballIsMovingRight = false;
                    break;
            }

        }

        // run method contains the funcationality of the thread
        // w/e is in run method will act likes its in a thread
        @Override
        public void run() {
            while (playingSquash){
                updateCourt(); // controls movement and collision detection
                drawCourt();
                controlFPS(); // locks game to consistent frame rate
            }
        }

        public void updateCourt()
        {
            if (racketIsMovingRight){
                racketPosition.x = racketPosition.x + 10;
            }
            if (racketIsMovingLeft){
                racketPosition.x = racketPosition.x - 10;
            }

            // detect collisions
            // hit right of screen
            if (ballPosition.x + ballWidth > screenWidth){
                ballIsMovingLeft = true;
                ballIsMovingRight = false;
                soundPool.play(sample1,1,1,0,0,1);
            }

            // hit left of screen
            if (ballPosition.x < 0)
            {
                ballIsMovingLeft = false;
                ballIsMovingRight = true;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            // edge of ball has hit bottom of screen
            if (ballPosition.y > screenHeight - ballWidth)
            {
                lives = lives -1;
                if (lives == 0)
                {
                    lives = 3;
                    score = 0;
                    soundPool.play(sample4, 1, 1, 0, 0, 1);
                }
                ballPosition.y = 1 + ballWidth; // back to top of screen

                Random randomNumber = new Random();
                int startX = randomNumber.nextInt(screenWidth - ballWidth) + 1;
                ballPosition.x = startX + ballWidth;

                int ballDirection = randomNumber.nextInt(3);
                switch (ballDirection){
                    case 0:
                        ballIsMovingLeft = true;
                        ballIsMovingRight = false;
                        break;
                    case 1:
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                        break;
                    case 2:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = false;
                        break;
                }
            }

            //hit the top of the screen
            if (ballPosition.y <= 0)
            {
                ballIsMovingDown = true;
                ballIsMovingUp = false;
                ballPosition.y = 1;
                soundPool.play(sample2, 1, 1, 0, 0, 1);
            }

            // depending on the 2 directions we should be moving in adjust our x and y positions
            if (ballIsMovingDown){
                ballPosition.y += 6;
            }
            if (ballIsMovingUp){
                ballPosition.y -= 10;
            }
            if (ballIsMovingLeft)
            {
                ballPosition.x -= 12;
            }
            if (ballIsMovingRight)
            {
                ballPosition.x += 12;
            }

            // has ball hit racket
            // check if ball reached/gone passed the top side of racket
            if (ballPosition.y + ballWidth >= (racketPosition.y - racketHeight / 2))
            {
                int halfRacket = racketWidth/2;
                // check if the right hand side of the ball is > than far left side of racket and whehter it touches
                // also checks if balls left edge is not past the far right of the racket
                if (ballPosition.x + ballWidth > racketPosition.x - halfRacket
                        && ballPosition.x - ballWidth < (racketPosition.x +halfRacket))
                {
                    // rebound the ball vertically and play a sound
                    soundPool.play(sample3, 1, 1, 0, 0, 1);
                    score++;
                    ballIsMovingUp = true;
                    ballIsMovingDown = false;
                    // now decide how to rebound the ball horizontally
                    // check left hand edge of ball is past centre of racket
                    // if yes move ball to the right
                    if (ballPosition.x > racketPosition.x){
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                    } else {
                        ballIsMovingRight = false;
                        ballIsMovingLeft = true;
                    }
                }
            }
        }
    }

    public void drawCourt()
    {
        if (ourHolder.getSurface().isValid()){
            canvas = ourHolder.lockCanvas();
            // Paint paint = new Paint();
            canvas.drawColor(Color.BLACK);// the background
            paint.setColor(Color.argb(255, 255, 255, 255));
            paint.setTextSize(45);
            canvas.drawText("Score: " + score + " Lives: " + lives + " fps: " + fps, 20, 40, paint);

            // drawk the squash racket
            canvas.drawRect(racketPosition.x - (racketWidth)/2, racketPosition.y - (racketHeight / 2), racketPosition.x + (racketWidth/2)
            , racketPosition.y + racketHeight, paint);

            // draw the ball
            canvas.drawRect(ballPosition.x, ballPosition.y, ballPosition.x+ballWidth, ballPosition.y+ballWidth, paint);

            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    // locks frame rate or something smooth and consistent
    public void controlFPS()
    {
        long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
        long timeToSleep = 15 - timeThisFrame;
        if (timeThisFrame > 0)
        {
            fps = (int) (1000/timeThisFrame);
        }
        if (timeToSleep > 0)
        {
            try{
                ourThread.sleep(timeToSleep);
            } catch (InterruptedIOException e)
            {

            }
        }
        lastFrameTime = System.currentTimeMillis();
    }
    // ensure thread is ended or started safely when the player has finished or resumed our game
    public void pause()
    {
        playingSquash = false;
        try{
            ourThread.join();
        } catch (InterruptedIOException e){

        }
    }

    public void resume(){
        playingSquash = true;
        ourThread = new Thread(this);
        ourThread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        System.out.println("Testing Github Commmit");
        System.out.println("Testing push on github");
    }


}
