package lpool.gdx;

import java.util.Observable;
import java.util.Observer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.utils.Array;

import lpool.gdx.assets.Sounds;
import lpool.logic.Ball;
import lpool.logic.BodyInfo;
import lpool.logic.Match;
import lpool.logic.Table;

public class MatchScene implements Screen, Observer{
	private OrthographicCamera camera;

	private ModelBatch modelBatch = new ModelBatch();
	private Environment environment;

	private ShapeRenderer shapeRenderer;
	private SpriteBatch batch;
	private Texture table;
	private lpool.gdx.BallModel[] ballModels;
	private Array<ModelInstance> modelInstances;

	private Sprite qr_sprite;

	private lpool.logic.Game game;

	public MatchScene(lpool.logic.Game game, int width, int height, String qr_dir)
	{
		camera = new OrthographicCamera(Table.width, Table.width * height / width);
		camera.position.set(new Vector2(Table.width / 2, Table.height / 2), 0);
		camera.update();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
		environment.add(new DirectionalLight().set(0.9f, 0.9f, 0.9f, -0.4f, -0.6f, -1f));

		ballModels = new lpool.gdx.BallModel[16];
		for (int i = 0; i < ballModels.length; i++)
		{
			ballModels[i] = new BallModel(i);
		}

		modelInstances = new Array<ModelInstance>();

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();

		table = new Texture("table.png");
		this.game = game;
		game.getMatch().addColisionObserver(this);

		qr_sprite = null;
		if(qr_dir != "") {
			Texture tex = new Texture(qr_dir);
			qr_sprite = new Sprite(tex);
		}
	}

	@Override
	public void render(float delta)
	{
		game.tick(delta);

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		lpool.logic.Match m = game.getMatch();

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(table, 0, 0, Table.width, Table.height);
		batch.end();

		modelBatch.begin(camera);
		modelInstances.clear();
		Ball[] balls = m.getBalls();
		for (int i = 0; i < balls.length; i++)
		{
			if (balls[i].isOnTable())
				modelInstances.add(ballModels[balls[i].getNumber()].instanciateModel(balls[i].getPosition(), balls[i].getRotation()));
		}
		modelBatch.render(modelInstances, environment);
		modelBatch.end();

		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Filled);
		shapeRenderer.setColor(Color.WHITE);

		float cueAngle = m.getCueAngle();

		Vector2[] prediction = m.predictShot();
		if (prediction[4] != null)
			shapeRenderer.rectLine(m.getCueBall().getPosition(), prediction[4], 0.005f); // Aiming line
		if (prediction[0] != null && prediction[2] != null)
			shapeRenderer.rectLine(prediction[0], prediction[0].cpy().add(prediction[2].cpy().scl(0.075f)), 0.0025f); // Cue ball
		if (prediction[1] != null && prediction[3] != null)
			shapeRenderer.rectLine(prediction[1], prediction[1].cpy().add(prediction[3].cpy().scl(0.15f)), 0.0025f); // 2nd ball

		shapeRenderer.end();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resize(int width, int height) {
		camera.viewportWidth = Table.width;
		camera.viewportHeight = Table.width * height / width;
		camera.update();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void show() {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Observable o, Object obj) {
		Contact contact = (Contact)obj;

		BodyInfo userDataA = ((BodyInfo)contact.getFixtureA().getUserData());
		BodyInfo userDataB = ((BodyInfo)contact.getFixtureB().getUserData());

		if (userDataA == null || userDataB == null)
			return;

		switch (userDataA.getType())
		{
		case BALL:
			if (userDataB.getType() == BodyInfo.Type.BALL)
				ballBallCollisionHandler(userDataA.getID(), userDataB.getID());
			break;
		case TABLE:
			break;
		case HOLE:
			break;
		case BALL_SENSOR:
			break;
		default:
			break;
		}
	}

	private void ballBallCollisionHandler(int ballNumber1, int ballNumber2)
	{
		Ball[] balls = game.getMatch().getBalls();
		Vector2 impactVelocity = balls[ballNumber1].getVelocity().sub(balls[ballNumber2].getVelocity());
		Sounds.getInstance().getBallBallCollision().play(impactVelocity.len2());
	}
}