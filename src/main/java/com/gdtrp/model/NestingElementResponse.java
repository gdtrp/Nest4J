package com.gdtrp.model;

import java.util.UUID;

public class NestingElementResponse {
    private int count;
    private UUID partId;

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
