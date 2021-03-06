// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.arch.datatable.column.entity;

import java.util.Set;

import edu.harvard.med.screensaver.db.datafetcher.Tuple;
import edu.harvard.med.screensaver.db.datafetcher.TupleDataFetcher;
import edu.harvard.med.screensaver.model.AbstractEntity;
import edu.harvard.med.screensaver.model.meta.PropertyPath;
import edu.harvard.med.screensaver.model.meta.RelationshipPath;
import edu.harvard.med.screensaver.ui.arch.datatable.column.EnumColumn;

public class EnumTupleColumn<E extends AbstractEntity,K,ENUM extends Enum<ENUM>> extends EnumColumn<Tuple<K>,ENUM> implements HasFetchPaths<E>
{
  private FetchPaths<E,Tuple<K>> _fetchPaths;
  private String _propertyKey;
  
  public EnumTupleColumn(PropertyPath<E> propertyPath,
                               String name,
                               String description,
                               String group,
                               ENUM[] items)
  {
    super(name, description, group, items);
    _fetchPaths = new FetchPaths<E,Tuple<K>>(propertyPath);
    _propertyKey = TupleDataFetcher.makePropertyKey(_fetchPaths.getPropertyPath());
  }

  public void addRelationshipPath(RelationshipPath<E> path)
  {
    _fetchPaths.addRelationshipPath(path);
  }

  public PropertyPath<E> getPropertyPath()
  {
    return _fetchPaths.getPropertyPath();
  }

  public Set<RelationshipPath<E>> getRelationshipPaths()
  {
    return _fetchPaths.getRelationshipPaths();
  }

  public boolean isFetchableProperty()
  {
    return _fetchPaths.isFetchableProperty();
  }

  @Override
  public ENUM getCellValue(Tuple<K> tuple)
  {
    return (ENUM) tuple.getProperty(_propertyKey);
  }
}
