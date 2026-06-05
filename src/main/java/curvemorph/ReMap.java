package curvemorph;

public class ReMap
{
	
	final public int [][] newCoords;
	final public double [][] oldCoords;
	final public int nLength;
	final public int nWidth;
	
	public ReMap(final int nLength, final int nWidth)
	{
		this.nLength = nLength;
		this.nWidth = nWidth;
		newCoords = new int[ nLength * nWidth ][2];
		oldCoords = new double[ nLength * nWidth ][2];
		for(int x = 0; x < nLength; x++)
			for(int y = 0; y < nWidth; y++)
			{
				final int ind = x + y * nLength;
				newCoords[ind][0] = x;
				newCoords[ind][1] = y;
			}
	}
}
