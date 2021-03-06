// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.activities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;

import org.joda.time.LocalDate;

import edu.harvard.med.screensaver.model.AbstractEntityVisitor;
import edu.harvard.med.screensaver.model.users.AdministratorUser;

/**
 * Represents an activity involving administrative decisions or changes to data.
 * Provides auditing capabilities to data modifications by tracking the
 * administrator who performed the activity.
 * 
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 */
@Entity
@PrimaryKeyJoinColumn(name="activityId")
@org.hibernate.annotations.ForeignKey(name="fk_administrative_activity_to_activity")
@org.hibernate.annotations.Proxy
public class AdministrativeActivity extends TypedActivity<AdministrativeActivityType>
{
  private static final long serialVersionUID = 1L;

  public static final AdministrativeActivity Null = new AdministrativeActivity();

  @Override
  public Object acceptVisitor(AbstractEntityVisitor visitor)
  {
    return visitor.visit(this);
  }

  // HACK: we don't need AdminActivity.type to be updatable, but since ServiceActivity.type must be updatable, 
  // and they share a parent class, we need to make them both updatable, lest our model tests report that public setter should not exist
  @Column(name = "administrativeActivityType", nullable = false/* , updatable=false */)
  @org.hibernate.annotations.Type(type = "edu.harvard.med.screensaver.model.activities.AdministrativeActivityType$UserType")
  public AdministrativeActivityType getType()
  {
    return _type;
  }
  
  /**
   * Create an AdministrativeActivity such as a simple data update, where the admin recording the activity is necessarily the same as the user performing the activity (i.e., the admin is not recording the activity on someone else's behalf).
   * @param recordedBy
   * @param dateOfActivity
   * @param type
   */
  public AdministrativeActivity(AdministratorUser recordedBy, 
                                LocalDate dateOfActivity,
                                AdministrativeActivityType type)
  {
    this(recordedBy, recordedBy, dateOfActivity, type);
  }

  /**
   * Construct an initialized <code>AdministrativeActivity</code>.
   */
  public AdministrativeActivity(AdministratorUser recordedBy, 
                                AdministratorUser performedBy,
                                LocalDate dateOfActivity,
                                AdministrativeActivityType type)
  {
    super(recordedBy, performedBy, dateOfActivity, type);

//    // we normally do not get involved with maintaining bi-directional relationships in the
//    // constructors, because normally we have a @ContainedEntity(containingEntityClass) that
//    // has factory methods to create the child entities and manage the relationships. but
//    // AdministrativeActivities don't have any containing entities. you might argue that they
//    // could be children of the performedBy, but this is problematic because we probably would
//    // want the administrative activities to live beyond the scope of the administrator (i guess
//    // administrators should be un-deletable), and also because they are bundled with other,
//    // non-administrative activities in ScreensaverUser.activitiesPerformed. and those other
//    // activities have a different parent entity (Screen). this is the only time i had to do
//    // something like this so far, and maybe i should come back to it, but i think its perfectly
//    // alright. -s
//    performedBy.getActivitiesPerformed().add(this);
  }

  /**
   * Construct an uninitialized <code>AdministrativeActivity</code>.
   * @motivation for hibernate and proxy/concrete subclass constructors
   */
  protected AdministrativeActivity() {}
}
