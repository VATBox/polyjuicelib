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

package com.vatbox.polyjuice

import com.vatbox.polyjuice.internal.PolyjuiceMapper

/**
  * Created by talg on 14/09/2016.
  */
object Polyjuice {
  private[polyjuice] val StringValue = "__$polyjuice_string_value__"
  private[polyjuice] val NumberValue = "__$polyjuice_number_value__"
  private[polyjuice] val BooleanValue = "__$polyjuice_boolean_value__"
  private[polyjuice] val DateValue = "__$polyjuice_date_value__"
  private[polyjuice] val NullValue = "__$polyjuice_null_value__"

  private val DefaultVariableName = "__$polyjuiceObj"
  def createMapper(varName: String = DefaultVariableName, userCode: String): PolyjuiceMapper = {
    new PolyjuiceMapper {
      override protected val variableName: String = varName

      override protected def payload = userCode
    }
  }
}
