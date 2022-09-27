package net.diyigemt.arona.remote.action

import kotlinx.serialization.Serializable
import net.diyigemt.arona.Arona
import net.diyigemt.arona.db.DataBaseProvider
import net.diyigemt.arona.db.gacha.GachaCharacterTable
import net.diyigemt.arona.db.gacha.GachaPool
import net.diyigemt.arona.db.gacha.GachaPoolCharacter
import net.diyigemt.arona.db.gacha.GachaPoolTable
import net.diyigemt.arona.remote.RemoteService
import net.diyigemt.arona.remote.RemoteServiceAction
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import net.diyigemt.arona.db.gacha.GachaCharacter as GC

class GachaPoolUpdateRemoteService : RemoteService<GachaPoolUpdateData> {
  //  override val kType: KType = List::class.createType(listOf(KTypeProjection.invariant(AnnouncementItem::class.starProjectedType)))
  override val kType: KType = GachaPoolUpdateData::class.createType()
  override val type: RemoteServiceAction = RemoteServiceAction.ANNOUNCEMENT

  override fun handleService(data: GachaPoolUpdateData, time: String) {
    var pool = DataBaseProvider.query {
      GachaPool.find { GachaPoolTable.name eq data.name }
    }?.first()
    // 没有同名池子 新建池子
    if (pool == null) {
      DataBaseProvider.query {
        pool = GachaPool.new {
          this.name = data.name
        }
      }
    } else {
      // 有同名池子, 自己新建一个
      DataBaseProvider.query {
        pool = GachaPool.new {
          this.name = "${data.name}(新)"
        }
      }
    }
    val insertCharacter = insertCharacter(pool!!.id.value, data.character)
    val massage = """
      新池子: ${pool!!.name} 已添加, id: ${pool!!.id}
      ${insertCharacter.joinToString(", ") { "${it.name}(${it.star})" }}
      使用 /gacha setpool ${pool!!.id} 指令来切换到这个池子
    """.trimIndent()
  }

  private fun insertCharacter(pool: Int, list: List<GachaCharacter>): List<GC> {
    return DataBaseProvider.query { _ ->
      list.map {
        var gc = GC.find {
          GachaCharacterTable.name eq it.name
        }.firstOrNull()
        if (gc == null) {
          gc = GC.new {
            this.name = it.name
            this.star = it.star
            this.limit = it.limit == 1
          }
        }
        GachaPoolCharacter.new {
          this.poolId = poolId
          this.characterId = gc.id
        }
        return@map gc
      }
    }!!
  }
}

@Serializable
data class GachaCharacter(
  val name: String,
  val star: Int,
  val limit: Int
)

@Serializable
data class GachaPoolUpdateData(
  val name: String,
  val character: List<GachaCharacter>
)