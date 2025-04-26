package com.gdtrp;

import com.gdtrp.model.SingleElementNestingResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

public class TestSingleNesting {
    @Test
    public void testBug() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/bug.svg");
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 18, 2, 5, 1250, 2500);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 18;
    }

    @Test
    public void testNestingRound3() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/test.svg");
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 10, 4, 2, 800, 800);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 150;
    }

    @Test
    public void testNestingRound2() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/round2.svg");
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 100, 4, 10, 10000, 10000);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 150;
    }

    @Test
    public void testNestingRound() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/round.svg");
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 150, 4, 10, 1000, 1000);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 150;
    }

    @Test
    public void testNestingStrange() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/strange.svg");
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 11, 4, 2, 1000, 1000);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 7;
    }

    @Test
    public void testNestingSquare() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/square.svg");
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 60, 4, 2, 450, 450);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 17;
    }
}
