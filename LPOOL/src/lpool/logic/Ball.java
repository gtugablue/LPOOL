package lpool.logic;

import java.util.Random;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

public class Ball {
	static public final float radius = 0.028575f * 1.3f;
	static public final float mass = 0.163f;
	private int number;
	
	private Body body;
	
	public Ball(World world, Vec2 position, int number) {
		BodyDef bd = new BodyDef();
		bd.position.set(position);
		bd.type = BodyType.DYNAMIC;
		
		CircleShape cs = new CircleShape();
		cs.m_radius = radius;
		
		FixtureDef fd = new FixtureDef();
		fd.shape = cs;
		fd.density = (float) (mass / (Math.PI * Math.pow(radius, 2)));
		fd.friction = 1.0f;
		fd.restitution = 0.7f;
		
		float force = 0.87441024f;
		
		body = world.createBody(bd);
		body.createFixture(fd);
		Random r = new Random();
		body.applyLinearImpulse(new Vec2(r.nextFloat() * force - force / 2, r.nextFloat() * force - force / 2), new Vec2(0, 0));
		body.setLinearDamping(0.5f);
		body.setAngularDamping(1.0f);
		body.setBullet(true);
		
		this.number = number;
	}

	public int getNumber() {
		return number;
	}
	
	public Vec2 getPosition()
	{
		return body.getPosition();
	}
	
	public void tick()
	{
		if (body.getLinearVelocity().lengthSquared() < 0.002)
		{
			body.setLinearVelocity(new Vec2(0, 0));
		}
	}
}
