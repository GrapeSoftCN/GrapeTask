package interfaceApplication;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import JGrapeSystem.rMsg;
import apps.appsProxy;
import authority.privilige;
import cache.CacheHelper;
import check.checkHelper;
import database.DBHelper;
import database.db;
import email.mail;
import httpClient.request;
import interrupt.interrupt;
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import session.session;
import sms.ruoyaMASDB;
import string.StringHelper;
import time.TimeHelper;

public class task {
	private CacheHelper caches = new CacheHelper();
	private DBHelper tasks = new DBHelper("mongodb", "task");
	private static String nodeString = "mongodb";
	private DBHelper dbHelper;
	private String appid = appsProxy.appidString();
	private session se;
	private JSONObject userInfo = new JSONObject();
	private JSONObject _obj = new JSONObject();
	private String sid = null;

	public task() {
		se = new session();
		sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			// userInfo = (JSONObject) se.get(sid);
			userInfo = se.getSession(sid);
		}
		searchAll(null);
	}

	private db bind() {
		return tasks.bind(appid);
	}

	private db bind(DBHelper helper) {
		return helper.bind(appid);
	}

	/**
	 * 新增任务信息
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param taskInfo
	 *            ,包含Api地址需进行base64编码+特殊格式编码
	 *
	 */
	public String AddTask(String taskInfo) {
		int code = 0;
		taskInfo = CheckParam(taskInfo);
		JSONObject object = JSONObject.toJSON(taskInfo);
		if (object.containsKey("errorcode")) {
			return taskInfo;
		}
		code = bind().data(object).insertOnce() != null ? 0 : 99;
		if (code == 0) {
			caches.delete("APICheck_" + appid);
			searchAll(null);
		}
		return resultMessage(code, "新增任务成功");
	}

	/**
	 * 修改任务信息
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param taskInfo
	 *            ,包含Api地址需进行base64编码+特殊格式编码
	 *
	 */
	public String UpdateTask(String tid, String taskInfo) {
		int code = 99;
		int openstate;
		taskInfo = CheckParam(taskInfo);
		JSONObject object = JSONObject.toJSON(taskInfo);
		if (object != null && object.size() > 0) {
			if (object.containsKey("errorcode")) {
				return taskInfo;
			}
			code = bind().eq("_id", tid).data(object).update() != null ? 0 : 99;
			if (code == 0) {
				caches.delete("APICheck_" + appid);
				searchAll(null);
			}
			if (object.containsKey("state")) {
				String state = object.getString("state");
				openstate = state.equals("1") ? 1 : 0;
				String temp = ExcuteTask(0, openstate);
				System.out.println(temp);
			}
		}
		return resultMessage(code, "任务信息修改成功");
	}

	/**
	 * 参数验证
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param taskInfo
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private String CheckParam(String taskInfo) {
		String temp = "";
		int code = 0;
		JSONObject object = JSONObject.toJSON(taskInfo);
		if (object != null && object.size() > 0) {
			if (object.containsKey("phone")) {
				temp = object.getString("phone");
				if (!temp.equals("")) {
					code = checkHelper.checkMobileNumber(temp) ? 0 : 1;
					if (code != 0) {
						return resultMessage(code);
					}
				}
			}
			if (object.containsKey("email")) {
				temp = object.getString("email");
				if (!temp.equals("")) {
					code = checkHelper.checkEmail(temp) ? 0 : 2;
					if (code != 0) {
						return resultMessage(code);
					}
				}
			}
			if (object.containsKey("taskContent")) {
				temp = object.getString("taskContent");
				if (!temp.equals("")) {
					temp = codec.DecodeHtmlTag(temp);
					temp = codec.decodebase64(temp);
					object.put("taskContent", temp);
				}
			}
		}
		return (object != null && object.size() > 0) ? object.toString() : null;
	}

	/**
	 * 执行全部处于开启状态的任务，每30秒执行一次任务
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param hostid
	 *            邮箱id
	 *
	 * @param state
	 *
	 */
	public String ExcuteTask(int emailid, int state) {
		String result = (String) appsProxy.proxyCall("/GrapeHeart/Heart/check/int:" + emailid + "/int:" + state, null, null);
		return result;
	}

	public String DeleteTask(String id) {
		int code = 99;
		String[] ids = null;
		db db = bind();
		if (id != null && !id.equals("") && !id.equals("null")) {
			ids = id.split(",");
			if (ids != null && ids.length > 0) {
				db.or();
				for (String string : ids) {
					if (ObjectId.isValid(string)) {
						db.eq("_id", string);
					}
				}
				code = db.deleteAll() == ids.length ? 0 : 99;
			}
		}
		return rMsg.netMSG(code, code == 0 ? "删除成功" : "删除失败");
	}

	public String PageTask(int idx, int pageSize, String condString) {
		searchAll(null);
		ExcuteTask(0, 1);
		db db = bind();
		JSONArray array = null;
		long total = 0, totalSize = 0;
		if (condString != null && !condString.equals("") && !condString.equals("null")) {
			JSONArray condArray = JSONArray.toJSONArray(condString);
			if (condArray != null && condArray.size() > 0) {
				db.where(condArray);
			} else {
				array = null;
			}
		}
		array = db.dirty().page(idx, pageSize);
		totalSize = db.pageMax(pageSize);
		total = db.count();
		return PageShow(array, total, totalSize, idx, pageSize);
	}

	// 分页显示部分
	@SuppressWarnings("unchecked")
	private String PageShow(JSONArray array, long total, long totalSize, int current, int PageSize) {
		JSONObject object = new JSONObject();
		object.put("data", (array != null && array.size() != 0) ? array : new JSONArray());
		object.put("total", total);
		object.put("totalSize", totalSize);
		object.put("currentPage", current);
		object.put("PageSize", PageSize);
		return resultMessage(object);
	}

	// 获取所有的任务信息
	@SuppressWarnings("unchecked")
	private JSONObject searchAll(String authcode) {
		JSONObject object = new JSONObject();
		if (caches.get("APICheck_" + appid) != null) {
			object = JSONObject.toJSON(caches.get("APICheck_" + appid));
		} else {
			JSONArray array = bind().eq("state", 0).select();
			JSONObject emailObj = joinObj(array, "email");
			JSONObject phoneObj = joinObj(array, "phone");
			object.put("email", emailObj);
			object.put("phone", phoneObj);
			caches.setget("APICheck_" + appid, object, 60 * 60 * 12);
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	private JSONObject joinObj(JSONArray array, String filed) {
		String key, tempkey, value = "", temp;
		JSONObject object, Obj = new JSONObject();
		if (array != null && array.size() > 0) {
			for (Object object2 : array) {
				object = (JSONObject) object2;
				tempkey = object.getString(filed);
				temp = object.getString("host");
				if (Obj != null && Obj.size() > 0) {
					for (Object obj : Obj.keySet()) {
						key = obj.toString();
						value = Obj.getString(key);
						if (key.equals(tempkey)) {
							value += "," + temp;
						} else {
							value = temp;
						}
					}
					Obj.put(tempkey, StringHelper.fixString(value, ','));
				} else {
					Obj.put(tempkey, temp);
				}
			}
		}
		return Obj;
	}

	/**
	 * 获取appid，类名称，方法名称，参数
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param str
	 * @return appid.class.func.param1.param2.param3
	 *
	 */
	public String getFunc(String str) {
		String[] strs = str.split("/");
		String result = strs[3] + "." + strs[4] + "." + strs[5] + "." + strs[6];
		for (int i = 7; i < strs.length; i++) {
			result += "." + strs[i];
		}
		return result;
	}

	/**
	 * 待更新栏目统计
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param idx
	 * @param pageSize
	 * @param num
	 * @return
	 *
	 */
	public String getPendingColumn(int idx, int pageSize) {
		JSONArray column = getColumnInfo("objectGroup", idx, pageSize);
		JSONArray array = getRestCount("objectList", column);
		return resultMessage(array);
	}

	/**
	 * 文章超链接检测
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public String CheckLink(int idx, int pageSize) {
		nlogger.logout("Link");
		int count = 0;
		JSONObject object, objId, obj = new JSONObject();
		String content, id;
		JSONArray array = getColumnByType(idx, pageSize);
		nlogger.logout(array);
		JSONArray arrays = new JSONArray();
		if (array != null && array.size() != 0) {
			int l = array.size();
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				content = object.getString("content");
				objId = (JSONObject) object.get("_id");
				id = objId.getString("$oid");
				if (!content.equals("")) {
					String mString = request.Get(content);
					if (mString.equals("")) {
						count++;
						object.remove("_id");
						object.put("id", id);
						arrays.add(object);
					}
				}
			}
			obj.put("errcount", count);
			obj.put("data", arrays);
		}
		return resultMessage(obj);
	}

	/**
	 * 获取待更新栏目数据
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param idx
	 * @param pageSize
	 * @return
	 *
	 */
	public JSONArray getColumnInfo(String tableName, int idx, int pageSize) {
		int role = getRoleSign();
		String wbid = "", userid = "";
		JSONArray array = null;
		dbHelper = new DBHelper(nodeString, tableName);
		db db = bind(dbHelper);
		if ((userInfo != null && userInfo.size() != 0)) {
			wbid = userInfo.get("currentWeb").toString(); // 当前网站
			userid = ((JSONObject) userInfo.get("_id")).getString("$oid"); // 当前登录用户id
		}
		if (role == 3) {
			db.eq("wbid", wbid);
		}
		if (role == 2) {
			db.eq("wbid", wbid).eq("ownid", userid);
		}
		if (role == 7) { // 栏目编辑
			db.eq("wbid", wbid).eq("editor", userid);
		}
		array = db.field("_id,name,lastTime,timediff,editCount").page(idx, pageSize);
		return RestTime(array);
	}

	// 获取最新咨询及举报
	@SuppressWarnings("unchecked")
	public JSONArray GetLetter(String sid) {
		JSONObject obj;
		String id;
		JSONObject users = se.getSession(sid);
		id = (users != null && users.size() != 0) ? ((JSONObject) users.get("_id")).getString("$oid") : " ";
		db Sugdb = new DBHelper("mongodb", "suggest").bind(appsProxy.appidString());
		db Repdb = new DBHelper("mongodb", "report").bind(appsProxy.appidString());
		JSONArray SuggestArray = Sugdb.desc("time").eq("userid", id).limit(50).select();
		JSONArray ReportArray = Repdb.desc("time").eq("userid", id).limit(50).select();
		for (Object object : ReportArray) {
			obj = (JSONObject) object;
			SuggestArray.add(obj);
		}
		return SuggestArray;
	}

	/**
	 * 获取剩余更新时间
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file ContentGroup.java
	 * 
	 * @param array
	 * @param currentTime
	 *            当前时间
	 * @param timediff
	 *            更新周期
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONArray RestTime(JSONArray array) {
		JSONArray array2 = new JSONArray();
		long RestTime = 0;
		String timediff = "0", lastTime = "0";
		JSONObject object;
		JSONObject obj;
		int l = array.size();
		for (int i = 0; i < l; i++) {
			object = (JSONObject) array.get(i);
			obj = getRestTime(object);
			if (obj != null && obj.size() != 0) {
				lastTime = obj.getString("lastTime");
				timediff = obj.getString("timediff");
				RestTime = obj.getLong("RestTime");
				if (lastTime != null && !lastTime.equals("") && timediff != null && !timediff.equals("")) {
					if (RestTime < 0) {
						RestTime = -RestTime;
						object.put("restTime", RestTime > Long.parseLong(timediff) ? 0 : RestTime);
					}
					array2.add(object);
				}
			}
		}
		return array2;
	}

	/**
	 * 获取栏目剩余更新时间,上次更新时间，更新周期，需更新的文章总数
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param object
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getRestTime(JSONObject object) {
		JSONObject obj = new JSONObject();
		long currentTime = TimeHelper.nowMillis();
		long timediff = 0;
		long RestTime = 0, lastTime = 0;
		long editCount = 0;
		String temp, tempTimediff, editTemp;
		temp = object.getString("lastTime");
		tempTimediff = object.getString("timediff");
		editTemp = object.getString("editCount");
		if (temp != null && !temp.equals("") && tempTimediff != null && !tempTimediff.equals("") && editTemp != null && !editTemp.equals("")) {
			if (temp.contains("$numberLong")) {
				temp = JSONObject.toJSON(temp).getString("$numberLong");
			}
			if (tempTimediff.contains("$numberLong")) {
				tempTimediff = JSONObject.toJSON(tempTimediff).getString("$numberLong");
			}
			if (editTemp.contains("$numberLong")) {
				editTemp = JSONObject.toJSON(editTemp).getString("$numberLong");
			}
			lastTime = Long.parseLong(temp); // 上次更新时间
			timediff = Long.parseLong(tempTimediff); // 更新周期
			editCount = Long.parseLong(editTemp); // 更新周期
			RestTime = lastTime + timediff - currentTime;
			obj.put("lastTime", lastTime);
			obj.put("timediff", timediff);
			obj.put("RestTime", RestTime);
			obj.put("editCount", editCount);
		}
		return obj;
	}

	/**
	 * 获取剩余更新文章数量
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java 以上次更新时间为开始时间，结束时间为当前时间，获取对应栏目已更新文章数量
	 * 
	 * @param tableName
	 * @param lastTime
	 * @param num
	 *            时间周期内需要更新的文章总量
	 * @param arrayColum
	 *            栏目数据
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONArray getRestCount(String tableName, JSONArray arrayColum) {
		int column;
		String columnId, tempTime, tempnum;
		// JSONArray arrays = new JSONArray();
		JSONObject tempObj;
		long currentTime = TimeHelper.nowMillis(), count = 0, rest = 0, lastTime = 0, num = 0;
		dbHelper = new DBHelper(nodeString, tableName);
		db db = bind(dbHelper);
		column = arrayColum.size();
		if (arrayColum != null && column != 0) {
			for (int i = 0; i < column; i++) {
				tempObj = (JSONObject) arrayColum.get(i);
				columnId = ((JSONObject) tempObj.get("_id")).getString("$oid");
				tempTime = tempObj.getString("lastTime");
				tempnum = tempObj.getString("editCount");
				if (tempTime.contains("$numberLong")) {
					tempTime = JSONObject.toJSON(tempTime).getString("$numberLong");
				}
				if (tempnum.contains("$numberLong")) {
					tempnum = JSONObject.toJSON(tempnum).getString("$numberLong");
				}
				lastTime = Long.parseLong(tempTime);
				num = Long.parseLong(tempnum);
				count = db.gte("time", lastTime).lte("time", currentTime).eq("ogid", columnId).count();
				rest = num - count;
				tempObj.put("restArticle", rest < 0 ? 0 : rest);
				tempObj.put("id", columnId); // 栏目id
				tempObj.remove("lastTime");
				tempObj.remove("editCount");
				tempObj.remove("timediff");
				tempObj.remove("_id");
				arrayColum.set(i, tempObj);
			}
		}
		return (arrayColum != null && arrayColum.size() != 0) ? arrayColum : null;
	}

	/**
	 * 根据角色plv，获取角色级别
	 * 
	 * @project GrapeSuggest
	 * @package interfaceApplication
	 * @file Suggest.java
	 * 
	 * @return
	 *
	 */
	private int getRoleSign() {
		int roleSign = 0; // 游客
		if (sid != null) {
			try {
				privilige privil = new privilige(sid);
				int roleplv = privil.getRolePV(appid);
				// int roleplv = 0;
				if (roleplv >= 1000 && roleplv < 3000) {
					roleSign = 1; // 普通用户即企业员工
				}
				if (roleplv >= 3000 && roleplv < 5000) {
					roleSign = 2; // 栏目管理员
				}
				if (roleplv >= 5000 && roleplv < 8000) {
					roleSign = 3; // 企业管理员
				}
				if (roleplv >= 8000 && roleplv < 10000) {
					roleSign = 4; // 监督管理员
				}
				if (roleplv >= 10000 && roleplv < 12000) {
					roleSign = 5; // 总管理员
				}
				if (roleplv >= 12000 && roleplv < 14000) {
					roleSign = 6; // 总管理员，只读权限
				}
				if (roleplv >= 14000 && roleplv < 16000) {
					roleSign = 7; // 栏目编辑人员
				}
			} catch (Exception e) {
				nlogger.logout(e);
				roleSign = 0;
			}
		}
		return roleSign;
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONArray array) {
		if (array == null) {
			array = new JSONArray();
		}
		_obj.put("records", array);
		return resultMessage(0, _obj.toString());
	}

	private String resultMessage(int num) {
		return resultMessage(num, "");
	}

	private String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "手机号格式错误，请检查并修正";
			break;
		case 2:
			msg = "邮箱格式错误，请检查并修正";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}

	/**
	 * 获取栏目类型id为5的文章内容
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONArray getColumnByType(int idx, int pageSize) {
		String id, ogid = "", Column = "";
		JSONObject object, rs = new JSONObject();
		JSONArray array = null;
		// temp =
		// appsProxy.proxyCall("/GrapeContent/ContentGroup/FindByTypes/5/int:50",null,null).toString();
		// object = (JSONObject) JSONObject.toJSON(temp).get("message");
		// if (object != null && object.size() != 0) {
		// temp = object.getString("records");
		// }
		// JSONArray columns = JSONArray.toJSONArray(temp);
		JSONArray columns = getColumnByType();
		nlogger.logout("columns : " + columns);
		if (columns != null && columns.size() != 0) {
			int l = columns.size();
			if (l > 0) {
				for (int i = 0; i < l; i++) {
					object = (JSONObject) columns.get(i);
					Column = object.getString("name");
					object = (JSONObject) object.get("_id");
					id = object.getString("$oid");
					rs.put(id, Column);
					ogid = id + ",";
				}
			}
		}
		if (ogid.length() > 0) {
			ogid = StringHelper.fixString(ogid, ',');
		}
		dbHelper = new DBHelper("mongodb", "objectList");
		db db = bind(dbHelper);
		// 根据ogid,批量查询文章
		if (ogid != null && !ogid.equals("")) {
			String[] ids = ogid.split(",");
			db.or();
			for (String cid : ids) {
				db.eq("ogid", cid);
			}
			array = db.field("_id,ogid,content,mainName").page(idx, pageSize);
		}
		return getColumn(rs, array);
	}

	/**
	 * 获取栏目名称
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param rs
	 * @param array
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONArray getColumn(JSONObject rs, JSONArray array) {
		JSONObject object;
		String ColumnName = "", ogid;
		// 获取栏目名称
		if (rs != null && rs.size() != 0 && array != null && array.size() != 0) {
			int l = array.size();
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				ogid = object.getString("ogid");
				ColumnName = (ogid != null && !ogid.equals("")) ? rs.getString(ogid) : "";
				object.put("ColumnName", ColumnName);
				array.set(i, object);
			}
		}
		return array;
	}

	private JSONArray getColumnByType() {
		String wbid = (userInfo != null && userInfo.size() != 0) ? userInfo.getString("currentWeb") : "";
		DBHelper dbHelper = new DBHelper("mongodb", "objectGroup");
		db db = bind(dbHelper);
		if (wbid != null && !wbid.equals("")) {
			db.eq("wbid", wbid);
		}
		db.eq("contentType", "5").field("_id,name").limit(50);
		nlogger.logout("condString: " + db.condString());
		JSONArray array = db.select();
		db.clear();
		return array;
	}
}
