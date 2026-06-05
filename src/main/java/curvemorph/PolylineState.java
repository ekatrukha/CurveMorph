package curvemorph;

public class PolylineState
{
	public double [] centroid;
	public double initialAngle;
	public double[] lengths;
	public double[] angles; // Turning angles between segments
	public double[][] points;
	public int nPointsN;
	
    public PolylineState(final CurveLerp curveLerp)
    {
    	centroid = new double[2];
    	double [] cumLength = curveLerp.getCumLength();
    	lengths =  new double [curveLerp.getCumLength().length];
    	points = curveLerp.getXY();
    	//initial angle
    	double dx = points[0][1] - points[0][0];
    	double dy = points[1][1] - points[1][0];
    	initialAngle = Math.atan2(dy, dx);
    	nPointsN = curveLerp.nOrigPoints;
    	angles = new double[nPointsN - 2 ];
    	
    	double [] prev = new double[2];
    	double [] curr = new double[2];
    	double [] next = new double[2];
    	for(int i = 0; i < nPointsN - 1; i++)
    	{
    		lengths[i] = cumLength[i+1] - cumLength[i]; 
    	}
    	
    	for(int i = 0; i < nPointsN - 2; i++)
    	{
    		for(int d = 0; d < 2; d++)
    		{
    			prev[d] = points[d][i];
    			curr[d] = points[d][i + 1];
    			next[d] = points[d][i + 2];    			
    		}
    		angles[i] = getTurningAngle(prev, curr, next);
    	}
    	centroid = getCentroid(points);
    }
    
    public double getTurningAngle(double[] pPrev, double[] pCurr, double[] pNext) {
        double angle1 = Math.atan2(pCurr[1] - pPrev[1], pCurr[0] - pPrev[0]);
        double angle2 = Math.atan2(pNext[1] - pCurr[1], pNext[0] - pCurr[0]);
        
        double diff = angle2 - angle1;
        
        // Normalize to (-PI, PI) to ensure the shortest rotation
        while (diff <= -Math.PI) diff += 2 * Math.PI;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        
        return diff;
    }
    
    public static double [] getCentroid (double [][] xy)
    {
    	double [] out = new double [2];
    	final int nPoints = xy[0].length;
    	for(int i = 0; i < nPoints; i++)
    	{
    		for (int d = 0; d < 2; d++)
    		{
    			out[d] += xy[d][i];
    		}
    	}
		for (int d = 0; d < 2; d++)
		{
			out[d] /= nPoints;
		}	
		return out;
    }
    
}
