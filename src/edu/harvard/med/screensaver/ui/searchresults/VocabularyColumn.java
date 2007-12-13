// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/branches/schema-upgrade-2007/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.searchresults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;

import edu.harvard.med.screensaver.ui.table.TableColumn;

public abstract class VocabularyColumn<T,V> extends TableColumn<T,V>
{
  private Set<V> _items;
  private ArrayList<SelectItem> _selectItems;

  public VocabularyColumn(String name,
                          String description,
                          Converter converter,
                          Set<V> items)
  {
    super(name, description, ColumnType.VOCABULARY);
    setConverter(converter);
    _items = new LinkedHashSet<V>(items);
  }

  public VocabularyColumn(String name,
                           String description,
                           Converter converter,
                           V[] items)
  {
    this(name, description, converter, new TreeSet<V>(Arrays.asList(items)));
  }

  public Set<V> getVocabulary()
  {
    return _items;
  }

  public List<SelectItem> getVocabularySelections()
  {
    if (_selectItems == null) {
      _selectItems = new ArrayList<SelectItem>();
      _selectItems.add(new SelectItem("", ""));
      for (V v : getVocabulary()) {
        _selectItems.add(new SelectItem(v));
      }
    }
    return _selectItems;
  }
}