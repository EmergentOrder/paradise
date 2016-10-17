package org.scalameta.tests

import scala.util.Try

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._

import org.scalameta.paradise.converters.ConvertException

class PropertySuite extends ConverterSuite {
  override val compilationUnit: Boolean = true

  def err2message(e: Throwable): String = {
    val details =
      e match {
        case e: ConvertException if e.getMessage.startsWith("unsupported") =>
          val culprit = e.culprit.toString.lines.take(1).mkString
          s"${e.culprit.getClass.getSimpleName}"
        case e => e.getMessage.lines.take(1).toSeq.mkString
      }
    s"${e.getClass.getSimpleName}: $details"
  }

  val files   = ScalaFile.getAll
  val counter = new AtomicInteger()
  val results = new java.util.concurrent.CopyOnWriteArrayList[String]()

  test("basic") {
    files.toArray.par.foreach { file =>
      val code = file.read
      if (code.nonEmpty && !ScalacParser.checkParseFails(code)) {
        val n = counter.incrementAndGet()
        if (n % 1000 == 0) println(s"$n...")
        Try(getConvertedMetaTree(code)) match {
          // Uncomment to investigate a specific error further.
//          case scala.util.Failure(e: ConvertException)
//              if e.culprit.getClass.getSimpleName == "EmptyTree$" =>
//            e.printStackTrace()
//            results.add(err2message(e))
          case scala.util.Failure(e) =>
            results.add(err2message(e))
          case _ =>
            results.add("Success")
        }
      }
    }

    val mappedResults =
      results.asScala
        .groupBy(x => x)
        .mapValues(_.length)

    mappedResults
      .filter(_._2 > 1)
      .toArray
      .sortBy(_._2)
      .foreach {
        case (k, v) => println(s"$k: $v")
      }
    println(s"Total: ${results.size()}")
    assert(mappedResults("Success") > 18000) // baseline
  }
}
