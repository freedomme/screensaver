// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.io.cherrypicks;

import edu.harvard.med.screensaver.io.workbook.Cell;

import org.apache.log4j.Logger;

public class CherryPicksDataException extends RuntimeException
{
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(CherryPicksDataException.class);

  public CherryPicksDataException()
  {
  }

  public CherryPicksDataException(String s, int cherryPickRequestNumber, int row)
  {
    super("CPR " + cherryPickRequestNumber + " row " + (row + 1) + ": " + s);
  }
  
  public CherryPicksDataException(String s, int cherryPickRequestNumber, int row, int col)
  {
    super("CPR " + cherryPickRequestNumber + " cell (" + Cell.columnIndexToLabel(col) + "" + (row + 1) + "): " + s);
  }
}
