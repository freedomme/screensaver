package edu.harvard.med.iccbl.platereader.parser;

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader
import org.apache.commons.io.IOUtils
import edu.harvard.med.iccbl.platereader.PlateMatrix
import java.io.StringReader
import scala.util.parsing.input.Reader

class TabDelimitedPlateReaderRawDataParser(rows: Int, cols: Int) extends RegexParsers with JavaTokenParsers with PlateReaderRawDataParser {

  class MetaDataLineParser extends Parser[String] {
    type Elem = Char

    // note: '(?m)' sets MULTILINE regex option, allowing the '^' and '$' to match after/before any type of newline ("\n", "\r\n", etc.)
    def metaDataLine: Parser[String] = """(?m)^.*$""".r

    def apply(in: Input) =
      if (parse(plateColumnHeaders, in).successful) {
        Failure("at plate column headers line, so not a meta data line", in)
      } else {
        parse(metaDataLine, in)
      }
  }

  def plateMatrices: Parser[List[PlateMatrix]] = rep(plateRecord) <~ opt(postscript)
  def postscript: Parser[Any] = metadata
  def plateRecord: Parser[PlateMatrix] = metadata ~> plateMatrix
  def metadata: Parser[Any] = rep(new MetaDataLineParser())
  def plateMatrix: Parser[PlateMatrix] = plateColumnHeaders ~> plateDataRows ^^ { new PlateMatrix(_) }
  def plateColumnHeaders: Parser[Any] = repN(cols, columnHeaderLabel)
  def columnHeaderLabel: Parser[Any] = wholeNumber
  def plateDataRows: Parser[List[List[BigDecimal]]] = repN(rows, plateDataRow)
  def plateDataRow: Parser[List[BigDecimal]] = rowHeaderLabel ~> repN(cols, datum)
  def rowHeaderLabel: Parser[Any] = "[A-Z]".r
  def datum: Parser[BigDecimal] = floatingPointNumber ^^ { BigDecimal(_) }

  def apply(in: Input) = parseAll(plateMatrices, EnvisionRawDataCleaner.clean(in))

  class ParseException(result: ParseResult[Any]) extends RuntimeException

  import collection.JavaConversions._

  def parse(in: java.io.Reader): java.util.List[PlateMatrix] = {
    // TODO: streamed reader
    //    class JavaReaderAdapter extends PagedSeqReader
    val s = IOUtils.toString(in)
    val result = this(new CharSequenceReader(s))
    if (result.successful) result.get
    else throw new RuntimeException(result.toString)
  }
}

/** For Envision plate reader, eliminate the postscript, as it contains a dummy plate matrix that otherwise confuses the parser */
object EnvisionRawDataCleaner extends RawDataCleaner {
  val postscriptPrefix = "Basic assay information";
  def clean(in: Reader[Char]) = {
    val s = in.source.toString
    if (s.contains(postscriptPrefix))
      new CharSequenceReader(s.substring(0, s.indexOf(postscriptPrefix)), 0)
    else
      in
  }
}