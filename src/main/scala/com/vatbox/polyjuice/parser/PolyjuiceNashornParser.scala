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

package com.vatbox.polyjuice.parser

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.vatbox.polyjuice.Polyjuice
import com.vatbox.polyjuice.exception.TypeErrorException
import org.json4s.native.JsonMethods
import org.json4s.{Formats, _}
/**
  * Created by talg on 18/10/2016.
  */
object PolyjuiceNashornParser {
  private val LocalDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
  /**
    * Parse given stringified object into JValue, we assume that it is a valid JSON
    */
  private[polyjuice] def deserialize[T](str: String)(implicit formats: Formats, manifest: Manifest[T]) : Option[T] = {
    val jValue = JsonMethods.parseOpt(str)
    jValue.flatMap(_ match {
      case JNull => None
      case JObject(JField(Polyjuice.StringValue, JString(strValue)) :: Nil) if manifest.runtimeClass == classOf[String] => Option(strValue)
      case JObject(JField(Polyjuice.StringValue, JString(strValue)) :: Nil) => throw TypeErrorException("String", manifest.runtimeClass)
      case JObject(JField(Polyjuice.NumberValue, JDouble(dblValue)) :: Nil) if manifest.runtimeClass == classOf[Double] => Option(dblValue)
      case JObject(JField(Polyjuice.NumberValue, JDouble(dblValue)) :: Nil) => throw TypeErrorException("Double", manifest.runtimeClass)
      case JObject(JField(Polyjuice.NumberValue, JInt(intValue)) :: Nil) if manifest.runtimeClass == classOf[Int] => Option(intValue.toInt)
      case JObject(JField(Polyjuice.NumberValue, JInt(intValue)) :: Nil) if manifest.runtimeClass == classOf[Long] => Option(intValue.toLong)
      case JObject(JField(Polyjuice.NumberValue, JInt(intValue)) :: Nil) => throw TypeErrorException("Number", manifest.runtimeClass)
      case JObject(JField(Polyjuice.BooleanValue, JBool(boolValue)) :: Nil) if manifest.runtimeClass == classOf[Boolean] => Option(boolValue)
      case JObject(JField(Polyjuice.BooleanValue, JBool(boolValue)) :: Nil) => throw TypeErrorException("Boolean", manifest.runtimeClass)
      case JObject(JField(Polyjuice.DateValue, JString(isoDateStr)) :: Nil) if manifest.runtimeClass == classOf[LocalDateTime] => Option(LocalDateTime.parse(isoDateStr, LocalDateTimeFormatter))
      case JObject(JField(Polyjuice.DateValue, JString(isoDateStr)) :: Nil) if manifest.runtimeClass == classOf[String] => Option(isoDateStr)
      case JObject(JField(Polyjuice.DateValue, JString(isoDateStr)) :: Nil) => throw TypeErrorException("Date", manifest.runtimeClass)
      case JObject(JField(Polyjuice.NullValue, JBool(_)) :: Nil) => None
      case json => Extraction.extractOpt[T](json)
    }).map { unknownTypeVar =>
      unknownTypeVar.asInstanceOf[T]
    } // Should fail if it's not the case
  }
}
