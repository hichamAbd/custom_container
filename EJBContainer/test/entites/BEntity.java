package entites;

import org.isima.ejb.annotations.Entity;
import org.isima.ejb.annotations.Id;

@Entity
public class BEntity {
	@Id
	private int id;
	
	private int x;
	private double y;
	private String z;

	public BEntity(int x,double y,String z){
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public String getZ() {
		return z;
	}

	public void setZ(String z) {
		this.z = z;
	}
}
