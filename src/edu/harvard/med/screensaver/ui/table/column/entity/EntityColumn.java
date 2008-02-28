// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.table.column.entity;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.med.screensaver.model.AbstractEntity;
import edu.harvard.med.screensaver.model.PropertyPath;
import edu.harvard.med.screensaver.model.RelationshipPath;
import edu.harvard.med.screensaver.ui.table.Criterion;
import edu.harvard.med.screensaver.ui.table.column.ColumnType;
import edu.harvard.med.screensaver.ui.table.column.TableColumn;

/**
 * TableColumn that represents a model entity property, maintaining a data
 * binding, enabling a persistence layer to find the data for this property in
 * the underlying data schema. Maintains a PropertyPath that specifies path to
 * the property in the data model, relative to the root entity type of the
 * column. Also maintains one or more RelationshipPaths, which are used by a
 * persistence layer to form a query that fetches the entity (or entities)
 * containing the data needed to populate this column.
 * <p>
 * The constructor that takes a PropertyPath should be used whenever possible,
 * since this PropertyPath is used by a persistence layer to generate queries
 * for <i>filtering</i> and <i>sorting</i> on this column, allowing these
 * operations to be performed at the database level. Specifying a PropertyPath
 * implies a RelationshipPath.
 * <p>
 * The constructor that takes a RelationshipPath is used (as a last resort) for
 * properties that are derived, or otherwise not directly stored by the data
 * schema used to persist the data model. Note that if this Relationship
 * constructor is used, a persistence layer will not be able to filter and sort
 * on this column at the persistence layer (i.e., by the database server), since
 * a particular schema field is not bound to this column.
 * <p>
 * If the property has a dependency on multiple fields in the data schema that
 * are stored in other related entities, additional RelationshipPaths can be
 * added to ensure these entities' fields are fetched as well. For example, a
 * derived property may be calculated from multiple properties of related
 * entities. Or, for example, a relationship that is a Map, keyed on an entity
 * type, there may be performance benefits to fetching the entities that
 * represent the keys of that map (to avoid Hibernate from fetching each of
 * these keys in an individual SELECT); e.g., Well.resultValues is a Map<ResultValueType,ResultValue>.
 * 
 * @param E the root entity type of this column
 * @param T the data type of the property displayed by this column
 * @author ant4
 */
public abstract class EntityColumn<E extends AbstractEntity,T> extends TableColumn<E,T>
{
  private PropertyPath<E> _propertyPath;
  private Set<RelationshipPath<E>> _relationshipPaths = new HashSet<RelationshipPath<E>>(1);

  /**
   * Constructs an EntityColumn with a RelationshipPath, thus specifying how to
   * fetch the particular data that is to be displayed by this column, relative
   * to a root entity type.
   * @param group TODO
   */
  public EntityColumn(RelationshipPath<E> relationshipPath,
                      String name,
                      String description,
                      ColumnType columnType, 
                      String group)
  {
    super(name, description, columnType, group);
    _relationshipPaths.add(relationshipPath);
    addCriterion(new Criterion<T>(getColumnType().getDefaultOperator(), null));
  }

  /**
   * Constructs an EntityColumn with a PropertyPath, thus specifying how to fetch,
   * filter, and sort the particular data that is to be displayed by this
   * column, relative to a root entity type.
   * @param group TODO
   */
  public EntityColumn(PropertyPath<E> propertyPath,
                      String name,
                      String description,
                      ColumnType columnType, 
                      String group)
  {
    super(name, description, columnType, group);
    _relationshipPaths.add(propertyPath.getRelationshipPath());
    _propertyPath = propertyPath;
  }
  
  public Set<RelationshipPath<E>> getRelationshipPaths()
  {
    return _relationshipPaths;
  }

  public PropertyPath<E> getPropertyPath()
  {
    return _propertyPath;
  }
  
  /**
   * Add additional an RelationshipPath that should be fetched by the
   * persistence layer when fetching data for this column.
   * 
   * @param path
   */
  public void addRelationshipPath(RelationshipPath<E> path) 
  {
    _relationshipPaths.add(path);
  }
}