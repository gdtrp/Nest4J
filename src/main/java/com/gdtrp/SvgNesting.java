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
    public static List<NestingResponse> nestSvg(List<NestingElement> elements, double binWidth, double binHeight) {
        return nestSvg(elements, 0, 2, binWidth, binHeight);
    }

    public static List<NestingResponse> nestSvg(List<NestingElement> elements, int rotations, double binWidth, double binHeight) {
        return nestSvg(elements, rotations, 2, binWidth, binHeight);
    }

    public static List<NestingResponse> nestSvg(List<NestingElement> elements, int rotations, double spacing, double binWidth, double binHeight) {

        Config config = new Config();
        config.SPACING = spacing;
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
                    z.setRotation(rotations);
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


    public static List<NestingResponse> singleSheet(NestingElement element, int count, int rotations, double spacing, double binWidth, double binHeight) {
        Config config = new Config();
        config.SPACING = spacing;
        config.POPULATION_SIZE = 50;

        NestPath bin = new NestPath();
        bin.add(0, 0);
        bin.add(binWidth, 0);
        bin.add(binWidth, binHeight);
        bin.add(0, binHeight);

        AtomicInteger ai = new AtomicInteger(0);
        Map<Integer, UUID> mapping = new HashMap<>();
        //31
        //300 300
        //1000 1000
        int idx = 1;
        List<List<Placement>> output = List.of();
        List<NestingElement> elements = List.of();
        while (idx <= count) {
            elements = IntStream.range(0, idx).mapToObj(y -> element).toList();
            List<NestPath> list = IntStream.range(0, idx).mapToObj(y -> {
                List<NestPath> item;
                try {
                    item = SvgToNestPathWithTransform.convertSvgToNestPaths(new ByteArrayInputStream(element.getSvg()));
                    item.forEach(z -> {
                        z.bid = ai.getAndIncrement();
                        z.setRotation(rotations);
                        mapping.put(z.bid, element.getPartId());
                    });

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return item;
            }).flatMap(Collection::stream).toList();
            Nest nest = new Nest(bin, list, config, 50);
            output = nest.startNest();
            if (output.isEmpty()) {
                break;
            }
            if (output.size() >= 2) {
                break;
            }
            idx = Math.min(idx * 2, count);
        }


        //TODO: proper dedup
        output = output.stream().map(x -> new ArrayList<>(new HashSet<>(x))).map(x -> (List<Placement>) x).toList();
        List<String> resultSVG = SvgUtil.svgGenerator(elements.stream().flatMap(x -> {
            return IntStream.range(0, x.getCount()).mapToObj(i -> new String(x.getSvg()));
        }).toList(), output, binWidth, binHeight);

        AtomicInteger items = new AtomicInteger(0);
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
            resp.setPlacement(resultSVG.get(items.getAndIncrement()).getBytes());
            resp.setItems(x);
            return resp;
        }).toList();


    }
}
