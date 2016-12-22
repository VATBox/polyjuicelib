package com.vatbox.polyjuice

import generators.Generator
import org.json4s.native.Serialization
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.{FreeSpec, Matchers, OptionValues, TryValues}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
  * Created by talg on 22/12/2016.
  */
class Benchamarks extends FreeSpec with TryValues with OptionValues with Matchers with ScalaFutures with GeneratorDrivenPropertyChecks {
  implicit val formats = org.json4s.DefaultFormats
  val amount = 10000
  override implicit val patienceConfig = PatienceConfig(Span(1, Minutes), Span(10, Millis))
  val mapper = Polyjuice.createMapper(varName = "model", userCode = s"""if (model.bool) return model.int; else return model.str;""")
  "Map many" in {
    var future = Future[Boolean](true)
    forAll(Generator.modelGen, minSuccessful(amount)) { model =>
      val json = Serialization.write(model)
      if (model.bool) {
        val result = mapper.map[Int](json).futureValue.get
        assert(result === model.int)
      }
      else {
        val result = mapper.map[String](json).futureValue.get
        assert(result === model.str)
      }
    }
  }
}
