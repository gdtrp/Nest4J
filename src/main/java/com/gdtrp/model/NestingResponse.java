package com.gdtrp.model;

import java.util.List;

public class NestingResponse {
    private byte[] placement;
    private List<NestingElementResponse> items;

    public List<NestingElementResponse> getItems() {
        return items;
    }

    public void setItems(List<NestingElementResponse> items) {
        this.items = items;
    }

    public byte[] getPlacement() {
        return placement;
    }

    public void setPlacement(byte[] placement) {
        this.placement = placement;
    }
}
