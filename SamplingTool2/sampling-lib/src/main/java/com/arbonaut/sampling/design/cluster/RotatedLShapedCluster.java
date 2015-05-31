
package com.arbonaut.sampling.design.cluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * Rotated ("upside down") L-shaped plot cluster.
 */
public class RotatedLShapedCluster implements PlotCluster {
    
    private double plotDistance;
    private int numPlots;  
    
    /**
     * LShapedCluster constructor.
     *
     * @param plotDistance  Distance between sub-plots   
     * @param numPlots   Number of plots in the cluster     
     */              
    public RotatedLShapedCluster(double plotDistance, int numPlots)
    {
        this.plotDistance = plotDistance;
        this.numPlots = numPlots;
    }

    public CoordinateSequence create(Coordinate centre)
    {
    	Coordinate[] coords = new Coordinate[this.numPlots];

    	// Determine number of plots to be created along each axis of the L
    	int numPlotsVertical = 0;
    	int numPlotsHorizontal = 0;

    	// odd total number of Plots shaping the cluster
    	if(this.numPlots % 2 != 0){ 
    		// in case of an odd total Plot number, both axes are of the same length
    		numPlotsVertical = numPlotsHorizontal = (this.numPlots-1) / 2;
    	} 
		// even total number of Plots shaping the cluster
    	else{ 
			// in case of an even total Plot number, the vertical axis will be one Plot longer than the horizontal axis
			numPlotsVertical = this.numPlots / 2;
			numPlotsHorizontal = (this.numPlots / 2) -1;
		} 

		// extract seed point coords and use them to build the other Plots
		double x = centre.x;
    	double y = centre.y;

    	int n = 0; // index for coords[], as it has to be in continuous usage throughout 2 for-loops
		
    	// vertical axis
		for(int i = 0; i <= numPlotsVertical; i++){
			coords[n] = new Coordinate(x, y);	
			// the only difference to regular LShapedClusters is that these use decreasing y values
			y -= this.plotDistance; // as we build along the vertical axis, only y coords are affected
			n++;
				
		}

		// reset y coord for horizontal axis 
		y = centre.y;

		// horizontal axis
		for(int i = 0; i < numPlotsHorizontal; i++){
			x += this.plotDistance; // as we build along the vertical axis, only x coords are affected
			coords[n] = new Coordinate(x, y);	
			n++;
		}
		return new CoordinateArraySequence(coords);
    }
}