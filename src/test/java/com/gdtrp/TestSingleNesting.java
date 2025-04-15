package com.gdtrp;

import com.gdtrp.model.NestingElement;
import com.gdtrp.model.NestingResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class TestSingleNesting {

    @Test
    public void testNestingSquare() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("svg/square.svg");
        NestingElement element = new NestingElement();
        element.setSvg(IOUtils.toByteArray(is));
        element.setCount(30);
        element.setPartId(UUID.randomUUID());
        List<NestingResponse> result = SvgNesting.singleSheet(element, 31, 4, 10, 300, 300);
        System.out.println("max total:" + result.get(0).getItems().get(0).getCount());
        System.out.println("sheets total:" + result.size());
        System.out.println(new String(result.get(0).getPlacement()));
    }
}
