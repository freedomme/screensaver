// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.screenresults;

import java.util.Arrays;

import javax.faces.model.DataModel;

import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.db.LibrariesDAO;
import edu.harvard.med.screensaver.model.screenresults.ResultValueType;
import edu.harvard.med.screensaver.ui.libraries.WellViewer;
import edu.harvard.med.screensaver.ui.table.DataTableRowsPerPageUISelectOneBean;

import org.apache.log4j.Logger;

public class PositivesOnlyScreenResultDataTable extends ScreenResultDataTable
{
  // static members

  private static Logger log = Logger.getLogger(PositivesOnlyScreenResultDataTable.class);


  // instance data members

  private ResultValueType _positivesDataHeader;


  // constructors

  /**
   * @motivation for CGLIB2
   */
  protected PositivesOnlyScreenResultDataTable()
  {
  }

  public PositivesOnlyScreenResultDataTable(ScreenResultViewer screenResultViewer,
                                            WellViewer wellViewer,
                                            LibrariesDAO librariesDao,
                                            GenericEntityDAO dao)
  {
    super(screenResultViewer, wellViewer, librariesDao, dao);
  }


  // public methods

  public ResultValueType getPositivesDataHeader()
  {
    return _positivesDataHeader;
  }

  public void setPositivesDataHeader(ResultValueType positivesDataHeader)
  {
    _positivesDataHeader = positivesDataHeader;
    rebuildRows();
  }


  // abstract & template method implementations

  @Override
  protected DataTableRowsPerPageUISelectOneBean buildRowsPerPageSelector()
  {
    DataTableRowsPerPageUISelectOneBean rowsPerPageSelector =
      new DataTableRowsPerPageUISelectOneBean(Arrays.asList(10, 20, 50, 100));
    return rowsPerPageSelector;
  }

  @Override
  public DataModel buildDataModel()
  {
    return new PositivesOnlyScreenResultDataModel(getScreenResult(),
                                                  getResultValueTypes(),
                                                  getRowsPerPage(),
                                                  getSortManager().getSortColumn(),
                                                  getSortManager().getSortDirection(),
                                                  _dao,
                                                  _positivesDataHeader);
  }


  // private methods

}
