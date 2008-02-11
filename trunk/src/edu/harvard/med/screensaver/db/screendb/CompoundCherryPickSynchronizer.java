// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.db.screendb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.db.LibrariesDAO;
import edu.harvard.med.screensaver.model.DuplicateEntityException;
import edu.harvard.med.screensaver.model.cherrypicks.CherryPickAssayPlate;
import edu.harvard.med.screensaver.model.cherrypicks.CherryPickLiquidTransfer;
import edu.harvard.med.screensaver.model.cherrypicks.CherryPickRequest;
import edu.harvard.med.screensaver.model.cherrypicks.CompoundCherryPickRequest;
import edu.harvard.med.screensaver.model.cherrypicks.LabCherryPick;
import edu.harvard.med.screensaver.model.cherrypicks.LegacyCherryPickAssayPlate;
import edu.harvard.med.screensaver.model.cherrypicks.ScreenerCherryPick;
import edu.harvard.med.screensaver.model.libraries.Copy;
import edu.harvard.med.screensaver.model.libraries.CopyUsageType;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.PlateType;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.libraries.WellName;
import edu.harvard.med.screensaver.model.libraries.WellType;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.users.ScreeningRoomUser;

import org.apache.log4j.Logger;

public class CompoundCherryPickSynchronizer
{

  // static members

  private static Logger log = Logger.getLogger(CompoundCherryPickSynchronizer.class);


  // instance data members

  private Connection _connection;
  private GenericEntityDAO _dao;
  private LibrariesDAO _librariesDao;
  private UserSynchronizer _userSynchronizer;
  private ScreenSynchronizer _screenSynchronizer;


  // public constructors and methods

  public CompoundCherryPickSynchronizer(
    Connection connection,
    GenericEntityDAO dao,
    LibrariesDAO librariesDao,
    UserSynchronizer userSynchronizer,
    ScreenSynchronizer screenSynchronizer)
  {
    _connection = connection;
    _dao = dao;
    _librariesDao = librariesDao;
    _userSynchronizer = userSynchronizer;
    _screenSynchronizer = screenSynchronizer;
  }

  // WARNING: for efficiency, this method has the side-effect of
  // clearing the Hibernate session cache, so that any subsequent accesses
  // of previously loaded entities' uninitialized relationships will cause
  // LazyInitExceptions
  public void synchronizeCompoundCherryPicks() throws ScreenDBSynchronizationException
  {
    try {
      Statement statement = _connection.createStatement();
      final ResultSet resultSet = statement.executeQuery(
      "SELECT * FROM visits v WHERE visit_type = 'Cherry Pick'");
      while (resultSet.next()) {
        // clear Hibernate session to reclaim/conserve memory
        _dao.clear();

        final Integer visitId = resultSet.getInt("id");
        log.debug("synchronizing compound cherry pick visit " + visitId + "..");
        deleteOldCompoundCherryPickRequest(visitId);

        try {
          BigDecimal volumeTransferred = getVolumeTransferred(visitId);
          CompoundCherryPickRequest request =
            createCompoundCherryPickRequest(resultSet, volumeTransferred);
          CherryPickLiquidTransfer liquidTransfer =
            createCherryPickLiquidTransfer(resultSet, volumeTransferred, request);
          CherryPickAssayPlate assayPlate =
            createCherryPickAssayPlate(visitId, request, liquidTransfer);
          if (volumeTransferred != null) {
            // TODO: null volumeTransferreds tend to break the *CherryPick
            // classes. there are a slew of compound cherry picks in
            // ScreenDB with null volumes, ranging in visit dates from
            // 2001-12 to 2004-05. im not really hip to trying to fix
            // those data issues - probably completely impossible. in the
            // cases that i looked at, there are a lot of null fields and
            // the populated fields look like they are mis-populated, so i
            // am going to venture that the data is total crap anyway, and
            // no good reason to try to add them as cherry picks here. i
            // may change my mind in a couple of days and ask some people
            // on the team what they think of these old cherry picks with
            // null volumes (or volumes, as they say in screendb-land :)
            createCherryPicks(visitId, request, assayPlate);
          }
          _dao.saveOrUpdateEntity(request);
          // flush before clearing Hibernate session
          _dao.flush();
        }
        catch (SQLException e) {
          throw new ScreenDBSynchronizationException("Encountered an SQL exception while synchronizing library screenings: " +
                                                     e.getMessage(),
                                                     e);
        }
        log.debug("done synchronizing compound cherry pick visit " + visitId + ".");
      }
      statement.close();
    }
    catch (SQLException e) {
      throw new ScreenDBSynchronizationException("Encountered an SQL exception while synchronizing library screenings: " +
                                                 e.getMessage(),
                                                 e);
    }
  }


  // private instance methods

  private void deleteOldCompoundCherryPickRequest(final Integer visitId)
  {
    CompoundCherryPickRequest request = _dao.findEntityByProperty(CompoundCherryPickRequest.class,
                                                                  "legacyCherryPickRequestNumber",
                                                                  visitId);
    if (request != null) {
      // load data into session efficiently, to avoid N+1 select problem
      _dao.need(request, "screenerCherryPicks.rnaiKnockdownConfirmation");
      _dao.need(request, "labCherryPicks.wellVolumeAdjustments");
      _dao.need(request, "cherryPickAssayPlates");
      // TODO: was hoping this would coax Hibernate into performing 1-shot
      // delete on each collection, but not happening (probably because each
      // collections' entities have additional cascade-delete relationships)
//    request.getLabCherryPicks().clear();
//    request.getScreenerCherryPicks().clear();
//    request.getCherryPickAssayPlates().clear();
      _dao.deleteEntity(request);
      _dao.flush(); // delete now, to avoid Hibernate delete/insert ordering problems later on
      log.debug("deleted " + request);
    }
  }

  private void loadOrCreateWellsForCherryPicks(int visitId) throws SQLException
  {
    Set<Library> librariesToLoadOrCreateWellsFor = new HashSet<Library>();
    PreparedStatement statement =
      _connection.prepareStatement("SELECT DISTINCT plate FROM cherry_pick WHERE visit_id = ? AND plate IS NOT NULL ORDER BY plate");
    statement.setInt(1, visitId);
    ResultSet resultSet = statement.getResultSet();
    while (resultSet != null && resultSet.next()) {
      Integer sourcePlateNumber = resultSet.getInt("plate");
      Library library = _librariesDao.findLibraryWithPlate(sourcePlateNumber);
      assert(library != null);
      if (library != null) {
        librariesToLoadOrCreateWellsFor.add(library);
      }
      else {
        log.error("Library with plate " + sourcePlateNumber + " not found");
      }
    }
    for (Library library : librariesToLoadOrCreateWellsFor) {
      _librariesDao.loadOrCreateWellsForLibrary(library);
    }
  }

  /**
   * Get a volume transferred value for the cherry pick request. Return the ScreenDB value
   * cherry_pick.liq_volumn only if all the cherry picks for the visit have the same volume.
   * Otherwise, return null.
   *
   * TODO: ScreenDB visit 4602 has cherry picks with 3 different volumes. what to do in
   * this case? currently i just use a null volume everywhere. note this method could be
   * simpler (and less costly in db cycles) if i could assume that every cp for a single
   * visit had the same volume.
   */
  private BigDecimal getVolumeTransferred(Integer visitId) throws SQLException
  {
    BigDecimal volumeTransferred = null;
    PreparedStatement preparedStatement = _connection.prepareStatement(
      "SELECT DISTINCT(liq_volumn) FROM cherry_pick WHERE visit_id = ?\n" +
      "AND (SELECT COUNT(DISTINCT(liq_volumn)) FROM cherry_pick WHERE visit_id = ?) = 1");
    preparedStatement.setInt(1, visitId);
    preparedStatement.setInt(2, visitId);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      volumeTransferred = new BigDecimal(resultSet.getFloat("liq_volumn"));
      break;
    }
    preparedStatement.close();
    return volumeTransferred;
  }

  private CompoundCherryPickRequest createCompoundCherryPickRequest(ResultSet resultSet, BigDecimal volumeTransferred)
  throws SQLException
  {
    Screen screen = _screenSynchronizer.getScreenForScreenNumber(resultSet.getInt("screen_id"));
    screen = _dao.reloadEntity(screen);
    ScreeningRoomUser requestedBy =
      _userSynchronizer.getScreeningRoomUserForScreenDBUserId(resultSet.getInt("performed_by"));
    requestedBy = _dao.reloadEntity(requestedBy);
    Date dateRequested = resultSet.getDate("cherry_pick_request_date");
    if (dateRequested == null) {
      dateRequested = resultSet.getDate("date_of_visit");
    }
    Integer visitId = resultSet.getInt("id");
    CompoundCherryPickRequest request = (CompoundCherryPickRequest)
      screen.createCherryPickRequest(requestedBy, dateRequested, visitId);
    request.setMicroliterTransferVolumePerWellRequested(volumeTransferred);
    request.setMicroliterTransferVolumePerWellApproved(volumeTransferred);
    request.setComments(resultSet.getString("comments"));
    return request;
  }

  private CherryPickLiquidTransfer createCherryPickLiquidTransfer(
    ResultSet resultSet,
    BigDecimal volumeTransferred,
    CompoundCherryPickRequest request)
  throws SQLException
  {
    ScreeningRoomUser performedBy =
      _userSynchronizer.getScreeningRoomUserForScreenDBUserId(resultSet.getInt("performed_by"));
    performedBy = _dao.reloadEntity(performedBy);
    CherryPickLiquidTransfer liquidTransfer = request.getScreen().createCherryPickLiquidTransfer(
      performedBy,
      resultSet.getDate("date_created"),
      resultSet.getDate("date_of_visit"), // date_of_visit => CPLiquidTransfer.dateOfActivity
      request);
    liquidTransfer.setMicroliterVolumeTransferedPerWell(volumeTransferred);
    return liquidTransfer;
  }

  private CherryPickAssayPlate createCherryPickAssayPlate(
    Integer visitId,
    CompoundCherryPickRequest request,
    CherryPickLiquidTransfer liquidTransfer)
  throws SQLException
  {
    String filename = getCherryPickFilename(visitId);
    // TODO: is EPPENDORF the correct plate type here?
    LegacyCherryPickAssayPlate assayPlate = request.createLegacyCherryPickAssayPlate(1, 0, PlateType.EPPENDORF, filename);
    assayPlate.setCherryPickLiquidTransfer(liquidTransfer);
    return assayPlate;
  }

  private String getCherryPickFilename(Integer visitId) throws SQLException
  {
    String cherryPickFilename = null;
    PreparedStatement preparedStatement = _connection.prepareStatement(
      "SELECT filename FROM cherry_pick_file WHERE visit_id = ?");
    preparedStatement.setInt(1, visitId);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      cherryPickFilename = resultSet.getString("filename");
      break;
    }
    preparedStatement.close();
    return cherryPickFilename;
  }

  private void createCherryPicks(
    Integer visitId,
    CherryPickRequest request,
    CherryPickAssayPlate assayPlate)
  throws SQLException
  {
    loadOrCreateWellsForCherryPicks(visitId);

    PreparedStatement preparedStatement = _connection.prepareStatement(
      "SELECT * FROM cherry_pick WHERE visit_id = ?");
    preparedStatement.setInt(1, visitId);
    ResultSet resultSet = preparedStatement.executeQuery();
    while (resultSet.next()) {
      String sourceWellName = resultSet.getString("well");
      Integer sourcePlateNumber = resultSet.getInt("plate");
      String destinationWell = resultSet.getString("map");
      String copyName = resultSet.getString("copy");
      // TODO: need to capture ScreenDB comments here as well!

      // i can't do anything if the source well name is null or not valid
      if (sourceWellName == null || ! Well.isValidWellName(sourceWellName)) {
        continue;
      }

      Well sourceWell = _librariesDao.findWell(new WellKey(sourcePlateNumber, sourceWellName));
      if (sourceWell == null) {
        log.error(
          "couldn't find well with plate number " + sourcePlateNumber + " and well name " + sourceWellName);
        continue;
      }

      // this will produce errors when running against a newly initialized Screensaver database (ie without
      // loaded libraries), or with a Screensaver database that is not terribly recent. see class javadocs
      // for ScreenDBSynchronizer for details.
      if (! sourceWell.getWellType().equals(WellType.EXPERIMENTAL)) {
        log.error(
          "ScreenDB cherry pick for visit id " + visitId + " is against a non-experimental well: " + sourceWell + ". " +
          "One possible reason for this is that the library contents have not been loaded into Screensaver yet.");
      }

      try {
        ScreenerCherryPick screenerCherryPick = request.createScreenerCherryPick(sourceWell);
        LabCherryPick labCherryPick = request.createLabCherryPick(screenerCherryPick, sourceWell);

      labCherryPick.setAllocated(getSourceCopy(sourceWell.getLibrary(), copyName));

      // i cant map it if the destination well name is null or not valid
      if (destinationWell == null || ! Well.isValidWellName(destinationWell)) {
        continue;
      }

      WellName destinationWellName = new WellName(destinationWell);
      labCherryPick.setMapped(assayPlate,
        destinationWellName.getRowIndex(),
        destinationWellName.getColumnIndex());
    }
      catch (DuplicateEntityException e) {
        log.error("ignoring duplicate cherry pick: " + e.getMessage());
      }

    }
    preparedStatement.close();
  }

  private Copy getSourceCopy(Library sourceLibrary, String copyName)
  {
    String copyId = sourceLibrary.getShortName() + ":" + copyName;
    Copy sourceCopy = _dao.findEntityById(Copy.class, copyId);
    if (sourceCopy == null) {
      sourceCopy = sourceLibrary.createCopy(CopyUsageType.FOR_CHERRY_PICK_SCREENING, copyName);
      _dao.saveOrUpdateEntity(sourceCopy);
    }
    return sourceCopy;
  }
}