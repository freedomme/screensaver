// $HeadURL: http://forge.abcd.harvard.edu/svn/screensaver/branches/iccbl/data-sharing-levels/src/edu/harvard/med/screensaver/ui/table/column/entity/TextSetEntityColumn.java $
// $Id: TextSetEntityColumn.java 3633 2009-11-16 17:31:25Z atolopko $
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.table.column.entity;

import java.util.Set;

import edu.harvard.med.screensaver.model.AbstractEntity;
import edu.harvard.med.screensaver.model.meta.PropertyPath;
import edu.harvard.med.screensaver.model.meta.RelationshipPath;
import edu.harvard.med.screensaver.ui.table.column.ColumnType;
import edu.harvard.med.screensaver.ui.table.column.SetColumn;

public abstract class IntegerSetEntityColumn<E extends AbstractEntity> extends SetColumn<E,Integer> implements HasFetchPaths<E>
{
  private FetchPaths<E> _fetchPaths;
  
  public IntegerSetEntityColumn(RelationshipPath<E> relationshipPath, String name, String description, String group)
  {
    super(name, description, group, ColumnType.INTEGER_SET);
    _fetchPaths = new FetchPaths<E>(relationshipPath);
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
}