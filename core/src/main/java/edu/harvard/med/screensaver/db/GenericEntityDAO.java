// $HeadURL$
// $Id$

// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.

// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.db;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ScrollableResults;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.med.screensaver.model.Entity;
import edu.harvard.med.screensaver.model.meta.RelationshipPath;

public interface GenericEntityDAO
{
  /**
   * This method can be called before invoking other GenericEntityDAO methods that issue HQL
   * (or Criteria-based) queries to ensure that newly instantiated and persisted
   * entities are considered by the query. This is never necessary if the
   * Hibernate session flush mode is AUTO or or ALWAYS, since in these cases
   * Hibernate will ensure the session is always flushed prior to executing an
   * HQL query.
   */
  public void flush();

  public void clear();

  /**
   * Executes a block of code, presumably with multiple GenericEntityDAO calls, in a single transaction.
   * <p>
   * <i>Where possible, it is the preferred coding style to make a block of code transactional by using the
   * {@link Transactional} annotation on Spring-managed bean class methods.</i>
   * 
   * @param daoTransaction the object encapsulating the transactional code to execute.
   */
  public void doInTransaction(DAOTransaction daoTransaction);

  public <T> List<T> runQuery(Query<T> query);

  public ScrollableResults runScrollQuery(edu.harvard.med.screensaver.db.ScrollQuery query);

  /**
   * Make the specified entity persistent. The entity's ID property will be set
   * upon return. This method calls the underlying Hibernate Session.saveOrUpdate, which
   * does not have JPA semantics. We have encountered situations in which the save-or-update
   * cascades were not followed when this method was called, but only after the session
   * was flushed. If this seems like a problem to you, try {@link #persistEntity(Entity)} instead.
   * 
   * @param entity
   * @deprecated use {@link #mergeEntity(Entity)}
   */
  @Deprecated
  public void saveOrUpdateEntity(Entity entity);

  /**
   * Make the specified entity persistent. The entity's ID property will be set
   * upon return. This is the one with JPA semantics: save-update cascades will
   * be followed at the time this method is called. This ensures that all
   * cascade-reachable transient entities will be added to the session cache,
   * allowing subsequent
   * {@link #findEntityById(Class, Serializable)} to succeed prior to session flush.
   *
   * @param entity
   */
  public void persistEntity(Entity entity);

  /**
   * Reattach the entity to the current Hibernate session, allowing
   * modifications that have been made to it while detached, or that will be
   * made to the reattached entity, to be persisted. Note that this will cause
   * the entity (and any related entities that are reachable via "update"
   * cascades) to be version checked against the database, possibly causing a
   * ConcurrencyFailureException to be thrown if concurrent modification of this
   * or its related entities is detected.
   * <p>
   * Internally, version checking is effected by Hibernate by issuing SQL update statements to increment the version
   * field of each entity at time of this method call. This can be expensive for large entity networks with "update"
   * cascades enabled.
   * </p>
   * 
   * @param <E>
   * @param entity the entity to be reattached
   * @return the same entity instance passed in
   * @deprecated use {@link #mergeEntity(Entity)}
   */
  @Deprecated
  public <E extends Entity> E reattachEntity(E entity);

  /**
   * Merge the entity network into the current persistence context.
   * 
   * @return a <i>new</i> managed entity instance, if the specified entity is
   *         not already managed
   */
  public <E extends Entity> E mergeEntity(E entity);

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
  public <E extends Entity> E reloadEntity(E entity);

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
   * Any relationships that may have been initialized in the passed-in entity (network) will <i>not</i> be
   * pre-initialized in the new instance of the returned entity, so consider the potential performance impact of
   * navigating through previously initialized lazy relationships, except for those requested via the
   * <code>relationships</code> argument.
   * </p>
   * 
   * @param <E>
   * @param entity the entity to be reloaded
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationship the related entity or entities to be eagerly fetched
   * @return a new Hibernate-managed instance of the specified entity
   */
  public <E extends Entity> E reloadEntity(E entity,
                                           boolean readOnly,
                                           RelationshipPath<E> relationship);

  public <E extends Entity> E reloadEntity(E entity,
                                           boolean readOnly);

  /**
   * Loads the specified relationships of a given entity, allowing these
   * relationships to be navigated after the entity is detached from the
   * Hibernate session.
   * 
   * @param entity the root entity
   * @param relationship the related entity or entities to be eagerly fetched
   */
  public <E extends Entity> void need(E entity,
                                      RelationshipPath<E> relationship);

  /**
   * Loads the specified relationships of a given entity, allowing these
   * relationships to be navigated after the entity is detached from the
   * Hibernate session. See class-level documentation of {@link GenericEntityDAO} for issues related to loading
   * read-only entities.
   * 
   * @param entity the root entity
   * @param relationship the relationships to be loaded, relative to the root entity
   */
  public <E extends Entity> void needReadOnly(E entity,
                                              RelationshipPath<E> relationship);

  public void deleteEntity(Entity entity);


  /**
   * Retrieve and return a list of entities of the specified type.
   *
   * @param <E> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @return a list of the entities of the specified type
   */
  public <E extends Entity> List<E> findAllEntitiesOfType(Class<E> entityClass);

  /**
   * @param <E> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationship the relationship to be loaded, relative to the root entity
   * @return a list of the entities of the specified type
   */
  public <E extends Entity> List<E> findAllEntitiesOfType(Class<E> entityClass,
                                                          boolean readOnly,
                                                          RelationshipPath<E> relationship);

  public <E extends Entity> List<E> findAllEntitiesOfType(Class<E> entityClass,
                                                          boolean readOnly);


  /**
   * Retrieve and return an entity by its identifier (primary key). If the
   * requested entity is in the Hibernate session, no database I/O will occur.
   * Therefore this method provide an efficient means of retrieving entities
   * that have already been fetched from the database.
   *
   * @param <E> the type of the entity to retrieve
   * @param id the identifier of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @return the entity of the specified type, with the specified identifier.
   *         Return null if there is no such entity.
   */
  public <E extends Entity<K>,K extends Serializable> E findEntityById(Class<E> entityClass, K id);

  /**
   * See @{@link #findEntityById(Class, Serializable)}.
   * 
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationship the related entity or entities to be eagerly fetched
   */
  public <E extends Entity<K>,K extends Serializable> E findEntityById(Class<E> entityClass,
                                                                       K id,
                                                                       boolean readOnly,
                                                                       RelationshipPath<E> relationship);

  public <E extends Entity<K>,K extends Serializable> E findEntityById(Class<E> entityClass,
                                                                       K id,
                                                                       boolean readOnly);


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
  public <E extends Entity> List<E> findEntitiesByProperties(Class<E> entityClass,
                                                             Map<String,Object> name2Value);

  /**
   * See @{@link #findEntitiesByProperties(Class, Map)}.
   * 
   * @param name2Value a <code>Map</code> containing entries for each
   *          property/value pair to query against
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationship the related entity or entities to be eagerly fetched
   */
  public <E extends Entity> List<E> findEntitiesByProperties(Class<E> entityClass,
                                                             Map<String,Object> name2Value,
                                                             boolean readOnly,
                                                             RelationshipPath<E> relationship);

  public <E extends Entity> List<E> findEntitiesByProperties(Class<E> entityClass,
                                                             Map<String,Object> name2Value,
                                                             boolean readOnly);


  /**
   * Retrieve and return a list of entities that have specific values for the
   * specified properties.
   *
   * @param <E> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve
   * @param name2Value a <code>Map</code> containing entries for each
   *          property/value pair to query against
   * @return a list of entities that have the specified value for the specified
   *         property
   * @exception InvalidArgumentException when there is more
   *    than one entity with the specified value for the property
   */
  public <E extends Entity> E findEntityByProperties(Class<E> entityClass,
                                                     Map<String,Object> name2Value);

  /**
   * See @{@link #findEntityByProperties(Class, Map)}.
   * 
   * @param name2Value a <code>Map</code> containing entries for each
   *          property/value pair to query against
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationship the related entity or entities to be eagerly fetched
   */
  public <E extends Entity> E findEntityByProperties(Class<E> entityClass,
                                                     Map<String,Object> name2Value,
                                                     boolean readOnly,
                                                     RelationshipPath<E> relationship);

  public <E extends Entity> E findEntityByProperties(Class<E> entityClass,
                                                     Map<String,Object> name2Value,
                                                     boolean readOnly);


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
  public <E extends Entity> List<E> findEntitiesByProperty(Class<E> entityClass,
                                                           String propertyName,
                                                           Object propertyValue);

  /**
   * See @{@link #findEntitiesByProperty(Class, String, Object)}.
   * 
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationship the relationship to be loaded, relative to the root entity
   */
  public <E extends Entity> List<E> findEntitiesByProperty(Class<E> entityClass,
                                                           String propertyName,
                                                           Object propertyValue,
                                                           boolean readOnly,
                                                           RelationshipPath<E> relationship);

  public <E extends Entity> List<E> findEntitiesByProperty(Class<E> entityClass,
                                                           String propertyName,
                                                           Object propertyValue,
                                                           boolean readOnly);
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
  public <E extends Entity> E findEntityByProperty(Class<E> entityClass,
                                                   String propertyName,
                                                   Object propertyValue);

  /**
   * See @{@link #findEntityByProperty(Class, String, Object)}.
   * 
   * @param readOnly see class-level documentation of {@link GenericEntityDAO}
   * @param relationship the related entity or entities to be eagerly fetched
   */
  public <E extends Entity> E findEntityByProperty(Class<E> entityClass,
                                                   String propertyName,
                                                   Object propertyValue,
                                                   boolean readOnly,
                                                   RelationshipPath<E> relationship);

  public <E extends Entity> E findEntityByProperty(Class<E> entityClass,
                                                   String propertyName,
                                                   Object propertyValue,
                                                   boolean readOnly);

  public <E extends Entity> List<E> findEntitiesByHql(Class<E> entityClass,
                                                      String hql,
                                                      Object... hqlParameters);
  
  public <E extends Entity,T> Set<T> findDistinctPropertyValues(Class<E> entityClass,
                                                                String propertyName);
}

