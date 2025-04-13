package com.gdtrp;

import com.gdtrp.util.SvgUtil;
import com.qunhe.util.nest.Nest;
import com.qunhe.util.nest.data.NestPath;
import com.qunhe.util.nest.data.Placement;
import com.qunhe.util.nest.data.Segment;
import com.qunhe.util.nest.util.Config;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

public class TestSvgNesting {
    @Test
    public void testSvgNesting() throws Exception {
        Config config = new Config();
        config.SPACING = 2;
        config.POPULATION_SIZE = 5;
        NestPath bin = new NestPath();
        double binWidth = 100;
        double binHeight = 100;
        bin.add(0, 0);
        bin.add(binWidth, 0);
        bin.add(binWidth, binHeight);
        bin.add(0, binHeight);
        List<String> svgs = new ArrayList<>();
        List<NestPath> list = IntStream.range(0, 10).mapToObj(x -> {
            List<NestPath> item = null;
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("svg/svg2.svg");
                InputStream is1 = getClass().getClassLoader().getResourceAsStream("svg/svg2.svg");

                svgs.add(IOUtils.toString(is1));
                item = SvgToNestPathWithTransform.convertSvgToNestPaths(is);
                item.forEach(y -> {
                    y.bid = x;
                });


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return item;
        }).flatMap(Collection::stream).toList();
        //assertEquals(list.size(), 10);
        Nest nest = new Nest(bin, list, config, 2);
        // nest.addParts(parts);
        List<List<Placement>> output = nest.startNest();
        output = output.stream().map(x -> {
            return new ArrayList<>(new HashSet<>(x));
        }).map(x -> (List<Placement>) x).toList();
        List<String> resultSVG = SvgUtil.svgGenerator(svgs, output, binWidth, binHeight);
        System.out.println(resultSVG.size());
        for (String s : resultSVG) {
            System.out.println("------------------");
            System.out.println(s);
        }
    }
}
