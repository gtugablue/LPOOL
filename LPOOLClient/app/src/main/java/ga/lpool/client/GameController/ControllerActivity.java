package ga.lpool.client.GameController;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import ga.lpool.client.Network.Connector;
import ga.lpool.client.Network.Receiver;
import ga.lpool.client.Network.Utilities;
import ga.lpool.client.R;

/**
 * Created by André on 03/06/2015.
 */
public class ControllerActivity extends Activity implements Receiver{

    private GameState currentState;
    private GameState states[];

    private LinearLayout end_layout;

    private Connector connector;

    private int current_ball_type = 0;
    private boolean terminating = false;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        runOnUiThread(new Runnable() {
            public void run() {
                setContentView(R.layout.activity_game);
            }
        });
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        states = new GameState[3];

        states[GameState.Value.WAIT.ordinal()] = new WaitState(this);
        states[GameState.Value.SHOOT.ordinal()] = new ShootState(this);
        states[GameState.Value.PLACE_BALL.ordinal()] = new PlaceBallState(this);

        currentState = states[GameState.Value.WAIT.ordinal()];
        currentState.start();

        Bundle b = getIntent().getExtras();
        int server_port = b.getInt("port");
        String server_ip = b.getString("ip");
        String username = b.getString("name");
        if(username == null) {
            username = "";
        }

        if(!Utilities.isValidPort(server_port) || server_ip == null || !Utilities.isValidIP(server_ip))
        {
            Toast.makeText(this, "Invalid server parameters", Toast.LENGTH_SHORT).show();
            finish();
        }

        String connect_message = "" + Connector.ProtocolCmd.JOIN.ordinal() + " " + username + '\n';
        connector = new Connector(server_ip, server_port, connect_message);
        connector.addReceiver(this);

        end_layout = (LinearLayout) findViewById(R.id.final_layout);
    }

    public void getMessage(String message) {
        System.out.println("Got message " + message);
        GameCommand cmd = new GameCommand(message);
        if((cmd.getCmd() == null) || terminating) return;

        System.out.println("Received command " + cmd.getCmd());

        if(!currentState.isSameAsCmd(cmd.getCmd())) {
            currentState.interrupt();
            switch (cmd.getCmd()) {
                case WAIT:
                    currentState = states[GameState.Value.WAIT.ordinal()];
                    break;
                case PLAY:
                    currentState = states[GameState.Value.SHOOT.ordinal()];
                    if(cmd.getArgs().size() == 1)
                        current_ball_type = (int) cmd.getArgs().get(0);
                    else
                        current_ball_type = -1;
                    break;
                case BIH:
                    currentState = states[GameState.Value.PLACE_BALL.ordinal()];
                    break;
                case END:
                case KICK:
                    terminating = true;
                    game_ended(cmd);
                    return;
                default:
                    currentState = states[GameState.Value.WAIT.ordinal()];
                    break;
            }
            currentState.start();
        }

    }

    private void set_ball_type(int type) {
        current_ball_type = type;
    }

    public int ball_type() {
        return current_ball_type;
    }

    private void game_ended(GameCommand cmd) {
        stop();
        end_layout = (LinearLayout) findViewById(R.id.final_layout);
        final ImageView img = (ImageView) findViewById(R.id.picture_view);
        final TextView txt = (TextView) findViewById(R.id.end_event_description);
        final Activity act = this;
        end_layout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                act.finish();
                return true;
            }
        });

        if(cmd.getCmd() == Connector.ProtocolCmd.KICK) {
            runOnUiThread(new Runnable() {
                public void run() {
                    img.setImageResource(R.mipmap.terminated);
                    txt.setText(getResources().getString(R.string.kicked));
                }
            });
        } else if(cmd.getCmd() == Connector.ProtocolCmd.END) {
            Boolean win = (Boolean) cmd.getArgs().get(0);
            Connector.EndReason reason = (Connector.EndReason) cmd.getArgs().get(1);

            switch (reason) {
                case BLACK_BALL_SCORED_AS_LAST:
                    if(win) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                img.setImageResource(R.mipmap.winner);
                                txt.setText(getResources().getString(R.string.win_black_last));
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                img.setImageResource(R.mipmap.loser);
                                txt.setText(getResources().getString(R.string.lose_black_last));
                            }
                        });
                    }
                    break;
                case BLACK_BALL_SCORED_ACCIDENTALLY:
                    if(win) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                img.setImageResource(R.mipmap.winner);
                                txt.setText(getResources().getString(R.string.win_black_accident));
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                img.setImageResource(R.mipmap.loser);
                                txt.setText(getResources().getString(R.string.lose_black_accident));
                            }
                        });
                    }
                    break;
                case TIMEOUT:
                    runOnUiThread(new Runnable() {
                        public void run() {
                            img.setImageResource(R.mipmap.terminated);
                            txt.setText(getResources().getString(R.string.disconnected_timeout));
                        }
                    });
                    break;
                case DISCONNECT:
                    runOnUiThread(new Runnable() {
                        public void run() {
                            img.setImageResource(R.mipmap.terminated);
                            txt.setText(getResources().getString(R.string.disconnected_voluntary));
                        }
                    });
                    break;
            }
        }

        runOnUiThread(new Runnable() {
            public void run() {
                end_layout.setVisibility(View.VISIBLE);
            }
        });
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void setState(GameState newState) {
        currentState.interrupt();
        currentState = newState;
        newState.start();
    }

    public GameState.Value currentValue() {
        return currentState.getValue();
    }

    public void switchStateValue(GameState.Value value) {
        if(value != currentState.getValue()) {
            currentState.interrupt();
            currentState = states[value.ordinal()];
            currentState.start();
        }
    }

    public Boolean sendUDPMessage(String message) {
        if(connector != null) {
            return connector.sendUDPMessage(message);
        }
        return false;
    }

    public Boolean sendTCPMessage(String message) {
        if(connector != null) {
            return connector.sendTCPMessage(message);
        }
        return false;
    }

    protected void onPause() {
        super.onPause();
        currentState.onPause();
    }

    protected void onResume() {
        super.onResume();
        currentState.onResume();
    }

    private void stop() {
        System.out.println("Controller stopped");
        terminating = true;
        currentState.interrupt();
        connector.disconnect();
    }

    public void terminate() {
        System.out.println("Controller terminated");
        stop();
        this.finish();
    }

    public void disconnect() {

        System.out.println("Controller disconnected");
        stop();
        end_layout = (LinearLayout) findViewById(R.id.final_layout);
        runOnUiThread(new Runnable() {
            public void run() {
                end_layout.setVisibility(View.VISIBLE);
            }
        });
        final ImageView img = (ImageView) findViewById(R.id.picture_view);
        final TextView txt = (TextView) findViewById(R.id.end_event_description);
        final Activity act = this;
        if(end_layout != null) {
            end_layout.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View arg0, MotionEvent arg1) {
                    act.finish();
                    return true;
                }
            });
        }

        /*runOnUiThread(new Runnable() {
            public void run() {
                if(img != null)
                    img.setImageResource(R.mipmap.terminated);
                if(txt != null)
                    txt.setText(getResources().getString(R.string.disconnected_quit));
            }
        });*/
    }

    public void onBackPressed() {
        System.out.println("Back pressed");

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Leaving game")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        disconnect();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }
}
