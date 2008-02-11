// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.harvard.med.screensaver.model.cherrypicks.CherryPickRequest;
import edu.harvard.med.screensaver.model.cherrypicks.LabCherryPick;
import edu.harvard.med.screensaver.model.libraries.Copy;
import edu.harvard.med.screensaver.model.libraries.Gene;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.LibraryType;
import edu.harvard.med.screensaver.model.libraries.SilencingReagent;
import edu.harvard.med.screensaver.model.libraries.SilencingReagentType;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.libraries.WellType;
import edu.harvard.med.screensaver.model.libraries.WellVolumeAdjustment;
import edu.harvard.med.screensaver.model.screens.ScreenType;
import edu.harvard.med.screensaver.ui.libraries.WellCopyVolume;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.TransientObjectException;
import org.springframework.orm.hibernate3.HibernateCallback;

public class LibrariesDAOImpl extends AbstractDAO implements LibrariesDAO
{
  // static members

  private static Logger log = Logger.getLogger(LibrariesDAOImpl.class);


  // instance data members

  private GenericEntityDAO _dao;


  // public constructors and methods

  /**
   * @motivation for CGLIB dynamic proxy creation
   */
  public LibrariesDAOImpl()
  {
  }

  public LibrariesDAOImpl(GenericEntityDAO dao)
  {
    _dao = dao;
  }

  public Well findWell(WellKey wellKey)
  {
    return _dao.findEntityById(Well.class, wellKey.getKey());
  }

  public Well findWell(WellKey wellKey, boolean loadContents)
  {
    return _dao.findEntityById(
      Well.class,
      wellKey.getKey(),
      false,
      "compounds",
      "silencingReagents.gene");
  }

  public List<String> findAllVendorNames()
  {
    String hql = "select distinct l.vendor from Library l where l.vendor is not null";
    @SuppressWarnings("unchecked")
    List<String> vendorNames = getHibernateTemplate().find(hql);
    return vendorNames;
  }

  public SilencingReagent findSilencingReagent(
    Gene gene,
    SilencingReagentType silencingReagentType,
    String sequence)
  {
    return _dao.findEntityById(SilencingReagent.class,
                               gene.toString() + ":" +
                               silencingReagentType.toString() + ":" +
                               sequence);
  }

  @SuppressWarnings("unchecked")
  public Library findLibraryWithPlate(Integer plateNumber)
  {
    String hql =
      "select library from Library library where " +
      plateNumber + " between library.startPlate and library.endPlate";
    List<Library> libraries = (List<Library>) getHibernateTemplate().find(hql);
    if (libraries.size() == 0) {
      return null;
    }
    return libraries.get(0);
  }

  @SuppressWarnings("unchecked")
  public boolean isPlateRangeAvailable(Integer startPlate, Integer endPlate)
  {
    if (startPlate <= 0 || endPlate <= 0) {
      return false;
    }
    // swap, if necessary
    if (startPlate > endPlate) {
      Integer tmp = endPlate;
      endPlate = startPlate;
      startPlate = tmp;
    }
    String hql =
      "from Library library where not" +
      "(library.startPlate > :endPlate or library.endPlate < :startPlate)";
    List<Library> libraries = (List<Library>)
    getHibernateTemplate().findByNamedParam(hql,
                                            new String[] {"startPlate", "endPlate"},
                                            new Integer[] {startPlate, endPlate});
    return libraries.size() == 0;
  }

  public void deleteLibraryContents(Library library)
  {
    for (Well well : library.getWells()) {
      if (well.getWellType().equals(WellType.EXPERIMENTAL)) {
        well.setGenbankAccessionNumber(null);
        well.setIccbNumber(null);
        well.setMolfile(null);
        well.setSmiles(null);
        well.removeCompounds();
        well.removeSilencingReagents();
        well.setWellType(WellType.EMPTY);
        well.setReagent(null); // do this after well type, exp well must have reagent!
      }
    }
    log.info("deleted library contents for " + library.getLibraryName());
  }

  @SuppressWarnings("unchecked")
  public Set<Well> findWellsForPlate(int plate)
  {
    return new TreeSet<Well>(getHibernateTemplate().find("from Well where plateNumber = ?", plate));
  }

  @SuppressWarnings("unchecked")
  public void loadOrCreateWellsForLibrary(Library library)
  {
    // Cases that must be handled:
    // 1. Library is transient (not in Hibernate session), and not in database 
    // 2. Library is managed (in Hibernate session), but not in database
    // 3. Library is managed (in Hibernate session), and in database

    if (library.getLibraryId() != null) { // case 2 or 3
      // reload library, fetching all wells; 
      // if library is already in session, we obtain that instance
      Library reloadedLibrary = _dao.reloadEntity(library, false, "wells");
      if (reloadedLibrary == null) { // case 2
        log.debug("library is Hibernate-managed, but not yet persisted in database");
        _dao.saveOrUpdateEntity(library);
      }
      else { // case 3
        log.debug("library is Hibernate-managed and persisted in database");
        // if provided Library is not same instance as the one in the session, this method cannot be called
        if (reloadedLibrary != library) {
          throw new IllegalArgumentException("provided Library instance is not the same as the one in the current Hibernate session; cannot load/create wells for that provided library");
        }           
      }
    }
    else { // case 1
      log.debug("library is transient");
    }
    
    // create wells for library, if needed
    if (library.getWells().size() == 0) {
      for (int iPlate = library.getStartPlate(); iPlate <= library.getEndPlate(); ++iPlate) {
        for (int iRow = 0; iRow < Well.PLATE_ROWS; ++iRow) {
          for (int iCol = 0; iCol < Well.PLATE_COLUMNS; ++iCol) {
            library.createWell(new WellKey(iPlate, iRow, iCol), WellType.EMPTY);
          }
        }
      }
      // persistEntity() call will place all wells in session *now*
      // (as opposed t saveOrUpdate(), which does upon session flush), so that
      // subsequent code can find them in the Hibernate session
      _dao.persistEntity(library);
      log.info("created wells for library " + library.getLibraryName());
    }

  }

  @SuppressWarnings("unchecked")
  public List<Library> findLibrariesOfType(final LibraryType[] libraryTypes,
                                           final ScreenType[] screenTypes)
  {
    return (List<Library>) getHibernateTemplate().executeFind(new HibernateCallback() {
      public Object doInHibernate(org.hibernate.Session session)
      throws org.hibernate.HibernateException, java.sql.SQLException
      {
        Query query = session.createQuery("from Library where libraryType in (:libraryTypes) and screenType in (:screenTypes)");
        query.setParameterList("libraryTypes", libraryTypes);
        query.setParameterList("screenTypes", screenTypes);
        return query.list();
      }
    });
  }

  public BigDecimal findRemainingVolumeInWellCopy(Well well, Copy copy)
  {
    String hql;

    hql = "select ci.microliterWellVolume from CopyInfo ci where ci.copy=? and ci.plateNumber=? and ci.dateRetired is null";
    List result = getHibernateTemplate().find(hql, new Object[] { copy, well.getPlateNumber() });
    if (result == null || result.size() == 0) {
      return BigDecimal.ZERO.setScale(Well.VOLUME_SCALE);
    }
    BigDecimal initialMicroliterVolume = (BigDecimal) result.get(0);

    hql = "select sum(wva.microliterVolume) from WellVolumeAdjustment wva where wva.copy=? and wva.well=?";
    BigDecimal deltaMicroliterVolume = (BigDecimal) getHibernateTemplate().find(hql, new Object[] { copy, well }).get(0);
    if (deltaMicroliterVolume == null) {
      deltaMicroliterVolume = BigDecimal.ZERO.setScale(Well.VOLUME_SCALE);
    }
    return initialMicroliterVolume.add(deltaMicroliterVolume).setScale(Well.VOLUME_SCALE);
  }

  @SuppressWarnings("unchecked")
  public Collection<WellCopyVolume> findWellCopyVolumes(Library libraryIn)
  {
    Library library = _dao.reloadEntity(libraryIn, true, "wells");
    _dao.needReadOnly(library, "copies.copyInfos");
    String hql = "from WellVolumeAdjustment wva where wva.copy.library = ?";
    List<WellVolumeAdjustment> wellVolumeAdjustments = getHibernateTemplate().find(hql, new Object[] { library });
    List<WellCopyVolume> result = new ArrayList<WellCopyVolume>();
    return aggregateWellVolumeAdjustments(makeEmptyWellVolumes(library, result), wellVolumeAdjustments);
  }

  @SuppressWarnings("unchecked")
  public Collection<WellCopyVolume> findWellCopyVolumes(Copy copy)
  {
    // TODO: eager fetch copies and wells
    String hql = "from WellVolumeAdjustment wva where wva.copy = ?";
    List<WellVolumeAdjustment> wellVolumeAdjustments = getHibernateTemplate().find(hql, new Object[] { copy });
    List<WellCopyVolume> result = new ArrayList<WellCopyVolume>();
    return aggregateWellVolumeAdjustments(makeEmptyWellVolumes(copy, result), wellVolumeAdjustments);
  }

  @SuppressWarnings("unchecked")
  public Collection<WellCopyVolume> findWellCopyVolumes(Copy copy, Integer plateNumber)
  {
    // TODO: eager fetch copies and wells
    String hql = "select wva from WellVolumeAdjustment wva join wva.copy c join c.copyInfos ci where wva.copy = ? and ci.plateNumber = ?";
    List<WellVolumeAdjustment> wellVolumeAdjustments = getHibernateTemplate().find(hql, new Object[] { copy, plateNumber });
    List<WellCopyVolume> result = new ArrayList<WellCopyVolume>();
    if (wellVolumeAdjustments.size() == 0) {
      return result;
    }
    return aggregateWellVolumeAdjustments(makeEmptyWellVolumes(copy, plateNumber, result), wellVolumeAdjustments);
  }

  @SuppressWarnings("unchecked")
  public Collection<WellCopyVolume> findWellCopyVolumes(Integer plateNumber)
  {
    // TODO: eager fetch copies and wells
    String hql = "select wva from WellVolumeAdjustment wva join wva.copy c join c.copyInfos ci where ci.plateNumber = ?";
    List<WellVolumeAdjustment> wellVolumeAdjustments = getHibernateTemplate().find(hql, new Object[] { plateNumber });
    List<WellCopyVolume> result = new ArrayList<WellCopyVolume>();
    if (wellVolumeAdjustments.size() == 0) {
      return result;
    }
    return aggregateWellVolumeAdjustments(makeEmptyWellVolumes(wellVolumeAdjustments.get(0).getCopy(), plateNumber, result), wellVolumeAdjustments);
  }

  @SuppressWarnings("unchecked")
  public Collection<WellCopyVolume> findWellCopyVolumes(WellKey wellKey)
  {
    String hql = "select distinct wva from WellVolumeAdjustment wva left join fetch wva.copy left join fetch wva.well w left join fetch w.library l left join fetch l.copies where w.id = ?";
    List<WellVolumeAdjustment> wellVolumeAdjustments = getHibernateTemplate().find(hql, new Object[] { wellKey.toString() });
    List<WellCopyVolume> result = new ArrayList<WellCopyVolume>();
    Well well = null;
    if (wellVolumeAdjustments.size() == 0) {
      well = findWell(wellKey);
      if (well == null) {
        // no such well
        return result;
      }
      // well exists, but just doesn't have any wellVolumeAdjustments (which is
      // valid); in this case we still want to return a collection that contains
      // an element for each copy of the well's library
    }
    else {
      // wel exists, and has wellVolumeAdjustments
      well = wellVolumeAdjustments.get(0).getWell();
    }
    return aggregateWellVolumeAdjustments(makeEmptyWellVolumes(well, result), wellVolumeAdjustments);
  }

  @SuppressWarnings("unchecked")
  public Collection<WellCopyVolume> findWellCopyVolumes(CherryPickRequest cherryPickRequest,
                                                        boolean forUnfufilledLabCherryPicksOnly)
  {
    cherryPickRequest = _dao.reloadEntity(cherryPickRequest,
                                          true,
                                          "labCherryPicks.sourceWell.library");
    _dao.needReadOnly(cherryPickRequest,
                      "labCherryPicks.wellVolumeAdjustments");
    if (forUnfufilledLabCherryPicksOnly) {
      // if filtering unfulfilled lab cherry picks, we need to fetch more relationships, to be efficient
      _dao.needReadOnly(cherryPickRequest,
                        "labCherryPicks.assayPlate.cherryPickLiquidTransfer");
    }
    List<WellVolumeAdjustment> wellVolumeAdjustments = new ArrayList<WellVolumeAdjustment>();
    for (LabCherryPick labCherryPick : cherryPickRequest.getLabCherryPicks()) {
      if (!forUnfufilledLabCherryPicksOnly || labCherryPick.isUnfulfilled()) {
        wellVolumeAdjustments.addAll(labCherryPick.getWellVolumeAdjustments());
      }
    }

    List<WellCopyVolume> emptyWellVolumes = makeEmptyWellVolumes(cherryPickRequest,
                                                                 new ArrayList<WellCopyVolume>(),
                                                                 forUnfufilledLabCherryPicksOnly);
    return aggregateWellVolumeAdjustments(emptyWellVolumes, wellVolumeAdjustments);
  }


  // private methods

  private List<WellCopyVolume> makeEmptyWellVolumes(Library library, List<WellCopyVolume> wellVolumes)
  {
    for (Copy copy : library.getCopies()) {
      makeEmptyWellVolumes(copy, wellVolumes);
    }
    return wellVolumes;
  }

  private List<WellCopyVolume> makeEmptyWellVolumes(Copy copy, List<WellCopyVolume> wellVolumes)
  {
    for (int plateNumber = copy.getLibrary().getStartPlate(); plateNumber <= copy.getLibrary().getEndPlate(); ++plateNumber) {
      makeEmptyWellVolumes(copy, plateNumber, wellVolumes);
    }
    return wellVolumes;
  }

  private List<WellCopyVolume> makeEmptyWellVolumes(Copy copy, int plateNumber, List<WellCopyVolume> wellVolumes)
  {
    for (int iRow = 0; iRow < Well.PLATE_ROWS; ++iRow) {
      for (int iCol = 0; iCol < Well.PLATE_COLUMNS; ++iCol) {
        wellVolumes.add(new WellCopyVolume(findWell(new WellKey(plateNumber, iRow, iCol)), copy));
      }
    }
    return wellVolumes;
  }

  private List<WellCopyVolume> makeEmptyWellVolumes(Well well, List<WellCopyVolume> result)
  {
    for (Copy copy : well.getLibrary().getCopies()) {
      result.add(new WellCopyVolume(well, copy));
    }
    return result;
  }

  private List<WellCopyVolume> makeEmptyWellVolumes(CherryPickRequest cherryPickRequest,
                                                    List<WellCopyVolume> result,
                                                    boolean forUnfufilledLabCherryPicksOnly)
  {
    for (LabCherryPick lcp : cherryPickRequest.getLabCherryPicks()) {
      if (!forUnfufilledLabCherryPicksOnly || lcp.isUnfulfilled()) {
        makeEmptyWellVolumes(lcp.getSourceWell(), result);
      }
    }
    return result;
  }

  private Collection<WellCopyVolume> aggregateWellVolumeAdjustments(List<WellCopyVolume> wellCopyVolumes,
                                                                    List<WellVolumeAdjustment> wellVolumeAdjustments)
  {
    Collections.sort(wellCopyVolumes, new Comparator<WellCopyVolume>() {
      public int compare(WellCopyVolume wcv1, WellCopyVolume wcv2)
      {
        int result = wcv1.getWell().compareTo(wcv2.getWell());
        if (result == 0) {
          result = wcv1.getCopy().getName().compareTo(wcv2.getCopy().getName());
        }
        return result;
      }
    });
    Collections.sort(wellVolumeAdjustments, new Comparator<WellVolumeAdjustment>() {
      public int compare(WellVolumeAdjustment wva1, WellVolumeAdjustment wva2)
      {
        int result = wva1.getWell().compareTo(wva2.getWell());
        if (result == 0) {
          result = wva1.getCopy().getName().compareTo(wva2.getCopy().getName());
        }
        return result;
      }
    });
    Iterator<WellCopyVolume> wcvIter = wellCopyVolumes.iterator();
    Iterator<WellVolumeAdjustment> wvaIter = wellVolumeAdjustments.iterator();
    if (wcvIter.hasNext()) {
      WellCopyVolume wellCopyVolume = wcvIter.next();
      while (wvaIter.hasNext()) {
        WellVolumeAdjustment wellVolumeAdjustment = wvaIter.next();
        while (!wellCopyVolume.getWell().equals(wellVolumeAdjustment.getWell()) ||
          !wellCopyVolume.getCopy().equals(wellVolumeAdjustment.getCopy())) {
          if (!wcvIter.hasNext()) {
            throw new IllegalArgumentException("wellVolumeAdjustments exist for wells that were not in wellCopyVolumes: " +
                                               wellVolumeAdjustment.getWell() + ":" + wellVolumeAdjustment.getCopy().getName());
          }
          wellCopyVolume = wcvIter.next();
        }
        wellCopyVolume.addWellVolumeAdjustment(wellVolumeAdjustment);
      }
    }
    return wellCopyVolumes;
  }
}