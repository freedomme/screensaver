// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.arch.util.converter;

import edu.harvard.med.screensaver.db.Criterion.Operator;

public class OperatorConverter extends VocabularyConverter<Operator>
{
  public OperatorConverter()
  {
    super(Operator.ALL_OPERATORS);
  }
}
