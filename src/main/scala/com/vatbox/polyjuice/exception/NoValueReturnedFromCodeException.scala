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

package com.vatbox.polyjuice.exception

import scala.concurrent.duration.Duration

/**
  * Created by talg on 14/09/2016.
  */
abstract class PolyjuiceException(msg: String,throwable: Throwable) extends RuntimeException(msg, throwable) {
  def this(msg: String) = this(msg,null)
  def this(throwable: Throwable) = this(throwable.toString, throwable)
}

case object NoValueReturnedFromCodeException extends PolyjuiceException("Your code didn't returned any value, call 'return <value>;'")

case class ValueReturnedMustBeOfSupportedTypeException(badType: String) extends PolyjuiceException(s"Your code returned $badType type, which is not supported")

case class UnknownJavaScriptException(th: Throwable) extends PolyjuiceException(th)

case class NotAJsonObjectException(th: Throwable, notJson: String) extends PolyjuiceException(th)

case class TimedoutExecution(duration: Duration) extends PolyjuiceException(s"Timeout of ${duration.toString} reached")

case class TypeErrorException(codeType: String, expectedType: Class[_]) extends PolyjuiceException(s"Code returned [$codeType], but the expected type was [${expectedType.getSimpleName}]")

case class InternalPolyjuiceException(msg: String) extends PolyjuiceException(msg)