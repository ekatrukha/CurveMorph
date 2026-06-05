package curvemorph;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;

import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;

public class CMDialog implements DialogListener
{
	
	/**  0 - morphing; 1 - xy interpolation**/
	public int nAlgorithm = (int)Prefs.get( "CurveMorph.nAlgorithm", 0 );
	
	/** ends mapping, 0 - closest end point, 1 - start always first, 2 - end always first **/
	public int nEndsMap = (int)Prefs.get( "CurveMorph.nEndsMap", 0 );
	
	/** ends mapping, 0 - closest end point, 1 - start always first, 2 - end always first **/
	public boolean bUseCentriod = Prefs.get( "CurveMorph.bUseCentriod", false );
	
	/** range of morphing 0 - image defined, 1 - ROIs defined**/
	public int nRange = (int)Prefs.get( "CurveMorph.nRange", 0 );
	
	/** whether to add to overlay **/
	public boolean bAddToOverlay = Prefs.get( "CurveMorph.bAddToOverlay", true );
	
	/** whether to add to ROI Manager **/
	public boolean bAddToManager = Prefs.get( "CurveMorph.bAddToManager", false );
	
	/** whether to make kymograph **/
	public boolean bMakeKymograph = Prefs.get( "CurveMorph.bMakeKymograph", false );

	/** kymograph transverse type 0 - MAX proj, 1 - Average Proj **/
	public int nKymoType = (int)Prefs.get( "CurveMorph.nKymoType", 0 );
	
	/** kymograph alignment type 0 - center, 1 - start, 2 - end **/
	public int nKymoAlign = (int)Prefs.get( "CurveMorph.nKymoAlign", 0 );
	
	/** whether to show stack of kymograph **/
	public boolean bShowKymoStack = Prefs.get( "CurveMorph.bShowKymoStack", false );
	
	Choice chAlgo;
	Choice chEndsMap;
	Checkbox cUseCentriod;
	Checkbox cMakeKymograph;
	Choice chKymoType;
	Choice chKymoAlign;
	Checkbox cShowStack;
	
	GenericDialog gdParams;
		
	boolean showDialog()
	{
		gdParams = new GenericDialog( "CurveMorph parameters" );
		final String [] sAlgorithm = new String[] {"Shape morphing", "XY interpolation"};
		final String [] sEndsMap = new String[] {"Closest", "Always first", "Always last"};
		final String [] sRange = new String[] {"All image span", "Defined by ROIs"};
		final String [] sKymoType = new String[] {"Maximum", "Average"};
		final String [] sKymoAlign = new String[] {"Center", "Start point", "End point"};
		
		int nChoiceN = 0;
		int nCheckboxN = 0;
		gdParams.addChoice( "Algorithm", sAlgorithm,  null);
		chAlgo = ((Choice)gdParams.getChoices().get( nChoiceN ));
		nChoiceN++;
		chAlgo.select( nAlgorithm );
		
		gdParams.addChoice( "Ends mapping", sEndsMap,  null);
		chEndsMap = ((Choice)gdParams.getChoices().get( nChoiceN ));
		nChoiceN++;
		chEndsMap.select( nEndsMap );
		
		gdParams.addCheckbox( "Use centriod", bUseCentriod );
		cUseCentriod = ( Checkbox ) gdParams.getCheckboxes().get( nCheckboxN  );
		nCheckboxN ++;
		
		gdParams.addChoice( "Range", sRange,  null);
		((Choice)gdParams.getChoices().get( nChoiceN )).select( nRange );
		nChoiceN++;
		
		gdParams.addCheckbox( "Add to overlay", bAddToOverlay );
		nCheckboxN ++;
		
		gdParams.addCheckbox( "Add to ROI Manager", bAddToManager );
		nCheckboxN ++;
		
		gdParams.addCheckbox( "Make kymograph", bMakeKymograph );
		cMakeKymograph = ( Checkbox ) gdParams.getCheckboxes().get( nCheckboxN  );
		nCheckboxN ++;
		
		gdParams.addChoice( "Kymo transverse intensity", sKymoType,  null);
		chKymoType = ((Choice)gdParams.getChoices().get( nChoiceN ));
		nChoiceN++;
		chKymoType.select( nKymoType );
		
		gdParams.addChoice( "Kymo align", sKymoAlign,  null);		
		chKymoAlign = ((Choice)gdParams.getChoices().get( nChoiceN ));
		nChoiceN++;
		chKymoAlign.select( nKymoAlign );
		
		gdParams.addCheckbox( "Show kymograph stack", bShowKymoStack );
		cShowStack = ( Checkbox ) gdParams.getCheckboxes().get( nCheckboxN  );
		nCheckboxN ++;
		
		gdParams.addDialogListener( this );
		updateDialog();
		gdParams.pack();
		gdParams.showDialog();
		
		if ( gdParams.wasCanceled() )
			return false;
		return true;
	}
	
	void updateDialog()
	{
		if(chAlgo.getSelectedIndex() == 0)
		{
			cUseCentriod.setEnabled( true );
		}
		else
		{
			cUseCentriod.setEnabled( false );
		}
		chEndsMap.setEnabled( true );

		if(cMakeKymograph.getState())
		{
			chKymoType.setEnabled( true );
			chKymoAlign.setEnabled( true );
			cShowStack.setEnabled( true );
		}
		else
		{
			chKymoType.setEnabled( false );
			chKymoAlign.setEnabled( false );
			cShowStack.setEnabled( false );
		}
	}
	
	@Override
	public boolean dialogItemChanged( GenericDialog gd, AWTEvent e )
	{
		if(e != null)
		{
			updateDialog();
		}
		if(gdParams.wasOKed())
		{
			readDialogParameters();
		}
		return true;
	}
	
	void readDialogParameters()
	{
		nAlgorithm = gdParams.getNextChoiceIndex();
		Prefs.set( "CurveMorph.nAlgorithm", (double) nAlgorithm );
		
		nEndsMap = gdParams.getNextChoiceIndex();
		Prefs.set( "CurveMorph.nEndsMap", (double) nEndsMap );

		bUseCentriod = gdParams.getNextBoolean();
		Prefs.set( "CurveMorph.bUseCentriod", bUseCentriod );
		
		nRange = gdParams.getNextChoiceIndex();
		Prefs.set( "CurveMorph.nRange", (double) nRange );
		
		bAddToOverlay = gdParams.getNextBoolean();
		Prefs.get( "CurveMorph.bAddToOverlay", bAddToOverlay );
		
		bAddToManager = gdParams.getNextBoolean();
		Prefs.get( "CurveMorph.bAddToManager", bAddToManager );
		
		bMakeKymograph = gdParams.getNextBoolean();
		Prefs.get( "CurveMorph.bMakeKymograph", bMakeKymograph );
		
		if(bMakeKymograph)
		{
			nKymoType = gdParams.getNextChoiceIndex();
			Prefs.set( "CurveMorph.nKymoType", (double) nKymoType );
			nKymoAlign = gdParams.getNextChoiceIndex();
			Prefs.set( "CurveMorph.nKymoAlign", (double) nKymoAlign );
			bShowKymoStack = gdParams.getNextBoolean();
			Prefs.set( "CurveMorph.bShowKymoStack", bShowKymoStack );
		}
	}
	
}
