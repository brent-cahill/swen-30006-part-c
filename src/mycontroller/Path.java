package mycontroller;

import utilities.Coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Path {
    public List<Coordinate> path;
    public int damage;
    //public Set<Integer> keys;

    Path(){ path = new ArrayList<>(); damage = 0; }

    public void add(Coordinate coordinate, int damage){
        path.add(coordinate);
        this.damage += damage;
    }

    public void reverse(){
        Collections.reverse(path);
    }

    public Path clone() {
        Path c = new Path();
        c.path = this.path;
        c.damage = this.damage;
        return c;
    }
}
