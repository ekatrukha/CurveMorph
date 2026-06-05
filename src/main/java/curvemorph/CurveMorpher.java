package curvemorph;

public class CurveMorpher
{

	public static double[][] getInterpolatedState(PolylineState start, PolylineState end, double t)
	{
		double[][] out = new double [2][start.nPointsN];
		for (int i = 0; i < start.nPointsN; i++) 
		{
			for(int d = 0; d < 2; d ++)
			{
				out[d][i] = lerp(start.points[d][i], end.points[d][i], t);
			}
		}	
		
		return out;
	}
	public static double[][] getMorphState(PolylineState start, PolylineState end, double t, boolean bUseCentroid)
	{
		
		double[][] out = new double [2][start.nPointsN];
		
		double currentAngle = interpolateAngle(start.initialAngle, end.initialAngle, t);
		
		for(int d = 0; d < 2; d++)
		{
			out[d][0] = lerp((float)start.points[d][0], end.points[d][0], t);
		}
		double curX = out[0][0];
		double curY = out[1][0];

		for (int i = 0; i < start.nPointsN - 1; i++) 
		{
	        double len = lerp(start.lengths[i], end.lengths[i], t);
	        curX += len * Math.cos(currentAngle);
	        curY += len * Math.sin(currentAngle);
	        out[0][i+1] = curX;
	        out[1][i+1] = curY;
	        
	        // Update angle for next segment using internal turning angle
	        if (i < start.nPointsN - 2) 
	        {
	            currentAngle += interpolateAngle(start.angles[i], end.angles[i], t);
	        }
	    }
		if(bUseCentroid)
		{
			double [] targetCentroid = new double [2];
			for(int d = 0; d < 2; d++)
			{
				targetCentroid[d] = lerp(start.centroid[d], end.centroid[d], t);
			}
			double [] currCent = PolylineState.getCentroid( out );
	
			for (int i = 0; i < start.nPointsN; i++) 
				for(int d = 0; d < 2; d++)
				{
					out[d][i] += targetCentroid[d] - currCent[d];
				}
		}
		return out;		
	}
	
	public static float[][] getMorphStateFloat(PolylineState start, PolylineState end, double t, boolean bUseCentroid)
	{
		double[][] doubleout = getMorphState( start,  end,  t, bUseCentroid);
		int nPoints = doubleout[0].length;
		float [][] floatout = new float [2][nPoints];
		for(int i = 0; i < nPoints; i++)
		{
			for (int d = 0; d < 2; d++)
			{
				floatout[d][i] = (float)doubleout[d][i];
			}
		}
		return floatout;
	}
	
	public static double lerp(double a, double b, double t)
	{
		return a + (b - a) * t;
	}
	
	public static double interpolateAngle(double startAngle, double endAngle, double t) {
	    // 1. Get the raw difference
	    double delta = endAngle - startAngle;

	    // 2. Normalize difference to [-PI, PI]
	    // This finds the shortest way around the circle
	    while (delta <= -Math.PI) delta += 2 * Math.PI;
	    while (delta > Math.PI)  delta -= 2 * Math.PI;

	    // 3. Interpolate from the start by the shortest delta
	    return startAngle + (delta * t);
	}
}
