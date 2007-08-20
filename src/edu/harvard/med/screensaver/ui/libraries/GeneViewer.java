// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.libraries;

import edu.harvard.med.screensaver.model.libraries.Gene;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.ui.AbstractBackingBean;
import edu.harvard.med.screensaver.ui.namevaluetable.GeneNameValueTable;
import edu.harvard.med.screensaver.ui.searchresults.WellSearchResults;

public class GeneViewer extends AbstractBackingBean
{
  
  // private instance fields
  
  private Gene _gene;
  private WellSearchResults _wellSearchResults;
  private GeneNameValueTable _geneNameValueTable;
  private Well _parentWellOfInterest;

  
  // public instance methods

  public Gene getGene()
  {
    return _gene;
  }

  public void setGene(Gene gene)
  {
    _gene = gene;
  }

  public WellSearchResults getWellSearchResults()
  {
    return _wellSearchResults;
  }

  public void setWellSearchResults(WellSearchResults wellSearchResults)
  {
    _wellSearchResults = wellSearchResults;
  }

  public GeneNameValueTable getGeneNameValueTable()
  {
    return _geneNameValueTable;
  }

  public void setGeneNameValueTable(GeneNameValueTable geneNameValueTable)
  {
    _geneNameValueTable = geneNameValueTable;
  }

  /**
   * Set the parent Well of interest, for which this compound is being viewed (a
   * compound can be in multiple wells, but the UI may want to be explicit about
   * which Well "led" to this viewer").
   * 
   * @param parentWellOfInterest the parent Well of interest, for which this
   *          compound is being viewed; may be null
   */
  public void setParentWellOfInterest(Well parentWellOfInterest)
  {
    _parentWellOfInterest = parentWellOfInterest;
  }

  /**
   * Get the parent Well of interest, for which this compound is being viewed (a
   * compound can be in multiple wells, but the UI may want to be explicit about
   * which Well "led" to this viewer").
   * 
   * @return the parent Well of interest, for which this compound is being
   *         viewed; may be null
   */
  public Well getParentWellOfInterest()
  {
    return _parentWellOfInterest;
  }
}
