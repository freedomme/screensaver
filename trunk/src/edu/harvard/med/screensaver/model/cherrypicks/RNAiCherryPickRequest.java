// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.cherrypicks;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

import edu.harvard.med.screensaver.model.AbstractEntityVisitor;
import edu.harvard.med.screensaver.model.libraries.PlateType;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.users.ScreeningRoomUser;

/**
 * A hibernate entity representing an RNAi cherry pick request.
 *
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
@Entity
@PrimaryKeyJoinColumn(name="cherryPickRequestId")
@org.hibernate.annotations.ForeignKey(name="fk_rnai_cherry_pick_request_to_cherry_pick_request")
@org.hibernate.annotations.Proxy
public class RNAiCherryPickRequest extends CherryPickRequest
{

  // private static data

  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(RNAiCherryPickRequest.class);
  private static final Set<Integer> REQUIRED_EMPTY_COLUMNS =
    new HashSet<Integer>(Arrays.asList(Well.MIN_WELL_COLUMN,
                                       Well.MIN_WELL_COLUMN + 1,
                                       Well.MAX_WELL_COLUMN - 1,
                                       Well.MAX_WELL_COLUMN ));
  private static final Set<Character> REQUIRED_EMPTY_ROWS =
    new HashSet<Character>(Arrays.asList(Well.MIN_WELL_ROW,
                                         new Character((char) (Well.MIN_WELL_ROW + 1)),
                                         new Character((char) (Well.MAX_WELL_ROW - 1)),
                                         Well.MAX_WELL_ROW));
  /* Currently (2007-04-20), all RNAi cherry pick assay plates use EPPENDORF plate types. */
  public static final PlateType RNAI_CHERRY_PICK_ASSAY_PLATE_TYPE = PlateType.EPPENDORF;
  private static final int CHERRY_PICK_SILENCING_AGENT_ALLOWANCE = 500 * 4;


  // private instance datum

  private String _assayProtocol;


  // public constructor

  /**
   * Construct an initialized <code>RNAiCherryPickRequest</code>. Intended only for use
   * by {@link Screen#createCherryPickRequest(ScreeningRoomUser, Date, Integer)}.
   * @param screen the screen
   * @param requestedBy the user that made the request
   * @param dateRequested the date created
   * @param legacyId the legacy id from ScreenDB
   * @motivation for creating CherryPickRequests from legacy ScreenDB cherry pick visits
   */
  public RNAiCherryPickRequest(
    Screen screen,
    ScreeningRoomUser requestedBy,
    Date dateRequested,
    Integer legacyCherryPickRequestNumber)
  {
    super(screen, requestedBy, dateRequested, legacyCherryPickRequestNumber);
  }


  // public instance methods

  @Override
  public Object acceptVisitor(AbstractEntityVisitor visitor)
  {
    return visitor.visit(this);
  }

  @Override
  @Transient
  public PlateType getAssayPlateType()
  {
    return RNAI_CHERRY_PICK_ASSAY_PLATE_TYPE;
  }

  @Override
  @Transient
  public int getCherryPickAllowance()
  {
    return CHERRY_PICK_SILENCING_AGENT_ALLOWANCE;
  }

  @Override
  @Transient
  public int getCherryPickAllowanceUsed()
  {
    int silencingAgentsUsed = 0;
    for (ScreenerCherryPick screenerCherryPick : getScreenerCherryPicks()) {
      silencingAgentsUsed += screenerCherryPick.getScreenedWell().getSilencingReagents().size();
    }
    return silencingAgentsUsed;
  }

  @Override
  @Transient
  public Set<Integer> getRequiredEmptyColumnsOnAssayPlate()
  {
    return REQUIRED_EMPTY_COLUMNS;
  }

  @Override
  @Transient
  public Set<Character> getRequiredEmptyRowsOnAssayPlate()
  {
    return REQUIRED_EMPTY_ROWS;
  }

  /**
   * Get the assay protocol.
   * @return the assay protocol
   */
  @org.hibernate.annotations.Type(type="text")
  public String getAssayProtocol()
  {
    // TODO: is assayProtocol needed here any more? i think the only assay protocol for cherry
    // pick requests would end up in the RNAiCherryPickScreening
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


  // protected constructor

  /**
   * Construct an uninitialized <code>RNAiCherryPickRequest</code>.
   * @motivation for hibernate and proxy/concrete subclass constructors
   */
  protected RNAiCherryPickRequest() {}
}
