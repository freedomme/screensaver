// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.cherrypicks;


import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Parameter;

import edu.harvard.med.screensaver.model.AbstractEntity;
import edu.harvard.med.screensaver.model.AbstractEntityVisitor;
import edu.harvard.med.screensaver.model.DataModelViolationException;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.meta.Cardinality;
import edu.harvard.med.screensaver.model.meta.RelationshipPath;
import edu.harvard.med.screensaver.model.screens.Screen;


/**
 * Represents a screener-selected set of wells from a {@link Screen} that are to
 * be screened again for validation purposes. ScreenerCherryPicks are managed by
 * a {@link CherryPickRequest}.
 *
 * @see LabCherryPick
 * @see CherryPickRequest
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
@Entity
@Table(uniqueConstraints={ @UniqueConstraint(columnNames={ "cherryPickRequestId", "screenedWellId" }) })
@org.hibernate.annotations.Proxy
@edu.harvard.med.screensaver.model.annotations.ContainedEntity(containingEntityClass=CherryPickRequest.class)
public class ScreenerCherryPick extends AbstractEntity<Integer>
{

  // private static data

  private static final Logger log = Logger.getLogger(ScreenerCherryPick.class);
  private static final long serialVersionUID = 0L;
  
  public static final RelationshipPath<ScreenerCherryPick> cherryPickRequest = RelationshipPath.from(ScreenerCherryPick.class).to("cherryPickRequest", Cardinality.TO_ONE);
  public static final RelationshipPath<ScreenerCherryPick> screenedWell = RelationshipPath.from(ScreenerCherryPick.class).to("screenedWell", Cardinality.TO_ONE);
  public static final RelationshipPath<ScreenerCherryPick> labCherryPicks = RelationshipPath.from(ScreenerCherryPick.class).to("labCherryPicks");


  // private instance data

  private Integer _version;
  private CherryPickRequest _cherryPickRequest;
  private Well _screenedWell;
  private Set<LabCherryPick> _labCherryPicks = new HashSet<LabCherryPick>();


  // public instance methods

  @Override
  public Object acceptVisitor(AbstractEntityVisitor visitor)
  {
    return visitor.visit(this);
  }

  @Override
  public boolean equals(Object obj)
  {
    return obj == this || (obj instanceof ScreenerCherryPick && obj.hashCode() == hashCode());
  }

  @Override
  public int hashCode()
  {
    return _cherryPickRequest.hashCode() * 7 + _screenedWell.hashCode() * 17;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "(" + _cherryPickRequest.toString() + ":" + _screenedWell.toString() + ")";
  }

  /**
   * Get the id for the screener cherry pick.
   * @return the id for the screener cherry pick
   */
  @Id
  @org.hibernate.annotations.GenericGenerator(
    name="screener_cherry_pick_id_seq",
    strategy="sequence",
    parameters = { @Parameter(name="sequence", value="screener_cherry_pick_id_seq") }
  )
  @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="screener_cherry_pick_id_seq")
  public Integer getScreenerCherryPickId()
  {
    return getEntityId();
  }

  /**
   * Get the cherry pick request.
   * @return the cherry pick request
   */
  @ManyToOne
  @JoinColumn(name="cherryPickRequestId", nullable=false, updatable=false)
  @org.hibernate.annotations.ForeignKey(name="fk_screener_cherry_pick_to_cherry_pick_request")
  public CherryPickRequest getCherryPickRequest()
  {
    return _cherryPickRequest;
  }

  /**
   * Get the screened library well for this cherry pick. The screened well
   * corresponds to a well that took part in the screen that generated this
   * cherry pick.  Screened wells are specified by the screener.
   * @return the screened well
   * @see LabCherryPick#getSourceWell()
   */
  @ManyToOne
  @JoinColumn(name="screenedWellId", nullable=false, updatable=false)
  @org.hibernate.annotations.ForeignKey(name="fk_screener_cherry_pick_to_screened_well")
  @edu.harvard.med.screensaver.model.annotations.ToOne(unidirectional = true)
  public Well getScreenedWell()
  {
    return _screenedWell;
  }

  /**
   * Get the set of lab cherry picks associated with this screener cherry pick.
   * @return the set of lab cherry picks associated with this screener cherry pick
   */
  @OneToMany(mappedBy = "screenerCherryPick", cascade = { CascadeType.ALL })
  public Set<LabCherryPick> getLabCherryPicks()
  {
    return _labCherryPicks;
  }


  // package instance methods

  /**
   * Construct an initialized <code>ScreenerCherryPick</code>. Intended only for use by {@link CherryPickRequest}.
   * @param cherryPickRequest the cherry pick request
   * @param screenedWell the screened well
   */
  ScreenerCherryPick(CherryPickRequest cherryPickRequest, Well screenedWell)
  {
    if (cherryPickRequest == null || screenedWell == null) {
      throw new NullPointerException();
    }
    // TODO: verify well was actually one that was screened
    _cherryPickRequest = cherryPickRequest;
    _screenedWell = screenedWell;
  }


  // protected constructor

  /**
   * Construct an uninitialized <code>ScreenerCherryPick</code>.
   * @motivation for hibernate and proxy/concrete subclass constructors
   */
  protected ScreenerCherryPick() {}


  // private instance methods

  /**
   * Set the id for the screener cherry pick.
   * @param screenerCherryPickId the new id for the screener cherry pick
   * @motivation for hibernate
   */
  private void setScreenerCherryPickId(Integer screenerCherryPickId)
  {
    setEntityId(screenerCherryPickId);
  }

  /**
   * Get the version for the screener cherry pick.
   * @return the version for the screener cherry pick
   * @motivation for hibernate
   */
  @Column(nullable=false)
  @Version
  private Integer getVersion()
  {
    return _version;
  }

  /**
   * Set the version for the screener cherry pick.
   * @param version the new version for the screener cherry pick
   * @motivation for hibernate
   */
  private void setVersion(Integer version)
  {
    _version = version;
  }

  /**
   * Set the cherry pick request.
   * @param cherryPickRequest the new cherry pick request
   * @motivation for hibernate
   */
  private void setCherryPickRequest(CherryPickRequest cherryPickRequest)
  {
    _cherryPickRequest = cherryPickRequest;
  }

  /**
   * Set the screened well.
   * @param screenedWell the new screened well
   * @motivation for hibernate
   */
  private void setScreenedWell(Well screenedWell)
  {
    _screenedWell = screenedWell;
  }

  /**
   * Set the set of lab cherry picks associated with this screener cherry pick.
   * @param labCherryPicks the new set of lab cherry picks
   * @motivation for hibernate
   */
  private void setLabCherryPicks(Set<LabCherryPick> labCherryPicks)
  {
    _labCherryPicks = labCherryPicks;
  }

  /**
   * Create and return a new lab cherry pick for the cherry pick request.
   * 
   * @throws DataModelViolationException whenever the cherry pick request for
   *           the provided screener cherry pick does not match the cherry pick
   *           request asked to create the lab cherry pick
   */
  public LabCherryPick createLabCherryPick(Well sourceWell)
  {
    LabCherryPick labCherryPick = new LabCherryPick(this, sourceWell);
    _cherryPickRequest.addLabCherryPick(labCherryPick);
    getLabCherryPicks().add(labCherryPick);
    return labCherryPick;
  }
}
