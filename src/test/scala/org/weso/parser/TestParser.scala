package org.weso.parser

import scala.util.parsing.combinator.RegexParsers
import org.weso.rdfNode.BNodeId
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec

trait TestParser 
	extends RegexParsers 
	with FunSpec 
	with ShouldMatchers {

  def shouldParse(p:Parser[String], s : String) {
     shouldParseGeneric(p,s,s)
   }

   // Only checks if parser succeeds
   def shouldParseGen[A](p:Parser[A], s : String) {
    it("Should parse \"" + s + "\"") {
      val result = parseAll(p,s) match {
        case Success(x,_) => true 
        case NoSuccess(msg,_) => fail(msg)
      }
    }
   }

    def shouldParseGeneric[A](p:Parser[A], s : String, a : A) {
    it("Should parse \"" + s + "\"" + " and return " + a.toString) {
      val result = parseAll(p,s) match {
        case Success(x,_) => x 
        case NoSuccess(msg,_) => fail(msg)
      }
      result should be(a)
    }
   }
 
    def shouldNotParse[A](p:Parser[A], s : String) {
    it("Should not parse \"" + s + "\"") {
      val result = parseAll(p,s) match {
        case Success(x,_) => fail("Should not parse " + s + ", but parsed value " + x) 
        case NoSuccess(msg,_) => success(msg)
      }
    }
   } 
    
   /** 
    *  Checks if parser succeeds
    *  @param testName name of the test
    *  @param p parser
    *  @param s input string to parse
    *  
    */
   def shouldParseNamed[A](testName: String, 
		   				   p:Parser[A], 
		   				   s : String) {
    it("Should parse " + testName) {
      val result = parseAll(p,s) match {
        case Success(x,_) => true 
        case NoSuccess(msg,_) => 
          	fail(msg + "\n" + s + 
        	     "\n-----------------\n")
      }
    }
   }

}