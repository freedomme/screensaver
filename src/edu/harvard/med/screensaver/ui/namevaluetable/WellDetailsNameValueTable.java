// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.namevaluetable;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.ListDataModel;

import org.apache.log4j.Logger;

import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.ui.control.LibrariesController;

/**
 * A NameValueTable for the well details portion of the Well Viewer.
 *
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
public class WellDetailsNameValueTable extends NameValueTable
{
  
  // private static final fields
  
  private static final Logger log = Logger.getLogger(WellDetailsNameValueTable.class);
  private static final String GENBANK_ACCESSION_NUMBER_LOOKUP_URL_PREFIX =
    "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?val=";
  private static final String ENTREZGENE_ID_LOOKUP_URL_PREFIX =
    "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=";
  
  // the row names:
  private static final String LIBRARY = "Library";
  private static final String PLATE = "Plate";
  private static final String WELL = "Well";
  private static final String WELL_TYPE = "Well&nbsp;Type";
  private static final String ICCB_NUMBER = "ICCB&nbsp;Number";
  private static final String VENDOR_IDENTIFIER = "Vendor&nbsp;Identifier";

  
  // private instance fields
  
  private LibrariesController _librariesController;
  private Well _well;
  private List<String> _names = new ArrayList<String>();
  private List<Object> _values = new ArrayList<Object>();
  private List<ValueType> _valueTypes = new ArrayList<ValueType>();
  
  
  // public constructor and implementations of NameValueTable abstract methods
  
  public WellDetailsNameValueTable(LibrariesController librariesController, Well well)
  {
    _librariesController = librariesController;
    _well = well;
    initializeLists(well);
    setDataModel(new ListDataModel(_values));
  }

  @Override
  public int getNumRows()
  {
    return _names.size();
  }

  @Override
  public String getName(int index)
  {
    return _names.get(index);
  }

  @Override
  public ValueType getValueType(int index)
  {
    return _valueTypes.get(index);
  }

  @Override
  public Object getValue(int index)
  {
    return _values.get(index);
  }

  @Override
  public String getAction(int index, String value)
  {
    String name = getName(index);
    if (name.equals(LIBRARY)) {
      return _librariesController.viewLibrary(_well.getLibrary(), null);
    }
    // other fields do not have actions
    return null;
  }
  
  @Override
  public String getLink(int index, String value)
  {
    // no well detail fields have links
    return null;
  }

  
  // private instance methods
  
  /**
   * Initialize the lists {@link #_names}, {@link #_values}, and {@link #_valueTypes}. Don't
   * add rows for missing values.
   */
  private void initializeLists(Well well) {
    addItem(LIBRARY, well.getLibrary().getLibraryName(), ValueType.COMMAND);
    addItem(PLATE, Integer.toString(well.getPlateNumber()), ValueType.TEXT);
    addItem(WELL, well.getWellName(), ValueType.TEXT);
    addItem(WELL_TYPE, well.getWellType(), ValueType.TEXT);
    addItem(ICCB_NUMBER, well.getIccbNumber(), ValueType.TEXT);
    addItem(VENDOR_IDENTIFIER, well.getVendorIdentifier(), ValueType.TEXT);
  }

  private void addItem(String name, Object value, ValueType valueType)
  {
    _names.add(name);
    _values.add(value);
    _valueTypes.add(valueType);
  }
}

