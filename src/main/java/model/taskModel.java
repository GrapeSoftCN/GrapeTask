package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import check.formHelper;
import check.formHelper.formdef;
import database.DBHelper;
import database.db;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;
import time.TimeHelper;

public class taskModel {
	private DBHelper dbtask;
	private formHelper form;
	private JSONObject _obj = new JSONObject();
	private JSONObject UserInfo = null;
	private session session = new session();

	private db bind() {
		return dbtask.bind(String.valueOf(appsProxy.appid()));
	}

	public taskModel() {
		dbtask = new DBHelper(appsProxy.configValue().get("db").toString(), "task");
		form = dbtask.getChecker();
		form.putRule("name", formdef.notNull);
		form.putRule("timediff", formdef.notNull);
		String sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			UserInfo = session.getSession(sid);
		}
	}

	public String Add(JSONObject Info) {
		String tips = "";
		if (Info != null) {
			if (!form.checkRuleEx(Info)) {
				return resultMessage(1, "必填项为空");
			}
			tips = bind().data(Info).insertOnce().toString();
		}
		if (("").equals(tips)) {
			return resultMessage(99);
		}
		JSONObject object2 = find(tips);
		return resultMessage(object2);
	}

	public int update(String id, JSONObject Info) {
		int code = 99;
		if (Info != null) {
			try {
				JSONObject object = bind().eq("_id", new ObjectId(id)).data(Info).update();
				code = (object != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int delete(String id) {
		int code = 99;
		try {
			JSONObject object = bind().eq("_id", new ObjectId(id)).delete();
			code = (object != null ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int delete(String[] ids) {
		int code = 99;
		try {
			bind().or();
			for (int i = 0, len = ids.length; i < len; i++) {
				bind().eq("_id", new ObjectId(ids[i]));
			}
			long codes = bind().deleteAll();
			code = (Integer.parseInt(String.valueOf(codes)) == ids.length ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public String find(JSONObject info) {
		JSONArray array = null;
		if (info != null) {
			try {
				array = new JSONArray();
				for (Object object2 : info.keySet()) {
					bind().like(object2.toString(), info.get(object2.toString()));
				}
				array = bind().limit(30).select();
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return resultMessage(array);
	}

	public JSONObject find(String taskid) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("_id", new ObjectId(taskid)).find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return object != null ? object : null;
	}

	@SuppressWarnings("unchecked")
	public String page(int ids, int pageSize) {
		JSONObject object = null;
		try {
			JSONArray array = bind().page(ids, pageSize);
			object = new JSONObject();
			object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
			object.put("currentPage", ids);
			object.put("pageSize", pageSize);
			object.put("data", array);
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String page(int ids, int pageSize, JSONObject info) {
		JSONObject object = null;
		if (info != null) {
			try {
				for (Object object2 : info.keySet()) {
					if ("_id".equals(object2.toString())) {
						bind().eq("_id", new ObjectId(info.get("_id").toString()));
					}
					bind().eq(object2.toString(), info.get(object2.toString()));
				}
				JSONArray array = bind().dirty().page(ids, pageSize);
				object = new JSONObject();
				object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
				object.put("currentPage", ids);
				object.put("pageSize", pageSize);
				object.put("data", array);
			} catch (Exception e) {
				object = null;
			}finally {
				bind().clear();
			}
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String notice() {
		if (UserInfo == null) {
			return null;
		}
		// 获取当前登录用户信息
		String ownid = UserInfo.getString("ownid");
		JSONArray array = bind().eq("ownid", ownid).limit(20).select();
		JSONArray arrays = new JSONArray();
		String currentTime = String.valueOf(TimeHelper.nowMillis());
		for (int i = 0, len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			String lasttime = object.get("lasttime").toString();
			int diff = (int) Math
					.ceil((double) (Long.parseLong(currentTime) - Long.parseLong(lasttime)) / (1000 * 60 * 60 * 24));
			int timediff = Integer.parseInt(object.get("timediff").toString());
			if (diff <= timediff) {
				continue;
			}
			arrays.add(object);
		}
		return resultMessage(arrays);
	}

	/**
	 * 将map添加至JSONObject中
	 * 
	 * @param map
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject AddMap(HashMap<String, Object> map, JSONObject object) {
		if (object != null) {
			if (map.entrySet() != null) {
				Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
					if (!object.containsKey(entry.getKey())) {
						object.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		return object;
	}

	private String resultMessage(int num) {
		return resultMessage(num, "");
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

	public String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "必填项没有填";
			break;
		case 2:
			msg = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 3:
			msg = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 4:
			msg = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		default:
			msg = "其它异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
