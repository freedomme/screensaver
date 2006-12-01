// $HeadURL: svn+ssh://ant4@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.settings/org.eclipse.jdt.ui.prefs $
// $Id: org.eclipse.jdt.ui.prefs 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.harvard.med.screensaver.model.screenresults.ResultValueType;
import edu.harvard.med.screensaver.model.screenresults.ScreenResult;

public class UniqueDataHeaderNames
{
  private ScreenResult _screenResult;
  private Map<ResultValueType,String> _uniqueDataHeaderNamesMap;
  private Map<String,ResultValueType> _uniqueDataHeaderName2ResultValueType;

  public UniqueDataHeaderNames(ScreenResult screenResult)
  {
    _screenResult = screenResult;
    _uniqueDataHeaderNamesMap = new LinkedHashMap<ResultValueType,String>();
    _uniqueDataHeaderName2ResultValueType = new HashMap<String,ResultValueType>();
    List<String> names = new ArrayList<String>();
    for (ResultValueType rvt : screenResult.getResultValueTypes()) {
      names.add(rvt.getName());
    }
    List<String> names2 = new ArrayList<String>();
    for (ResultValueType rvt : screenResult.getResultValueTypes()) {
      String name = rvt.getName();
      if (Collections.frequency(names, name) > 1) {
        names2.add(name);
        name += " (" + Collections.frequency(names2, name) + ")";
      }
      _uniqueDataHeaderNamesMap.put(rvt, name);
      _uniqueDataHeaderName2ResultValueType.put(name, rvt);
    }
  }

  public String get(ResultValueType rvt)
  {
    return _uniqueDataHeaderNamesMap.get(rvt);
  }

  public String get(Integer ordinal)
  {
    return _uniqueDataHeaderNamesMap.get(_screenResult.getResultValueTypesList().get(ordinal));
  }
  
  public ResultValueType get(String uniqueDataHeaderName)
  {
    return _uniqueDataHeaderName2ResultValueType.get(uniqueDataHeaderName);
  }
  
  public List<String> get(List<ResultValueType> rvts) {
    List<String> uniqueDataHeaderNames = new ArrayList<String>(rvts.size());
    for (ResultValueType rvt : rvts) {
      uniqueDataHeaderNames.add(_uniqueDataHeaderNamesMap.get(rvt));
    }
    return uniqueDataHeaderNames;
  }

  public List<String> asList()
  {
    return new ArrayList<String>(_uniqueDataHeaderNamesMap.values());
  }
  
  public String[] asArray()
  {
    return asList().toArray(new String[size()]);
  }

  public int size()
  {
    return _uniqueDataHeaderNamesMap.size();
  }
}
