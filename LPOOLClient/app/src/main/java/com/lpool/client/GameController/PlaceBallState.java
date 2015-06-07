package com.lpool.client.GameController;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.lpool.client.Network.Connector;
import com.lpool.client.R;

/**
 * Created by André on 05/06/2015.
 */
public class PlaceBallState implements GameState {

    private Value value = Value.PLACE_BALL;
    private Boolean active;
    private LinearLayout own_layout;
    private ControllerActivity caller;
    private float ballX = (float)0.5;
    private float ballY = (float)0.5;

    private static final float xProportionLimit = (float)0.051;
    private static final float yProportionLimit = (float)0.091;

    public PlaceBallState(ControllerActivity caller) {
        this.caller = caller;
        own_layout = (LinearLayout) caller.findViewById(R.id.placeBallLayout);
    }

    private void initializeElements() {
        final ImageView cueBallPlace = (ImageView) caller.findViewById(R.id.cueBallPlacable);
        final RelativeLayout placeBall = (RelativeLayout) caller.findViewById(R.id.tableandball);

        placeBall.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                final float x = event.getX();
                final float y = event.getY();
                if(x >= 0 + xProportionLimit*placeBall.getWidth() &&
                        x <= placeBall.getX() + placeBall.getWidth() - cueBallPlace.getWidth() - xProportionLimit*placeBall.getWidth() &&
                        y >= 0 + yProportionLimit*placeBall.getHeight() &&
                        y <= placeBall.getY() + placeBall.getHeight() - cueBallPlace.getHeight() - yProportionLimit*placeBall.getHeight()) {


                    caller.runOnUiThread(new Runnable() {
                        public void run() {
                            cueBallPlace.setX(x);
                            cueBallPlace.setY(y);
                        }
                    });

                    ballX = (x)/(placeBall.getWidth() - cueBallPlace.getWidth());
                    ballY = (y)/(placeBall.getHeight() - cueBallPlace.getHeight());
                    moveCueBall();
                }
                return true;
            }
        });

        final Button placeButton = (Button) caller.findViewById(R.id.placeBallButton);
        placeButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                placeCueBall(v);
            }
        });
    }

    private void moveCueBall() {
        System.out.println("Moving to: " + ballX + " " + ballY);
        caller.sendTCPMessage("" + Connector.ProtocolCmd.MOVECB.ordinal() + " " + ballX + " " + ballY + " " + '\n');
    }

    public void placeCueBall(View v) {
        System.out.println("Placing ball on: " + ballX + " " + ballY);
        caller.sendTCPMessage("" + Connector.ProtocolCmd.PLACECB.ordinal() + " " + ballX + " " + ballY + " " + '\n');
    }

    public void onPause() {}

    public void onResume() {}

    public Boolean isSameAsCmd(Connector.ProtocolCmd cmd) {
        return (cmd == Connector.ProtocolCmd.BIH);
    }

    public Value getValue() {
        return value;
    }

    public Boolean isActive() {
        return active;
    }

    public void interrupt() {
        caller.runOnUiThread(new Runnable() {
            public void run() {
                own_layout.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void start() {
        caller.runOnUiThread(new Runnable() {
            public void run() {
                own_layout.setVisibility(View.VISIBLE);
            }
        });
        ballX = (float)0.5;
        ballY = (float)0.5;
        final ImageView cueBallPlace = (ImageView) caller.findViewById(R.id.cueBallPlacable);
        caller.runOnUiThread(new Runnable() {
            public void run() {
                cueBallPlace.setX(ballX);
                cueBallPlace.setY(ballY);
            }
        });
        initializeElements();
        // TODO reset ball position
    }
}
