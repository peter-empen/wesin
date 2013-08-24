package org.weso.rdfTriple.jenaMapper

import org.weso.rdfTriple.RDFTriple
import com.hp.hpl.jena.rdf.model.{Model => JenaModel}
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.{RDFNode => JenaRDFNode}
import org.weso.rdfNode.RDFNode
import com.hp.hpl.jena.rdf.model.Property
import com.hp.hpl.jena.rdf.model.Resource
import org.weso.rdfNode._
import com.hp.hpl.jena.rdf.model.AnonId
import com.hp.hpl.jena.datatypes.BaseDatatype
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.rdf.model.Model

trait JenaMapper {

  def RDFTriples2Model(triples: List[RDFTriple]) : JenaModel = {
    val m = ModelFactory.createDefaultModel()
    for (t <- triples) {
     val subj = createResource(m,t.subj)
     val pred = createProperty(m,t.pred)
     val obj  = createRDFNode(m,t.obj)
     val stmt = m.createStatement(subj, pred, obj)
     m.add(stmt)
    }
    m
  }

  def createResource(m:JenaModel,node:RDFNode) : Resource = {
    node match {
      case BNodeId(id) => m.createResource(new AnonId(id.toString))
      case IRI(str) => m.createResource(str)
      case _ => throw new Exception("Cannot create a resource from " + node)
    }
  }

  def createRDFNode(m:JenaModel,node:RDFNode) : JenaRDFNode = {
   node match {
     case BNodeId(id) 						 => 
       	m.createResource(new AnonId(id.toString))
     case IRI(str) 							 => 
       	m.createResource(str)
     case StringLiteral(str) 				 => 
       	m.createLiteral(str,false)
     case DatatypeLiteral(str,IRI(datatype)) => 
       	m.createTypedLiteral(str,new BaseDatatype(datatype))
     case DecimalLiteral(d) 		=> 
     	m.createTypedLiteral(d.toString,XSDDatatype.XSDdecimal)
     case IntegerLiteral(i) 		=> 
       	m.createTypedLiteral(i.toString,XSDDatatype.XSDinteger)
     case LangLiteral(l,Lang(lang)) => m.createLiteral(l,lang)
     case BooleanLiteral(b) => 
        m.createTypedLiteral(b.toString,XSDDatatype.XSDboolean)
     case DoubleLiteral(d: Double) => 
        m.createTypedLiteral(d.toString,XSDDatatype.XSDdouble)
     case _ => 
       throw new Exception("Cannot create a resource from " + node)
   }  
  }

  def createProperty(m:Model,pred:IRI) : Property = {
    m.createProperty(pred.str)
  }
  
}