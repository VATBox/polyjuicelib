package generators

import org.scalacheck.{Arbitrary, Gen}

/**
  * Created by talg on 22/12/2016.
  */
object Generator {
  def modelGen: Gen[Model] = for {
    s <- Gen.choose(1, 10).flatMap(size => Gen.listOfN(size,Gen.alphaNumChar)).map(_.mkString)
    i <- Arbitrary.arbInt.arbitrary
    b <-Arbitrary.arbBool.arbitrary
  } yield Model(
    str = s,
    int = i,
    bool = b
  )
}

case class Model(str: String, int: Int, bool: Boolean)