// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.users;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import edu.harvard.med.screensaver.model.AbstractEntityVisitor;
import edu.harvard.med.screensaver.model.BusinessRuleViolationException;
import edu.harvard.med.screensaver.model.DataModelViolationException;
import edu.harvard.med.screensaver.model.meta.Cardinality;
import edu.harvard.med.screensaver.model.meta.RelationshipPath;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.users.LabHeadAppointmentCategory;

/**
 * The head honcho of a {@link Lab}.
 * 
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
@Entity
@PrimaryKeyJoinColumn(name="screensaverUserId")
@org.hibernate.annotations.ForeignKey(name="fk_lab_head_to_screening_room_user")
@org.hibernate.annotations.Proxy
public class LabHead extends ScreeningRoomUser
{
  // static members

  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(LabHead.class);
  
  public static final RelationshipPath<LabHead> screensHeaded = RelationshipPath.from(LabHead.class).to("screensHeaded");
  public static final RelationshipPath<LabHead> labMembers = RelationshipPath.from(LabHead.class).to("labMembers");
  public static final RelationshipPath<LabHead> labAffiliation = RelationshipPath.from(LabHead.class).to("labAffiliation", Cardinality.TO_ONE);


  // instance data members

  private Set<Screen> _screensHeaded = new HashSet<Screen>();
  private Set<ScreeningRoomUser> _labMembers = new HashSet<ScreeningRoomUser>();
  private LabAffiliation _labAffiliation;

  private LabHeadAppointmentCategory _labHeadAppointmentCategory;
  private LabHeadAppointmentDepartment _labHeadAppointmentDepartment;
  private LocalDate _labHeadAppointmentUpdateDate;

  // public constructors and methods

  protected LabHead() {}
  
  public LabHead(AdministratorUser createdBy)
  {
    super(createdBy);
    setUserClassification(ScreeningRoomUserClassification.PRINCIPAL_INVESTIGATOR);
  }

  @Override
  public Object acceptVisitor(AbstractEntityVisitor visitor)
  {
    return visitor.visit(this);
  }

  /** for test code only */
  public LabHead(String firstName,
                 String lastName,
                 LabAffiliation labAffilliation)
  {
    super(firstName,
          lastName,
          ScreeningRoomUserClassification.PRINCIPAL_INVESTIGATOR);
    _labAffiliation = labAffilliation;
  }

  @Transient
  public boolean isHeadOfLab()
  {
    return true;
  }

  @Transient
  @Override
  public Lab getLab()
  {
    if (_lab == null) {
      _lab = new Lab(this);
      _lab.setLabAffiliation(_labAffiliation);
      _lab.setLabMembers(_labMembers);
    }
    return _lab;
  }

  /**
   * @motivation overriding superclass only to add annotations needed by model
   *             unit tests, since this is an immutable property in the
   *             subclass, but not in the superclass
   */
  @Override
  @Column(updatable = false)
  @edu.harvard.med.screensaver.model.annotations.Column(hasNonconventionalSetterMethod = true)
  @Transient
  public ScreeningRoomUserClassification getUserClassification() 
  {
    return super.getUserClassification();
  }

  @Override
  public void setUserClassification(ScreeningRoomUserClassification userClassification)
  {
    if (userClassification != ScreeningRoomUserClassification.PRINCIPAL_INVESTIGATOR) {
      throw new BusinessRuleViolationException("cannot change the classification of a principal investigator");
    }
    _userClassification = userClassification;
  }

  /**
   * Get the set of screens for which this user was the lab head.
   * @return the set of screens for which this user was the lab head
   */
  @OneToMany(
    mappedBy="labHead",
    fetch=FetchType.LAZY
  )
  @edu.harvard.med.screensaver.model.annotations.ToMany(singularPropertyName="screenHeaded")
  public Set<Screen> getScreensHeaded()
  {
    return _screensHeaded;
  }

  /**
   * Add the screens for which this user was the lab head.
   * @param screenHeaded the screens for which this user was the lab hea to add
   * @return true iff the screening room user did not already have the screens for which this user was the lab hea
   */
  public boolean addScreenHeaded(Screen screenHeaded)
  {
    if (_screensHeaded.add(screenHeaded)) {
      screenHeaded.setLabHead(this);
      return true;
    }
    return false;
  }

  /**
   * @return a Set of Screens comprised of the screens this user has headed, led and collaborated on.
   */
  @Transient
  @Override
  public Set<Screen> getAllAssociatedScreens()
  {
    Set<Screen> screens = super.getAllAssociatedScreens();
    screens.addAll(getScreensHeaded());
    return screens;
  }

  // private methods

  /**
   * Set the screens for which this user was the lab head.
   * @param screensHeaded the new screens for which this user was the lab head
   * @motivation for hibernate
   */
  private void setScreensHeaded(Set<Screen> screensHeaded)
  {
    _screensHeaded = screensHeaded;
  }

  /**
   * Get the set of lab members. The result will <i>not</i> contain this
   * LabHead.
   *
   * @return the set of lab members
   */
  @OneToMany(mappedBy = "labHead", targetEntity=ScreeningRoomUser.class, fetch = FetchType.LAZY)
  private Set<ScreeningRoomUser> getLabMembers()
  {
    if (_lab != null) {
      return _lab.getLabMembers();
    }
    else {
      return _labMembers;
    }
  }

  /**
   * Set the lab members.
   * @param labMembers the new lab members
   * @motivation for hibernate
   */
  private void setLabMembers(Set<ScreeningRoomUser> labMembers)
  {
    _labMembers = labMembers;
  }

  /**
   * Get the lab affiliation.
   * 
   * @return the lab affiliation
   */
  @ManyToOne(fetch=FetchType.EAGER,
             cascade={ CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name="labAffiliationId", nullable=true)
  @org.hibernate.annotations.ForeignKey(name="fk_lab_head_to_lab_affiliation")
  @org.hibernate.annotations.LazyToOne(value=org.hibernate.annotations.LazyToOneOption.FALSE)
  @org.hibernate.annotations.Cascade(value={
    org.hibernate.annotations.CascadeType.SAVE_UPDATE
  })
  @edu.harvard.med.screensaver.model.annotations.ToOne(unidirectional=true)
  private LabAffiliation getLabAffiliation()
  {
    if (_lab != null) {
      return _lab.getLabAffiliation();
    }
    else {
      return _labAffiliation;
    }
  }

  /**
   * Set the lab affiliation.
   * @param labAffiliation the new lab affiliation
   */
  private void setLabAffiliation(LabAffiliation labAffiliation)
  {
    _labAffiliation = labAffiliation;
  }

  /**
   * Set the lab head.
   * @param labHead the new lab head
   */
  @Override
  protected void setLabHead(LabHead labHead)
  {
    if (labHead != null) {
      throw new DataModelViolationException("a lab head cannot itself have a lab head");
    }
    super.setLabHead(labHead);
  }
  
  @Column(nullable=true)
  @org.hibernate.annotations.Type(
    type="edu.harvard.med.screensaver.model.users.LabHeadAppointmentDepartment$UserType"
  )
  public LabHeadAppointmentDepartment getLabHeadAppointmentDepartment()
  {
    return _labHeadAppointmentDepartment;
  }

  public void setLabHeadAppointmentDepartment(LabHeadAppointmentDepartment value)
  {
    _labHeadAppointmentDepartment = value;
  }
  
  @Type(type="edu.harvard.med.screensaver.db.usertypes.LocalDateType")
  public LocalDate getLabHeadAppointmentUpdateDate()
  {
    return _labHeadAppointmentUpdateDate;
  }

  public void setLabHeadAppointmentUpdateDate(LocalDate value)
  {
    _labHeadAppointmentUpdateDate = value;
  }
  
  @Column(nullable=true)
  @org.hibernate.annotations.Type(
    type="edu.harvard.med.screensaver.model.users.LabHeadAppointmentCategory$UserType"
  )
  public LabHeadAppointmentCategory getLabHeadAppointmentCategory()
  {
    return _labHeadAppointmentCategory;
  }

  /**
   */
  public void setLabHeadAppointmentCategory(LabHeadAppointmentCategory value)
  {
    _labHeadAppointmentCategory = value;
  }
  
  
  
}
