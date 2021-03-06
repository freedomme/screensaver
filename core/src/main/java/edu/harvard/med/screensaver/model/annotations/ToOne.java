// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an entity bean's property (on the getter method) as a {one,many}-to-one relationship
 * with another entity. Provides custom extensions for javax.persistence.{One,Many}ToOne.
 * 
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToOne {

  /**
   * Return the name of the inverse property for this to-many relationship. This is only
   * needed when the inverse property is not simply the lower-cased version of the class name
   * of this entity.
   * @return the name of the inverse property for this relationship
   */
  String inverseProperty() default "";

  /**
   * Return true iff this property is unidirectional.
   * @return true iff this property is unidirectional
   */
  boolean unidirectional() default false;

  /**
   * Indicates whether this relationship cannot be tested via normal setter or
   * instantiation (constructor or factory) methods. If true, it is the
   * developer's responsibility to write a custom unit test should be written
   * for this relationship.
   */
  boolean hasNonconventionalSetterMethod() default false;
}

