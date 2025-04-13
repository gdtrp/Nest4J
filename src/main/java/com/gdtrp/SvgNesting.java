package com.gdtrp;

import com.gdtrp.model.NestingElement;
import com.gdtrp.model.NestingElementResponse;
import com.gdtrp.model.NestingResponse;
import com.gdtrp.util.SvgUtil;
import com.qunhe.util.nest.Nest;
import com.qunhe.util.nest.data.NestPath;
import com.qunhe.util.nest.data.Placement;
import com.qunhe.util.nest.util.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SvgNesting {
    public static List<NestingResponse> nestSvg(List<NestingElement> elements, double binWidth, double binHeight){

        Config config = new Config();
        config.SPACING = 2;
        config.POPULATION_SIZE = 5;
        NestPath bin = new NestPath();
        bin.add(0, 0);
        bin.add(binWidth, 0);
        bin.add(binWidth, binHeight);
        bin.add(0, binHeight);

        AtomicInteger ai = new AtomicInteger(0);
        Map<Integer, UUID> mapping = new HashMap<>();
        List<NestPath> list = elements.stream().flatMap(x -> IntStream.range(0, x.getCount()).mapToObj(y -> {
            List<NestPath> item;
            try {
                item = SvgToNestPathWithTransform.convertSvgToNestPaths(new ByteArrayInputStream(x.getSvg()));
                item.forEach(z -> {
                    z.bid = ai.getAndIncrement();
                    mapping.put(z.bid, x.getPartId());
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return item;
        }).flatMap(Collection::stream)).toList();
        Nest nest = new Nest(bin, list, config, 2);
        List<List<Placement>> output = nest.startNest();

        //TODO: proper dedup
        output = output.stream().map(x -> new ArrayList<>(new HashSet<>(x))).map(x -> (List<Placement>) x).toList();
        List<String> resultSVG = SvgUtil.svgGenerator(elements.stream().flatMap(x -> {
           return IntStream.range(0, x.getCount()).mapToObj(i -> new String(x.getSvg()));
        }).toList(), output, binWidth, binHeight);

        AtomicInteger idx = new AtomicInteger(0);
        return output.stream().map(x -> {
            List<NestingElementResponse> r = new ArrayList<>();
            x.stream().map(y -> mapping.get(y.bid)).collect(Collectors.groupingBy(y -> y, Collectors.counting())).forEach((k, v) -> {
                NestingElementResponse item = new NestingElementResponse();
                item.setCount(v.intValue());
                item.setPartId(k);
                r.add(item);
            });
            return r;
        }).map(x -> {
            NestingResponse resp = new NestingResponse();
            resp.setPlacement(resultSVG.get(idx.getAndIncrement()).getBytes());
            resp.setItems(x);
            return resp;
        }).toList();


    }
}
