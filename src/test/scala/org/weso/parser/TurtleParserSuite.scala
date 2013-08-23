package org.weso.parser

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.combinator.lexical.Lexical
import scala.util.parsing.input.Positional

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import scala.io.Source

import org.weso.rdfNode._
import org.weso.rdfTriple._
import scala.util.parsing.input.CharArrayReader

class TurtleParserSuite 
	extends TurtleParser 
	with TestParser
	with FunSpec 
	with ShouldMatchers {

  describe("Turtle Parser") {

   describe("turtleDoc") {
     implicit val s = TurtleParserState.initial
     val p = turtleDoc
     
     val a01 = RDFTriple(BNodeId(0),RDFNode.rdftype,BNodeId(1))
     val a02 = RDFTriple(BNodeId(0),RDFNode.rdftype,BNodeId(2))
     val abc = RDFTriple(IRI("a"),IRI("b"),IRI("c"))
     
     shouldParseRDF(p," _:0 a _:1,_:2 .",List(a01,a02))
     shouldParseRDF(p,"_:0 a _:1,_:2 .",List(a01,a02))
     shouldParseRDF(p,"_:0 a _:1; a _:2 .",List(a01,a02))
     shouldParseRDF(p,"<a> <b> <c> .",List(abc))
     shouldParseRDF(p,"# Example \n <a> <b> <c> .",List(abc))
     shouldParseRDF(p,"# No triples ",List())
     shouldParseRDF(p,
         """|@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.""".stripMargin,
            List()
            )
     shouldParseRDF(p,
         """|@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            |""".stripMargin,
            List()
            )
     shouldParseRDF(p,
         """|@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            |@prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .
            |<s> <p> "123"^^xsd:string .""".stripMargin,
            List(RDFTriple(IRI("s"),
            			   IRI("p"),
            			   DatatypeLiteral("123",IRI("http://www.w3.org/2001/XMLSchema#string"))
                 ))
     )
   }
            
   describe("prefixId") {
     val p = prefixId
     shouldParseGeneric(p,
                    "@prefix a: <http://example.org/a#>",
                   ("a:",IRI("http://example.org/a#")))
     shouldParseGeneric(p,
                    "@prefix : <http://example.org/a#>",
                   (":",IRI("http://example.org/a#")))
     shouldParseGeneric(p,
                    "@prefix año: <http://example.org/a#>",
                   ("año:",IRI("http://example.org/a#")))
   }
    
   describe("SPARQLPrefix") {
     val p = SPARQLPrefix
     shouldParseGeneric(p,
                    "prefix a: <http://example.org/a#>",
                   ("a:",IRI("http://example.org/a#")))
     shouldParseGeneric(p,
                    "PREFIX : <http://example.org/a#>",
                   (":",IRI("http://example.org/a#")))
     shouldParseGeneric(p,
                    "Prefix año: <http://example.org/a#>",
                   ("año:",IRI("http://example.org/a#")))
   }

   describe("triples") {
     val state =
     	   TurtleParserState.initial.
     	     addPrefix("a:", IRI("http://example.org/a#")).
     	     addCurPredicate(IRI("a")).
     	     addCurSubject(IRI("a"))
     val p = triples(state)
     val a01 = RDFTriple(BNodeId(0),RDFNode.rdftype,BNodeId(1))
     val a02 = RDFTriple(BNodeId(0),RDFNode.rdftype,BNodeId(2))

     shouldParseRDF(p," ",List())
     shouldParseRDF(p,"_:0 a _:1,_:2",List(a01,a02))
     shouldParseRDF(p,"_:0 a _:1; a _:2",List(a01,a02))
   }

   describe("subjPredicateObjectList") {
     val state =
     	   TurtleParserState.initial.
     	     addPrefix("a:", IRI("http://example.org/a#")).
     	     addCurPredicate(IRI("a")).
     	     addCurSubject(IRI("a"))
     val p = subjPredicatesObjectList(state)
     val a12 = (RDFNode.rdftype,List(BNodeId(1),BNodeId(2)))
     val a34 = (RDFNode.rdftype,List(BNodeId(3),BNodeId(4)))
     shouldParseRDF(p,"_:0 a _:1,_:2 ; a _:3, _:4",(BNodeId(0),List(a12,a34)))
   }

   describe("predicateObjectList") {
     val state =
     	   TurtleParserState.initial.
     	     addPrefix("a:", IRI("http://example.org/a#")).
     	     addCurPredicate(IRI("a")).
     	     addCurSubject(IRI("a"))
     val p = predicateObjectList(state)
     val a01 = (RDFNode.rdftype,List(BNodeId(0),BNodeId(1)))
     val a12 = (RDFNode.rdftype,List(IntegerLiteral(1),IntegerLiteral(2)))
     val a23 = (RDFNode.rdftype,List(BNodeId(2),BNodeId(3)))
     val abac1 = (IRI("http://example.org/a#b"),List(IRI("http://example.org/a#c"),IntegerLiteral(1)))
     val result = List(a01,abac1)

     shouldParseRDF(p,"a 1,2",List(a12))
     shouldParseRDF(p,"a 1,2;",List(a12))
     shouldParseRDF(p,"a 1,2;;",List(a12))
     shouldParseRDF(p,"a _:1,_:2",List(a01))
     shouldParseRDF(p,"a _:1,_:2 ;",List(a01))
     shouldParseRDF(p,"a _:1,_:2 ; a _:3, _:4",List(a01,a23))
     shouldParseRDF(p,"a _:1,_:2 ; ; a _:3, _:4 ;",List(a01,a23))
     shouldParseRDF(p,"a _:1,_:2;a:b a:c, 1 ",result)
     shouldParseRDF(p,"""|a _:1,_:2 ;
    		 		         |a:b a:c, 1 """.stripMargin, result) 
     }

   
   describe("verbObjectList") {
     val state =
     	   TurtleParserState.initial.
     	     addPrefix("a:", IRI("http://example.org/a#")).
     	     addCurPredicate(IRI("a")).
     	     addCurSubject(IRI("a"))
     val p = verbObjectList(state)
     shouldParseRDF(p,"a _:1,_:2",(RDFNode.rdftype,List(BNodeId(0),BNodeId(1)))) 
     shouldParseRDF(p," a _:1, _:2",(RDFNode.rdftype,List(BNodeId(0),BNodeId(1)))) 
     shouldParseRDF(p,"a:b _:1,_:2",(IRI("http://example.org/a#b"),List(BNodeId(0),BNodeId(1)))) 
     }

   describe("objectList") {
     val state =
     	   TurtleParserState.initial.
     	     addPrefix("a:", IRI("http://example.org/a#")).
     	     addCurPredicate(IRI("a")).
     	     addCurSubject(IRI("a"))
     val p = objectList(state)
     shouldParseRDF(p,"_:1,_:2",List(BNodeId(0),BNodeId(1))) 
     shouldParseRDF(p,"_:1,_:2,_:1",List(BNodeId(0),BNodeId(1),BNodeId(0))) 
     shouldParseRDF(p,"_:1,a:b,_:1",List(BNodeId(0),IRI("http://example.org/a#b"),BNodeId(0))) 
     shouldParseRDF(p,"1,2,3",List(IntegerLiteral(1),IntegerLiteral(2),IntegerLiteral(3))) 
     shouldParseRDF(p,"1, 2, 3",List(IntegerLiteral(1),IntegerLiteral(2),IntegerLiteral(3))) 
     shouldParseRDF(p,"1 , 2 , 3 ",List(IntegerLiteral(1),IntegerLiteral(2),IntegerLiteral(3))) 
     shouldParseRDF(p,"1 , true , false ",List(IntegerLiteral(1),RDFNode.trueLiteral,RDFNode.falseLiteral)) 
     shouldParseRDF(p,"#Comment\n 1 , true # Other comment\n , false ",List(IntegerLiteral(1),RDFNode.trueLiteral,RDFNode.falseLiteral)) 
     shouldParseRDF(p,"_:1, a:b, _:1",List(BNodeId(0),IRI("http://example.org/a#b"),BNodeId(0))) 
     }
     
   describe("rdf_object") {
     val state =
     	   TurtleParserState.initial.addPrefix("a:", IRI("http://example.org/a#"))
     val p = rdf_object(state)
     shouldParseRDF(p,"\"Hi\"",StringLiteral("Hi")) 
     shouldParseRDF(p,"2",IntegerLiteral(2)) 
     shouldParseRDF(p,"2.3",DecimalLiteral(2.3)) 
     shouldParseRDF(p,"2.3e-1",DoubleLiteral(2.3e-1)) 
     shouldParseRDF(p,"-2.3e-1",DoubleLiteral(-2.3e-1)) 
     shouldParseRDF(p,"a:b",IRI("http://example.org/a#b")) 
     shouldParseRDF(p,"<http://example.org/a#b>",IRI("http://example.org/a#b")) 
     shouldParseRDF(p,"_:1",BNodeId(0)) 
     shouldParseRDF(p,"[]",BNodeId(0)) 
    }
   
   describe("literal") {
     val prefixMap =
           PrefixMap.addPrefix("a:",IRI("http://example.org/a#"))(
     	   PrefixMap.addPrefix(":",IRI("http://example.org#"))(
     	   PrefixMap.addPrefix("año:",IRI("http://example.org/año#"))(
     	   PrefixMap.empty)))
     val p = literal(prefixMap)
     shouldParseGeneric(p,"1.2",DecimalLiteral(1.2))
     shouldParseGeneric(p,"12",IntegerLiteral(12))
     shouldParseGeneric(p,"1.2e9",DoubleLiteral(1.2e09))
     shouldParseGeneric(p,"\"a\"^^a:b",DatatypeLiteral("a",IRI("http://example.org/a#b")))
     shouldParseGeneric(p,"\"a\"",StringLiteral("a"))
     shouldParseGeneric(p,"\"a\"@es",LangLiteral("a",Lang("es")))
   }

   
   describe("blankNodePropertyList") {
     
   }

   describe("collection") {
     
   }

   describe("NumericLiteral") {
     val p = NumericLiteral
     shouldParseGeneric(p,"1.2",DecimalLiteral(1.2))
     shouldParseGeneric(p,"12",IntegerLiteral(12))
     shouldParseGeneric(p,"1.2e9",DoubleLiteral(1.2e09))
     shouldParseGeneric(p,"-1.2e9",DoubleLiteral(-1.2e09))
   }

   describe("RDFLiteral") {
     val prefixMap =
           PrefixMap.addPrefix("a:",IRI("http://example.org/a#"))(
     	   PrefixMap.addPrefix(":",IRI("http://example.org#"))(
     	   PrefixMap.addPrefix("año:",IRI("http://example.org/año#"))(
     	   PrefixMap.empty)))
     val p = RDFLiteral(prefixMap)
	 shouldParseGeneric(p,"\"123\"^^a:integer",DatatypeLiteral("123",IRI("http://example.org/a#integer")))
	 shouldParseGeneric(p,"\"123\"^^<http://example.org/a#integer>",DatatypeLiteral("123",IRI("http://example.org/a#integer")))
	 shouldParseGeneric(p,"\"\"\"John Doe\"\"\"",StringLiteral("John Doe"))
	 shouldParseGeneric(p,"\"one\"@es",LangLiteral("one",Lang("es")))
	 shouldParseGeneric(p,"\"one\"",StringLiteral("one"))
	 shouldParseGeneric(p,"\"one two\"",StringLiteral("one two"))
	 shouldParseGeneric(p,"\"\"\"John \nDoe\"\"\"",StringLiteral("John \nDoe"))
	 shouldParseGeneric(p,"\'\'\'John \nDoe\'\'\'",StringLiteral("John \nDoe"))
	 shouldNotParse(p,".")
   }

   describe("BooleanLiteral") {
     val p = BooleanLiteral
     shouldParseGeneric(p,"true",RDFNode.trueLiteral)
	 shouldParseGeneric(p,"false",RDFNode.falseLiteral)
	 shouldNotParse(p,"tres")
	 shouldNotParse(p,"one")
   }

	 
	 describe("string") {
     val p = string
     shouldParseGeneric(p,"\"hi\"","hi")
	 shouldParseGeneric(p,"'Hi'","Hi")
	 shouldParseGeneric(p,"'''Hi'John'''","Hi\'John")
	 shouldNotParse(p,"3.2")
   }

   describe("iri") {
     val prefixMap =
           PrefixMap.addPrefix("a:",IRI("http://example.org/a#"))(
     	   PrefixMap.addPrefix(":",IRI("http://example.org#"))(
     	   PrefixMap.addPrefix("año:",IRI("http://example.org/año#"))(
     	   PrefixMap.empty)))
     val p = iri(prefixMap)
     shouldParseGeneric(p,":a",IRI("http://example.org#a"))
	 shouldParseGeneric(p,"a:b",IRI("http://example.org/a#b"))
	 shouldParseGeneric(p,"a:",IRI("http://example.org/a#"))
	 shouldParseGeneric(p,"<http://a.com>",IRI("http://a.com"))
	 shouldNotParse(p,"3.2")
   }

   describe("PrefixedName") {
     val prefixMap =
           PrefixMap.addPrefix("a:",IRI("http://example.org/a#"))(
     	   PrefixMap.addPrefix(":",IRI("http://example.org#"))(
     	   PrefixMap.addPrefix("año:",IRI("http://example.org/año#"))(
     	   PrefixMap.empty)))     
     val p = PrefixedName(prefixMap)
     shouldParseGeneric(p,":a",IRI("http://example.org#a"))
	 shouldParseGeneric(p,"a:b",IRI("http://example.org/a#b"))
	 shouldParseGeneric(p,"a:",IRI("http://example.org/a#"))
	 shouldNotParse(p,"<a>")
   } 

   describe("BlankNode") {
     val bNodeTable = BNodeTable.empty
     val table1 = bNodeTable.getOrAddBNode("1")
     val tableA = bNodeTable.getOrAddBNode("a")
     val tableAnon = bNodeTable.newBNode
     val p = BlankNode(bNodeTable)
     shouldParseRDF(p,"_:1",BNodeId(0))
	 shouldParseRDF(p,"_:a",BNodeId(0))
	 shouldParseRDF(p,"[]",BNodeId(0))
	 shouldNotParse(p,"<a>")
   }

   /** 
    *  This is the same as shouldParseGeneric but 
    *  ignores the state
    */
    def shouldParseRDF[A](p:Parser[A], s : String, a : A) {
    it("Should parse \"" + s + "\"" + " and return " + a.toString) {
      val result = parseAll(p,s) match {
        case Success((x,_),_) => x 
        case NoSuccess(msg,_) => fail(msg)
      }
      result should be(a)
    }
   }

 }
}