package com.arbonaut.sampling.design.cluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * Grid-shaped plot cluster.
 */
public class GridShapedCluster implements PlotCluster {
    
	private double xDistance;
	private double yDistance;
    private int numPlotsX; 
    private int numPlotsY; 
    
    
    
    /**
     * GridShapedCluster constructor.
     * The first sub-plot is in the upper left (=north-western) corner.
     * @param xDistance  Distance between sub-plots along the x axis     
     * @param yDistance  Distance between sub-plots along the y axis  
     * @param numPlotsX   Number of sub-plots along the x axis   
     * @param numPlotsY   Number of sub-plots along the y axis     
     */              
    public GridShapedCluster(double xDistance, double yDistance, int numPlotsX, int numPlotsY)
    {
        this.xDistance =  xDistance;
        this.yDistance =  yDistance;
        this.numPlotsX = numPlotsX;
        this.numPlotsY = numPlotsY;
    }
    
    /**
     * GridShapedCluster constructor.
     * The first sub-plot is in the upper left (=north-western)corner.
     * @param gridDistance  Distance between sub-plots in both directions  
     * @param numPlotsX   Number of sub-plots along the x axis   
     * @param numPlotsY   Number of sub-plots along the y axis    
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
        
		//nested loop: for each plots along the x axis create all plots along the y axis
		// 
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
