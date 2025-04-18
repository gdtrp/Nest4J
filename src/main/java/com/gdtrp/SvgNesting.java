package com.gdtrp;

import com.gdtrp.model.NestingElement;
import com.gdtrp.model.NestingElementResponse;
import com.gdtrp.model.NestingResponse;
import com.gdtrp.model.SingleElementNestingResponse;
import com.gdtrp.util.SvgUtil;
import com.qunhe.util.nest.Nest;
import com.qunhe.util.nest.data.NestPath;
import com.qunhe.util.nest.data.Placement;
import com.qunhe.util.nest.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SvgNesting {
    private final static Logger logger = LoggerFactory.getLogger(SvgNesting.class);

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


    public static SingleElementNestingResponse singleSheet(byte[] data, int count, int rotations, double spacing, double binWidth, double binHeight) {
        Config config = new Config();
        config.SPACING = spacing;


        NestPath bin = new NestPath();
        bin.add(0, 0);
        bin.add(binWidth, 0);
        bin.add(binWidth, binHeight);
        bin.add(0, binHeight);

        Map<Integer, UUID> mapping = new HashMap<>();
        //31
        //300 300
        //1000 1000
        int idx = 1;
        List<List<Placement>> output = List.of();
        List<byte[]> elements = List.of();
        long start = System.currentTimeMillis();
        long round = System.currentTimeMillis();
        UUID id = UUID.randomUUID();
        int populations = 1;
        int loops = 1;
        int attempts = 0;
        while (true) {
            config.POPULATION_SIZE = populations;
            AtomicInteger ai = new AtomicInteger(0);

            elements = IntStream.range(0, idx).mapToObj(y -> data).toList();
            List<NestPath> list = IntStream.range(0, idx).mapToObj(y -> {
                List<NestPath> item;
                try {
                    item = SvgToNestPathWithTransform2.convertSvgToNestPaths(new ByteArrayInputStream(data));
                    item.forEach(z -> {
                        z.bid = ai.getAndIncrement();
                        z.setRotation(rotations);
                        mapping.put(z.bid, id);
                    });


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return item;
            }).flatMap(Collection::stream).map(x -> {
                NestPath r = RamerDouglasPeucker.simplify(x, 1);
                r.setRotation(x.getRotation());
                r.setSource(x.getSource());
                return r;
            }).toList();
            long startNesting = System.currentTimeMillis();
            Nest nest = new Nest(bin, list, config, loops);

            List<List<Placement>> tempOutput = nest.startNest();
            logger.info("nested in {} ms. populations {}. loops {}", System.currentTimeMillis() - startNesting, populations, loops);
            if (tempOutput.isEmpty()) {
                break;
            }
            tempOutput = tempOutput.stream().map(x -> new ArrayList<>(new HashSet<>(x))).map(x -> (List<Placement>) x).toList();

            long roundExecuted = System.currentTimeMillis() - round;
            boolean improved = false;

            if (output.isEmpty() || output.get(0).size() < tempOutput.get(0).size()) {
                improved = true;
                output = tempOutput;
            }
            if (output.get(0).size() == count) {
                logger.info("placed maximum possible count on a list {}", count);
                break;
            }

            if (System.currentTimeMillis() - start > 180 * 1000) {
                logger.info("timeout exceeded:{}. final result {}", System.currentTimeMillis() - start, output.get(0).size());
                break;
            }

            if (improved && idx == output.get(0).size()) {
                loops = 1;
                populations = 1;
                int oldIdx = idx;
                idx = Math.min(roundExecuted > 15 * 1000 ? idx + 10 : idx * 2, count);
                logger.info("reached max possible value {}. increased to {}", oldIdx, idx);
            } else {
                loops = Math.min(loops + 10, 100);
                populations = Math.min(populations + 10, 50);
                if (loops == 100) {
                    attempts++;
                }
                if (attempts > 50) {
                    logger.info("too many attempts. final result {}", output.get(0).size());
                    break;
                }
                logger.info("trying same {}. placed {}", idx, output.get(0).size());
            }
            round = System.currentTimeMillis();
        }
        if (output.isEmpty()) {
            return new SingleElementNestingResponse();
        }
        //TODO: proper dedup
        output = output.stream().map(x -> new ArrayList<>(new HashSet<>(x))).map(x -> (List<Placement>) x).toList();
        int maxSize = output.get(0).size();
        int sheets = (count / maxSize) + count % maxSize > 0 ? 1 : 0;
        int remainder = count - sheets * maxSize;
        logger.info("sheets: {}  max size: {} remainder: {}. time: {}ms", sheets, maxSize, remainder, System.currentTimeMillis() - start);


        List<String> resultSVG = SvgUtil.svgGenerator(elements.stream().flatMap(x -> IntStream.range(0, count).mapToObj(i -> new String(data))).toList(), output, binWidth, binHeight);
        SingleElementNestingResponse response = new SingleElementNestingResponse();
        response.setFirstPageParts(maxSize);
        response.setPage(resultSVG.get(0).getBytes());
        response.setLastPageParts(remainder);
        response.setTotalSheets(sheets);
        response.setLastPage(renderLastPage(data, remainder, rotations, spacing, binWidth, binHeight, 0));
        return response;
    }

    private static byte[] renderLastPage(byte[] data, int count, int rotations, double spacing, double binWidth, double binHeight, int attempt) {
        Config config = new Config();
        config.SPACING = spacing;
        config.POPULATION_SIZE = 50;


        NestPath bin = new NestPath();
        bin.add(0, 0);
        bin.add(binWidth, 0);
        bin.add(binWidth, binHeight);
        bin.add(0, binHeight);


        List<byte[]> elements = IntStream.range(0, count).mapToObj(y -> data).toList();
        AtomicInteger ai = new AtomicInteger(0);
        List<NestPath> list = IntStream.range(0, count).mapToObj(y -> {
            List<NestPath> item;
            try {
                item = SvgToNestPathWithTransform2.convertSvgToNestPaths(new ByteArrayInputStream(data));
                item.forEach(z -> {
                    z.bid = ai.getAndIncrement();
                    z.setRotation(rotations);
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return item;
        }).flatMap(Collection::stream).toList();
        Nest nest = new Nest(bin, list, config, 50);
        List<List<Placement>> output = nest.startNest();
        if (output.isEmpty()) {
            return null;
        }
        output = output.stream().map(x -> new ArrayList<>(new HashSet<>(x))).map(x -> (List<Placement>) x).toList();
        if (output.get(0).size() < count) {
            if (attempt >= 3) {
                return null;
            }
            return renderLastPage(data, count, rotations, spacing, binWidth, binHeight, attempt++);
        }
        List<String> resultSVG = SvgUtil.svgGenerator(elements.stream().flatMap(x -> IntStream.range(0, count).mapToObj(i -> new String(data))).toList(), output, binWidth, binHeight);
        return resultSVG.get(0).getBytes();
    }

}
