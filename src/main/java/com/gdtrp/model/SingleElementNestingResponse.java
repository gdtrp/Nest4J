package com.gdtrp.model;

public class SingleElementNestingResponse {
    private byte[] page;
    private int firstPageParts;
    private byte[] lastPage;
    private int lastPageParts;
    private int totalSheets;

    public byte[] getPage() {
        return page;
    }

    public void setPage(byte[] page) {
        this.page = page;
    }

    public int getFirstPageParts() {
        return firstPageParts;
    }

    public void setFirstPageParts(int firstPageParts) {
        this.firstPageParts = firstPageParts;
    }

    public byte[] getLastPage() {
        return lastPage;
    }

    public void setLastPage(byte[] lastPage) {
        this.lastPage = lastPage;
    }

    public int getLastPageParts() {
        return lastPageParts;
    }

    public void setLastPageParts(int lastPageParts) {
        this.lastPageParts = lastPageParts;
    }

    public int getTotalSheets() {
        return totalSheets;
    }

    public void setTotalSheets(int totalSheets) {
        this.totalSheets = totalSheets;
    }
}
