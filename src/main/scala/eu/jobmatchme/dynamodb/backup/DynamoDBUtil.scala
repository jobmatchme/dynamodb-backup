package eu.jobmatchme.dynamodb.backup

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import play.api.libs.json._
import scala.collection.JavaConverters._

object DynamoDBUtil {
  def toAttribute(json: JsObject): java.util.Map[String, AttributeValue] = {
    val result: java.util.Map[String, AttributeValue] = new java.util.HashMap()
    json.fields.foreach(field => {
      result.put(field._1, toAttributeValue(field._2))
    })
    result
  }

  def toAttribute(json: JsArray): java.util.Collection[AttributeValue] = {
    val result: java.util.Collection[AttributeValue] = new java.util.LinkedList()
    json.value.foreach(value => {
      result.add(toAttributeValue(value))
    })
    result
  }

  def toAttributeValue(json: JsValue): AttributeValue = {
    val attribute = new AttributeValue()
    json match {
      case value: JsObject =>
        attribute.setM(toAttribute(value))
      case value: JsArray =>
        attribute.setL(toAttribute(value))
      case value: JsString =>
        attribute.setS(value.as[String])
      case value: JsNumber =>
        attribute.setN(value.as[BigDecimal].toString)
      case value: JsBoolean =>
        attribute.setBOOL(value.as[Boolean])
      case _: JsNull.type =>
        attribute.setNULL(true)
    }
    attribute
  }

  def mapToJson(values: Map[String, AttributeValue]) = {
    JsObject(values.map(entry => (entry._1, DynamoDBUtil.toJson(entry._2))))
  }

  def toJson(value: AttributeValue): JsValue = {
    if (value.getM != null) {
      // Map[String, AttributeValue]
      JsObject(value.getM.asScala.map(entry => (entry._1, toJson(entry._2))))
    } else if (value.getL != null) {
      // List[AttributeValue]
      JsArray(value.getL.asScala.map(v => toJson(v)))
    } else if (value.getS != null) {
      JsString(value.getS)
    } else if (value.getN != null) {
      JsNumber(BigDecimal(value.getN))
    } else if (value.getBOOL != null) {
      JsBoolean(value.getBOOL)
    } else {
      JsNull
    }
  }
}
