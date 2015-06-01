package com.arbonaut.sampling.design.cluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * H-shaped plot cluster.
 */
public class HShapedCluster implements PlotCluster {
    
    private double plotDistance;
    private int numPlotsVerticalLine;
    private int numPlotsHorizontalLine;
    
    // xMin/xMax values describe the horizontal line end points needed to build the vertical lines 
    private double xMax;
    private double xMin;
    
    
    /**
     * HShapedCluster constructor.
     * The number of sub-plots is determined separately for the
     * vertical and horizontal lines of the H. 
     * Note: sub-plots located on both horizontal AND vertical lines 
	 * are considered to belong only to vertical lines (so in case of odd number of plots in vertical  lines 
	 * the horizontal line endpoints do not belong to horizontal line but to vertical lines instead) 
     * @param plotDistance  Distance between sub-plots     
     * @param numPlotsVerticalLine   number of sub-plots per vertical line.
	 * @param numPlotsHorizontalLine: number of sub-plots per horizontal line.
	 * 
     */              
    public HShapedCluster(double plotDistance, int numPlotsVerticalLine, int numPlotsHorizontalLine)
    {
        this.plotDistance = plotDistance;
        this.numPlotsVerticalLine = numPlotsVerticalLine;
        this.numPlotsHorizontalLine = numPlotsHorizontalLine;
        
    }
    
    public CoordinateSequence create(Coordinate centre)
    {
    	// calculate total number of sub-plots per cluster
    	int numPlots = (this.numPlotsVerticalLine * 2) + this.numPlotsHorizontalLine;
    	Coordinate[] coords = new Coordinate[numPlots];
    	// horizontal line end points are needed for vertical lines construction 
    	this.xMin = xMax = centre.x;
    	
    	// create horizontal line first (only create plots if there are any)
    	Coordinate[] coordsHorizontal = new Coordinate[this.numPlotsHorizontalLine];
    	if(this.numPlotsHorizontalLine > 0){ 
    		coordsHorizontal = createHorizontalLine( centre );
    		// copy plots in output array
        	System.arraycopy(coordsHorizontal, 0, coords, 0, coordsHorizontal.length);
    	}
    	
    	// create vertical lines from horizontal line end points (only create plots if there are any)
    	if(this.numPlotsVerticalLine > 0){ 
    		Coordinate[] coordsVerticalWest;
    		Coordinate[] coordsVerticalEast;
    		
    		// X offset for vertical lines is different depending on whether numPlotsVerticalLine is even or odd
    		// odd numPlotsVerticalLine --> sub-plots are exactly in line with horizontal line plots
    		if((this.numPlotsVerticalLine % 2) != 0){
    			double startXWest = this.xMin - plotDistance;
    			coordsVerticalWest = createVerticalLine(startXWest, centre.y); // left
    			double startXEast = this.xMax + plotDistance; 
    			coordsVerticalEast = createVerticalLine(startXEast, centre.y); // right    		
    		}else{ // even numPlotsVerticalLine --> sub-plots are not exactly in line with horizontal line plots
    			double xOffset =  plotDistance * Math.sqrt(0.75) ;  // derived from Pythagorean theorem
    			double startXWest = this.xMin - xOffset;
    			coordsVerticalWest = createVerticalLine(startXWest, centre.y); // left
    			double startXEast = this.xMax + xOffset; 
    			coordsVerticalEast = createVerticalLine(startXEast, centre.y); // right    		
    		}
    		// copy plots to output array
        	System.arraycopy(coordsVerticalWest, 0, coords, coordsHorizontal.length,
        			coordsVerticalWest.length);
        	System.arraycopy(coordsVerticalEast, 0, coords, (coordsHorizontal.length + coordsVerticalWest.length),
        			coordsVerticalEast.length);
    	}
    	
    	return new CoordinateArraySequence(coords);
    }


    private Coordinate[] createHorizontalLine(Coordinate centre){
    	Coordinate[] coords = new Coordinate[this.numPlotsHorizontalLine];
    	double x = centre.x;
    	double y = centre.y;

    	// create points from west to east (=from left to right)
    	double xOffset = plotDistance * ((double)this.numPlotsHorizontalLine -1) / 2;
    	x -= xOffset;
    	this.xMin = this.xMax = x; 

    	for(int i = 0; i < this.numPlotsHorizontalLine ; i++){
    		coords[i] = new Coordinate( x, y );
    		this.xMax = x; // end point needed for vertical lines construction 
    		x += this.plotDistance;
    	}
    	return coords;
    } 

    
    private Coordinate[] createVerticalLine(double x, double startY){
    	//startX can either be xMin or xMax (because there are 2 vertical lines)
    	Coordinate[] coords = new Coordinate[this.numPlotsVerticalLine];
    	
    	// create points from north to south
    	double y = startY;
    	double yOffset = plotDistance * (((double)this.numPlotsVerticalLine -1) / 2);
    	y += yOffset;

    	for(int i = 0; i < this.numPlotsVerticalLine ; i++){
    		coords[i] = new Coordinate( x, y );
    		y -= this.plotDistance;
    	}
    	return coords;
    }
    
}