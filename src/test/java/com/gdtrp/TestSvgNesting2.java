package com.gdtrp;

import com.gdtrp.model.NestingElement;
import com.gdtrp.model.NestingResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestSvgNesting2 {
    @Test
    public void testNesting() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/svg_data.svg");
        List<NestingElement> elements = new ArrayList<>();
        NestingElement element = new NestingElement();
        element.setSvg(IOUtils.toByteArray(is));
        element.setCount(5);
        element.setPartId(UUID.randomUUID());
        elements.add(element);
        List<NestingResponse> result = SvgNesting.nestSvg(elements, 300, 300);
        System.out.println(new String(result.get(0).getPlacement()));
    }

    @Test
    public void testNestingSquare() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/square.svg");
        List<NestingElement> elements = new ArrayList<>();
        NestingElement element = new NestingElement();
        element.setSvg(IOUtils.toByteArray(is));
        element.setCount(30);
        element.setPartId(UUID.randomUUID());
        elements.add(element);
        List<NestingResponse> result = SvgNesting.nestSvg(elements, 10, 300, 300);
        System.out.println(new String(result.get(0).getPlacement()));
    }
}
