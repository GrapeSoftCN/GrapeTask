package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import authority.privilige;
import database.DBHelper;
import database.db;
import httpClient.request;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;
import string.StringHelper;
import time.TimeHelper;

public class task {
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
			userInfo = se.getSession(sid);
		}
	}

	private db bind(DBHelper tasks) {
		return tasks.bind(appid);
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
		int count = 0;
		JSONObject object,objId, obj = new JSONObject();
		String content,id;
		JSONArray array = getColumnByType(idx,pageSize);
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
		if (temp != null && !temp.equals("") && tempTimediff != null && !tempTimediff.equals("") && editTemp != null
				&& !editTemp.equals("")) {
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
				tempObj.put("id", columnId); //栏目id
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

	private String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
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
	private JSONArray getColumnByType(int idx,int pageSize) {
		String temp, id, ogid = "";
		JSONObject object;
		JSONArray array = null;
		temp = appsProxy.proxyCall("/GrapeContent/ContentGroup/FindByType/5/int:50").toString();
		object = (JSONObject) JSONObject.toJSON(temp).get("message");
		if (object != null && object.size() != 0) {
			temp = object.getString("records");
		}
		JSONArray columns = JSONArray.toJSONArray(temp);
		int l = columns.size();
		if (l > 0) {
			for (int i = 0; i < l; i++) {
				object = (JSONObject) columns.get(i);
				object = (JSONObject) object.get("_id");
				id = object.getString("$oid");
				ogid = id + ",";
			}
		}
		if (ogid.length() > 0) {
			ogid = StringHelper.fixString(ogid, ',');
		}
		dbHelper = new DBHelper("mongodb", "objectList");
		db db = bind(dbHelper);
		// 根据ogid,批量查询文章
		if (ogid != null) {
			String[] ids = ogid.split(",");
			db.or();
			for (String cid : ids) {
				db.eq("ogid", cid);
			}
			array = db.field("_id,ogid,content,mainName").page(idx, pageSize);
		}
		return array;
	}
}
