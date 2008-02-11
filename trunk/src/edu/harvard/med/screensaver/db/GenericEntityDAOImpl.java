// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.db;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import edu.harvard.med.screensaver.model.AbstractEntity;
import edu.harvard.med.screensaver.util.StringUtils;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.CollectionType;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * GenericEntityDAO that provides basic data access methods that are applicable
 * to all AbstractEntity types.
 * <p>
 * Each of the find* methods has two overloaded versions: a simple version that
 * takes only the basic arguments needed to find the entity (or entities), and
 * an "advanced" version that takes an additional <code>readOnly</code> flag
 * parameter and a <code>relationships</code> (var arg) array of Strings.
 * <p>
 * The relationships specify what related data should be loaded from the
 * database at the same time the entity itself is being loaded (i.e., within a
 * single SQL call). This is useful in cases where the returned entity (or
 * entities) will be used outside of a Hibernate session (i.e., "detached").
 * This is also useful for minimizing the number of SQL calls that are used to
 * fetch data for each of the relationships that will be traversed while using
 * the entity within an active Hibernate session. This or reduce or eliminate
 * the "N+1 selects" performance problem, as discussed in Hibernate
 * documentation. Each relationship is specified as a dot-separated path of
 * relationship property names, relative to the root entity. For example, if
 * loading a Parent entity, one might specify the following (hypothetical)
 * relationships: <code>"children.toys"</code>,
 * <code>"children.friends.toys"</code>. Intermediate relationships do not
 * need to be specified independently, so that, for example,
 * <code>"children.friends.toys"</code> is sufficient and <code>{"children",
 * "children.friends", "children.friends.toys"}</code>
 * is unnecessary. relationships that are more than 1 level deep, all
 * intermediate relationships will be loaded (as one would expect). <i>Warning</i>:
 * specifying more than a single to-many relationship will generate an SQL query
 * whose result will be the cross-product of all the relationships' entities.
 * This can grow quite large, quickly! Unless you are sure that the multiple
 * to-many relationships only contain a small number of entities, you should
 * retrieve each of the root entity's additional to-many relationships via
 * subsequent calls to {@link #need} or {@link #needReadOnly}.
 * <p>
 * If <code>readOnly</code> is true, the entities that are loaded by Hibernate
 * will not be "managed". This means that they will not be dirty-checked at
 * flush time, and modifications made to the objects will not be persisted. This
 * is beneficial for performance, but of course only makes sense if the client
 * code does not intend to make modifications that need to be persisted. Note
 * that this does not in any way make the loaded entity instances immutable, so
 * if any of these entity instances happen to have already been loaded into the
 * Hibernate session as managed (read-write) entities, changes to them <i>will</i>
 * be persisted! So the best practice is for the client code to never modify
 * entities loaded as read-only.
 *
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 */
public class GenericEntityDAOImpl extends AbstractDAO implements GenericEntityDAO
{
  // static members

  private static Logger log = Logger.getLogger(GenericEntityDAOImpl.class);
  private static final Logger entityInflatorLog = Logger.getLogger(GenericEntityDAOImpl.class.getName() + ".EntityInflator");

  public static String makeQueryIdList(List<? extends AbstractEntity> entities)
  {
    List<Object> ids = new ArrayList<Object>(entities.size());
    for (AbstractEntity entity : entities) {
      ids.add(entity.getEntityId());
    }
    return StringUtils.makeListString(ids, ", ");
  }


  // instance data members

  // public methods

  /**
   * @motivation for CGLIB dynamic proxy creation
   */
  public GenericEntityDAOImpl()
  {
  }

  /**
   * @deprecated Use this method prevents compile-time checking of constructor
   *             signature. Instantiate the entity via its constructor and use
   *             {@link #saveOrUpdateEntity(AbstractEntity)} instead.
   */
  public <E extends AbstractEntity> E defineEntity(
    Class<E> entityClass,
    Object... constructorArguments)
  {
    Constructor<E> constructor = getConstructor(entityClass, constructorArguments);
    E entity = newInstance(constructor, constructorArguments);
    getHibernateTemplate().save(entity);
    return entity;
  }

  /**
   * Make the specified entity persistent. The entity's ID property will be set
   * upon return. This method calls the underlying Hibernate Session.saveOrUpdate, which
   * does not have JPA semantics. We have encountered situations in which the save-or-update
   * cascades were not followed when this method was called, but only after the session
   * was flushed. If this seems like a problem to you, try {@link #persistEntity(AbstractEntity)}
   * instead.
   *
   * @param entity
   */
  public void saveOrUpdateEntity(AbstractEntity entity)
  {
    getHibernateTemplate().saveOrUpdate(entity);
  }

  /**
   * Make the specified entity persistent. The entity's ID property will be set
   * upon return. This is the one with JPA semantics: save-update cascades will be followed
   * at the time this method is called.
   *
   * @param entity
   */
  public void persistEntity(final AbstractEntity entity)
  {
    getHibernateTemplate().execute(new HibernateCallback()
    {
      public Object doInHibernate(Session session) throws HibernateException, SQLException
      {
        session.persist(entity);
        return null;
      }
    });
  }

  /**
   * Reattach the entity to the current Hibernate session, allowing
   * modifications that have been made to it while detached, or that will be
   * made to the reattached entity, to be persisted. Note that this will cause
   * the entity (and any related entities that are reachable via "update"
   * cascades) to be version checked against the database, possibly causing a
   * ConcurrencyFailureException to be thrown if concurrent modification of this
   * or its related entities is detected.
   * <p>
   * Internally, version checking is effected by Hibernate by issuing SQL update
   * statements to increment the version field of each entity at time of this
   * method call. This can be expensive for large entity networks with "update"
   * cascades enabled.
   * </p>
   *
   * @param entity the entity to be reattached
   * @return the same entity instance passed in
   */
  public <E extends AbstractEntity> E reattachEntity(E entity)
  {
    getHibernateTemplate().update(entity);
    return entity;
  }

  /**
   * For a given detached entity, return a <i>new, Hibernate-managed instance</i>.
   * If called within a transaction, all lazy relationships on the returned
   * entity can be safely navigated (i.e., w/o throwing
   * LazyInitializationExceptions). Note that if the entity already exists in
   * the session, the entity will <i>not</i> actually be reloaded from the
   * database; instead the returned instance will be the one already cached with
   * the session. The passed-in entity instance will be unchanged, and its
   * uninitialized relationships cannot be navigated.
   * <p>
   * Any relationships that may have been initialized in the passed-in entity
   * (network) will <i>not</i> be pre-initialized in the new instance of the
   * returned entity, so consider the potential performance impact of navigating
   * through previously initialized lazy relationships.
   * </p>
   *
   * @param entity the entity to be reloaded
   * @return a new Hibernate-managed instance of the specified entity
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> E reloadEntity(E entity)
  {
    // TODO: throw exception if entity already exists in the session
    return reloadEntity(entity, false);
  }

  /**
   * For a given detached entity, return a <i>new, Hibernate-managed instance</i>.
   * If called within a transaction, all lazy relationships on the returned
   * entity can be safely navigated (i.e., w/o throwing
   * LazyInitializationExceptions). Note that if the entity already exists in
   * the session, the entity will <i>not</i> actually be reloaded from the
   * database; instead the returned instance will be the one already cached with
   * the session. The passed-in entity instance will be unchanged, and its
   * uninitialized relationships cannot be navigated.
   * <p>
   * Any relationships that may have been initialized in the passed-in entity
   * (network) will <i>not</i> be pre-initialized in the new instance of the
   * returned entity, so consider the potential performance impact of navigating
   * through previously initialized lazy relationships, except for those
   * requested via the <code>relationships</code> argument.
   * </p>
   *
   * @param <E>
   * @param entity the entity to be reloaded
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   * @return a new Hibernate-managed instance of the specified entity
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> E reloadEntity(E entity, boolean readOnly, String... relationships)
  {
    // TODO: throw exception if entity already exists in the session
    if (entity != null) {
      log.debug("reloading entity " + entity);
      return (E) findEntityById(entity.getEntityClass(), entity.getEntityId(), readOnly, relationships);
    }
    return null;
  }

  /**
   * Loads the specified relationships of a given entity, allowing these
   * relationships to be navigated after the entity is detached from the
   * Hibernate session.
   *
   * @param entity the root entity
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  public void need(AbstractEntity entity,
                   String... relationships)
  {
    if (entity == null) {
      return;
    }
    inflate(entity, false, relationships);
  }

  /**
   * Loads the specified relationships of a given entity, allowing these
   * relationships to be navigated after the entity is detached from the
   * Hibernate session. See class-level documentation of
   * {@link GenericEntityDAO} for issues related to loading read-only entities.
   *
   * @param entity the root entity
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  public void needReadOnly(AbstractEntity entity,
                           String... relationships)
  {
    if (entity == null) {
      return;
    }
    inflate(entity, true, relationships);
  }

  /**
   * Returns the size of a to-many relationship collection, and does so
   * efficiently, without loading the entities in the relationship.
   *
   * @param persistentCollection
   */
  public int relationshipSize(final Object persistentCollection)
  {
    Number size = (Number) getHibernateTemplate().execute(new HibernateCallback()
    {
      public Object doInHibernate(Session session) throws HibernateException, SQLException
      {
        return ((Number) session.createFilter(persistentCollection, "select count(*)" ).list().get(0)).intValue();
      }
    });
    return size.intValue();
  }

  /**
   * Returns the size of a to-many relationship collection, and does so
   * efficiently, without loading the entities in the relationship.
   *
   * @param entity
   * @param relationship
   */
  public int relationshipSize(final AbstractEntity entity, final String relationship)
  {
    Number size = (Number) getHibernateTemplate().execute(new HibernateCallback()
    {
      public Object doInHibernate(Session session) throws HibernateException, SQLException
      {
        String entityName = session.getEntityName(entity);
        String idProperty = session.getSessionFactory().getClassMetadata(entityName).getIdentifierPropertyName();
        Query query = session.createQuery("select count(*) from " + entityName + " e join e." + relationship + " where e." + idProperty + " = :id");
        query.setString("id", entity.getEntityId().toString());
        return query.list().get(0);
      }
    });
    return size.intValue();
  }

  public int relationshipSize(
    final AbstractEntity entity,
    final String relationship,
    final String relationshipProperty,
    final String relationshipPropertyValue)
  {
    Number size = (Number) getHibernateTemplate().execute(new HibernateCallback()
    {
      public Object doInHibernate(Session session) throws HibernateException, SQLException
      {
        String entityName = session.getEntityName(entity);
        String idProperty = session.getSessionFactory().getClassMetadata(entityName).getIdentifierPropertyName();
        Query query = session.createQuery(
          "select count(*) from " + entityName + " e join e." + relationship + " r " +
          "where e." + idProperty + " = :id " +
          "and r." + relationshipProperty + " = :propValue");
        query.setString("id", entity.getEntityId().toString());
        query.setString("propValue", relationshipPropertyValue);
        return query.list().get(0);
      }
    });
    return size.intValue();
  }

  public void deleteEntity(AbstractEntity entity)
  {
    getHibernateTemplate().delete(entity);
  }

  /**
   * Retrieve and return a list of entities of the specified type.
   *
   * @param <E> The type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @return a list of the entities of the specified type
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> List<E> findAllEntitiesOfType(Class<E> entityClass)
  {
    return (List<E>) getHibernateTemplate().loadAll(entityClass);
  }

  /**
   * @param <E>
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> List<E> findAllEntitiesOfType(Class<E> entityClass,
                                                                  boolean readOnly,
                                                                  String... relationships)
  {
    return (List<E>) findEntitiesByProperties(entityClass, null, readOnly, relationships);
  }

  /**
   * Retrieve and return an entity by its identifier (primary key).
   *
   * @param <E> the type of the entity to retrieve
   * @param id the identifier of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @return the entity of the specified type, with the specified identifier.
   *         Return null if there is no such entity.
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> E findEntityById(Class<E> entityClass, Serializable id)
  {
    return (E) getHibernateTemplate().get(entityClass, id);
  }

  /**
   * See @{@link #findEntityById(Class, Serializable)}.
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> E findEntityById(Class<E> entityClass,
                                                     Serializable id,
                                                     boolean readOnly,
                                                     String... relationships)
  {
    return findEntityByProperty(entityClass, "id", id, readOnly, relationships);
  }

  /**
   * Retrieve and return the entity that has specific values for the specified
   * properties. Return <code>null</code> if no entity has that value for that
   * set of properties.
   *
   * @param <E> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @param name2Value a <code>Map</code> containing entries for each
   *          property/value pair to query against
   * @return the entity that has the specified values for the specified
   *         set of properties
   */
  public <E extends AbstractEntity> List<E> findEntitiesByProperties(Class<E> entityClass,
                                                                     Map<String,Object> name2Value)
  {
    return findEntitiesByProperties(entityClass, name2Value, false);
  }

  /**
   * See @{@link #findEntitiesByProperties(Class, Map)}.
   * @param name2Value a <code>Map</code> containing entries for each
   *          property/value pair to query against
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationshipsIn the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> List<E> findEntitiesByProperties(Class<E> entityClass,
                                                                     Map<String,Object> name2Value,
                                                                     final boolean readOnly,
                                                                     String... relationshipsIn)
  {
    String entityName = entityClass.getSimpleName();
    final StringBuffer hql = new StringBuffer();

    List<String> relationships = expandRelationships(relationshipsIn);
    Map<String,String> path2Alias = makeAliases(relationships);
    String entityAlias = "x";
    hql.append("select distinct x from ").append(entityName).append(' ').append(entityAlias);
    for (String relationship : relationships) {
      int finalPathSeparatorPos = relationship.lastIndexOf('.');
      String fromAlias = null;
      String association = null;
      if (finalPathSeparatorPos < 0) {
        fromAlias = "x";
        association = relationship;
      }
      else {
        String pathToRel = relationship.substring(0, finalPathSeparatorPos);
        if (!path2Alias.containsKey(pathToRel)) {
          throw new IllegalArgumentException("relationship " + relationship + " requires previous intermediate relationship " + pathToRel);
        }
        fromAlias = path2Alias.get(pathToRel);
        association = relationship.substring(finalPathSeparatorPos + 1);
      }
      String asAlias = path2Alias.get(relationship);
      hql.append(" left join fetch ").append(fromAlias).append(".").append(association).append(' ').append(asAlias);
    }

    boolean first = true;
    if (name2Value == null) {
      name2Value = Collections.EMPTY_MAP;
    }
    final Object[] values = new Object[name2Value.size()];
    int i = 0;
    for (String propertyName : name2Value.keySet()) {
      if (first) {
        hql.append(" where ");
        first = false;
      }
      else {
        hql.append(" and ");
      }
      hql.append("x.").append(propertyName).append(" = ?");
      values[i++]= name2Value.get(propertyName);
    }

    if (log.isDebugEnabled()) {
      log.debug(hql.toString());
    }

    List<E> result = (List<E>) getHibernateTemplate().execute(new HibernateCallback()
    {
      public Object doInHibernate(Session session) throws HibernateException, SQLException
      {
        Query query = session.createQuery(hql.toString());
        query.setReadOnly(readOnly);
        int pos = 0;
        for (Object arg : values) {
          query.setParameter(pos++, arg);
        }
        return query.list();
      }
    });
    LinkedHashSet<E> distinctResult = new LinkedHashSet<E>(result);
    if (result.size() > distinctResult.size()) {
      return new ArrayList<E>(distinctResult);
    }
    return result;
  }

  /**
   * Retrieve and return a list of entities that have specific values for the
   * specified properties.
   *
   * @param <E> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @return a list of entities that have the specified value for the specified
   *         property
   * @param name2Value a <code>Map</code> containing entries for each
   *          property/value pair to query against
   * @exception InvalidArgumentException when there is more
   *    than one entity with the specified value for the property
   */
  public <E extends AbstractEntity> E findEntityByProperties(Class<E> entityClass,
                                                             Map<String,Object> name2Value)
  {
    return findEntityByProperties(entityClass,
                                  name2Value,
                                  false);
  }

  /**
   * See @{@link #findEntityByProperties(Class, Map)}.
   * @param name2Value a <code>Map</code> containing entries for each
   *          property/value pair to query against
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  public <E extends AbstractEntity> E findEntityByProperties(Class<E> entityClass,
                                                             Map<String,Object> name2Value,
                                                             boolean readOnly,
                                                             String... relationships)
  {
    List<E> entities = findEntitiesByProperties(
      entityClass,
      name2Value,
      readOnly,
      relationships);
    if (entities.size() == 0) {
      return null;
    }
    if (entities.size() > 1) {
      throw new IllegalArgumentException(
        "more than one result for GenericEntityDAO.findEntityByProperties");
    }
    return entities.get(0);
  }

  /**
   * Retrieve and return the entities that have a specific value for the
   * specified property. Return empty list if no entity has that value for that
   * property.
   *
   * @param <E> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @param propertyName the name of the property to query against
   * @param propertyValue the value of the property to query for
   * @return the entity that has the specified value for the specified property
   */
  public <E extends AbstractEntity> List<E> findEntitiesByProperty(Class<E> entityClass,
                                                                   String propertyName,
                                                                   Object propertyValue)
  {
    return findEntitiesByProperty(entityClass, propertyName, propertyValue, false);
  }

  /**
   * See @{@link #findEntitiesByProperty(Class, String, Object)}.
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> List<E> findEntitiesByProperty(Class<E> entityClass,
                                                                   String propertyName,
                                                                   Object propertyValue,
                                                                   boolean readOnly,
                                                                   String... relationships)
  {
    Map<String,Object> props = new HashMap<String,Object>();
    props.put(propertyName, propertyValue);
    return findEntitiesByProperties(entityClass, props, readOnly, relationships);
  }

  /**
   * Retrieve and return the entity that has a specific value for the specified
   * property. Return <code>null</code> if no entity has that value for that
   * property. Throw an <code>InvalidArgumentException</code> if there is more
   * than one entity with the specified value.
   *
   * @param <E> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @param propertyName the name of the property to query against
   * @param propertyValue the value of the property to query for
   * @return the entity that has the specified value for the specified property
   * @exception InvalidArgumentException when there is more
   *    than one entity with the specified value for the property
   */
  public <E extends AbstractEntity> E findEntityByProperty(Class<E> entityClass,
                                                           String propertyName,
                                                           Object propertyValue)
  {
    return findEntityByProperty(entityClass,
                                propertyName,
                                propertyValue,
                                false);
  }

  /**
   * See @{@link #findEntityByProperty(Class, String, Object)}.
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationships the relationships to loaded, relative to the root
   *          entity, specified as a dot-separated path of relationship property
   *          names; see class-level documentation of {@link GenericEntityDAO}
   */
  public <E extends AbstractEntity> E findEntityByProperty(Class<E> entityClass,
                                                           String propertyName,
                                                           Object propertyValue,
                                                           boolean readOnly,
                                                           String... relationships)
  {
    List<E> entities = findEntitiesByProperty(
      entityClass,
      propertyName,
      propertyValue,
      readOnly,
      relationships);
    if (entities.size() == 0) {
      return null;
    }
    if (entities.size() > 1) {
      throw new IllegalArgumentException(
        "more than one result for GenericEntityDAO.findEntityByProperty");
    }
    return entities.get(0);
  }

//  @SuppressWarnings("unchecked")
//  public <E extends AbstractEntity> List<E> findEntitiesByPropertyPattern(
//    Class<E> entityClass,
//    String propertyName,
//    String propertyPattern)
//  {
//    String entityName = entityClass.getSimpleName();
//    String hql = "from " + entityName + " x where x." + propertyName + " like ?";
//    propertyPattern = propertyPattern.replaceAll( "\\*", "%" );
//    return (List<E>) getHibernateTemplate().find(hql, propertyPattern);
//  }

  @SuppressWarnings("unchecked")
  public <E extends AbstractEntity> List<E> findEntitiesByHql(
    Class<E> entityClass,
    String hql,
    Object... hqlParameters)
  {
    return (List<E>) getHibernateTemplate().find(hql, hqlParameters);
  }


  // private instance methods

  /**
   * Get the constructor for the given Entity class and arguments.
   * @param <E> the entity type
   * @param entityClass the entity class
   * @param arguments the (possibly empty) constructor arguments
   * @return the constructor for the given Entity class and arguments
   * @exception IllegalArgumentException whenever the implied constructor
   * does not exist or is not public
   */
  private <E extends AbstractEntity> Constructor<E> getConstructor(
    Class<E> entityClass,
    Object... arguments)
  {
    Class[] argumentTypes = getArgumentTypes(arguments);
    try {
      return entityClass.getConstructor(argumentTypes);
    }
    catch (SecurityException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Return an array of types that correspond to the array of arguments.
   *
   * @param arguments the arguments to get the types for
   * @return an array of types that correspond to the array of arguments
   */
  private Class[] getArgumentTypes(Object [] arguments)
  {
    Class [] argumentTypes = new Class [arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      Class argumentType;
      if (arguments[i] instanceof AbstractEntity) {
        argumentType = ((AbstractEntity) arguments[i]).getEntityClass();
      }
      else {
        argumentType = arguments[i].getClass();
      }
      if (argumentType.equals(Boolean.class)) {
        argumentType = Boolean.TYPE;
      }
      argumentTypes[i] = argumentType;
    }
    return argumentTypes;
  }

  /**
   * Construct and return a new entity object.
   *
   * @param <E> the entity type
   * @param constructor the constructor to invoke
   * @param constructorArguments the (possibly empty) list of arguments to
   * pass to the constructor
   * @return the newly constructed entity object
   */
  private <E extends AbstractEntity> E newInstance(
    Constructor<E> constructor,
    Object... constructorArguments)
  {
    try {
      return constructor.newInstance(constructorArguments);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(e);
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException(e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private boolean verifyEntityRelationshipExists(Session session, Class entityClass, String relationship)
  {
    ClassMetadata metadata = session.getSessionFactory().getClassMetadata(entityClass);
    if (relationship.contains(".")) {
      int next = relationship.indexOf(".");
      String nextRelationship = relationship.substring(next + 1);
      relationship = relationship.substring(0, next);
      if (!verifyEntityRelationshipExists(session, entityClass, relationship)) {
        return false;
      }

      org.hibernate.type.Type nextType = metadata.getPropertyType(relationship);
      if (nextType.isCollectionType()) {
        nextType = ((CollectionType) nextType).getElementType((SessionFactoryImplementor) session.getSessionFactory());
      }
      Class nextEntityClass = nextType.getReturnedClass();
      return verifyEntityRelationshipExists(session,
                                            nextEntityClass,
                                            nextRelationship);
    }
    else {
      if (!Arrays.asList(metadata.getPropertyNames()).contains(relationship)) {
        // TODO: this should probably be a Java assert instead of just a log error msg
        log.error("relationship does not exist: " + entityClass.getSimpleName() + "." + relationship);
        return false;
      }
      return true;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void inflate(final AbstractEntity entity,
                           boolean readOnly,
                           final String... relationships)
  {
    long start = 0;
    if (entityInflatorLog.isDebugEnabled()) {
      entityInflatorLog.debug("inflating " + entity + " for relationships: " + relationships);
      start = System.currentTimeMillis();
    }
    findEntityById(entity.getEntityClass(), entity.getEntityId(), readOnly, relationships);
    if (entityInflatorLog.isDebugEnabled()) {
      entityInflatorLog.debug("inflating " + entity + " took " + (System.currentTimeMillis() - start) / 1000.0 + " seconds");
    }
  }

  private Map<String,String> makeAliases(List<String> relationships)
  {
    int nextAlias = 1;
    Map<String,String> path2Alias = new HashMap<String,String>();
    for (String relationship : relationships) {
      if (!path2Alias.containsKey(relationship)) {
        path2Alias.put(relationship, "x" + nextAlias++);
      }
    }
    return path2Alias;
  }

  /**
   * Returns an ordered set of the relationships, expanded to include all
   * implicit, intermediate relationships. For example, if input is { "w", "x.y.z", },
   * output will be { "w", "x", "x.y", "x.y.z" }.
   */
  private List<String> expandRelationships(String[] relationships)
  {
    LinkedHashSet<String> expandedRelationships = new LinkedHashSet<String>();
    for (String relationship : relationships) {
      int pos = -1;
      do {
        pos = relationship.indexOf('.', pos + 1);
        if (pos < 0) {
          expandedRelationships.add(relationship);
        }
        else if (pos > 0 && pos < relationship.length()) {
          expandedRelationships.add(relationship.substring(0, pos));
        }
      } while (pos >= 0);
    }
     return new ArrayList<String>(expandedRelationships);
  }
}
