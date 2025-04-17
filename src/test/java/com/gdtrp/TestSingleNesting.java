package com.gdtrp;

import com.gdtrp.model.NestingElement;
import com.gdtrp.model.SingleElementNestingResponse;
import com.gdtrp.util.SvgStringNormalizer;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.UUID;

public class TestSingleNesting {


    @Test
    public void testNestingStrange() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/strange.svg");
        NestingElement element = new NestingElement();
        element.setCount(30);
        element.setPartId(UUID.randomUUID());
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 11, 4, 10, 1000, 1000);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 8;
    }

    @Test
    public void testNestingSquare() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/square.svg");
        NestingElement element = new NestingElement();
        element.setSvg(SvgStringNormalizer.normalizeSvgWithRewrittenCoordinates(IOUtils.toString(is)).getBytes());
        element.setCount(30);
        element.setPartId(UUID.randomUUID());
        SingleElementNestingResponse result = SvgNesting.singleSheet(IOUtils.toString(is).getBytes(), 31, 4, 10, 300, 300);
        System.out.println("max total:" + result.getFirstPageParts());
        System.out.println("sheets total:" + result.getTotalSheets());
        System.out.println(new String(result.getPage()));
        assert result.getFirstPageParts() == 17;
    }
}
