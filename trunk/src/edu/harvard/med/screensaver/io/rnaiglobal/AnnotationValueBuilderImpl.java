// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.io.rnaiglobal;

import jxl.Cell;

import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.model.DataModelViolationException;
import edu.harvard.med.screensaver.model.libraries.Reagent;
import edu.harvard.med.screensaver.model.libraries.ReagentVendorIdentifier;
import edu.harvard.med.screensaver.model.screenresults.AnnotationType;

class AnnotationValueBuilderImpl implements AnnotationValueBuilder
{
  private int _sourceColumnIndex;
  private AnnotationType _annotationType;
  private GenericEntityDAO _dao;

  public AnnotationValueBuilderImpl(int sourceColumnIndex,
                                    AnnotationType annotationType,
                                    GenericEntityDAO dao)
  {
    _sourceColumnIndex = sourceColumnIndex;
    _annotationType = annotationType;
    _dao = dao;
  }

  public void addAnnotationValue(Cell[] row)
  {
    String value = transformValue(row[_sourceColumnIndex].getContents());
    ReagentVendorIdentifier reagentVendorIdentifier = new ReagentVendorIdentifier(DHARMACON_VENDOR_NAME,
                                                                                  row[0].getContents());
    Reagent reagent = _dao.findEntityById(Reagent.class, reagentVendorIdentifier);
    if (reagent == null) {
      throw new DataModelViolationException("no such reagent " + reagentVendorIdentifier);
    }
    _annotationType.createAnnotationValue(reagent, value);
  }

  public AnnotationType getAnnotationType()
  {
    return _annotationType;
  }

  public String transformValue(String value)
  {
    return value;
  }
}