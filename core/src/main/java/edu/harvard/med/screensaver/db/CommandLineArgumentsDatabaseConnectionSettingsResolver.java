// $HeadURL$
// $Id$
//
// Copyright © 2010 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.db;

import org.apache.log4j.Logger;

import edu.harvard.med.screensaver.DatabaseConnectionSettings;

public class CommandLineArgumentsDatabaseConnectionSettingsResolver implements DatabaseConnectionSettingsResolver
{
  private static final Logger log = Logger.getLogger(CommandLineArgumentsDatabaseConnectionSettingsResolver.class);

  public static final String CMD_LINE_ARGS_DATABASE_CONNECTION_SETTINGS = "cmdline.args.database.connection.settings";

  @Override
  public DatabaseConnectionSettings resolve() throws DatabaseConnectionSettingsResolutionException
  {
    DatabaseConnectionSettings settings = (DatabaseConnectionSettings) System.getProperties().get(CMD_LINE_ARGS_DATABASE_CONNECTION_SETTINGS);
    if (settings == null) {
      log.warn("command line arguments do not contain database connection settings");
      return null;
    }
    return settings;
  }
}
