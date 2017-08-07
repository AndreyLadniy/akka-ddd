package pl.newicom.dddd.messaging

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.{Processed, Receipt, alod}
import pl.newicom.dddd.messaging.MetaAttribute.Publisher_Type.PublisherTypeValue

import scala.util.{Success, Try}
import pl.newicom.dddd.messaging.MetaAttribute._

trait Message extends Serializable {

  type MessageImpl <: Message

  def id: EntityId =
    metadata.get(Id)

  def timestamp: DateTime =
    metadata.get(Timestamp)

  def metadata: MetaData

  protected def withNewMetaData(m: MetaData): MessageImpl

  def withMetaData(m: MetaData): MessageImpl =
    withMetaData(m.content)

  def withMetaData(attributes: Map[String, Any]): MessageImpl =
    withNewMetaData(this.metadata.withMetaData(MetaData(attributes)))

  def withMetaAttribute(attrName: String, value: Any): MessageImpl =
    withMetaData(Map(attrName -> value))

  def withMetaAttribute[A](attr: MetaAttribute[A], value: A): MessageImpl =
    withMetaData(Map(attr.entryName -> value))

  def hasMetaAttribute(attrName: String): Boolean =
    metadata.contains(attrName)

  def getMetaAttribute[B](attr: MetaAttribute[B]): B =
    tryGetMetaAttribute(attr).get

  def getMetaAttribute[B](attrName: String): B =
    tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: String): Option[B] =
    metadata.tryGet[B](attrName)

  def tryGetMetaAttribute[B](attr: MetaAttribute[B]): Option[B] =
    metadata.tryGet(attr)

  def deliveryReceipt(result: Try[Any] = Success("OK")): Receipt =
    deliveryId.map(id => alod.Processed(id, result)).getOrElse(Processed(result))

  def withDeliveryId(deliveryId: Long): MessageImpl =
    withMetaAttribute(Delivery_Id, deliveryId)

  def withEventNumber(eventNumber: Int): MessageImpl =
    withMetaAttribute(Event_Number, eventNumber)

  def withCorrelationId(correlationId: EntityId): MessageImpl =
    withMetaAttribute(Correlation_Id, correlationId)

  def withCausationId(causationId: EntityId): MessageImpl =
    withMetaAttribute(Causation_Id, causationId)

  def withMustFollow(mustFollow: Option[String]): MessageImpl =
    mustFollow.map(msgId => withMetaAttribute(Must_Follow, msgId)).getOrElse(this.asInstanceOf[MessageImpl])

  def withTag(tag: String): MessageImpl =
    withMetaAttribute(MetaAttribute.Tags, tags + tag)

  def withTags(tags: String*): MessageImpl =
    withMetaAttribute(Tags, this.tags ++ tags)

  def withPublisherType(publisherType: PublisherTypeValue.Value): MessageImpl =
    withMetaAttribute(MetaAttribute.Publisher_Type, publisherType.toString)

  def withReused(reused: Boolean): MessageImpl =
    if (reused)
      withMetaAttribute(Reused, reused)
    else
      this.asInstanceOf[MessageImpl]

  def tags: Set[String] =
    tryGetMetaAttribute(Tags).toSet.flatten

  def deliveryId: Option[Long] =
    tryGetMetaAttribute(Delivery_Id).map {
      case bigInt: scala.math.BigInt => bigInt.toLong
      case l: Long                   => l
    }

  def correlationId: Option[EntityId] =
    tryGetMetaAttribute(Correlation_Id)

  def causationId: Option[EntityId] =
    tryGetMetaAttribute(Causation_Id)

  def mustFollow: Option[String] =
    tryGetMetaAttribute(Must_Follow)

  def eventNumber: Option[Int] =
    tryGetMetaAttribute(Event_Number)

  def publisherType: Option[PublisherTypeValue.Value] =
    tryGetMetaAttribute(MetaAttribute.Publisher_Type).map(PublisherTypeValue.withName)

  def reused: Option[Boolean] =
    tryGetMetaAttribute(Reused)

}
