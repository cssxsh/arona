package net.diyigemt.arona.util

import com.taptap.pinyin.PinyinPlus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult
import net.diyigemt.arona.Arona
import net.diyigemt.arona.advance.AronaUpdateChecker
import net.diyigemt.arona.command.CallMeCommand
import net.diyigemt.arona.command.TrainerCommand
import net.diyigemt.arona.config.AronaConfig
import net.diyigemt.arona.db.DataBaseProvider
import net.diyigemt.arona.db.DataBaseProvider.query
import net.diyigemt.arona.db.image.ImageTable
import net.diyigemt.arona.db.image.ImageTableModel
import net.diyigemt.arona.db.name.TeacherName
import net.diyigemt.arona.db.name.TeacherNameTable
import net.diyigemt.arona.entity.FuzzyImageResult
import net.diyigemt.arona.entity.ImageRequestResult
import net.diyigemt.arona.entity.ServerResponse
import net.diyigemt.arona.interfaces.InitializedFunction
import net.diyigemt.arona.util.NetworkUtil.BACKEND_ADDRESS
import net.diyigemt.arona.util.NetworkUtil.baseRequest
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jetbrains.exposed.sql.and
import org.jsoup.Connection
import org.jsoup.Connection.Response
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object GeneralUtils: InitializedFunction() {

  private const val IMAGE_FOLDER = "/image"
  private const val BACKEND_IMAGE_RESOURCE = "${BACKEND_ADDRESS}$IMAGE_FOLDER"

  fun checkService(group: Contact?): Boolean = when(group) {
    is Group -> AronaConfig.groups.contains(group.id)
    else -> false
  }

  fun clearExtraQute(s: String): String {
    if (s.replace("\"", "").length + 2 == s.length) {
      return s.replaceFirst("\"", "").substring(0, s.length - 2)
    }
    return s
  }

  fun queryTeacherNameFromDB(contact: Contact, user: UserOrBot): String {
    if (!CallMeCommand.enable) return user.nameCardOrNick
    val name = query {
      TeacherName.find { (TeacherNameTable.group eq contact.id) and (TeacherNameTable.id eq user.id) }.firstOrNull()
    }?.name ?: user.nameCardOrNick
    return if (AronaConfig.endWithSensei.isNotBlank() && !name.endsWith(AronaConfig.endWithSensei)) "${name}${AronaConfig.endWithSensei}" else name
  }

  fun randomInt(bound: Int): Int = (System.currentTimeMillis() % bound).toInt()

  fun randomBoolean(): Boolean = System.currentTimeMillis().toString().let {
    it.substring(it.length - 1).toInt() % 2 == 0
  }

  suspend fun uploadChapterHelper() {
    doUpload("/map-cache")
  }

  suspend fun uploadStudentInfo() {
    doUpload("/student_info")
  }

  private suspend fun doUpload(path: String) {
    val imageFileList = File(Arona.dataFolderPath() + path).listFiles() ?: return
    val g = Arona.arona.groups[1002484182]!!
    imageFileList.forEach {
      val name = it.name
      val res = it.toExternalResource("png")
      val upload = g.uploadImage(res)
      val msg = g.sendMessage(upload)
      Arona.info("$name ${msg.source.originalMessage.serializeToMiraiCode()}")
      Thread.sleep(1000)
      res.closed
    }
  }

  /**
   * 向后端请求图片 更新本地图片数据库并下载
   * 当且仅当远端或本地具有精确匹配结果时才返回图片文件, 否则返回上级进行模糊搜索
   * @param name 精确搜索目标
   * @return 当后端正常响应时, 判断是否为模糊搜索结果, 若为模糊搜索, 返回上级继续处理
   * 若为精确搜索结果, 判断本地是否已有图片, 若有则根据hash判断是否更新图片;
   * 否则下载图片并进行相应操作后返回本地图片文件;
   * 当后端未正常响应, 判断本地精确匹配结果, 若有则返回图片文件, 否则返回上级继续模糊搜索本地图片
   */
  fun loadImageOrUpdate(name: String): ImageRequestResult {
    val localDB = query {
      ImageTableModel.find { ImageTable.name eq name }.firstOrNull()
    }
    val result = kotlin.runCatching {
      NetworkUtil.requestImage(name)
    }.onFailure {
      // 服务器寄了 或者精确匹配与模糊查询结果均为空 尝试从本地拿
      // 如果本地数据库有精确匹配结果, 直接发送
      if (localDB != null) {
        val localFile = localImageFile(localDB.path)
        return if (localFile.exists()) ImageRequestResult(file = localFile) else ImageRequestResult()
      }
      // 否则对数据库内容和自定义配置文件进行模糊搜索(指令上级)
      return ImageRequestResult()
    }.getOrNull() ?: return ImageRequestResult()
    // 服务器没寄, 判断结果是图片文件还剩模糊查询
    val imageResultList = result.data
    val imageResult = imageResultList[0]
    // 模糊查询结果
    if (imageResult.type == FuzzyImageResult) {
      return ImageRequestResult(list = imageResultList)
    }
    // 精确结果
    val localFile = localImageFile(imageResult.path)
    // 没有本地图片, 向后端下载并存入数据库中
    return if (localDB == null) {
      imageRequest(imageResult.path, localFile)
      // 将本地图片信息写入数据库
      query {
        ImageTableModel.new {
          this.name = name
          this.path = imageResult.path
          this.hash = imageResult.hash
          this.type = imageResult.type
        }
      }
      ImageRequestResult(file = localFile)
    } else {
      // 有本地图片 如果本地hash值与服务器不一致或者本地文件不存在则获取图片
      return if (localDB.hash != imageResult.hash || !localFile.exists()) {
        // 删除本地图片并重新获取
        if (localDB.path != imageResult.path) {
          localImageFile(localDB.path).delete()
        }
        // 否则直接写入旧文件
        imageRequest(imageResult.path, localFile)
        // 更新hash
        query {
          localDB.hash = imageResult.hash
          localDB.path = imageResult.path
          localDB.type = imageResult.type
        }
        ImageRequestResult(file = localFile)
      } else {
        ImageRequestResult(file = localFile)
      }
    }
  }

  fun toPinyin(str: String): String = PinyinPlus.to(str)

  fun fuzzySearch(str: String, dict: List<String>, threshold: Int = 60): MutableList<ExtractedResult> = FuzzySearch.extractSorted(str, dict, threshold)

  private fun imageRequest(path: String, localFile: File): File {
    val connection = baseRequest(path, BACKEND_IMAGE_RESOURCE)
    val stream = connection.execute().bodyStream()
    val buffer = ByteArray(1024)
    val byteOutputStream = ByteArrayOutputStream()
    var len = stream.read(buffer)
    while (len != -1) {
      byteOutputStream.write(buffer, 0, len)
      len = stream.read(buffer)
    }
    localFile.writeBytes(byteOutputStream.toByteArray())
    stream.close()
    byteOutputStream.close()
    return localFile
  }

  private fun imageFileFolder(subFolder: String = "") = Arona.dataFolderPath(IMAGE_FOLDER) + subFolder

  fun localImageFile(path: String) = File(imageFileFolder(path.let { return@let if (path.startsWith("/")) path else "/$it" }))

  override fun init() {
    // 初始化本地图片文件夹
    File(imageFileFolder(TrainerCommand.ChapterMapFolder)).also { it.mkdirs() }
    File(imageFileFolder(TrainerCommand.StudentRankFolder)).also { it.mkdirs() }
    File(imageFileFolder(TrainerCommand.OtherFolder)).also { it.mkdirs() }
  }
}