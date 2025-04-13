package com.gdtrp.util;

import com.qunhe.util.nest.data.Placement;

import java.util.ArrayList;
import java.util.List;

public class SvgUtil {

    /**
     * Embeds multiple SVG snippets (provided as strings) into separate "bins."
     * For each bin, we draw an outline rectangle, then place each snippet at
     * the requested rotation/translation. Strips XML/DOCTYPE/root <svg> tags first.
     *
     * @param svgList    list of entire SVG strings you want to embed
     * @param applied    list of bins, each bin is a list of Placement (which snippet + transform)
     * @param binWidth   width for each bin's outline rectangle
     * @param binHeight  height for each bin's outline rectangle
     * @return a list of SVG <g> elements (as strings), each containing an outline rect + embedded snippets
     */public static List<String> svgGenerator(List<String> svgList,
                                               List<List<Placement>> applied,
                                               double binWidth,
                                               double binHeight)  {
        List<String> strings = new ArrayList<>();

        int binX = 0;  // initial x offset for each bin
        int binY = 0;   // initial y offset for each bin

        // For each "bin" in 'applied'
        for (List<Placement> binList : applied) {
            // 1) Position the bin itself at (binX, binY)
            String s ="<svg width=\" "+ binWidth + "\" height=\"" + binHeight + "\" xmlns=\"http://www.w3.org/2000/svg\">";
            s += "<g transform=\"translate(" + binX + " " + binY + ")\">\n";

            // 2) Draw a rectangle for the bin boundary
            s += "  <rect x=\"0\" y=\"0\" width=\"" + binWidth + "\" height=\"" + binHeight +
                    "\" fill=\"none\" stroke=\"#010101\" stroke-width=\"1\" />\n";

            // 3) Embed each snippet in this bin
            for (Placement placement : binList) {
                int bid = placement.bid;
                if (bid < 0 || bid >= svgList.size()) {
                    // If 'bid' is invalid, skip
                    continue;
                }

                // Clean the snippet (remove XML, DOCTYPE, <svg>, <title> tags, etc.)
                String embeddedSvg = cleanSvgSnippet(svgList.get(bid));

                // The local offset/rotation for this shape within the bin
                double ox = placement.translate.x;
                double oy = placement.translate.y;
                double rotate = placement.rotate;

                // 4) Use only (ox, oy) and rotate inside the bin
                //    (No need to add binX, binY again!)
                s += "  <g transform=\"translate(" + ox + " " + oy + ") rotate(" + rotate + ")\">\n";
                s += "    " + embeddedSvg + "\n";
                s += "  </g>\n";
            }

            s += "</g>\n";

            // Move binY down for the next bin
            //binY += binHeight + 50;
            s += "</svg>\n";
            strings.add(s);
        }

        return strings;
    }

    /**
     * Removes XML declaration, DOCTYPE, and the root <svg> tag (plus </svg>) from
     * the given snippet. If you don't want to remove something (e.g. viewBox),
     * adjust or remove patterns as needed.
     */
    private static String cleanSvgSnippet(String svgSnippet) {
        // Regex patterns to remove:
        //   1) <?xml ... ?>
        //   2) <!DOCTYPE ...>
        //   3) <svg ...>  (opening tag)
        //   4) </svg>     (closing tag)
        // These are case-insensitive to cover various capitalization,
        // and dotall or multiline to match across lines if needed.

        // Remove <?xml ... ?>
        svgSnippet = svgSnippet.replaceAll("(?is)<\\?xml.*?\\?>", "");
        svgSnippet = svgSnippet.replaceAll("(?is)<title[^>]*>.*?</title>", "");

        // Remove <!DOCTYPE ...>
        svgSnippet = svgSnippet.replaceAll("(?is)<!DOCTYPE.*?>", "");

        // Remove the opening <svg ...>
        svgSnippet = svgSnippet.replaceAll("(?is)<svg[^>]*>", "");

        // Remove the closing </svg>
        svgSnippet = svgSnippet.replaceAll("(?is)</svg>", "");

        // Trim extra whitespace or newlines
        svgSnippet = svgSnippet.trim();

        return svgSnippet;
    }
}
