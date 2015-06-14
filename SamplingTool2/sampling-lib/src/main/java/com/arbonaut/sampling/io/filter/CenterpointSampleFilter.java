
package com.arbonaut.sampling.io.filter;

import org.geotools.geometry.jts.JTSFactoryFinder;

import com.arbonaut.sampling.SamplePlot;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


/**
 * Sample writer that reduces the plot geometry to a single point placed at the
 * centre of the plot's envelope. 
 */
public class CenterpointSampleFilter implements SampleFilter {

    private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

    public SamplePlot filter(SamplePlot plot) throws Exception 
    {
        Geometry geom = plot.getGeometry();
    
        // Nothing to do
        if (geom instanceof Point) return plot;

        Envelope e = geom.getEnvelopeInternal();
        if (e == null) 
            throw new Exception("Geometry has null envelope");

        Geometry newgeom = gf.createPoint(e.centre());           
        return new SamplePlot(plot, newgeom, plot.getCRS());
    }	

}
