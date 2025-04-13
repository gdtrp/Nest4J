package com.qunhe.util.nest.data;

/**
 * @author yisa
 */
public class Placement {
    public int bid;
    public Segment translate;
    public double rotate;


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Placement) {
            Placement placement = (Placement) obj;
            return placement.translate.equals(translate) && placement.rotate == rotate;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) ( translate.hashCode() + rotate);
    }

    public Placement(int bid, Segment translate, double rotate) {
        this.bid = bid;
        this.translate = translate;
        this.rotate = rotate;
    }

    public Placement() {
    }
}
