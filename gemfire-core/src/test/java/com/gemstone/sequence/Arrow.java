/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.sequence;

import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * Created by IntelliJ IDEA.
 * User: dsmith
 * Date: Nov 12, 2010
 * Time: 12:02:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class Arrow {
    private static int ARROW_WIDTH=10;
    private static int CIRCLE_WIDTH=6;
    private static int LABEL_OFFSET=10;

    private final String label;
    private final LifelineState startingState;
    private final LifelineState endingState;

    public Arrow(String label, LifelineState startingState, LifelineState endingState) {
        this.label = label;
        this.startingState = startingState;
        this.endingState = endingState;
    }


    public void paint(Graphics2D g) {
        Rectangle boundary = g.getClipBounds();
        int y = endingState.getStartY();

        //don't paint if we're not in the clip area
        if(y + ARROW_WIDTH < boundary.getMinY() || y - ARROW_WIDTH > boundary.getMaxY()) {
            return;
        }

        //TODO - we need to clip by X coordinate as well.
        
        boolean isReflexsive = getStartingLine() == getEndingLine();
        if(isReflexsive) {
            paintReflexive(g);
        } else {
            paintNormal(g);
        }
    }
    
    private Lifeline getStartingLine() {
        return startingState.getLine();
    }
    
    private Lifeline getEndingLine() {
        return endingState.getLine();
    }

    private void paintReflexive(Graphics2D g) {
        Lifeline startingLine = getStartingLine();
        int x = startingLine.getX();
        int y = endingState.getStartY();

        g.drawArc(x + startingLine.getWidth() - ARROW_WIDTH / 2, y - ARROW_WIDTH, ARROW_WIDTH, ARROW_WIDTH, 90, -180);
        g.drawString(label, x + startingLine.getWidth() + LABEL_OFFSET, y);
//        GeneralPath path = new GeneralPath();
//        path.moveTo(x, y - ARROW_WIDTH);
//        path.quadTo(x, y - ARROW_WIDTH);
    }

    private void paintNormal(Graphics2D g) {
        Lifeline startingLine = getStartingLine();
        Lifeline endingLine = getEndingLine();
        int x1 = startingLine.getX();
        int x2 = endingLine.getX();
        int y = endingState.getStartY();

        if(x2 > x1) {
            int startX  = x1 + startingLine.getWidth();
            int endX = x2;

            GeneralPath path = new GeneralPath();
            path.moveTo(startX, y);
            path.lineTo(endX, y);
            path.lineTo(endX - ARROW_WIDTH, y - ARROW_WIDTH);
            path.moveTo(endX, y);
            path.lineTo(endX - ARROW_WIDTH, y + ARROW_WIDTH);
            g.draw(path);
            g.fillArc(startX, y - CIRCLE_WIDTH/2, CIRCLE_WIDTH, CIRCLE_WIDTH, 0, 360);
            g.drawString(label, startX + LABEL_OFFSET, y - LABEL_OFFSET);
        } else {
            int startX  = x1;
            int endX = x2 + endingLine.getWidth();

            GeneralPath path = new GeneralPath();
            path.moveTo(startX, y);
            path.lineTo(endX, y);
            path.lineTo(endX + ARROW_WIDTH, y - ARROW_WIDTH);
            path.moveTo(endX, y);
            path.lineTo(endX + ARROW_WIDTH, y + ARROW_WIDTH);
            g.draw(path);
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.fillArc(startX - CIRCLE_WIDTH/2, y - CIRCLE_WIDTH/2, CIRCLE_WIDTH, CIRCLE_WIDTH, 0, 360);
            g.drawString(label, startX - LABEL_OFFSET - labelWidth, y - LABEL_OFFSET);
        }
    }

    public LifelineState getStartingState() {
      return startingState;
    }
}
