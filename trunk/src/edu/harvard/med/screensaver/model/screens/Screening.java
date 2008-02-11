// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.screens;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;

import org.apache.log4j.Logger;

import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.users.ScreeningRoomUser;

/**
 * A screening room activity representing a screener screening various assay plates. These
 * assay plates could be plated from a library, as with a {@link LibraryScreening}, or from
 * a set of cherry picks, as with a {@link RnaiCherryPickScreening}.
 *
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
@Entity
@PrimaryKeyJoinColumn(name="activityId")
@org.hibernate.annotations.ForeignKey(name="fk_screening_to_activity")
@org.hibernate.annotations.Proxy
public abstract class Screening extends ScreeningRoomActivity
{
  // private static data

  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(Screening.class);


  // private instance data

  private String _assayProtocol;
  private Date _assayProtocolLastModifiedDate;
  private AssayProtocolType _assayProtocolType;
  private Integer _numberOfReplicates;
  private BigDecimal _estimatedFinalScreenConcentrationInMoles;


  // public instance methods

  /**
   * Get the assay protocol.
   * @return the assay protocol
   */
  @org.hibernate.annotations.Type(type="text")
  public String getAssayProtocol()
  {
    return _assayProtocol;
  }

  /**
   * Set the assay protocol.
   * @param assayProtocol the new assay protocol
   */
  public void setAssayProtocol(String assayProtocol)
  {
    _assayProtocol = assayProtocol;
  }

  /**
   * Get the date the assay protocol was last modified.
   * @return the date the assay protocol was last modified
   */
  public Date getAssayProtocolLastModifiedDate()
  {
    return _assayProtocolLastModifiedDate;
  }

  /**
   * Set the date the assay protocol was last modified.
   * @param assayProtocolLastModifiedDate the new date the assay protocol was last modified
   */
  public void setAssayProtocolLastModifiedDate(Date assayProtocolLastModifiedDate)
  {
    _assayProtocolLastModifiedDate = truncateDate(assayProtocolLastModifiedDate);
  }

  /**
   * Get the assay protocol type.
   * @return the assay protocol type
   */
  @org.hibernate.annotations.Type(type="edu.harvard.med.screensaver.model.screens.AssayProtocolType$UserType")
  public AssayProtocolType getAssayProtocolType()
  {
    return _assayProtocolType;
  }

  /**
   * Set the assay protocol type.
   * @param assayProtocolType the new assay protocol type
   */
  public void setAssayProtocolType(AssayProtocolType assayProtocolType)
  {
    _assayProtocolType = assayProtocolType;
  }

  /**
   * Get the number of replicates.
   * @return the number of replicates
   */
  public Integer getNumberOfReplicates()
  {
    return _numberOfReplicates;
  }

  /**
   * Set the number of replicates.
   * @param numberOfReplicates the new number of replicates
   */
  public void setNumberOfReplicates(Integer numberOfReplicates)
  {
    _numberOfReplicates = numberOfReplicates;
  }

  /**
   * Get the estimated final screen concentration, in Moles.
   * @return the estimated final screen concentration, in Moles
   */
  @org.hibernate.annotations.Type(type="big_decimal")
  public BigDecimal getEstimatedFinalScreenConcentrationInMoles()
  {
    return _estimatedFinalScreenConcentrationInMoles;
  }

  /**
   * Set the estimated final screen concentration, in Moles.
   * @param estimatedFinalScreenConcentrationInMoles the new estimated final screen concentration,
   * in Moles.
   */
  public void setEstimatedFinalScreenConcentrationInMoles(
    BigDecimal estimatedFinalScreenConcentrationInMoles)
  {
    if (estimatedFinalScreenConcentrationInMoles == null) {
      _estimatedFinalScreenConcentrationInMoles = null;
    }
    else {
      _estimatedFinalScreenConcentrationInMoles = estimatedFinalScreenConcentrationInMoles.setScale(Well.VOLUME_SCALE, RoundingMode.HALF_UP);
    }
  }


  // protected constructors

  /**
   * Construct an initialized <code>Screening</code>.
   * @param screen the screen
   * @param performedBy the user that performed the screening
   * @param dateCreated the date created
   * @param dateOfActivity the date the screening took place
   */
  protected Screening(
    Screen screen,
    ScreeningRoomUser performedBy,
    Date dateCreated,
    Date dateOfActivity)
  {
    super(screen, performedBy, dateCreated, dateOfActivity);
  }

  /**
   * Construct an uninitialized <code>Screening</code>.
   * @motivation for hibernate and proxy/concrete subclass constructors
   */
  protected Screening() {}
}
