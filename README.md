## Polyjuice library for mapping JSON objects with dynamic JavaScript code in Scala
[![CircleCI](https://circleci.com/gh/VATBox/polyjuicelib.svg?style=svg)](https://circleci.com/gh/VATBox/polyjuicelib)
### What it does
```
                                       +----------------+
                                       |                |
                                       | Dynamic        |
                                       | JavaScript code|
                                       |       +        |
                                       +-------|--------+
                                               |
                                               |
                                               |
                                               v
         +------------+                +---------------+                +-------------+
   JVM   |            |                |               |  mapped object |             |      JVM
+-object---> Json4s  +--object as JSON --> Polyjuice +----------by------->  Json4s   +--mapped object->
         | serialize  |                |               |  given js code | Deserialize |
         |            |                |               |     as JSON    |             |
         +------------+                +---------------+                +-------------+

```
 
### Why it was build
Let's say you have an expense report which is modeled something like this
```scala
case class Report(createdAt: Option[LocalDateTime], currency: Currency, amount: Double, country: Option[String], customFields: Map[String,CustomField]) 
```                        
Here at VATBox we are dealing with a lot of Reports (no they don't look like this unfortunately), each one of them have different custom fields which is customer depending.
For example one customer can have a custom field called "center" with ~20 different values one for each country it has a subsidiary in.
Since each "center" is in a different country there are different rules hence we need to __map__ each report by different custom fields with countries etc.

*This way we can write customer's report specific mapping code and just "upload" it to an already running applications without having them altered for this.*  
 
### Features
* [Nashorn](http://www.oracle.com/technetwork/articles/java/jf14-nashorn-2126515.html) 
* Supports 'primitive' types or your own custom models via [Json4s](https://github.com/json4s/json4s)
* Malicious code safety (endless loops, external libraries, native code)  

### SBT

```scala
"com.vatbox" %% "polyjuice" % "1.1.6"
```

### Examples
There are more examples in [Tests](/src/test/scala/com/vatbox/polyjuice/PolyjuiceSpec.scala)
* Return a String value of the "name" key
```scala
"Return String" in {
        val mapper = Polyjuice.createMapper(varName = "report", userCode = s"""return report.name;""")
        val triedT = mapper.map[String](s"""{"name" : "hello World"}""")
        triedT.futureValue.value shouldBe "hello World"
      }
```
* Return an Int
```scala
"Return Int" in {
        val mapper = Polyjuice.createMapper("expense", s"""if (expense.expense > 400) {return 1;} else {return 2;}""")
        val triedT = mapper.map[Int](s"""{"expense" : 500}""")
        triedT.futureValue.value shouldBe 1
      }
```
* Return a Double (well you got the idea)
```scala
"Return Double" in {
        val mapper = Polyjuice.createMapper("unused", s"""return 3.3;""")
        val triedT = mapper.map[Double](s"""{"irrelevant" : 500}""")
        triedT.futureValue.value shouldBe 3.3
      }
```
* Return a LocalDateTime
```scala
"Return Date as LocalDateTime" in {
        val mapper = Polyjuice.createMapper("unused", s"""return new Date();""")
        val triedT = mapper.map[LocalDateTime](s"""{"irrelevant" : 500}""")
        triedT.futureValue.value shouldBe a [LocalDateTime]
      }
```
* Now return that same Date but now as [8601](https://en.wikipedia.org/wiki/ISO_8601) String
```scala
"Return Date as String" in {
        val mapper = Polyjuice.createMapper("unused", s"""return new Date();""")
        val triedT = mapper.map[String](s"""{"irrelevant" : 500}""")
        triedT.futureValue.value shouldBe a [String]
        // 2016-10-19T10:07:05.427Z
      }
```
* Return a custom object
```scala 
case class SimpleTestModel(name: String, value: Int)
```
```scala
"Return Object as Object" in {
        val mapper = Polyjuice.createMapper("report", s"""return {name: report.firstName, value : 5};""")
        val triedT = mapper.map[SimpleTestModel](s"""{"firstName" : "Cool"}""")
        triedT.futureValue.value shouldBe SimpleTestModel("Cool", 5)
      }
```

### Safety part - endless loop
```scala
    "Infinite loop" in {
      val mapper = Polyjuice.createMapper("whatever",
        s"""
           |while(true){
           |}""".stripMargin)
      val triedT = mapper.map(jsonObject = s"""{ "name" : "somename" }""", timeout = 3 seconds)
      triedT.failed.futureValue shouldBe a [TimedoutExecution]
    }
```