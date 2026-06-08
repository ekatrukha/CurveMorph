package curvemorph;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.LUT;

public class TransferLUTs
{

	public static CompositeImage transfer(final ImagePlus example, final ImagePlus input)
	{
		LUT [] luts = new LUT[ example.getNChannels()];

		if ( example instanceof CompositeImage )
		{
			CompositeImage compositeImage = ( CompositeImage ) example;

			for ( int c = 0; c < example.getNChannels(); ++c )
			{
				luts[c] = compositeImage.getChannelLut( c + 1 );
			}
		}
		CompositeImage out = new CompositeImage(input);
		out.setLuts( luts );
		out.setMode( IJ.COMPOSITE );
		return out;
	}
}
