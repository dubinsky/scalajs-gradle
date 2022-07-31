package org.podval.tools.test.serializer

import org.gradle.internal.id.CompositeIdGenerator.CompositeId
import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}

final class IdSerializer extends Serializer[Object]:
  import IdSerializer.*

  override def write(encoder: Encoder, value: Object): Unit = value match
    case string: String =>
      encoder.writeByte(IdString)
      encoder.writeString(string)
    case long: java.lang.Long =>
      encoder.writeByte(IdLong)
      encoder.writeLong(long)
    case composite: CompositeId =>
      encoder.writeByte(IdComposite)
      this.write(encoder, composite.getScope)
      encoder.writeLong(composite.getId.asInstanceOf[Long])

  override def read(decoder: Decoder): Object = decoder.readByte match
    case IdString =>
      decoder.readString
    case IdLong =>
      decoder.readLong.asInstanceOf[Object]
    case IdComposite =>
      val scope: Object = this.read(decoder)
      val long: Long = decoder.readLong
      
      CompositeId(
        scope,
        long
      )

object IdSerializer:
  private val IdString   : Byte = 1
  private val IdLong     : Byte = 2
  private val IdComposite: Byte = 3
