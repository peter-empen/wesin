package org.weso.parser

import util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.combinator.{Parsers, RegexParsers}
import scala.util.parsing.combinator.lexical.Lexical
import scala.util.parsing.input.Positional
import scala.util.parsing.input._
import util.parsing.input.CharSequenceReader.EofCh
import org.weso.rdfNode._
import org.weso.rdfTriple._
import scala.util.parsing.combinator.PackratParsers
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StdTokenParsers
import scala.io.Codec
import scala.util.matching.Regex

trait TurtleParser extends Positional with RegexParsers {

  override val skipWhitespace = false

//  lazy val turtleDoc = PN_PREFIX

  def RDFLiteral(prefixMap: PrefixMap) = string ~ opt(LANGTAG | "^^" ~> iri(prefixMap)) ^^ {
    case str ~ None => StringLiteral(str) 
    case str ~ Some(Lang(l)) => LangLiteral(str,Lang(l)) 
    case str ~ Some(IRI(iri)) => DatatypeLiteral(str,IRI(iri))
  }
  
  lazy val BooleanLiteral : Parser[Literal] = 
  		( "true"  ^^ { _ => RDFNode.trueLiteral  } 
  		| "false" ^^ { _ => RDFNode.falseLiteral }
  		)
  		
  lazy val string : Parser[String] = 
    	STRING_LITERAL_LONG_QUOTE | STRING_LITERAL_LONG_SINGLE_QUOTE | 
  		STRING_LITERAL_QUOTE | STRING_LITERAL_SINGLE_QUOTE 
  					
  def iri(prefixMap: PrefixMap) = 
    	( IRIREF   
  		| PrefixedName(prefixMap) 
  		| failure("iri expected")
  		)
  		
  def PrefixedName(prefixMap: PrefixMap) : Parser[IRI] = 
      ( PNAME_LN(prefixMap) 
      | PNAME_NS(prefixMap) 
      )
      
  def BlankNode(bNodeTable: BNodeTable) : Parser[(BNodeId,BNodeTable)] = 
    	( BLANK_NODE_LABEL(bNodeTable) 
    	| ANON(bNodeTable) 
    	| failure("Blank Node expected")
    	)
  
  lazy val IRIREF_STR  = "<([^\\u0000-\\u0020<>\\\\\"{}\\|\\^`\\\\]|" + UCHAR + ")*>"
  lazy val IRIREF : Parser[IRI] = {
    IRIREF_STR.r ^^ {
      case x => val rex = "<(.*)>".r
                val rex(cleanIRI) = x  // removes < and >
                IRI(cleanIRI)
    }
  }

  def PNAME_NS_STR	= "(" + PN_PREFIX + ")?:"

  def PNAME_NS(prefixMap: PrefixMap): Parser[IRI] = {
   PNAME_NS_STR.r ^? 
        ({ case prefix 
  		    if (prefixMap.contains(prefix)) => { 
  		  	    prefixMap.getIRI(prefix).get
  		    }
  		 },
  		 { case prefix => 
  		   "Prefix " + prefix + 
  		   " not found in Namespce map: " + 
  		   prefixMap
  		 })
  }
  
  lazy val PNAME_LN_STR = PNAME_NS_STR + PN_LOCAL
  
  def PNAME_LN (prefixMap: PrefixMap): Parser[IRI]	= {
   PNAME_NS(prefixMap) ~ PN_LOCAL.r ^^
  		{ case prefix ~ local => 
  		  	{
  		  	  println("Prefix: " + prefix + ". Local: " + local)
  		  	  RDFNode.qNameIRI(prefix, unscapeReservedChars(local))
  		  	}
  		}
  }
  
  lazy val BLANK_NODE_LABEL_STR = "_:(" + PN_CHARS_U + "|[0-9])(("+ PN_CHARS + "|\\.)*" + PN_CHARS + ")?"
  
  def BLANK_NODE_LABEL(bNodeTable:BNodeTable) : Parser[(BNodeId,BNodeTable)] = 
    	BLANK_NODE_LABEL_STR.r ^^ { 
    	  s => bNodeTable.getOrAddBNode(s) 
      	}
  
  lazy val LANGTAG		= "@" ~> "[a-zA-Z]+(-[a-zA-Z0-9]+)*".r ^^ Lang 

  lazy val INTEGER: Parser[Literal]  = "[+-]?[0-9]+".r ^^ 
  				{ x => IntegerLiteral(str2Int(x)) }
  
  lazy val DECIMAL: Parser[Literal]	 = "[+-]?[0-9]*\\.[0-9]+".r ^^ 
		  		{ x => DecimalLiteral(str2Decimal(x))}

  lazy val DOUBLE : Parser[Literal]   = 
		  ("[+-]?([0-9]+\\.[0-9]*" + EXPONENT + "|\\.[0-9]+" + EXPONENT + "|[0-9]+" + EXPONENT + ")").r ^^ 
		  		{ x => DoubleLiteral(str2Double(x)) }
  
  lazy val EXPONENT		= "[eE][+-]?[0-9]+"

  lazy val STRING_LITERAL_QUOTE_STR : String    = "\"([^\\u0022\\u005C\\u000A\\u000D]|" + ECHAR + "|" + UCHAR + ")*\""
  lazy val STRING_LITERAL_SINGLE_QUOTE_STR      = "'([^\\u0027\\u005C\\u000A\\u000D]|" + ECHAR + "|" + UCHAR + ")*'"
  lazy val STRING_LITERAL_LONG_SINGLE_QUOTE_STR = "'''(('|'')?[^']|" + ECHAR + "|" + UCHAR + ")*'''"
  lazy val STRING_LITERAL_LONG_QUOTE_STR		= "\"\"\"((\"|\"\")?[^\"]|" + ECHAR + "|" + UCHAR + ")*\"\"\""
  
  
  lazy val STRING_LITERAL_QUOTE : Parser[String] = STRING_LITERAL_QUOTE_STR.r  ^^ {
   x => removeQuotes(unscape(x),"\"",1)
  }

  lazy val STRING_LITERAL_SINGLE_QUOTE 			 = STRING_LITERAL_SINGLE_QUOTE_STR.r ^^ {
   x => removeQuotes(unscape(x),"\'",1)
  }
  lazy val STRING_LITERAL_LONG_SINGLE_QUOTE      = STRING_LITERAL_LONG_SINGLE_QUOTE_STR.r ^^ 
    { x => removeQuotes(unscape(x),"\'",3) }
  lazy val STRING_LITERAL_LONG_QUOTE 	         = STRING_LITERAL_LONG_QUOTE_STR.r ^^ 
    { x => removeQuotes(unscape(x),"\"",3) }
  
  def removeQuotes(s : String, quote: String, number: Int) : String = {
    val rex = ("(?s)"+quote + "{" + number.toString + "}(.*)" + quote + "{"+number.toString + "}").r
    val rex(newS) = s
    newS 
  }

  lazy val UCHAR_Parser : Parser[Char] = UCHAR.r ^^ { x => UCHAR2char(x) }

  lazy val UCHAR 		= "\\\\u" + HEX + HEX + HEX + HEX + "|" + "\\\\U" + HEX + HEX + HEX + HEX + HEX + HEX
  
  lazy val ECHAR_Parser : Parser[Char] = ECHAR.r ^^ { x => ECHAR2char(x) }
  lazy val ECHAR 		= "\\\\[tbnrf\"]" 
  lazy val WS 			= """\u0020|\u0009|\u000D|\u000A"""

  lazy val ANON_STR = "\\[(" + WS + ")*\\]"  

  def ANON(bNodeTable: BNodeTable) : Parser[(BNodeId,BNodeTable)] = 
    ANON_STR.r ^^ { _ => bNodeTable.newBNode 
    } 

  lazy val PN_CHARS_BASE_Parser : Parser[Char] = PN_CHARS_BASE.r ^^ { x => str2Char(x) }

  lazy val PN_CHARS_BASE =
 	"""[a-zA-Z\u00C0-\u00D6\u00D8-\u00F6""" +
 	"""\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF""" +
 	"""\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF""" +
 	"""\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD""" + 
   	"""\x{10000}-\x{EFFFF}]""" 

 lazy val PN_CHARS_U    = PN_CHARS_BASE + "|_"
 
 lazy val PN_CHARS		= PN_CHARS_U + """|\-|[0-9]|\u00B7|[\u0300-\u036F]|[\u203F-\u2040]""" 
 
 lazy val PLX			= PERCENT + "|" + PN_LOCAL_ESC
 
 lazy val PN_PREFIX     = PN_CHARS_BASE + "((" + PN_CHARS + "|\\.)*" + PN_CHARS + ")?" 
 
 lazy val PN_LOCAL		= "(" + PN_CHARS_U + "|:|[0-9]|" + PLX + ")((" + PN_CHARS + "|\\.|:|" + PLX + ")*(" + PN_CHARS + "|:|" + PLX + "))?"     
 
 lazy val PERCENT 		= "%" + HEX + HEX 
 
 lazy val HEX 			= """[0-9A-Fa-f]"""  
 
 lazy val PN_LOCAL_ESC 	= """[\\][_~\.\-!$&'\(\)*+,;=/?#@%]"""
 

 def UCHAR2char (uchar:String) : Char = {
    val rex = """\\[uU](.*)""".r
    uchar match {
      case rex(x) => Integer.parseInt(x,16).toChar
      case _ => throw new Exception("Internal Error: cannot convert uchar " + uchar + " to " + rex.pattern)
    }
 }
  
 def ECHAR2char(echar:String) : Char = {
    echar match {
      case "\\t" => '\t'
      case "\\b" => '\b' 
      case "\\n" => '\n'
      case "\\r" => '\r'
      case "\\f" => '\f'
      case "\\\"" => '\"'
      case _ => throw new Exception("Internal Error: cannot convert ECHAR " + echar + " to character")
    }
 }

  
 def str2Double(s: String) : Double = s.toDouble 
 def str2Decimal(s: String) : BigDecimal = BigDecimal(s)
 def str2Int(s: String) : Integer = Integer.parseInt(s)

 // The following code does the unscape traversing the list recursively
 def str2Char(str:String) : Char = {
    str.charAt(0)
 }

 def hex2Char (s : List[Char]) : Char= {
   try {
      Integer.parseInt(s.mkString,16).toChar
   } catch {
     case e : Throwable => throw new 
    		 Exception("Internal Error 'hex2Char': cannot convert from unicode chars. Value: " + 
    				 s.mkString + "\n " + "Exception raised: " + e.toString)
   }
 }

 def unscape(s:String) : String = {
   unscapeList(s.toList).mkString
 }
 
 def unscapeList(s:List[Char]) : List[Char] = {
   s match { 
     case '\\' :: 'u' :: a :: b :: c :: d :: rs => hex2Char(a :: b :: c :: d :: Nil) :: unscapeList(rs)
     case '\\' :: 'U' :: a :: b :: c :: d :: e :: f :: rs => hex2Char(a :: b :: c :: d :: e :: f :: Nil) :: unscapeList(rs)
     case '\\' :: 't' :: rs => '\t' :: unscapeList(rs)
     case '\\' :: 'b' :: rs => '\b' :: unscapeList(rs)
     case '\\' :: 'n' :: rs => '\n' :: unscapeList(rs)
     case '\\' :: 'r' :: rs => '\r' :: unscapeList(rs)
     case '\\' :: 'f' :: rs => '\f' :: unscapeList(rs)
     case '\\' :: '\"' :: rs => '\"' :: unscapeList(rs)
     case '\\' :: '\'' :: rs => '\'' :: unscapeList(rs)
     case '\\' :: '\\' :: rs => '\\' :: unscapeList(rs)
     case c :: rs => c :: unscapeList(rs)
     case Nil => Nil
   }
 }

 def unscapeReservedChars(s:String) : String = 
   unscapeReservedCharsList(s.toList).mkString
   
 def unscapeReservedCharsList(s:List[Char]) : List[Char] = {
   s match {
     case '\\' :: c :: rs if "~.-!$&'()*+,;=/?#@%_".contains(c) => c :: unscapeReservedCharsList(rs)
     case c :: rs => c :: unscapeReservedCharsList(rs)
     case Nil => Nil
   }
 }
 // Alternative way to unscape using regular expressions....
 def hex2str(s : String) : String = {
   Integer.parseInt(s.mkString,16).toChar.toString
 }

 def unscapeUnicode4(s: String): String = {
    val rex = """\\u(\p{XDigit}{4})""".r
    rex.replaceAllIn(s, m => Regex quoteReplacement hex2str(m group 1))
 }
 
 def unscapeUnicode6(s: String): String = {
    val rex = """\\U(\p{XDigit}{6})""".r
    rex.replaceAllIn(s, m => Regex quoteReplacement hex2str(m group 1))
 }

 def unscapeCtrl(s:String) : String = {
    val ctrl = """\\[bnrtf\\"]""".r
    ctrl.replaceAllIn(s, m => Regex quoteReplacement ctrl2str(m group 1))
 }

 def ctrl2str(s : String) : String = {
  s match {
    case "\\b" => "\b"
    case "\\t" => "\t"
    case "\\n" => "\n"
    case "\\r" => "\r"
    case "\\f" => "\f"
    case "\\\"" => "\""
    case "\\\'" => "\'"
    case "\\\\" => "\\"
    case s => s      
  }
 }

 def unscape2(x:String) : String = 
     (unscapeUnicode4 _ andThen unscapeUnicode6 andThen unscapeCtrl)(x)
 //------------------------------------
}
 
object TurtleParser extends TurtleParser 

