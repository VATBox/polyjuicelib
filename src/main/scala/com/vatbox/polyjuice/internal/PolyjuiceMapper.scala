/*
 * Copyright 2016 VATBox Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vatbox.polyjuice.internal

import com.typesafe.scalalogging.LazyLogging
import com.vatbox.polyjuice.Polyjuice
import com.vatbox.polyjuice.exception._
import com.vatbox.polyjuice.parser.PolyjuiceNashornParser
import org.json4s.Formats

import scala.concurrent.duration._
import scala.concurrent._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by talg on 05/09/2016.
  */
trait PolyjuiceMapper extends LazyLogging {

  protected val variableName: String

  protected def payload: String

  private lazy val invocable = ScriptEngine.createNewEngine(nashornWrapper)

  private val FunctionName = """__map"""

  private val noValueText = """Your code didn't returned any value, call 'return <value>;'"""

  private val ValueIsNotSupported = """Your function return type """

  lazy private val WrongTypeRgx = s"""$ValueIsNotSupported\\[(.+)\\].*""".r

  private val NotAJsonString = """Invalid JSON passed for mapping"""

  private def nashornWrapper = {
    s"""
       |function $FunctionName(__input){
       |  var result = __actualMapping(__input);
       |  if (typeof result === 'string') {
       |    return JSON.stringify({ ${Polyjuice.StringValue} : result });
       |  } else if (typeof result === 'number') {
       |    return JSON.stringify({ ${Polyjuice.NumberValue} : result });
       |  } else if (typeof result === 'boolean') {
       |    return JSON.stringify({ ${Polyjuice.BooleanValue} : result });
       |  } else if (result === null || typeof result === 'undefined' ) {
       |    return JSON.stringify({ ${Polyjuice.NullValue} : true });
       |  } else if (Object.prototype.toString.call(result) === '[object Date]') {
       |    return JSON.stringify({ ${Polyjuice.DateValue} : result.toISOString() });
       |  } else if (typeof result === 'object') {
       |    return JSON.stringify(result);
       |  } else {
       |    var objClass = Object.prototype.toString.call(result);
       |    throw "$ValueIsNotSupported" + objClass + " is not supported";
       |  }
       |}
       |
       |function __actualMapping(__input){
       |  var $variableName;
       |  try {
       |    $variableName = JSON.parse(__input);
       |  } catch (e) {
       |    if ( e instanceof SyntaxError ) {
       |      throw "$NotAJsonString";
       |    } else {
       |      throw e;
       |    }
       |  }
       |  $payload
       |  throw "$noValueText";
       |}
     """.stripMargin
  }

  private def _map(jsonObject: String): Try[String] = {
    val result = invocable.map(inv => inv.invokeFunction(FunctionName, jsonObject)) match {
      case Failure(exception) if exception.getMessage.startsWith(ValueIsNotSupported) =>
        exception.getMessage match {
          case WrongTypeRgx(badType) => Failure(ValueReturnedMustBeOfSupportedTypeException(badType))
          case strangeMessage => Failure(ValueReturnedMustBeOfSupportedTypeException(strangeMessage))
        }
      case Failure(exception) if exception.getMessage.contains(noValueText) => Failure(NoValueReturnedFromCodeException)
      case Failure(exception) if exception.getMessage.contains(NotAJsonString) => Failure(NotAJsonObjectException(exception, jsonObject))
      case Failure(exception) => Failure(UnknownJavaScriptException(exception))
      case t@Success(value) if value.isInstanceOf[String] => t.map(_.toString)
    }
    result
  }

  def map[T: Manifest](jsonObject: String, timeout: Duration = 5 seconds)(implicit formats: Formats, executionContext: ExecutionContext): Future[Option[T]] = {
    var mapResult: Option[Try[String]] = None
    val (resultFuture, cancel) = interruptableFuture[Try[String]]( _ => {
      _map(jsonObject)
    })
    resultFuture.foreach(result => mapResult = Some(result)) // signal to the second future that this one is finished
    Future.firstCompletedOf(Seq(resultFuture, Future {
      val interval = 500
      var timeLeft = timeout.toMillis
      while (mapResult.isEmpty) {
        Thread.sleep(interval)
        if (timeLeft <= 0) {
          cancel()
          mapResult = Some(Failure(new RuntimeException("Mapping was unable to finish before timeout")))
        } else
          timeLeft -= interval
      }
      mapResult match {
        case Some(tryValue) => tryValue
        case None => Failure(new RuntimeException("No result but we should have finished...."))
      }
    })).flatMap { tryRes =>
        val parsedObj = tryRes.map(PolyjuiceNashornParser.deserialize[T])
        Future.fromTry(parsedObj)
      }.recoverWith{
      case ex: PolyjuiceException => Future.failed(ex)
      case ex: CancellationException => Future.failed(TimedoutExecution(timeout, ex))
      case ex =>
        logger.debug(s"Cancelling javascript failed", ex)
        Future.failed(InternalPolyjuiceException("Cancellation wasn't completed as expected", ex))
    }
  }

  /**
    * By Victor Klang - [[https://gist.github.com/viktorklang/5409467]]
    */
  private def interruptableFuture[T](fun: Future[T] => T)(implicit ex: ExecutionContext): (Future[T], () => Boolean) = {
    val p = Promise[T]()
    val f = p.future
    val lock = new Object
    var currentThread: Thread = null
    def updateCurrentThread(newThread: Thread): Thread = {
      val old = currentThread
      currentThread = newThread
      old
    }
    p tryCompleteWith Future {
      if (f.isCompleted) throw new CancellationException
      else {
        val thread = Thread.currentThread
        lock.synchronized {
          updateCurrentThread(thread)
        }
        try {
          val executed = fun(f)
          executed
        } finally {
          lock.synchronized {
            updateCurrentThread(null)
          } ne thread
        }
      }
    }

    (f, () => lock.synchronized {
      Option(updateCurrentThread(null)) exists {
        t =>
          // t.interrupt() is ignored, so we need to use ThreadDeath - if you know a better solution please let us know
          t.stop()
          p.tryFailure(new CancellationException)
      }
    })
  }
}
