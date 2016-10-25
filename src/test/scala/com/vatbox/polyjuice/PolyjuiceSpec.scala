package com.vatbox.polyjuice

import java.time.LocalDateTime

import com.vatbox.polyjuice.exception._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FreeSpec, Matchers, OptionValues, TryValues}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by talg on 14/09/2016.
  */
class PolyjuiceSpec extends FreeSpec with TryValues with OptionValues with Matchers with ScalaFutures {
  implicit val formats = org.json4s.DefaultFormats
  override implicit val patienceConfig = PatienceConfig(Span(15, Seconds),Span(200, Millis))
  "Nashorn spec" - {
    "Normal" - {
      "Return String" in {
        val mapper = Polyjuice.createMapper(varName = "report", userCode = s"""return report.name;""")
        val triedT = mapper.map[String](s"""{"name" : "hello World"}""")
        triedT.futureValue.value shouldBe "hello World"
      }

      "Return Int" in {
        val mapper = Polyjuice.createMapper("expense", s"""if (expense.expense > 400) {return 1;} else {return 2;}""")
        val triedT = mapper.map[Int](s"""{"expense" : 500}""")
        triedT.futureValue.value shouldBe 1
      }
      "Return Double" in {
        val mapper = Polyjuice.createMapper("report", s"""return 3.3;""")
        val triedT = mapper.map[Double](s"""{"expense" : 500}""")
        triedT.futureValue.value shouldBe 3.3
      }

      "Return Boolean" in {
        val mapper = Polyjuice.createMapper("report", s"""return true;""")
        val triedT = mapper.map[Boolean](s"""{"expense" : 500}""")
        triedT.futureValue.value shouldBe true
      }

      "Return Long" in {
        val mapper = Polyjuice.createMapper("report", s"""return 1231231231231;""")
        val triedT = mapper.map[Long](s"""{"expense" : 500}""")
        triedT.futureValue.value shouldBe 1231231231231L
      }

      "Return Date as LocalDateTime" in {
        val mapper = Polyjuice.createMapper("report", s"""return new Date();""")
        val triedT = mapper.map[LocalDateTime](s"""{"expense" : 500}""")
        triedT.futureValue.value shouldBe a [LocalDateTime]
      }
      "Return Date as String" in {
        val mapper = Polyjuice.createMapper("report", s"""return new Date();""")
        val triedT = mapper.map[String](s"""{"expense" : 500}""")
        triedT.futureValue.value shouldBe a [String]
      }

      "Return Object as Object" in {
        val mapper = Polyjuice.createMapper("report", s"""return {name: report.firstName, value : 5};""")
        val triedT = mapper.map[SimpleTestModel](s"""{"firstName" : "Cool"}""")
        triedT.futureValue.value shouldBe SimpleTestModel("Cool", 5)
      }
    }
    "Make sure exception is thrown if the 'code' doesn't return" in {
      val mapper = Polyjuice.createMapper("report", "")
      mapper.map("""{"name" : "helloworld"}""").failed.futureValue shouldBe NoValueReturnedFromCodeException
    }

    "Throw cast exception if expected type is different from the one retunred" in {
      val mapper = Polyjuice.createMapper("report", """return "20";""")
      mapper.map[Int]("""{"name" : "helloworld"}""").failed.futureValue shouldBe a [TypeErrorException]
    }

    "Make sure that the function returns supported value" in {
      val mapper = Polyjuice.createMapper("expense", s"""return new Function("return 42;");""")
      val triedT = mapper.map(s"""{"expense" : 500}""", 2 seconds)
      val throwable = triedT.failed.futureValue
      throwable shouldBe a [ValueReturnedMustBeOfSupportedTypeException]
    }

    "Make sure that no java code can be run" in {
      val javaCode = """
        |var File = java.io.File;
        |// list contents of the current directory!
        |for each (var f in new File(".").list())
        |   print(f)
      """.stripMargin
      val mapper = Polyjuice.createMapper("report", javaCode)
      mapper.map("""{"name" : "helloworld"}""").failed.futureValue shouldBe a [UnknownJavaScriptException]
    }
    "Make sure that no external libs are used" in {
      val javaCode = """
        |var xhr = new XMLHttpRequest();
        |xhr.open("GET", "https://www.google.com/", false);
        |return xhr.send();
      """.stripMargin
      val mapper = Polyjuice.createMapper("report", javaCode)
      val triedString = mapper.map("""{"name" : "helloworld"}""")
      triedString.failed.futureValue shouldBe a [UnknownJavaScriptException]
    }

    "Fail if payload is not a JSON" in {
      val mapper = Polyjuice.createMapper("expense", s"""return 1;""")
      val triedT = mapper.map(s"""not a JSON""")
      triedT.failed.futureValue shouldBe a [NotAJsonObjectException]
    }

    "Infinite loop" in {
      val mapper = Polyjuice.createMapper("expense",
        s"""
           |while(true){
           |}""".stripMargin)
      val triedT = mapper.map(jsonObject = s"""{ "name" : "somename" }""", timeout = 3 seconds)
      triedT.failed.futureValue shouldBe a [TimedoutExecution]
    }
  }
}

case class SimpleTestModel(name: String, value: Int)