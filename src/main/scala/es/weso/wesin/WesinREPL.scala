package es.weso.wesin;
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ILoop
 

/** REPL for Wesin
 *  TODO: finnish it
 * */

object TestConsole extends App {
  val settings = new Settings
//  settings.usejavacp.value = true
  settings.deprecation.value = true
 
  new SampleILoop().process(settings)
}
 
class SampleILoop extends ILoop {
  override def prompt = "==> "
 
  addThunk {
    intp.beQuietDuring {
      intp.addImports("java.lang.Math._")
    }
  }
 
  override def printWelcome() {
    echo("\n" + " Wellcome to a simple REPL ")
  }
 
}