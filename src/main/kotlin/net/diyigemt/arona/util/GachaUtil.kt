package net.diyigemt.arona.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.diyigemt.arona.command.constant.GachaConstant

object GachaUtil {

  fun pikerUp(): JsonElement {
    val random = (0..1000).random()
    var target: JsonElement = GachaConstant.REWARD_LIST_THREE_STAR;
    if (random in (0 .. GachaConstant.PICK_UP_ONE_STAR)) {
      target = GachaConstant.REWARD_LIST_ONE_STAR
    } else if (random in (GachaConstant.PICK_UP_ONE_STAR .. GachaConstant.PICK_UP_TOW_STAR)) {
      target = GachaConstant.REWARD_LIST_TWO_STAR
    } else if (random in (GachaConstant.PICK_UP_TOW_STAR .. GachaConstant.PICK_UP_THREE_STAR)) {
      target = GachaConstant.REWARD_LIST_THREE_STAR
    } else {
      return Json.parseToJsonElement("{\"star\":3,\"name\":\"亚津子(Pick Up)\"}")
    }
    val index = (0 until target.jsonArray.size).random()
    return target.jsonArray[index]
  }

  fun pickUpTwoStar(): JsonElement {
    val index = (0 until GachaConstant.REWARD_LIST_TWO_STAR.jsonArray.size).random()
    return GachaConstant.REWARD_LIST_TWO_STAR.jsonArray[index]
  }

  fun resultData2String(result: JsonElement): String {
    val name = result.jsonObject["name"].toString().replace("\"", "")
    val stars = result.jsonObject["star"]
    return "$name($stars${GachaConstant.star})"
  }

}