package com.gdtrp;

import com.qunhe.util.nest.data.NestPath;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.PathParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SvgToNestPathWithTransform2 {
    public static List<NestPath> convertSvgToNestPaths(InputStream svgInputStream) throws IOException {
        List<NestPath> nestPaths = new ArrayList<>();

        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document document = factory.createDocument(null, svgInputStream);

        Element rootElement = document.getDocumentElement();
        AffineTransform viewBoxTransform = parseViewBox(rootElement);

        processSvgNode(rootElement, viewBoxTransform, nestPaths);

        return nestPaths;
    }

    /**
     * Recursively process each SVG node, building up transforms and extracting shapes.
     */
    private static void processSvgNode(Node node, AffineTransform parentTransform, List<NestPath> nestPaths) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }

        Element elem = (Element) node;

        // 1) Build the cumulative transform for this element by parsing its "transform" attribute (if any)
        AffineTransform thisTransform = buildTransform(elem, parentTransform);

        // 2) Check element tag name and handle geometry
        String tagName = elem.getTagName();

        switch (tagName) {
            case "g", "svg", "title":
                // a <g> may contain nested nodes
                break;
            case "path":
                String dAttr = elem.getAttribute("d");
                if (dAttr != null && !dAttr.isEmpty()) {
                    Shape pathShape = parsePathData(dAttr);
                    List<NestPath> paths = shapeToNestPaths(pathShape, thisTransform);
                    nestPaths.addAll(paths);
                }
                break;
            case "rect":
                /*
                  <rect x="..." y="..." width="..." height="..." ... />
                  Convert it to a rectangle shape and transform it.
                 */
                Shape rectShape = parseRect(elem);
                if (rectShape != null) {
                    List<NestPath> rectPath = shapeToNestPaths(rectShape, thisTransform);
                    if (!rectPath.isEmpty()) {
                        nestPaths.addAll(rectPath);
                    }
                }
                break;
            case "circle":
                Shape circleShape = parseCircle(elem);
                if (circleShape != null) {
                    List<NestPath> circlePath = shapeToNestPaths(circleShape, thisTransform);
                    if (!circlePath.isEmpty()) {
                        nestPaths.addAll(circlePath);
                    }
                }
                break;
            case "line":
                Shape lineShape = parseLine(elem);
                if (lineShape != null) {
                    List<NestPath> linePath = shapeToNestPaths(lineShape, thisTransform);
                    if (!linePath.isEmpty()) {
                        nestPaths.addAll(linePath);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported tag: " + tagName);
                // similarly, you can add "case polygon:" or "case circle:" etc.
                // to handle other geometry
        }

        // 3) Recurse through children
        Node child = node.getFirstChild();
        while (child != null) {
            processSvgNode(child, thisTransform, nestPaths);
            child = child.getNextSibling();
        }
    }

    /**
     * Build a new transform by parsing the "transform" attribute (if any)
     * and concatenating it with the given parent transform.
     */
    public static AffineTransform buildTransform(Element elem, AffineTransform parentTransform) {
        // copy the parent transform so we don't mutate it
        AffineTransform newTransform = new AffineTransform();

        // get this node's "transform" attribute
        String transformStr = elem.getAttribute("transform");
        if (transformStr != null && !transformStr.isEmpty()) {
            try {
                // Let Batik parse the transform string
                // Alternatively, you could parse manually or use another approach
                // but Batik can handle typical "translate(...)", "rotate(...)" etc.
                org.apache.batik.parser.TransformListParser tListParser =
                        new org.apache.batik.parser.TransformListParser();
                final List<AffineTransform> transforms = new ArrayList<>();
                tListParser.setTransformListHandler(new org.apache.batik.parser.TransformListHandler() {
                    @Override
                    public void startTransformList() {
                    }

                    @Override
                    public void matrix(float a, float b, float c, float d, float e, float f) {
                        transforms.add(new AffineTransform(a, b, c, d, e, f));
                    }

                    @Override
                    public void rotate(float theta) {
                        transforms.add(AffineTransform.getRotateInstance(
                                Math.toRadians(theta)));
                    }

                    @Override
                    public void rotate(float theta, float cx, float cy) {
                        transforms.add(AffineTransform.getRotateInstance(
                                Math.toRadians(theta), cx, cy));
                    }

                    @Override
                    public void translate(float tx) {
                        transforms.add(AffineTransform.getTranslateInstance(tx, 0));
                    }

                    @Override
                    public void translate(float tx, float ty) {
                        transforms.add(AffineTransform.getTranslateInstance(tx, ty));
                    }

                    @Override
                    public void scale(float sx) {
                        transforms.add(AffineTransform.getScaleInstance(sx, sx));
                    }

                    @Override
                    public void scale(float sx, float sy) {
                        transforms.add(AffineTransform.getScaleInstance(sx, sy));
                    }

                    @Override
                    public void skewX(float skx) {
                        transforms.add(AffineTransform.getShearInstance(Math.tan(Math.toRadians(skx)), 0));
                    }

                    @Override
                    public void skewY(float sky) {
                        transforms.add(AffineTransform.getShearInstance(0, Math.tan(Math.toRadians(sky))));
                    }

                    @Override
                    public void endTransformList() {
                    }
                });
                tListParser.parse(transformStr);

                // Combine all transforms in the order they appear
                for (AffineTransform atf : transforms) {
                    newTransform.concatenate(atf);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // if transform parse fails, we just ignore or handle as needed
            }
        }

        return newTransform;
    }

    /**
     * Parse an SVG <path d="..."> string into a Java AWT Shape using Batik.
     */
    public static Shape parsePathData(String pathData) {
        PathParser pathParser = new PathParser();
        AWTPathProducer producer = new AWTPathProducer();
        producer.setWindingRule(PathIterator.WIND_NON_ZERO);
        pathParser.setPathHandler(producer);
        pathParser.parse(pathData);
        return producer.getShape();
    }

    private static Shape parseCircle(Element circleElem) {
        try {
            double cx = Double.parseDouble(circleElem.getAttribute("cx"));
            double cy = Double.parseDouble(circleElem.getAttribute("cy"));
            double r = Double.parseDouble(circleElem.getAttribute("r"));
            // Ellipse2D for the bounding box (cx-r, cy-r, 2r, 2r)
            return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        } catch (NumberFormatException e) {
            // Possibly missing or invalid attributes
            return null;
        }
    }

    private static Shape parseLine(Element lineElem) {
        try {
            double x1 = Double.parseDouble(lineElem.getAttribute("x1"));
            double y1 = Double.parseDouble(lineElem.getAttribute("y1"));
            double x2 = Double.parseDouble(lineElem.getAttribute("x2"));
            double y2 = Double.parseDouble(lineElem.getAttribute("y2"));
            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            return path;
        } catch (NumberFormatException e) {
            return null; // Skip invalid lines
        }
    }

    /**
     * Parse an SVG <rect> element into an AWT Shape (Rectangle2D).
     */
    private static Shape parseRect(Element rectElem) {
        try {
            double x = Double.parseDouble(rectElem.getAttribute("x"));
            double y = Double.parseDouble(rectElem.getAttribute("y"));
            double w = Double.parseDouble(rectElem.getAttribute("width"));
            double h = Double.parseDouble(rectElem.getAttribute("height"));
            return new Rectangle2D.Double(x, y, w, h);
        } catch (NumberFormatException e) {
            // If attributes are missing or invalid, skip
            return null;
        }
    }

    /**
     * Convert an AWT Shape to a NestPath, applying the specified AffineTransform
     * while flattening the shape into line segments.
     */
    private static List<NestPath> shapeToNestPaths(Shape shape, AffineTransform transform) {
        List<NestPath> nestPaths = new ArrayList<>();
        Area area = new Area(shape);

        // Extract all subpaths from the Area
        PathIterator pathIterator = area.getPathIterator(transform);
        double[] coords = new double[6];
        NestPath currentPath = null;

        while (!pathIterator.isDone()) {
            int segmentType = pathIterator.currentSegment(coords);
            switch (segmentType) {
                case PathIterator.SEG_MOVETO:
                    currentPath = new NestPath();
                    currentPath.add(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    currentPath.add(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    if (currentPath != null && currentPath.size() > 0) {
                        // Determine if this subpath is a hole
                        boolean isHole = !isClockwise(currentPath);
                        if (isHole && !nestPaths.isEmpty()) {
                            // Add as a hole to the last main path
                            nestPaths.get(nestPaths.size() - 1).addChildren(currentPath);
                        } else {
                            nestPaths.add(currentPath);
                        }
                        currentPath = null;
                    }
                    break;
            }
            pathIterator.next();
        }

        return nestPaths;
    }

    // Helper to check winding direction
    private static boolean isClockwise(NestPath path) {
        double area = 0;
        for (int i = 0; i < path.size(); i++) {
            int j = (i + 1) % path.size();
            area += (path.get(j).x - path.get(i).x) * (path.get(j).y + path.get(i).y);
        }
        return area > 0; // Positive area = clockwise
    }

    private static AffineTransform parseViewBox(Element svgElement) {
        String viewBoxStr = svgElement.getAttribute("viewBox");
        if (viewBoxStr.isEmpty()) return new AffineTransform();

        String[] parts = viewBoxStr.split(" ");
        if (parts.length != 4) return new AffineTransform();

        double vbx = Double.parseDouble(parts[0]);
        double vby = Double.parseDouble(parts[1]);
        double vbw = Double.parseDouble(parts[2]);
        double vbh = Double.parseDouble(parts[3]);

        double svgWidth = parseUnit(svgElement.getAttribute("width"));
        double svgHeight = parseUnit(svgElement.getAttribute("height"));

        // Calculate scale and translation for viewBox
        double scaleX = svgWidth / vbw;
        double scaleY = svgHeight / vbh;
        AffineTransform transform = new AffineTransform();
        transform.translate(-vbx * scaleX, -vby * scaleY);
        transform.scale(scaleX, scaleY);

        return transform;
    }

    private static double parseUnit(String value) {
        if (value.endsWith("mm")) {
            return Double.parseDouble(value.substring(0, value.length() - 2));
        }
        return Double.parseDouble(value);
    }

}