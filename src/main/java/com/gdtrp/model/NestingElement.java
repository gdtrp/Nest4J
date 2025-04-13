package com.gdtrp.model;

import java.util.UUID;

public class NestingElement {
    private UUID partId;
    private byte[] svg;
    private int count;

    public byte[] getSvg() {
        return svg;
    }

    public void setSvg(byte[] svg) {
        this.svg = svg;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public UUID getPartId() {
        return partId;
    }

    public void setPartId(UUID partId) {
        this.partId = partId;
    }
}
