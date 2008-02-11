// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.util;

import edu.harvard.med.screensaver.model.screens.StatusValue;

public class StatusValueConverter extends VocabularlyConverter<StatusValue>
{
  public StatusValueConverter()
  {
    super(StatusValue.values());
  }
}