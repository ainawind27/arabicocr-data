package model;

/**
 *
 * @author Bening Ranum
 */
public class Pixel {
    private int parent;
    private int child;

    public Pixel() {
    }

    public Pixel(int parent, int child) {
        this.parent = parent;
        this.child = child;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public int getChild() {
        return child;
    }

    public void setChild(int child) {
        this.child = child;
    }
}
