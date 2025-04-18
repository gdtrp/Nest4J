package com.gdtrp;

import com.qunhe.util.nest.data.NestPath;
import com.qunhe.util.nest.data.Segment;

import java.util.ArrayList;
import java.util.List;

public class RamerDouglasPeucker {

    public static NestPath simplify(NestPath path, double tolerance) {
        if (path.size() <= 2) return path;

        List<Segment> points = path.getSegments();
        List<Segment> simplifiedPoints = ramerDouglasPeucker(points, tolerance);

        NestPath simplifiedPath = new NestPath();
        simplifiedPoints.forEach(simplifiedPath::add);
        return simplifiedPath;
    }

    private static List<Segment> ramerDouglasPeucker(List<Segment> points, double tolerance) {
        if (points.size() < 3) return points;

        // Find the point with the maximum distance
        double maxDistance = 0;
        int index = 0;
        Segment start = points.get(0);
        Segment end = points.get(points.size() - 1);

        for (int i = 1; i < points.size() - 1; i++) {
            double d = perpendicularDistance(points.get(i), start, end);
            if (d > maxDistance) {
                maxDistance = d;
                index = i;
            }
        }

        // Recursively simplify
        List<Segment> result = new ArrayList<>();
        if (maxDistance > tolerance) {
            List<Segment> left = ramerDouglasPeucker(points.subList(0, index + 1), tolerance);
            List<Segment> right = ramerDouglasPeucker(points.subList(index, points.size()), tolerance);
            result.addAll(left.subList(0, left.size() - 1)); // Avoid duplicates
            result.addAll(right);
        } else {
            result.add(start);
            result.add(end);
        }

        return result;
    }

    private static double perpendicularDistance(Segment p, Segment lineStart, Segment lineEnd) {
        double dx = lineEnd.getX() - lineStart.getX();
        double dy = lineEnd.getY() - lineStart.getY();
        double mag = Math.hypot(dx, dy);
        if (mag == 0) return Math.hypot(p.getX() - lineStart.getX(), p.getY() - lineStart.getY());
        return Math.abs(dx * (lineStart.getY() - p.getY()) - dy * (lineStart.getX() - p.getX())) / mag;
    }
}