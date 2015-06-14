package com.arbonaut.sampling.design.cluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * Grid-shaped plot cluster.
 * This cluster plot design allows the flexible creation 
 * of X*Y plots grid clusters.
 */
public class GridShapedCluster implements PlotCluster {
    
	private double xDistance;
	private double yDistance;
    private int numPlotsX; 
    private int numPlotsY; 
    
    
    
    /**
     * Constructor.
     * The first sub-plot is in the upper left (=north-western) corner.
     * @param xDistance  distance between sub-plots along the x axis     
     * @param yDistance  distance between sub-plots along the y axis  
     * @param numPlotsX  number of sub-plots along the x axis   
     * @param numPlotsY  number of sub-plots along the y axis     
     */              
    public GridShapedCluster(double xDistance, double yDistance, int numPlotsX, int numPlotsY)
    {
        this.xDistance =  xDistance;
        this.yDistance =  yDistance;
        this.numPlotsX = numPlotsX;
        this.numPlotsY = numPlotsY;
    }
    
    /**
     * Constructor.
     * The first sub-plot is in the upper left (=north-western) corner.
     * @param gridDistance  distance between sub-plots in both directions  
     * @param numPlotsX     number of sub-plots along the x axis   
     * @param numPlotsY     number of sub-plots along the y axis    
     */              
    public GridShapedCluster(double gridDistance, int numPlotsX, int numPlotsY)
    {
       this(gridDistance, gridDistance, numPlotsX, numPlotsY);
    }
    
    public CoordinateSequence create(Coordinate centre)
    {
        Coordinate[] coords = new Coordinate[this.numPlotsX * this.numPlotsY];
        int n = 0; // index for coords[]
        double x = centre.x;
		double y = centre.y;
        
		//nested loop: for each plot along the x axis create all plots along the y axis
		for(int i = 0; i < numPlotsX; i++){
			// loop over numPlotsY
			for(int k = 0; k < numPlotsY; k++){
				coords[n] = new Coordinate(x, y);
				y-= this.yDistance;
				n++;
			}
			// reset y 
			y = centre.y;
			x+= this.xDistance;
		}
        
        return new CoordinateArraySequence(coords);
    }
  
}
