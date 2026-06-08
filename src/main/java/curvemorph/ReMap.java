package curvemorph;

public class ReMap
{
	
	final public double [][] mapCoords;
	final public int nLength;
	final public int nWidth;
	
	public ReMap(final int nLength, final int nWidth)
	{
		this.nLength = nLength;
		this.nWidth = nWidth;

		mapCoords = new double[ nLength * nWidth ][2];

	}
}
